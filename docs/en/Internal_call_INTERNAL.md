---
title: 'Internal call (INTERNAL)'
---

The platform allows an lsFusion-based system to invoke code running inside its own deployment components — the application-server JVM, the user's web client, or the database the platform uses — using various types of internal calls, as the internal counterpart to [access to an external system](Access_to_an_external_system_EXTERNAL.md). The interface of such an access is the execution of code in the environment of the target component with specified parameters and, if necessary, the return of certain values as *results* written into the specified properties (without parameters). It is assumed that all parameter and result objects are objects of [built-in classes](Built-in_classes.md).

## Types of internal calls

The platform currently supports the following types of internal calls:

### Java - executing Java code in the application-server JVM {#java}

For this type of interaction, a Java class is specified — either by a fully qualified class name, whose compiled class must be reachable from the application-server classpath, or by an inline Java source embedded directly in the lsFusion source (in which case the platform generates the surrounding class automatically). The Java class must extend `lsfusion.server.physics.dev.integration.internal.to.InternalAction`, and its `executeInternal(ExecutionContext context)` method runs on each invocation.

The action's parameter classes come from the enclosing action declaration or from an explicit list given at the declaration site. By default the action refuses `NULL` parameter values and is silently skipped when any is `NULL`; accepting `NULL`s has to be stated explicitly.

Inside the Java code the platform runtime is reached through the `context` parameter (the current change [session](Change_sessions.md), parameter values, and execution environment) and through the resolving methods of `InternalAction`. The full picture of what Java code can do on that side is covered in [access from an internal system](Access_from_an_internal_system.md).

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
