---
title: 'Custom Spring bean (EventServer)'
---

When you need to reach the lsFusion system from your own Java code — to run a background job, host an integration server, process a stream of events, expose an RMI service — that is done with a Spring bean extending the `EventServer` hierarchy.

This is the second of the two ways of [accessing lsFusion from an internal system](Access_from_an_internal_system.md), parallel to the [`InternalAction`](Internal_call_INTERNAL.md#java) path.

### Base class

The bean extends one of two classes:

- `MonitorServer` — for most components: schedulers, socket listeners, message-queue consumers, periodic jobs.
- `RmiServer` — when the bean must be reachable by external clients as an RMI service.

The minimum Java overrides are `getEventName()`, `getLogicsInstance()`, and the relevant lifecycle hooks.

`getEventName()` must return a constant (typically a string literal). It is called from the field initialiser of `EventServer` (`new TopExecutionStack(getEventName())`) before the subclass's own field initialisers and constructor body run, so it cannot rely on DI-injected values or subclass fields — at that moment they have not been assigned yet.

### Threads and stack

Each base class establishes its own thread context: `MonitorServer` — *monitor*, `RmiServer` — *RMI*. How to obtain an `ExecutionStack` depends on the kind of thread the code is running on:

1. **Own task pool.** Built through `ExecutorFactory.createMonitorThreadService(threads, this)` or `createMonitorScheduledThreadService(threads, this)` for a `MonitorServer` bean; `createRMIThreadService(threads, this)` for a `RmiServer`. Threads in such a pool enter and leave the appropriate thread context automatically; `getStack()` works inside the tasks.

2. **External callback thread** (worker threads of a RabbitMQ client, WebSocket library, etc. — threads not created through `ExecutorFactory`). The context is set up and torn down around the block manually; the code usually sits inside an anonymous class (`DefaultConsumer`, etc.), so the `MonitorServer` is referred to explicitly as `MyServer.this` (a bare `this` would point at the anonymous instance):

    ```java
    try {
        ThreadLocalContext.aspectBeforeMonitorHTTP(MyServer.this);
        try (DataSession session = createSession()) {
            // getStack() works here
            ...
        }
    } finally {
        ThreadLocalContext.aspectAfterMonitorHTTP(MyServer.this);
    }
    ```

    Inside the wrapped block `getStack()` is valid.

3. **Lifecycle method or one-off call** (the thread is in neither the monitor nor the RMI context, and there is no need to wrap it). Use `getTopStack()` — the top-level execution stack tied to the bean itself.

### RMI export

A complete RMI-service template is a `RmiServer` bean that implements a remote interface, plus a manual export through `RmiManager`.

**Remote interface** extends `lsfusion.interop.server.RmiServerInterface` (rather than the bare `java.rmi.Remote`); every remote method declares `throws RemoteException`:

```java
public interface MyRemoteInterface extends RmiServerInterface {
    String doSomething(String arg) throws RemoteException;
}
```

**The `RmiServer` bean** implements this interface. The subclass itself **is not exported over RMI automatically** — the export is done by hand through `RmiManager`:

- in `onStarted`: `getLogicsInstance().getRmiManager().bindAndExport(name, this)` — exports the object and registers it in the platform's RMI registry under the name `<exportName>/<name>`, where `exportName` is set in the platform's `rmiManager` configuration and `name` is a short service identifier (`"EquipmentServer"`, etc.).
- in `onStopping`: `getLogicsInstance().getRmiManager().unbindAndUnexport(name, this)` — unbinds and unexports.

If `RmiManager` is used often, a `getRmiManager()` helper method is usually added on the bean itself (see `EquipmentServer.getRmiManager()` in the ERP).

The RMI context inside a remote method is normally set up for you — just call `getStack()` and work with the session. An explicit `ThreadLocalContext.assureRmi(this)` may be added as a defensive call in non-standard paths, but no manual aspect setup is required in the typical case.

`createSession()` throws `SQLException`, and the `LP` / `LA` / `applyException` calls throw `SQLException` and `SQLHandledException`; in a remote method these are usually caught in one block and wrapped in `RemoteException`:

```java
@Override
public String doSomething(String arg) throws RemoteException {
    BusinessLogics BL = getLogicsInstance().getBusinessLogics();
    try (DataSession session = createSession()) {
        String result = "..."; // read/write properties, execute actions — getStack() is valid
        session.applyException(BL, getStack()); // required if anything was written
        return result;
    } catch (SQLException | SQLHandledException e) {
        throw new RemoteException("doSomething failed", e);
    }
}
```

The RMI client locates the service in the registry under the same `<exportName>/<name>`.

### Lifecycle

The bean is injected via Spring (the minimum is `logicsInstance` (`LogicsInstance`); through it the bean reaches `getBusinessLogics()`, `getDbManager()`, `getRmiManager()`). The standard hooks are used as follows:

- `afterPropertiesSet()` (when the bean implements `InitializingBean`) — fires right after Spring DI; only the injection check goes here (`Assert.notNull(...)`). The platform runtime is not ready yet, sessions cannot be opened.
- `onInit(LifecycleEvent)` — platform initialisation has started. This is where the [module](Modules.md) is resolved (`getLogicsInstance().getBusinessLogics().getModule("MyModule")`) and `LP` / `LA` wrappers are stored in fields via `LM.findProperty(...)` / `LM.findAction(...)`.
- `onStarted(LifecycleEvent)` — the platform is fully up. This is where background threads and listeners are started; `RmiServer`-style beans call `bindAndExport` here.
- `onStopping(LifecycleEvent)` — graceful shutdown: stop owned threads; for `RmiServer` call `unbindAndUnexport`.

If the component must come up after the main platform, pass `super(DAEMON_ORDER)` in the constructor.

### Reading, writing and executing

Inside the bean's methods, properties and actions are accessed through a `DataSession` and an `ExecutionStack` (rather than an `ExecutionContext`, as in `InternalAction`). Object arguments are passed as `DataObject` (or, more generally, `ObjectValue` — either `DataObject` or `NullValue`); the written property values are plain Java values of built-in classes (`String`, `Integer`, `LocalDateTime`, etc.). A full catalog of classes and methods is in [Java API for integrations](Java_integration_API.md). `LP` / `LA` wrappers are normally resolved once in `onInit` and stored in fields; here, for brevity, they are obtained at the call site:

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

`createSession()` opens a new change session; whatever is written into it accumulates until applied explicitly — `session.applyException(BL, stack)` (throws on apply failure) or `session.applyMessage(BL, stack)` (returns the error message, or `null` on success). If an exception is thrown before apply, try-with-resources rolls back everything that was not yet applied.

### Wiring up in Spring

To plug the bean in, the project module places its `lsfusion-bootstrap.xml` into `src/main/resources` (it shadows the platform file with the same name). It imports `lsfusion.xml`, declares the bean, and registers it in `customLifecycleListeners`:

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

`<util:list id="customLifecycleListeners">` overrides the same empty list from the platform's `lsfusion.xml` and lists the beans that should receive `onInit` / `onStarted` / `onStopping`.

:::info
If the bean must also be reachable from arbitrary code via `getLogicsInstance().getCustomObject(MyClass.class)`, list it additionally under `<util:list id="customObjects">` — the platform's service registry (a `Class → Object` map). In practice such beans are commonly registered in both lists at once.
:::

### Example

`DataSyncServer` is a periodic job that polls an external system once a minute and updates statistics in lsFusion properties. It resolves properties in `onInit`, starts an executor in `onStarted`, shuts the executor down in `onStopping`, and applies changes in `DataSession`.

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
            // (any uncaught exception — including runtime ones from fetchExternalCount —
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

`ExecutorFactory.createMonitorScheduledThreadService(threads, this)` brings up threads in the monitor context, so `getStack()` works inside the tasks. Exceptions inside the task must not propagate — `scheduleAtFixedRate` would suppress further runs, so errors are logged.
