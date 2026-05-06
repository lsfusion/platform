---
title: 'Java API для интеграций'
---

Каталог Java-классов и методов, используемых при обращении к lsFusion-системе из Java-кода — как изнутри подкласса `InternalAction` (см. [внутренний вызов (`INTERNAL`)](Internal_call_INTERNAL.md)), так и изнутри Spring bean (см. [свой Spring bean (`EventServer`)](Custom_Spring_bean_EventServer.md)).

### Полные имена ключевых классов

JDK- и инфраструктурные framework-типы (`java.rmi.RemoteException`, `java.sql.SQLException`, `org.springframework.beans.factory.InitializingBean` и т.п.) не перечислены.

```
lsfusion.server.logics.LogicsInstance
lsfusion.server.logics.BusinessLogics
lsfusion.server.language.ScriptingLogicsModule
lsfusion.server.language.ScriptingErrorLog
lsfusion.server.language.property.LP
lsfusion.server.language.action.LA
lsfusion.server.data.value.ObjectValue
lsfusion.server.data.value.DataObject
lsfusion.server.data.value.NullValue
lsfusion.server.logics.action.session.DataSession
lsfusion.server.logics.action.controller.context.ExecutionContext
lsfusion.server.logics.action.controller.stack.ExecutionStack
lsfusion.server.logics.classes.ValueClass
lsfusion.server.logics.classes.ConcreteClass
lsfusion.server.logics.classes.data.time.DateClass
lsfusion.server.logics.classes.user.CustomClass
lsfusion.server.logics.classes.user.ConcreteCustomClass
lsfusion.server.logics.property.classes.ClassPropertyInterface
lsfusion.server.logics.form.struct.group.Group
lsfusion.server.logics.form.struct.FormEntity
lsfusion.server.physics.dev.integration.internal.to.InternalAction
lsfusion.server.base.controller.lifecycle.LifecycleAdapter
lsfusion.server.base.controller.lifecycle.LifecycleEvent
lsfusion.server.base.controller.lifecycle.LifecycleListener
lsfusion.server.base.controller.manager.EventServer
lsfusion.server.base.controller.manager.MonitorServer
lsfusion.server.base.controller.remote.manager.RmiServer
lsfusion.server.base.controller.remote.RmiManager
lsfusion.server.base.controller.thread.ExecutorFactory
lsfusion.server.base.controller.thread.ThreadLocalContext
lsfusion.server.physics.exec.db.controller.manager.DBManager
lsfusion.interop.server.RmiServerInterface
```

`ScriptingErrorLog.SemanticErrorException` — внутренний класс `ScriptingErrorLog`; бросается резолвящими методами (`findProperty` / `findAction` / `findClass` / `findGroup` / `findForm`), поэтому конструкторы подклассов `InternalAction`, которые их вызывают, обычно объявляют `throws ScriptingErrorLog.SemanticErrorException`. Базовый `InternalAction(LM, classes...)` checked-исключений не объявляет. В lifecycle-методах bean-ов (которые тоже checked-исключений не объявляют) `SemanticErrorException` обычно ловится и заворачивается в `RuntimeException`.

### Корневые объекты

`LogicsInstance` — корневой объект платформы, инжектируется в Spring bean через DI (или достаётся через `context.getLogicsInstance()` внутри `InternalAction`).
- `getBusinessLogics()` → `BusinessLogics`
- `getDbManager()` → `DBManager`
- `getRmiManager()` → `RmiManager`
- `getCustomObject(Class<T>)` → bean из `<util:list id="customObjects">`

`BusinessLogics` (обычно `BL`) — корень бизнес-логики.
- `findProperty(canonicalName)` → `LP<?>`
- `findAction(canonicalName)` → `LA<?>`
- `findClass(canonicalName)` → `CustomClass`
- `getModule(name)` → `ScriptingLogicsModule` для модуля по имени

`ScriptingLogicsModule` (обычно `LM`) — конкретный [модуль](Modules.md).
- `findProperty(localName)` → `LP<?>` / `findAction(localName)` → `LA<?>` — резолвинг в рамках модуля
- `findClass(localName)` → `ValueClass` (более общий тип, чем у `BusinessLogics.findClass`; включает встроенные классы — для пользовательских требуется приведение к `ConcreteCustomClass`).
- `findGroup(localName)` → `Group` / `findForm(localName)` → `FormEntity`

### Свойства и действия

`LP<?>` — Java-обёртка над [свойством](Properties.md).
- `read(session, ObjectValue... params)` → `Object` — текущее значение (Java-значение для скалярных свойств; `Long`/идентификатор объекта для свойств объектного класса)
- `read(context, ObjectValue... params)` → `Object`
- `readClasses(session, ObjectValue... params)` → `ObjectValue` — для свойств объектного класса возвращает `DataObject` с правильным конкретным классом (или `NullValue`); удобно, когда значение нужно сразу передать в `LP.change` / `LA.execute` без ручного восстановления класса
- `readClasses(context, ObjectValue... params)` → `ObjectValue`
- `change(value, session, DataObject... params)` — запись Java-значения в сессию
- `change(value, context, DataObject... params)`
- `change(ObjectValue value, session, DataObject... params)` — запись объектного значения (полезно при копировании значения, прочитанного через `readClasses`)
- `change(ObjectValue value, context, DataObject... params)`

`LA<?>` — Java-обёртка над [действием](Actions.md).
- `execute(session, stack, ObjectValue... params)` — запуск с собственными session+stack
- `execute(context, ObjectValue... params)` — запуск из `InternalAction`

### Параметры-объекты

`ObjectValue` — общий базовый тип для значения объектного класса; либо `DataObject` (не-`NULL`), либо `NullValue` (`NullValue.instance`).

`DataObject(Object value, ConcreteClass cls)` — конструктор не-`NULL`-параметра с явным указанием класса (нужен, например, для дат и пользовательских классов). Ни `BusinessLogics.findClass(name)` (возвращает `CustomClass`), ни `LM.findClass(name)` (возвращает `ValueClass`) не дают `ConcreteClass` напрямую — для пользовательского класса требуется приведение к `ConcreteCustomClass`: `new DataObject(userId, (ConcreteCustomClass) BL.findClass("CustomUser"))`. Для встроенных классов используется их `instance`: `new DataObject(LocalDate.of(...), DateClass.instance)`. Для нескольких встроенных скалярных типов есть convenience-перегрузки без второго аргумента: `String`, `Integer`, `Long`, `Boolean`, `Double`.

### Сессия изменений

`DataSession` — [сессия изменений](Change_sessions.md), накапливает изменения до явного применения. Открывается через `EventServer.createSession()` или `dbManager.createSession()`. Реализует `AutoCloseable` — корректно использовать в `try`-with-resources, неприменённая сессия откатывается.
- `applyException(BL, stack)` — применить; бросить исключение при ошибке
- `applyMessage(BL, stack)` → `String` — применить; вернуть текст ошибки или `null`

### `InternalAction`

`InternalAction extends ExplicitAction` (`lsfusion.server.physics.dev.integration.internal.to.InternalAction`) — базовый класс Java-цели `INTERNAL`. Конструктор: `(ScriptingLogicsModule LM, ValueClass... classes)`.

- `executeInternal(ExecutionContext<ClassPropertyInterfaCustomce> context)` — единственная override-точка, вызывается на каждом запуске.
- `findProperty(name)` / `findAction(name)` / `findClass(name)` / `findGroup(name)` / `findForm(name)` — резолвинг через `LM`.
- `getParam(int i, context)` → `Object` — позиционный доступ к значению параметра.
- `getParamValue(int i, context)` → `ObjectValue` — позиционный `ObjectValue`-доступ.
- `getParamInterface(int i)` → `ClassPropertyInterface` — позиционный интерфейс параметра.
- `allowNulls()` — переопределить и вернуть `true`, чтобы действие принимало `NULL`-аргументы (по умолчанию `false`).
- Поле `interfaces` — все `ClassPropertyInterface` действия (в порядке объявления).

### `ExecutionContext`

`ExecutionContext<P>` — per-call контекст внутри `InternalAction.executeInternal`.
- `getKeyObject(P key)` → `Object` — исходное значение, может быть `null`
- `getKeyValue(P key)` → `ObjectValue` — `DataObject` или `NullValue`
- `getDataKeyValue(P key)` → `DataObject` — гарантированно не-`NULL` (для действий с `!allowNulls`)
- `getBL()` → `BusinessLogics`
- `getSession()` → текущая `DataSession`
- поле `stack` → текущий `ExecutionStack` (`context.stack`, публичное поле)
- `messageSuccess(message, header)` / `messageError(message)` / `messageError(message, header)` — пользовательские сообщения
- `delayUserInteraction(ClientAction)` — отложенное клиентское действие (выполнится после завершения текущего стека)
- `requestUserInteraction(ClientAction)` → `Object` — синхронное клиентское действие с ожиданием результата (например, диалог подтверждения)

### Иерархия `EventServer`

`LifecycleAdapter implements LifecycleListener` — базовый класс с lifecycle-хуками:
- `onInit(LifecycleEvent)`, `onStarted(LifecycleEvent)`, `onStopping(LifecycleEvent)`, `onStopped(LifecycleEvent)`, `onError(LifecycleEvent)` — переопределяемые
- `getOrder()` — порядок lifecycle-обхода. Стандартные значения определены константами на `LifecycleListener`: `LOGICS_ORDER` (100), `DBMANAGER_ORDER` (300), `SECURITYMANAGER_ORDER` (400), `RMIMANAGER_ORDER` (500), `BLLOADER_ORDER` (600), `DAEMON_ORDER` (8000), `REFLECTION_ORDER` (9000).

`EventServer extends LifecycleAdapter` (abstract) — основа для Spring bean-ов.
- `getEventName()` (abstract) — имя для логирования; **должен возвращать константу** (вызывается из field-initializer-а до DI и конструктора подкласса).
- `getLogicsInstance()` (abstract) — `LogicsInstance` для bean-а.
- `createSession()` → `DataSession` через `getLogicsInstance().getDbManager()`.
- `getTopStack()` → top-level `NewThreadExecutionStack` (один экземпляр на bean).

`MonitorServer extends EventServer` — `getStack()` через `ThreadLocalContext.assureMonitor(this)`. Для большинства Spring bean-компонентов.

`RmiServer extends EventServer` — `getStack()` через `ThreadLocalContext.assureRmi(this)`. Для bean-ов, экспортируемых через RMI; remote-интерфейс должен наследоваться от `lsfusion.interop.server.RmiServerInterface`.

### Потоки

`ExecutorFactory` — фабрики потоковых пулов в нужном thread-контексте; задачи в этих пулах могут вызывать `getStack()`.
- `createMonitorThreadService(Integer threads, MonitorServer monitor)` → `ExecutorService`
- `createMonitorScheduledThreadService(Integer threads, MonitorServer monitor)` → `ScheduledExecutorService`
- `createRMIThreadService(Integer threads, RmiServer rmi)` → `ExecutorService`

`ThreadLocalContext` — ручная установка thread-контекста для callback-потоков, не созданных через `ExecutorFactory`:
- `aspectBeforeMonitorHTTP(MonitorServer)` / `aspectAfterMonitorHTTP(MonitorServer)` — обрамить блок monitor-контекстом (`MyServer.this` внутри anonymous class).
- `assureRmi(RmiServer)` — защитный вызов в remote-методах (обычно избыточен, RMI-аспект устанавливает контекст автоматически).

### Правильная работа с потоками

Каждый поток, который собирается обращаться к lsFusion-системе через `getStack()`, `LP.read`/`LP.change`, `LA.execute`, `createSession()`, должен находиться в нужном *thread-контексте* — `ThreadLocal`-маркере, который ставит платформа: *monitor* для `MonitorServer`-bean-ов, *RMI* для `RmiServer`, *lifecycle* для платформенных init-фаз. Без него `getStack()` упадёт на assertion-е, а сессия может оказаться в состоянии без environment-а.

**Где контекст ставится автоматически:**

1. RMI-входящие вызовы — оборачиваются `RemoteContextAspect`-ом (Spring AOP).
2. Задачи в потоках из `ExecutorFactory.createMonitorThreadService(...)` / `createMonitorScheduledThreadService(...)` / `createRMIThreadService(...)` — каждый поток таких пулов входит и выходит из контекста через `aspectBefore...` / `aspectAfter...` без участия пользователя.
3. lifecycle-методы `EventServer` (`onInit`, `onStarted`, `onStopping`) — выполняются в *lifecycle*-контексте; здесь работает `getTopStack()`, но не `getStack()`.
4. Внутри `InternalAction.executeInternal` — контекст уже установлен (вызов через action-flow платформы).

**Когда контекст ставится вручную** — на callback-потоках внешних библиотек (RabbitMQ-клиент, MINA, WebSocket-провайдер и т.п.), в потоках от `Executors.newFixedThreadPool` (не от `ExecutorFactory`), в ручных `new Thread(...)`. Парность обязательна, всегда `try`/`finally`:

```java
try {
    ThreadLocalContext.aspectBeforeMonitorHTTP(MyServer.this);
    try (DataSession session = createSession()) {
        // здесь getStack() валиден, можно работать с LP / LA
    }
} finally {
    ThreadLocalContext.aspectAfterMonitorHTTP(MyServer.this);
}
```

Пропустить `aspectAfter...` нельзя: ThreadLocal останется «грязным», и следующая задача на этом потоке (особенно в shared-pool-е) попадёт в чужой контекст.

**Передача работы на другой поток.** `ExecutionContext` привязан к породившему его потоку (его `stack`, `getSession()`, активные aspect-ы) — нельзя просто передать `context` в `executor.submit(...)` и продолжить там работу. Корректные паттерны:

- В lsFusion-коде — оператор `NEWTHREAD action` / `NEWTHREAD action CONNECTION conn` / `NEWTHREAD action SCHEDULE PERIOD ms` (см. `NewThreadAction`). Платформа сама создаёт notification, доставляет на нужный navigator или scheduler-pool, на новом потоке через `context.override(env, stack, asyncResult)` строит свежий `ExecutionContext`.
- В Java-коде — `context.override(stack)` / `context.override(env, stack, ...)` для смены stack-а в рамках того же потока, и `RemoteNavigator.pushNotification(Notification)` для доставки работы на конкретный navigator на его собственном thread-context-е.

**Антипаттерны:**

- **`new Thread(() -> lp.read(session, ...)).start()`** — поток не в monitor-контексте, `getStack()` упадёт.
- **`Executors.newFixedThreadPool(N)` или `newScheduledThreadPool(N)` без `ExecutorFactory`** — то же самое, aspect-ов нет.
- **`aspectBefore...` без парного `aspectAfter...`** — следующая задача на этом потоке унаследует чужой ThreadLocal.
- **Передача `ExecutionContext` в фоновый поток как есть** — после возврата из родительского action-а его сессия может быть закрыта, продолжать работу с ним нельзя.
- **`getStack()` внутри `onInit` / `onStarted` / `onStopping`** — там lifecycle-контекст, а не monitor; нужен `getTopStack()`.
- **`DataSession` shared между потоками** — `DataSession` не thread-safe; всегда открывайте и закрывайте в одном потоке (try-with-resources).
- **Работа с `RemoteForm` / клиентским контекстом из произвольного потока без `pushNotification`** — клиентские структуры не thread-safe, доставка должна идти через notification-канал.

**Несколько одновременных потоков в одном bean-е** — фиксированный пул с monitor-контекстом:

```java
@Override
protected void onStarted(LifecycleEvent event) {
    workers = ExecutorFactory.createMonitorThreadService(N, this);
    // submit задач...
}

private void handleMessage(byte[] body) {
    workers.submit(() -> {
        try (DataSession session = createSession()) {
            // getStack() здесь валиден — поток в monitor-контексте
            ...
        } catch (Exception e) {
            logger.error("worker failed", e); // не пробрасывать в shared executor
        }
    });
}
```

Каждая задача — своя `DataSession` (не разделять между задачами). Исключения внутри submit-нутой задачи всегда ловить и логировать; в `scheduleAtFixedRate` пропущенное исключение **подавит дальнейшие запуски**, в обычном `submit` оно потеряется молча.

### RMI-экспорт

`RmiManager` (получается через `getLogicsInstance().getRmiManager()`):
- `bindAndExport(String name, Remote remote)` — экспорт + регистрация в RMI-registry под именем `<exportName>/<name>`
- `unbindAndUnexport(String name, Remote remote)` — обратная операция
- `export(Remote)` / `unexport(Remote)` / `bind(name, Remote)` / `unbind(name)` — низкоуровневые

Remote-интерфейс наследуется от `lsfusion.interop.server.RmiServerInterface`; remote-методы объявляют `throws RemoteException`.
