---
title: 'Internal call (INTERNAL)'
---

The platform allows an lsFusion-based system to invoke code running inside its own deployment components — the application-server JVM, the user's web client, or the database the platform uses — using various types of internal calls, as the internal counterpart to [access to an external system](Access_to_an_external_system_EXTERNAL.md). The interface of such an access is the execution of code in the environment of the target component with specified parameters and, if necessary, the return of certain values as *results* written into the specified properties (without parameters). For call types where parameters and results cross a serialization boundary (`CLIENT`, `DB`), values must belong to [built-in classes](Built-in_classes.md); the Java target runs on the same JVM as the platform, so inside `executeInternal` objects of user-defined classes are passed as `DataObject` / `ObjectValue` directly, without serialization.

## Types of internal calls

The platform currently supports the following types of internal calls:

### Java - executing Java code in the application-server JVM {#java}

The target is a Java class extending `lsfusion.server.physics.dev.integration.internal.to.InternalAction`; its `executeInternal(ExecutionContext context)` method runs on each invocation. How the target is specified (compiled class or inline Java fragment) is covered by the [`INTERNAL` operator](INTERNAL_operator.md).

The classes of the action's parameters and the behaviour for `NULL` arguments are defined by the [`INTERNAL` operator](INTERNAL_operator.md); by default the action rejects `NULL` parameter values, and on the Java side `NULL` acceptance can also be controlled by overriding `allowNulls()`.

Inside the Java code the platform runtime is reached through the `context` parameter (the current change [session](Change_sessions.md), parameter values, and execution environment) and through the resolving methods of `InternalAction`. Returning results in this call type needs no separate mechanism: the Java code writes values directly into lsFusion properties from the body of the action itself, within the same change session.

Inside `executeInternal(ExecutionContext<ClassPropertyInterface> context)` the full integration Java API is available — element lookup (`InternalAction.findProperty`/`findAction` or `context.getBL()`), action-parameter access (`context.getDataKeyValue` / `getKeyValue` / `getKeyObject` depending on `allowNulls()`), reading and writing properties (`LP.read` / `LP.change`), running other actions (`LA.execute`), session and apply control (`context.apply` / `context.cancel`, `session.applyException` / `applyMessage`, `setNoCancelInTransaction`), and user-facing messages (`context.messageSuccess` / `messageError` / `delayUserInteraction`). Changes made through `LP.change` / `LA.execute` land in the same change [session](Change_sessions.md) and are applied atomically with all dependent [events](Events.md), [constraints](Constraints.md), [aggregations](Aggregations.md), and [materializations](Materializations.md). The full catalog of classes, signatures, and threading rules is in [Java API for integrations](Java_integration_API.md).

Java access to the lsFusion system from objects that are not themselves `InternalAction`s — Spring bean components such as the scheduler and integration servers — is covered in [custom Spring bean (`EventServer`)](Custom_Spring_bean_EventServer.md).

### CLIENT - invoking code or a file in the user's web client {#client}

For this type of interaction, the name of a client-side target is specified — a JavaScript function already loaded in the user's web client, or a client-side file (bundled with the application or referenced externally at runtime, typically by URL but also by other file/path references resolved on the application server). Which of the two kinds of target a particular invocation addresses is decided at call time from the shape of the resource name. Scripts, stylesheets and fonts are handled by the client specially, via the mechanisms appropriate to each kind; all other file types, including images, are exposed to the client as generic files.

Client calls are asynchronous by default: the server does not wait for the client to complete. Synchronous execution has to be requested explicitly; capturing a result in the inline form additionally forces synchronous execution.

Auxiliary operations on already-loaded file resources — such as unloading a previously-loaded script or stylesheet — are expressed through a reserved prefix on the resource name; see the [`INTERNAL` operator](INTERNAL_operator.md) article for the exact syntax.

### DB - SQL against the platform's own database {#db}

For this type of interaction, an SQL command is specified as a property expression evaluated at call time. Parameter substitution, table-valued parameters, loading the command from a classpath resource, and routing results to properties all follow the same rules as [`EXTERNAL SQL`](Access_to_an_external_system_EXTERNAL.md#sql) — the differences are that no connection string is supplied and the call runs inside the current change [session](Change_sessions.md), so temporary tables, transactional state, and result writes roll back or commit together with that session. This is also the only supported way to run arbitrary SQL against the platform's own database.

## Language

To declare an action or write an action statement implemented via an internal call, use the [`INTERNAL` operator](INTERNAL_operator.md).

## Examples

Syntactic variants of `INTERNAL` on the lsFusion side (Java / CLIENT / DB targets, declarative and inline forms) are listed in the [operator article](INTERNAL_operator.md). The example below covers the Java target itself.

A minimal example of a Java class extending `InternalAction` — resolves a property and an action in the constructor via `findProperty` / `findAction`, then in `executeInternal` reads and writes a property and runs an action:

```java
package myapp.actions;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class SyncUserLoginAction extends InternalAction {

    private final LP<?> email;
    private final LP<?> login;
    private final LA<?> relogin;

    private final ClassPropertyInterface userInterface;

    public SyncUserLoginAction(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        this.email = findProperty("email[Contact]");
        this.login = findProperty("login[CustomUser]");
        this.relogin = findAction("relogin[CustomUser]");

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        userInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject user = context.getDataKeyValue(userInterface);

        String currentEmail = (String) email.read(context, user);
        if (currentEmail != null) {
            login.change(currentEmail, context, user);
            relogin.execute(context, user);
        }
    }
}
```

Wiring this class up from the lsFusion side:

```lsf
syncUserLogin 'Sync login from email' (CustomUser u)
    INTERNAL 'myapp.actions.SyncUserLoginAction';
```

The type of the constructor's first parameter must match the runtime class of the `LogicsModule` from which the action is wired up via `INTERNAL` (the constructor is looked up by exact class): for most scripted modules this is `ScriptingLogicsModule`, for modules with their own Java subclass — that subclass.
