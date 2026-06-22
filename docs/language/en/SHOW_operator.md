---
slug: "/SHOW_operator"
title: 'SHOW operator'
---

The `SHOW` operator creates an [action](../paradigm/Actions.md) that [opens a form](../paradigm/Open_form.md) in [interactive view](../paradigm/In_an_interactive_view_SHOW_DIALOG.md).

### Syntax

```
SHOW [formId =] target
    [formActionOptions]
    [{initActionOperator}]
```

Where `target` is one of two forms — the *named-form* opens a previously declared form:

```
formName [OBJECTS objName1 = expr1 [NULL], ..., objNameN = exprN [NULL]]
```

and the *class-form* opens the [list or edit form](../paradigm/Interactive_view.md#edtClass) of a class:

```
classFormType className = expr [NULL]
```

And `formActionOptions` is any combination of the following options, in any order:

```
FILTERS filterExpr1, ..., filterExprM
syncType
windowType
manageSessionType
sessionScopeType
cancelType
READONLY
CHECK
```

Where `windowType` is one of:

```
FLOAT
DOCKED
EMBEDDED
POPUP
IN containerName
```

### Description

The `SHOW` operator creates an action that opens the specified form. The `OBJECTS` block sets [initial values](../paradigm/Open_form.md#params) for the form's objects (in the class-form, the single `= expr` plays the same role for the form's only object, which is implicitly named `object` and can be referenced by that name inside `FILTERS` expressions). Inside `formActionOptions`, the `FILTERS` clause attaches [additional filters](../paradigm/Open_form.md#contextFilters) computed from the calling context; the remaining options control the form-opening behavior — flow, layout, session, system-action visibility, and other modifiers. The trailing block `{initActionOperator}` runs once when the form is opened.

### Parameters

- `formId`

    [String literal](Literals.md#strliteral) that uniquely identifies the opened form instance for later form-management actions (for example, to close that exact form). By default, the opened form has no such identifier.

- `formName`

    Form name. [Composite ID](IDs.md#cid).

- `classFormType`

    Type of the class form to open. It is specified by one of the keywords:

    - `LIST` — the selection (list) form
    - `EDIT` — the editing form

- `className`

    Name of the [custom class](../paradigm/User_classes.md) whose list or edit form is opened. Composite ID.

- `objName1, ..., objNameN`

    Names of form objects for which initial values are specified. [Simple IDs](IDs.md#id).

- `expr`, `expr1, ..., exprN`

    [Expressions](Expression.md) whose values are used as the initial values of the corresponding form objects.

- `NULL` after the initial value

    Keyword. Allows the passed initial value to be `NULL`. By default, if any passed value is `NULL`, the action is skipped and control passes to the next action.

- `initActionOperator`

    [Context-dependent action operator](Action_operators.md#contextdependent) that runs on form opening, after the form's [`EVENTS ON INIT`](Event_block.md) handlers.

### Options

- `filterExpr1, ..., filterExprM`

    Non-empty list of expressions that define additional filters applied to the form. Each expression can use both the calling context parameters and the form's own objects.

- `syncType`

    The [flow-control](../paradigm/In_an_interactive_view_SHOW_DIALOG.md#flow) mode. One of:

    - `WAIT` — synchronous (wait for the form to close)
    - `NOWAIT` — asynchronous (continue immediately after opening)

    By default, the platform picks `WAIT` when the call is itself made from a modal form, when more session usages will follow in the same call, or when the chosen window-type is itself modal (`FLOAT`, `EMBEDDED`, `POPUP`); otherwise `NOWAIT`.

- `windowType`

    The [form layout](../paradigm/In_an_interactive_view_SHOW_DIALOG.md#location): `FLOAT` shows the form as a floating window, `DOCKED` as a tab in the system forms window, `EMBEDDED` and `POPUP` as in-place editors, and `IN` places the form inside `containerName` — a form-qualified [design component](DESIGN_statement.md#selector) (the form's name followed by the component's path within that form's design) that must be a container. By default, `FLOAT` is used in synchronous mode and `DOCKED` in asynchronous mode.

- `manageSessionType`

    Overrides whether the opened form is the [session owner](../paradigm/Interactive_view.md#owner). One of:

    - `MANAGESESSION` — force ownership
    - `NOMANAGESESSION` — force non-ownership

    By default, the form is the session owner only if the session has no other owner at opening time.

- `sessionScopeType`

    The session in which the form is opened. One of:

    - `NEWSESSION` — a new top-level session
    - `NESTEDSESSION` — a new [nested](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md) session
    - `THISSESSION` — the current session (default)

- `cancelType`

    Overrides whether the *Cancel* system action (`System.formCancel[]`) is shown on the form. One of:

    - `CANCEL` — force visible
    - `NOCANCEL` — force hidden

    By default, it is shown if the form is the session owner and the form contains actions that can change the session.

- `READONLY`

    Keyword. If specified, the form is opened in [read-only](../paradigm/In_an_interactive_view_SHOW_DIALOG.md#extra) mode.

- `CHECK`

    Keyword. If specified, when the user presses the *OK* system action (`System.formOk[]`), the platform first validates the pending session changes (runs the apply pass — constraints, aggregations, event handlers — without committing); the form closes only if the validation passes, otherwise it stays open.

### Examples

```lsf
date = DATA DATE (Order);
FORM showForm
    OBJECTS dateFrom = DATE, dateTo = DATE PANEL
    PROPERTIES VALUE(dateFrom), VALUE(dateTo)

    OBJECTS o = Order
    FILTERS date(o) >= dateFrom, date(o) <= dateTo
;

testShow ()  {
    // initial values for two objects, init action after opening
    SHOW showForm OBJECTS dateFrom = 2010_01_01, dateTo = 2010_12_31 { MESSAGE 'On init'; };

    // open in a new session as a floating window, with the new object as the current one
    NEWSESSION {
        NEW s = Sku {
            SHOW sku OBJECTS s = s FLOAT;
        }
    }

    // open the named form with panel values, an additional filter, and an instance identifier
    SHOW 'recentOrders' = showForm OBJECTS dateFrom = 2024_01_01, dateTo = 2024_12_31 FILTERS number(o) > 1000 NOWAIT;

    // open the list form of the Sku class, focused on a specific sku
    NEW s = Sku { SHOW LIST Sku = s; }
}
```
