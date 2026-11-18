---
slug: "/Brief_via_app"
title: 'Brief: running application'
---

## Running application

An application's `/mcp` reaches its deployed source (`lsfusion_files_*`), data and actions (`lsfusion_eval`, `lsfusion_exec`). [Eval code](../paradigm/Eval_EVAL.md) can add local elements and call loaded ones but cannot change the project's persistent structure: that takes a source edit and a deployment, outside this kind. Settings and saved form customization are stored data and change in the running application.

## A call over `/mcp`

`Authentication.currentUser[]` is the MCP token's account, possibly a shared integration account rather than the person asking. A dialog has nobody to answer it: `ASK` receives YES without asking anyone, `INPUT` is unsupported, and an interactive form does not open. Discarding the call's session does not undo what called actions applied themselves, or effects outside the database.

## Saved customization

[`SystemEvents`](../paradigm/System_SystemEvents.md) stores customization code for all users and for one user:
- of a form — `EXTEND FORM` and `DESIGN` statements — in `dataExtendCode[Form]` and `dataExtendCode[Form, User]`, joined and compiled when the form is created;
- of the application — `NAVIGATOR` and `WINDOW` statements and the forms, actions and properties the added elements need — in `dataExtendCode[]` and `dataExtendCode[User]`, joined and compiled when a connection of the user starts. What it declares is seen by the scripts that user's calls evaluate.

A compilation error is reported, and the form, or the client's navigator, comes without the customization. `lsfusion_eval` reads a fragment with `RETURN`. The current user's fragment is replaced whole through `lsfusion_exec` by `SystemEvents.customizeFormUser[STRING, TEXT]`, whose `params` are the form's canonical name and the code, and by `SystemEvents.customizeNavigatorUser[TEXT]`, whose `params` are the code. They save it in a separate session without applying the caller's changes, and `NULL` or empty code removes it. `lsfusion_eval` writes the other fragments whole with `APPLY`. What is already open stays as it was: a form's change shows when the form is created again, the application's when a connection starts again. Over `/mcp` the user reopens the form or restarts the client, on the page see [recreating a form](Brief_via_webapp.md).

## Client actions over `/mcp`

A call over `/mcp` has no user's client: in the call itself a `MESSAGE` only returns to the caller in `messages`, and a form or a file cannot open. To run a client action in a connected user's client, the script delivers it to that user's [connection](../paradigm/System_SystemEvents.md) (`SystemEvents.Connection`) with [`NEWEXECUTOR ... CLIENT`](../language/NEWEXECUTOR_operator.md) and a nested plain `NEWTHREAD`:

```lsf
FOR userLogin(Connection c) = 'alice' AND connectionStatus(c) = ConnectionStatus.connectedConnection DO
    NEWEXECUTOR {
        NEWTHREAD MESSAGE 'The report is ready';
    } CLIENT c NOWAIT;
```

Here the connections are those of the user with the login `alice`. A user can have several connected connections — browser tabs, a desktop client — and `FOR` reaches each. `lastActivity[Connection]` tells the most recently active one. The delivered action runs on the server in a new session at that connection's navigator, not in an open form's session. Its interactive operators address that client's UI, and `Authentication.currentUser[]` there is the connection's user. `NOWAIT` returns at once, without waiting for the action to finish; `WAIT` waits, with an optional timeout.

On the application's page this is not needed for the user's own client: there [eval and exec](Brief_via_webapp.md) already run in it. Delivery to a connection remains there only for the rare case of acting in another tab or client of the user.
