---
slug: "/Internal_call_INTERNAL"
title: 'Internal call (INTERNAL)'
---

The platform allows an lsFusion-based system to invoke code running inside its own deployment components — the application-server JVM, the user's web client, or the database the platform uses — using various types of internal calls, as the internal counterpart to [access to an external system](Access_to_an_external_system_EXTERNAL.md). The interface of such an access is the execution of code in the environment of the target component with specified parameters and, if necessary, the return of certain values as *results* written into the specified properties (without parameters). For call types where parameters and results cross a serialization boundary (`CLIENT`, `DB`), values must belong to [built-in classes](Built-in_classes.md). The Java target runs in the same JVM as the platform and receives objects of user-defined classes without serialization.

## Types of internal calls

The platform currently supports the following types of internal calls:

### Java - executing Java code in the application-server JVM {#java}

The target is a Java class extending `InternalAction`. Its `executeInternal` method runs on each call.

The `INTERNAL` operator defines the action's parameter classes and whether it accepts `NULL` arguments. By default the action rejects `NULL` arguments.

The `context` parameter gives the Java code access to the platform runtime, including the current change [session](Change_sessions.md), parameter values, and the execution environment. The Java code returns results by writing values directly to lsFusion properties in the current change session. Those changes are applied atomically with all dependent [events](Events.md), [constraints](Constraints.md), [aggregations](Aggregations.md), and [materializations](Materializations.md).

### CLIENT - invoking code or a file in the user's web client {#client}

For this type of interaction, the name of a client-side target is specified — a JavaScript function already loaded in the user's web client, or a client-side file (bundled with the application or referenced externally at runtime, typically by URL but also by other file/path references resolved on the application server). Which of the two kinds of target a particular invocation addresses is decided at call time from the shape of the resource name. Scripts, stylesheets and fonts are handled by the client specially, via the mechanisms appropriate to each kind. All other file types, including images, are exposed to the client as generic files.

Client calls are asynchronous by default: the server does not wait for the client to complete. Synchronous execution has to be requested explicitly. Capturing a result in the inline form additionally forces synchronous execution.

Auxiliary operations on already-loaded file resources — such as unloading a previously-loaded script or stylesheet — are expressed through a reserved prefix on the resource name. See the `INTERNAL` operator article for the exact syntax.

### DB - SQL against the platform's own database {#db}

For this type of interaction, an SQL command is specified as a property value evaluated at call time. Parameter substitution, table-valued parameters, loading the command from a classpath resource, and routing results to properties all follow the same rules as [`EXTERNAL SQL`](Access_to_an_external_system_EXTERNAL.md#sql) — the differences are that no connection string is supplied and the call runs inside the current change [session](Change_sessions.md), so temporary tables, transactional state, and result writes roll back or commit together with that session. This is also the only supported way to run arbitrary SQL against the platform's own database.

## Language

Use the [`INTERNAL` operator](../language/INTERNAL_operator.md) to declare an action or write an action statement for an internal call. When declaring a Java action, specify the class or the inline Java fragment to execute.

The [Java API for integrations](../language/Java_integration_API.md) describes the classes, method signatures, and threading rules for Java actions.

A [custom Spring bean (`EventServer`)](../language/Custom_Spring_bean_EventServer.md) provides Java access to lsFusion for components such as the scheduler and integration servers.

A JavaScript function called by `INTERNAL CLIENT` in the web client receives [the form or navigator controller](../language/Form_and_navigator_controllers.md): on a form it can change the form and call the server, and in the navigator it can call the server and activate navigator elements. [Client JavaScript modules](../language/Client_JavaScript_modules.md#named-exports-and-auto-registration) describe how such a function is exposed and found by name.

## Examples

```lsf
// Java target — a compiled InternalAction class
cmd '{utils.cmd}' (TEXT command, TEXT directory, BOOLEAN isClient, BOOLEAN wait)
    INTERNAL 'lsfusion.server.physics.admin.interpreter.action.RunCommandAction';

// Java target — inline Java fragment, runtime is reached through `context`
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>;

// Client target — file resources loaded into the web client at start-up, each call waited for
onWebClientStarted() + {
    INTERNAL CLIENT WAIT 'plotly-3.0.1.min.js';
    INTERNAL CLIENT WAIT 'dashboard.js';
}

// Client target — a JavaScript function called synchronously, its return value captured
getCookie(STRING name) {
    LOCAL cookie = STRING();
    INTERNAL CLIENT 'getCookie' PARAMS name TO cookie;
}

// Internal SQL target — SQL executed inside the current change session
loadPrices() {
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Meat%';
    INTERNAL DB 'select price, barcode from $1' PARAMS exportFile() TO exportFile;
}
```

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
