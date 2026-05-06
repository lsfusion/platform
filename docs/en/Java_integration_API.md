---
title: 'Java API for integrations'
---

A catalog of Java classes and methods used to access the lsFusion system from Java code — both from inside an `InternalAction` subclass (see [internal call (`INTERNAL`)](Internal_call_INTERNAL.md)) and from inside a Spring bean (see [custom Spring bean (`EventServer`)](Custom_Spring_bean_EventServer.md)).

### Fully qualified names of the key classes

JDK and framework / infrastructure types (`java.rmi.RemoteException`, `java.sql.SQLException`, `org.springframework.beans.factory.InitializingBean`, etc.) are not listed.

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

`ScriptingErrorLog.SemanticErrorException` is an inner class of `ScriptingErrorLog`; thrown by the resolving methods (`findProperty` / `findAction` / `findClass` / `findGroup` / `findForm`), so subclass constructors of `InternalAction` that call them usually declare `throws ScriptingErrorLog.SemanticErrorException`. The base `InternalAction(LM, classes...)` itself declares no checked exceptions. In bean lifecycle methods (which also declare no checked exceptions) `SemanticErrorException` is commonly caught and wrapped in a `RuntimeException`.

### Root objects

`LogicsInstance` — root platform object, injected into a Spring bean via DI (or reached through `context.getLogicsInstance()` inside an `InternalAction`).
- `getBusinessLogics()` → `BusinessLogics`
- `getDbManager()` → `DBManager`
- `getRmiManager()` → `RmiManager`
- `getCustomObject(Class<T>)` → bean from `<util:list id="customObjects">`

`BusinessLogics` (commonly `BL`) — root of business logic.
- `findProperty(canonicalName)` → `LP<?>`
- `findAction(canonicalName)` → `LA<?>`
- `findClass(canonicalName)` → `CustomClass`
- `getModule(name)` → `ScriptingLogicsModule` for the module by name

`ScriptingLogicsModule` (commonly `LM`) — a specific [module](Modules.md).
- `findProperty(localName)` → `LP<?>` / `findAction(localName)` → `LA<?>` — module-scoped resolution
- `findClass(localName)` → `ValueClass` (a more general type than `BusinessLogics.findClass`'s; covers built-in classes — for user-defined classes a cast to `ConcreteCustomClass` is needed).
- `findGroup(localName)` → `Group` / `findForm(localName)` → `FormEntity`

### Properties and actions

`LP<?>` — Java wrapper over a [property](Properties.md).
- `read(session, ObjectValue... params)` → `Object` — current value (a Java value for scalar properties; a `Long` id for object-class properties)
- `read(context, ObjectValue... params)` → `Object`
- `readClasses(session, ObjectValue... params)` → `ObjectValue` — for object-class properties returns a `DataObject` with the concrete class set (or `NullValue`); convenient when the value has to be passed straight into `LP.change` / `LA.execute` without rebuilding the class manually
- `readClasses(context, ObjectValue... params)` → `ObjectValue`
- `change(value, session, DataObject... params)` — write a Java value into the session
- `change(value, context, DataObject... params)`
- `change(ObjectValue value, session, DataObject... params)` — write an object value (useful when copying a value read through `readClasses`)
- `change(ObjectValue value, context, DataObject... params)`

`LA<?>` — Java wrapper over an [action](Actions.md).
- `execute(session, stack, ObjectValue... params)` — run with own session+stack
- `execute(context, ObjectValue... params)` — run from inside an `InternalAction`

### Object parameters

`ObjectValue` — common base type for an object-class value; either `DataObject` (non-`NULL`) or `NullValue` (`NullValue.instance`).

`DataObject(Object value, ConcreteClass cls)` — non-`NULL` parameter constructor with an explicit class (needed for instance for dates and user-defined classes). Neither `BusinessLogics.findClass(name)` (returns `CustomClass`) nor `LM.findClass(name)` (returns `ValueClass`) gives a `ConcreteClass` directly — for a user-defined class a cast to `ConcreteCustomClass` is required: `new DataObject(userId, (ConcreteCustomClass) BL.findClass("CustomUser"))`. For built-in classes use the `instance` field: `new DataObject(LocalDate.of(...), DateClass.instance)`. For a few built-in scalar types convenience one-arg constructors exist: `String`, `Integer`, `Long`, `Boolean`, `Double`.

### Change session

`DataSession` — change [session](Change_sessions.md), accumulates changes until applied. Opened through `EventServer.createSession()` or `dbManager.createSession()`. Implements `AutoCloseable` — properly used in try-with-resources, an unapplied session is rolled back.
- `applyException(BL, stack)` — apply; throw on failure
- `applyMessage(BL, stack)` → `String` — apply; return error message or `null`

### `InternalAction`

`InternalAction extends ExplicitAction` (`lsfusion.server.physics.dev.integration.internal.to.InternalAction`) — base class for an `INTERNAL` Java target. Constructor: `(ScriptingLogicsModule LM, ValueClass... classes)`.

- `executeInternal(ExecutionContext<ClassPropertyInterface> context)` — the only override point, invoked on each run.
- `findProperty(name)` / `findAction(name)` / `findClass(name)` / `findGroup(name)` / `findForm(name)` — resolution through `LM`.
- `getParam(int i, context)` → `Object` — positional parameter value.
- `getParamValue(int i, context)` → `ObjectValue` — positional `ObjectValue`.
- `getParamInterface(int i)` → `ClassPropertyInterface` — positional parameter interface.
- `allowNulls()` — override and return `true` so the action accepts `NULL` arguments (defaults to `false`).
- Field `interfaces` — all `ClassPropertyInterface`s of the action (in declaration order).

### `ExecutionContext`

`ExecutionContext<P>` — per-call context inside `InternalAction.executeInternal`.
- `getKeyObject(P key)` → `Object` — raw value, may be `null`
- `getKeyValue(P key)` → `ObjectValue` — `DataObject` or `NullValue`
- `getDataKeyValue(P key)` → `DataObject` — guaranteed non-`NULL` (for actions with `!allowNulls`)
- `getBL()` → `BusinessLogics`
- `getSession()` → current `DataSession`
- field `stack` → current `ExecutionStack` (`context.stack`, public field)
- `messageSuccess(message, header)` / `messageError(message)` / `messageError(message, header)` — user-facing messages
- `delayUserInteraction(ClientAction)` — deferred client action (runs after the current stack completes)
- `requestUserInteraction(ClientAction)` → `Object` — synchronous client action that waits for a result (e.g. a confirmation dialog)

### `EventServer` hierarchy

`LifecycleAdapter implements LifecycleListener` — base class with lifecycle hooks:
- `onInit(LifecycleEvent)`, `onStarted(LifecycleEvent)`, `onStopping(LifecycleEvent)`, `onStopped(LifecycleEvent)`, `onError(LifecycleEvent)` — overridable
- `getOrder()` — lifecycle traversal order. Standard values are constants on `LifecycleListener`: `LOGICS_ORDER` (100), `DBMANAGER_ORDER` (300), `SECURITYMANAGER_ORDER` (400), `RMIMANAGER_ORDER` (500), `BLLOADER_ORDER` (600), `DAEMON_ORDER` (8000), `REFLECTION_ORDER` (9000).

`EventServer extends LifecycleAdapter` (abstract) — base for Spring beans.
- `getEventName()` (abstract) — name for logging; **must return a constant** (called from a field initialiser before DI and the subclass constructor body run).
- `getLogicsInstance()` (abstract) — the bean's `LogicsInstance`.
- `createSession()` → `DataSession` through `getLogicsInstance().getDbManager()`.
- `getTopStack()` → top-level `NewThreadExecutionStack` (a single instance per bean).

`MonitorServer extends EventServer` — `getStack()` via `ThreadLocalContext.assureMonitor(this)`. Used for most Spring bean components.

`RmiServer extends EventServer` — `getStack()` via `ThreadLocalContext.assureRmi(this)`. Used for beans exported over RMI; the remote interface must extend `lsfusion.interop.server.RmiServerInterface`.

### Threads

`ExecutorFactory` — thread pool factories that bring up threads in the right thread context; tasks in these pools can call `getStack()`.
- `createMonitorThreadService(Integer threads, MonitorServer monitor)` → `ExecutorService`
- `createMonitorScheduledThreadService(Integer threads, MonitorServer monitor)` → `ScheduledExecutorService`
- `createRMIThreadService(Integer threads, RmiServer rmi)` → `ExecutorService`

`ThreadLocalContext` — manual thread-context setup for callback threads not created through `ExecutorFactory`:
- `aspectBeforeMonitorHTTP(MonitorServer)` / `aspectAfterMonitorHTTP(MonitorServer)` — wrap a block in monitor context (`MyServer.this` inside an anonymous class).
- `assureRmi(RmiServer)` — defensive call inside remote methods (usually unnecessary; the RMI aspect sets up the context automatically).

### Working with threads correctly

Every thread that is going to reach the lsFusion system through `getStack()`, `LP.read`/`LP.change`, `LA.execute`, `createSession()` must be in the right *thread context* — a `ThreadLocal` marker set by the platform: *monitor* for `MonitorServer` beans, *RMI* for `RmiServer`, *lifecycle* for platform init phases. Without it, `getStack()` fails on an assertion, and a session may end up without an environment.

**Where the context is set up automatically:**

1. RMI inbound calls — wrapped by `RemoteContextAspect` (Spring AOP).
2. Tasks running in threads from `ExecutorFactory.createMonitorThreadService(...)` / `createMonitorScheduledThreadService(...)` / `createRMIThreadService(...)` — every thread of such pools enters and leaves the context via `aspectBefore...` / `aspectAfter...` without user intervention.
3. `EventServer` lifecycle methods (`onInit`, `onStarted`, `onStopping`) — run in *lifecycle* context; `getTopStack()` works there, but `getStack()` does not.
4. Inside `InternalAction.executeInternal` — the context is already set (the call comes through the platform's action flow).

**When the context has to be set up manually** — on callback threads of external libraries (RabbitMQ client, MINA, WebSocket provider, etc.), on threads from `Executors.newFixedThreadPool` (not from `ExecutorFactory`), on hand-rolled `new Thread(...)`. Pairing is mandatory, always `try`/`finally`:

```java
try {
    ThreadLocalContext.aspectBeforeMonitorHTTP(MyServer.this);
    try (DataSession session = createSession()) {
        // getStack() is valid here; LP / LA can be used
    }
} finally {
    ThreadLocalContext.aspectAfterMonitorHTTP(MyServer.this);
}
```

Skipping the `aspectAfter...` is not an option: the ThreadLocal stays dirty, and the next task on this thread (especially in a shared pool) ends up in the wrong context.

**Handing work over to another thread.** `ExecutionContext` is bound to the thread that produced it (its `stack`, `getSession()`, active aspects) — you cannot just pass `context` into `executor.submit(...)` and keep working there. The right patterns are:

- In lsFusion code — the `NEWTHREAD action` / `NEWTHREAD action CONNECTION conn` / `NEWTHREAD action SCHEDULE PERIOD ms` operators (see `NewThreadAction`). The platform builds a notification, delivers it to the right navigator or scheduler pool, and on the new thread builds a fresh `ExecutionContext` through `context.override(env, stack, asyncResult)`.
- In Java code — `context.override(stack)` / `context.override(env, stack, ...)` to swap the stack within the same thread, and `RemoteNavigator.pushNotification(Notification)` to deliver work to a specific navigator on its own thread context.

**Anti-patterns:**

- **`new Thread(() -> lp.read(session, ...)).start()`** — the thread is not in the monitor context, `getStack()` fails.
- **`Executors.newFixedThreadPool(N)` or `newScheduledThreadPool(N)` without `ExecutorFactory`** — same problem, no aspects.
- **`aspectBefore...` without a paired `aspectAfter...`** — the next task on this thread inherits foreign ThreadLocal state.
- **Passing `ExecutionContext` into a background thread as-is** — once the parent action returns, its session may be closed; further work on it breaks invariants.
- **`getStack()` inside `onInit` / `onStarted` / `onStopping`** — that is lifecycle context, not monitor; use `getTopStack()`.
- **`DataSession` shared across threads** — `DataSession` is not thread-safe; always open and close it in the same thread (try-with-resources).
- **Touching `RemoteForm` / client structures from an arbitrary thread without `pushNotification`** — client structures are not thread-safe, delivery must go through the notification channel.

**Multiple concurrent threads inside one bean** — a fixed pool with monitor context:

```java
@Override
protected void onStarted(LifecycleEvent event) {
    workers = ExecutorFactory.createMonitorThreadService(N, this);
    // submit tasks...
}

private void handleMessage(byte[] body) {
    workers.submit(() -> {
        try (DataSession session = createSession()) {
            // getStack() is valid here — the thread is in monitor context
            ...
        } catch (Exception e) {
            logger.error("worker failed", e); // do not propagate into a shared executor
        }
    });
}
```

Each task gets its own `DataSession` (do not share). Always catch and log exceptions inside a submitted task; in `scheduleAtFixedRate` an uncaught exception **suppresses further runs**, in a plain `submit` it is silently lost.

### RMI export

`RmiManager` (obtained via `getLogicsInstance().getRmiManager()`):
- `bindAndExport(String name, Remote remote)` — export + register in the RMI registry under `<exportName>/<name>`
- `unbindAndUnexport(String name, Remote remote)` — reverse
- `export(Remote)` / `unexport(Remote)` / `bind(name, Remote)` / `unbind(name)` — lower-level

The remote interface extends `lsfusion.interop.server.RmiServerInterface`; remote methods declare `throws RemoteException`.
