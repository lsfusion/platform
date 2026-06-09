---
slug: "/Interpreter"
title: 'Interpreter'
---

The interpreter is an interactive mechanism that lets the administrator execute arbitrary lsFusion code (or a call to Java code) on a running application server, without restarting and without rebuilding the project. The code is entered on the `System > Interpreter` form (`System.interpreter` in the navigator) and executed on the server in a new session.

Any code executed through the interpreter has the same access to system elements (classes, properties, actions, forms) as a regular module loaded at startup. This makes it possible to verify hypotheses, read and selectively change data, invoke existing actions and forms, and temporarily introduce new computations — without changing the project files.

### Input modes

The interpreter accepts four kinds of source code — the chosen mode determines how the entered text is wrapped before execution:

|Mode| What you enter                                                                          |What gets executed|
|---|-----------------------------------------------------------------------------------------|---|
|`Script`| A sequence of module statements that declares an action named `run`.                    |That `run` action is executed.|
|`Action`| The body of an action — a sequence of action operators and local property declarations. |The platform wraps the body into a `run` action and executes it.|
|`Form`| A form declaration.                                                                     |The platform creates a form from this declaration and opens it [in the interactive view](In_an_interactive_view_SHOW_DIALOG.md).|
|`Java`| A fragment of Java code.                                                                |The platform wraps the fragment into a Java [internal call](Internal_call_INTERNAL.md) and executes it.|

### Execution

The entered code is executed the same way as the [eval operator (`EVAL`)](Eval_EVAL.md): with full visibility of every loaded module of the project and inside a new [session](Change_sessions.md). To persist data changes to the database, the code itself must perform the [apply](Apply_changes_APPLY.md).

If an exception is thrown during execution, the platform opens a form with the exception message and the Java-side and lsFusion-side stack traces; the form lets you copy all of this to the clipboard.

### Log

Each interpreter run is recorded in the script log (`Script log` form) together with its author, the script text, the timestamp of the last text change, and the timestamp of the last execution. A saved script can be brought back into the interpreter with the `Find script` button.

### Temporary data

The interpreter form contains a table with string, numeric, and date columns (`string1`..`string10`, `numeric1`..`numeric5`, `date1`..`date3`) whose objects the executed code can read and write directly. This makes it possible to hand-enter input data for the script and immediately see the result on the same form.

### Access

Access to the interpreter is regulated by the [security policy](Security_policy.md): it is enough to grant the role access to the `System.interpreter` navigator form. A user with access to that form is also granted external access to the [program interface](Access_from_an_external_system.md) even when the working parameter `enableAPI` is disabled (see [working parameters](Working_parameters.md)), since that user can already execute any code through the interpreter.
