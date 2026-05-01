---
title: 'Internal call (INTERNAL)'
---

The platform allows an lsFusion-based system to invoke code running inside its own deployment components — the application-server JVM, the user's web client, or the database the platform uses — using various types of internal calls, as the internal counterpart to [access to an external system](Access_to_an_external_system_EXTERNAL.md). The interface of such an access is the execution of code in the environment of the target component with specified parameters and, if necessary, the return of certain values as *results* written into the specified properties (without parameters). It is assumed that all parameter and result objects are objects of [built-in classes](Built-in_classes.md).

## Types of internal calls

The platform currently supports the following types of internal calls:

### Java - executing Java code in the application-server JVM {#java}

For this type of interaction, a Java class is specified — either by a fully qualified class name, whose compiled class must be reachable from the application-server classpath, or by an inline Java source embedded directly in the lsFusion source (in which case the platform generates the surrounding class automatically). The Java class must extend `lsfusion.server.physics.dev.integration.internal.to.InternalAction`, and its `executeInternal(ExecutionContext context)` method runs on each invocation.

The action's parameter classes come from the enclosing action declaration or from an explicit list given at the declaration site. By default the action refuses `NULL` parameter values and is silently skipped when any is `NULL`; accepting `NULL`s has to be stated explicitly — either with the `NULL` keyword in the [`INTERNAL` operator](INTERNAL_operator.md), or by overriding the `allowNulls()` method in the class itself.

Inside the Java code the platform runtime is reached through the `context` parameter (the current change [session](Change_sessions.md), parameter values, and execution environment) and through the resolving methods of `InternalAction`. Returning results in this call type needs no separate mechanism: the Java code writes values directly into lsFusion properties from the body of the action itself, within the same change session.

Inside `executeInternal(ExecutionContext<ClassPropertyInterface> context)` — through the `InternalAction` class itself and through `context` — the following is available:

- **Element lookup** through the resolving methods of `InternalAction`: `findProperty(name)` and `findAction(name)` (returning Java wrappers `LP<?>` and `LA<?>` over a [property](Properties.md) or [action](Actions.md) found by [identifier](IDs.md#propertyid)), as well as `findClass`, `findGroup`, `findForm`. The same functionality is also available through `context.getBL()` — the root business-logic object (`BusinessLogics`), from which one can navigate to a specific [logic module](Modules.md) and on to the elements declared inside it.
- **Action parameters** — the mapping between interfaces and parameter positions comes from `interfaces` / `getOrderInterfaces()`. For an action with the default `NULL`-rejection behaviour, each parameter value is a guaranteed non-`NULL` `DataObject`, available through `context.getDataKeyValue(interface)` and ready to be passed directly to `LP.read` / `LP.change`. For an action accepting `NULL` (`INTERNAL ... NULL` or an overridden `allowNulls()`), use `context.getKeyValue(interface)` (`ObjectValue`, may be `NullValue`) or `context.getKeyObject(interface)` (the raw value, may be `null`) and handle `NULL` explicitly. The positional `InternalAction` helpers — `getParamValue(i, context)` (`ObjectValue`) and `getParam(i, context)` (`Object`) — wrap the same variants.
- **Reading property values** — `lp.read(context, params...)` returns the current value of the property on the supplied arguments.
- **Changing property values** — `lp.change(value, context, params...)`. Changes apply to the current change session atomically with the rest of the lsFusion transaction and with all dependent [events](Events.md), [constraints](Constraints.md), [aggregations](Aggregations.md), and [materializations](Materializations.md).
- **Executing actions** — `la.execute(context, params...)`. The action runs in the same change session and call stack as the enclosing `InternalAction`.
- **Session and transaction control** — `context.getSession()` and the methods on it (`apply`, `cancel`, `setNoCancelInTransaction`, etc.).
- **User-facing messages and feedback** — `context.messageSuccess`, `context.messageError`, `context.delayUserInteraction(...)`, and similar methods.

Java access to the lsFusion system from objects that are not themselves `InternalAction`s — such as Spring beans receiving `businessLogics` through dependency injection — is covered in [access from an internal system](Access_from_an_internal_system.md).

### CLIENT - invoking code or a file in the user's web client {#client}

For this type of interaction, the name of a client-side target is specified — a JavaScript function already loaded in the user's web client, or a client-side file (bundled with the application or referenced externally at runtime, typically by URL but also by other file/path references resolved on the application server). Which of the two kinds of target a particular invocation addresses is decided at call time from the shape of the resource name. Scripts, stylesheets and fonts are handled by the client specially, via the mechanisms appropriate to each kind; all other file types, including images, are exposed to the client as generic files.

Client calls are asynchronous by default: the server does not wait for the client to complete. Synchronous execution has to be requested explicitly; capturing a result in the inline form additionally forces synchronous execution.

Auxiliary operations on already-loaded file resources — such as unloading a previously-loaded script or stylesheet — are expressed through a reserved prefix on the resource name; see the [`INTERNAL` operator](INTERNAL_operator.md) article for the exact syntax.

### DB - SQL against the platform's own database {#db}

For this type of interaction, an SQL command is specified as a property expression evaluated at call time. Parameter substitution, table-valued parameters, loading the command from a classpath resource, and routing results to properties all follow the same rules as [`EXTERNAL SQL`](Access_to_an_external_system_EXTERNAL.md#sql) — the differences are that no connection string is supplied and the call runs inside the current change [session](Change_sessions.md), so temporary tables, transactional state, and result writes roll back or commit together with that session. This is also the only supported way to run arbitrary SQL against the platform's own database.

## Language

To declare an action or write an action statement implemented via an internal call, use the [`INTERNAL` operator](INTERNAL_operator.md).

## Examples

```lsf
// Java target — a pre-compiled InternalAction class
cmd '{utils.cmd}' (TEXT command, TEXT directory, BOOLEAN isClient, BOOLEAN wait)
    INTERNAL 'lsfusion.server.physics.admin.interpreter.action.RunCommandAction';

// Java target — inline Java fragment, reaching the platform runtime through `context`
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>;

// Client target — file resources loaded into the web client at startup, waiting for each
onWebClientStarted() + {
    INTERNAL CLIENT WAIT 'plotly-3.0.1.min.js';
    INTERNAL CLIENT WAIT 'dashboard.js';
}

// Client target — JavaScript function called synchronously, its return value captured
getCookie(STRING name) {
    LOCAL cookie = STRING();
    INTERNAL CLIENT 'getCookie' PARAMS name TO cookie;
}

// Internal database target — SQL running in the current change session
loadPrices() {
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Meat%';
    INTERNAL DB 'select price, barcode from $1' PARAMS exportFile() TO exportFile;
}
```

Example of a Java class extending `InternalAction` — resolving properties in the constructor through `findProperty`, then reading and writing them in `executeInternal` through `LP.read` / `LP.change` while passing `context`:

```java
package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.UserInfo;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;

public class GenerateLoginPasswordAction extends InternalAction {

    private final LP<?> email;
    private final LP<?> loginCustomUser;
    private final LP<?> sha256PasswordCustomUser;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordAction(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        this.email = findProperty("email[Contact]");
        this.loginCustomUser = findProperty("login[CustomUser]");
        this.sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject userObject = context.getDataKeyValue(customUserInterface);

        String currentEmail = (String) email.read(context, userObject);

        String login;
        int indexMail;
        if (currentEmail != null && (indexMail = currentEmail.indexOf("@")) >= 0)
            login = currentEmail.substring(0, indexMail);
        else
            login = "login" + userObject.object;

        Random rand = new Random();
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++)
            password.append(chars.charAt(rand.nextInt(chars.length())));

        if (loginCustomUser.read(context, userObject) == null)
            loginCustomUser.change(login, context, userObject);
        String sha256Password = BaseUtils.calculateBase64Hash("SHA-256", password.toString(), UserInfo.salt);
        sha256PasswordCustomUser.change(sha256Password, context, userObject);
    }
}
```

Wiring this class up from the lsFusion side:

```lsf
generateLoginPassword 'Generate login and password' (CustomUser u)
    INTERNAL 'lsfusion.server.logics.property.actions.GenerateLoginPasswordAction';
```

The type of the constructor's first parameter must match the runtime class of the `LogicsModule` from which the action is wired up via `INTERNAL` (the constructor is looked up by exact class): for most scripted modules this is `ScriptingLogicsModule`, for modules with their own Java subclass — that subclass.
