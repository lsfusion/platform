---
title: 'Свой Spring bean (EventServer)'
---

Когда из своего Java-кода нужно обращаться к lsFusion-системе — запустить фоновую задачу, поднять интеграционный сервер, обработать поток событий, выставить наружу RMI-сервис — это делается через Spring bean, наследующийся от иерархии `EventServer`.

Это второй из двух способов [обращения из внутренней системы](Access_from_an_internal_system.md), параллельный пути через [`InternalAction`](Internal_call_INTERNAL.md#java).

### Базовый класс

Bean наследуется от одного из двух классов:

- `MonitorServer` — для большинства компонентов: планировщиков, socket-listener-ов, message-queue-консьюмеров, периодических задач.
- `RmiServer` — если bean должен быть доступен внешним клиентам как RMI-сервис.

Минимальные Java-переопределения: `getEventName()`, `getLogicsInstance()` и нужные lifecycle-хуки.

`getEventName()` должен возвращать константу (как правило, строковый литерал). Он вызывается из field-initializer-а `EventServer` (`new TopExecutionStack(getEventName())`) ещё до того, как отрабатывают field-инициализаторы и конструктор подкласса, поэтому опираться на DI-инжектированные значения или поля подкласса нельзя — на момент вызова они ещё не присвоены.

### Потоки и stack

Каждый базовый класс задаёт свой thread-контекст: `MonitorServer` — *monitor*, `RmiServer` — *RMI*. От того, на каком потоке выполняется код, зависит, как получать `ExecutionStack`:

1. **Свой пул задач**. Создаётся через `ExecutorFactory.createMonitorThreadService(threads, this)` или `createMonitorScheduledThreadService(threads, this)` для `MonitorServer`-bean-а; `createRMIThreadService(threads, this)` — для `RmiServer`. Каждый поток в таком пуле автоматически входит и выходит из нужного thread-контекста; внутри задач работает `getStack()`.

2. **Внешний callback-поток** (рабочие потоки RabbitMQ-клиента, WebSocket-библиотеки, и т.п. — не созданные через `ExecutorFactory`). Контекст проставляется и снимается вокруг блока вручную; код обычно находится внутри anonymous class (`DefaultConsumer` и т.п.), поэтому в качестве `MonitorServer`-а явно передаётся `MyServer.this` (а не просто `this`, который сослался бы на anonymous instance):

    ```java
    try {
        ThreadLocalContext.aspectBeforeMonitorHTTP(MyServer.this);
        try (DataSession session = createSession()) {
            // getStack() работает здесь
            ...
        }
    } finally {
        ThreadLocalContext.aspectAfterMonitorHTTP(MyServer.this);
    }
    ```

    Внутри обрамления `getStack()` валиден.

3. **Lifecycle-метод или одноразовый вызов** (поток не в monitor- и не в RMI-контексте, оборачивать его не нужно). Используется `getTopStack()` — top-level execution stack, привязанный к самому bean-у.

### Экспорт по RMI

Полный шаблон RMI-сервиса — это `RmiServer`-bean, реализующий remote-интерфейс, плюс ручной экспорт через `RmiManager`.

**Remote-интерфейс** наследуется от `lsfusion.interop.server.RmiServerInterface` (а не от голого `java.rmi.Remote`); каждый remote-метод объявляет `throws RemoteException`:

```java
public interface MyRemoteInterface extends RmiServerInterface {
    String doSomething(String arg) throws RemoteException;
}
```

**`RmiServer`-bean** реализует этот интерфейс. Сам подкласс **не экспортируется по RMI автоматически** — экспорт делается вручную через `RmiManager`:

- в `onStarted`: `getLogicsInstance().getRmiManager().bindAndExport(name, this)` — экспортирует объект и регистрирует его в RMI-registry под именем `<exportName>/<name>`, где `exportName` задан в платформенной конфигурации `rmiManager`-а, а `name` — короткий идентификатор сервиса (`"EquipmentServer"` и т.п.).
- в `onStopping`: `getLogicsInstance().getRmiManager().unbindAndUnexport(name, this)` — снимает регистрацию и экспорт.

Если `RmiManager` используется часто, обычно добавляют helper-метод `getRmiManager()` на самом bean-е (см. `EquipmentServer.getRmiManager()` в ERP).

RMI-контекст внутри remote-метода обычно устанавливается автоматически — достаточно вызвать `getStack()` и работать с сессией. Явный `ThreadLocalContext.assureRmi(this)` можно добавить как защитный вызов в нестандартных путях, но обязательной ручной установки аспекта не требуется.

`createSession()` бросает `SQLException`, методы `LP` / `LA` / `applyException` — `SQLException` и `SQLHandledException`; в RMI-методе это обычно ловится одним блоком и заворачивается в `RemoteException`:

```java
@Override
public String doSomething(String arg) throws RemoteException {
    BusinessLogics BL = getLogicsInstance().getBusinessLogics();
    try (DataSession session = createSession()) {
        String result = "..."; // читать/писать свойства, выполнять действия — getStack() валиден
        session.applyException(BL, getStack()); // обязательно, если что-то писали
        return result;
    } catch (SQLException | SQLHandledException e) {
        throw new RemoteException("doSomething failed", e);
    }
}
```

RMI-клиент находит сервис в registry по тому же `<exportName>/<name>`.

### Жизненный цикл

Bean инжектируется через Spring (минимально достаточно `logicsInstance` (`LogicsInstance`); через него доступны `getBusinessLogics()`, `getDbManager()`, `getRmiManager()`). Стандартные хуки распределяются так:

- `afterPropertiesSet()` (когда bean реализует `InitializingBean`) — сразу после Spring DI; здесь только `Assert.notNull(...)` для проверки инжекции. Runtime платформы ещё не готов, открывать сессии нельзя.
- `onInit(LifecycleEvent)` — платформа начала инициализацию. Здесь резолвят [модуль](Modules.md) (`getLogicsInstance().getBusinessLogics().getModule("MyModule")`) и сохраняют `LP` / `LA`-обёртки в полях через `LM.findProperty(...)` / `LM.findAction(...)`.
- `onStarted(LifecycleEvent)` — платформа полностью поднята. Здесь стартуют фоновые потоки и listener-ы; для `RmiServer`-bean-ов делают `bindAndExport`.
- `onStopping(LifecycleEvent)` — корректно гасят свои потоки, для `RmiServer` делают `unbindAndUnexport`.

Если компонент должен подняться после основной платформы, в конструкторе передают `super(DAEMON_ORDER)`.

### Чтение, запись и выполнение

Внутри методов bean-а работа со свойствами и действиями идёт через `DataSession` и `ExecutionStack` (а не `ExecutionContext`, как у `InternalAction`). Аргументы-объекты передаются как `DataObject` (или более общий `ObjectValue` — `DataObject` или `NullValue`); записываемые значения свойств — обычные Java-значения встроенных классов (`String`, `Integer`, `LocalDateTime` и т.п.). Полный каталог классов и методов — в [Java API для интеграций](Java_integration_API.md). `LP` / `LA`-обёртки обычно резолвят один раз в `onInit` и хранят в полях; здесь для краткости они получаются прямо в месте вызова:

```java
BusinessLogics BL = getLogicsInstance().getBusinessLogics();
LP<?> someProperty = BL.findProperty("name[Class]");
LA<?> someAction = BL.findAction("name[Class]");
ExecutionStack stack = getStack();

DataObject keyObject = new DataObject(keyValue, (ConcreteCustomClass) BL.findClass("Class"));

try (DataSession session = createSession()) {
    Object value = someProperty.read(session, keyObject);
    someProperty.change(newJavaValue, session, keyObject);
    someAction.execute(session, stack, keyObject);
    session.applyException(BL, stack);
}
```

`createSession()` открывает новую сессию изменений; всё, что в неё записано, копится до явного применения — `session.applyException(BL, stack)` (бросает исключение при ошибке) или `session.applyMessage(BL, stack)` (возвращает текст ошибки или `null`). Если до применения вылетает исключение, `try`-with-resources откатит всё неприменённое.

### Подключение в Spring

Чтобы подключить свой bean, проектный модуль кладёт `lsfusion-bootstrap.xml` в `src/main/resources` (он перекрывает платформенный одноимённый файл). В нём импортируется `lsfusion.xml`, объявляется bean и регистрируется в `customLifecycleListeners`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:util="http://www.springframework.org/schema/util"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd"
       default-lazy-init="true">

    <import resource="classpath:lsfusion.xml"/>

    <bean id="dataSyncServer" class="myapp.controller.DataSyncServer">
        <property name="logicsInstance" ref="logicsInstance"/>
    </bean>

    <util:list id="customLifecycleListeners">
        <ref bean="dataSyncServer"/>
    </util:list>

</beans>
```

`<util:list id="customLifecycleListeners">` подменяет одноимённый пустой список из платформенного `lsfusion.xml` и перечисляет bean-ы, которые должны получать `onInit` / `onStarted` / `onStopping`.

:::info
Если bean дополнительно нужно делать доступным через `getLogicsInstance().getCustomObject(MyClass.class)` из произвольного места кода, его перечисляют ещё и в `<util:list id="customObjects">` — это сервисный реестр платформы (карта `Class → Object`). На практике такие bean-ы часто регистрируются сразу в обоих списках.
:::

### Пример

`DataSyncServer` — periodic-задача, которая раз в минуту опрашивает внешнюю систему и обновляет статистику в lsFusion-свойствах. Резолвит свойства в `onInit`, запускает executor в `onStarted`, гасит executor в `onStopping`, применяет изменения в `DataSession`.

```java
package myapp.controller;

import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataSyncServer extends MonitorServer implements InitializingBean {

    private static final Logger logger = ServerLoggers.startLogger;

    private LogicsInstance logicsInstance;
    private ScheduledExecutorService executor;

    private LP<?> lastSyncedAt;
    private LP<?> totalRecordCount;

    public DataSyncServer() {
        super(DAEMON_ORDER);
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) { this.logicsInstance = logicsInstance; }

    @Override public LogicsInstance getLogicsInstance() { return logicsInstance; }
    @Override public String getEventName() { return "data-sync"; }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(logicsInstance, "logicsInstance must be specified");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        ScriptingLogicsModule LM = getLogicsInstance().getBusinessLogics().getModule("DataSync");
        Assert.notNull(LM, "DataSync module not found");
        try {
            lastSyncedAt = LM.findProperty("lastSyncedAt[]");
            totalRecordCount = LM.findProperty("totalRecordCount[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        executor = ExecutorFactory.createMonitorScheduledThreadService(1, this);
        executor.scheduleAtFixedRate(this::syncOnce, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        if (executor != null) executor.shutdownNow();
    }

    private void syncOnce() {
        BusinessLogics BL = getLogicsInstance().getBusinessLogics();
        try (DataSession session = createSession()) {
            int recordCount = fetchExternalCount();
            lastSyncedAt.change(LocalDateTime.now(), session);
            totalRecordCount.change(recordCount, session);
            session.applyException(BL, getStack());
        } catch (Exception e) {
            // periodic task: log and let the next tick retry
            // (any uncaught exception, including runtime ones from fetchExternalCount,
            //  would suppress further runs of scheduleAtFixedRate)
            logger.error("data sync failed", e);
        }
    }

    private int fetchExternalCount() {
        // call external system
        return 0;
    }
}
```

`ExecutorFactory.createMonitorScheduledThreadService(threads, this)` поднимает потоки в monitor-контексте, поэтому внутри задач работает `getStack()`. Исключения внутри задачи не пропускаются наружу — `scheduleAtFixedRate` подавит дальнейшие запуски, поэтому ошибки логируются.
