---
slug: "/DIALOG_operator"
title: 'DIALOG operator'
---

The `DIALOG` operator creates an [action](../paradigm/Actions.md) that [opens a form](../paradigm/Open_form.md) in [interactive view](../paradigm/In_an_interactive_view_SHOW_DIALOG.md) as a [value-input dialog](../paradigm/In_an_interactive_view_SHOW_DIALOG.md#dialog).

### Syntax

```
DIALOG target
    [formActionOptions]
    [{initActionOperator}]
    [DO actionOperator [ELSE elseActionOperator]]
```

Where `target` is one of two forms — the *named-form* opens a previously declared form, attaching an `objSpec` to each form object:

```
formName [OBJECTS objName1 objSpec1, ..., objNameN objSpecN]
```

and the *class-form* opens the [list or edit form](../paradigm/Interactive_view.md#edtClass) of a class, with one implicit object whose `objSpec` is attached directly:

```
classFormType className objSpec
```

Each `objSpec` is:

```
[= expr [NULL]] [inputHead [alias] [NULL] [TO propId] [CONSTRAINTFILTER [= changeExpr]] [LIST listExpr]]
```

Where `inputHead` is one of two forms — the *input* form returns the chosen value:

```
INPUT
```

and the *input-with-write-back* form additionally writes it to a property:

```
CHANGE [= changeExpr] [NOCONSTRAINTFILTER] [NOCHANGE]
```

And `formActionOptions` is any combination of the following options, in any order:

```
FILTERS filterExpr1, ..., filterExprM
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

The `DIALOG` operator creates an action that opens the specified form for value input: every object marked with `INPUT` or `CHANGE` returns its last current value when the form closes. The `OBJECTS` block sets [initial values](../paradigm/Open_form.md#params) for the form's objects (in the class-form, the per-object spec attaches directly to the class and plays the same role for the form's only object, which is implicitly named `object` and serves as the default `alias` and as the parameter name inside `FILTERS` and `DO` expressions). Inside `formActionOptions`, the `FILTERS` clause attaches [additional filters](../paradigm/Open_form.md#contextFilters) computed from the calling context; the remaining options control the form-opening behavior — layout, session, system-action visibility, and other modifiers. The trailing block `{initActionOperator}` runs once when the form is opened.

When the user closes the form via the *OK* action (`System.formOk[]`), the chosen object values are returned; via the *Drop* action (`System.formDrop[]`), `NULL` values are returned. In both cases the input is [completed successfully](../paradigm/Value_input.md#result), and `actionOperator` runs with the returned values bound to the corresponding `alias` parameters. When the user closes the form any other way (`System.formClose[]`), the input is canceled and `elseActionOperator` runs instead.

The operator runs synchronously (waits for the form to close) whenever the input result is consumed downstream — when a `DO`/`ELSE` continuation is given, or when at least one object has `CHANGE` without `NOCHANGE` (the implicit write-back is in effect). Otherwise the synchronization mode is inferred from the calling context (the same heuristic as for the `WAIT`/`NOWAIT` option of [`SHOW`](SHOW_operator.md)) and may still come out synchronous, for example when the dialog is opened from an already-modal form.

### Parameters

- `formName`

    Form name. [Composite ID](IDs.md#cid).

- `classFormType`

    Type of the class form to open. It is specified by one of the keywords:

    - `LIST` — the selection (list) form
    - `EDIT` — the editing form

- `className`

    Name of the [custom class](../paradigm/User_classes.md) whose list or edit form is opened. Composite ID.

- `objName1, ..., objNameN`

    Names of form objects. [Simple IDs](IDs.md#id).

- `expr`

    [Expression](Expression.md) whose value is used as the initial value of the form object the enclosing `objSpec` is attached to.

- `NULL` after the initial value

    Keyword. Allows the passed initial value to be `NULL`. By default, if any passed value is `NULL`, the action is skipped and control passes to the next action. Automatically enabled if an `INPUT` / `CHANGE` input marker is specified for the object.

- `initActionOperator`

    [Context-dependent action operator](Action_operators.md#contextdependent) that runs on form opening, after the form's [`EVENTS ON INIT`](Event_block.md) handlers.

- `actionOperator`

    Context-dependent action operator that runs after the user closes the form successfully. The `alias` parameters added by the per-object `INPUT` / `CHANGE` markers are visible inside this action.

- `elseActionOperator`

    Context-dependent action operator that runs if the user cancels the input. The `alias` parameters added by the per-object `INPUT` / `CHANGE` markers are *not* visible inside this action.

### Input options

- `INPUT`

    Keyword. Marks the object as returning its last current value at form close. The returned value is bound to the local parameter `alias` and is also made available to the `actionOperator` of the `DO` clause.

- `CHANGE`

    Keyword. Like `INPUT`, but additionally writes the returned value back to a property. By default the written-to property is the one supplied as the initial value (`expr`); to use a different one, give `changeExpr` after `CHANGE`. Also, by default `CHANGE` adds a filter to the form so that only those object values are selectable that would not break any existing [constraint](../paradigm/Constraints.md) when assigned.

- `NOCONSTRAINTFILTER`

    Keyword. Disables the constraint-respecting filter that `CHANGE` adds by default.

- `NOCHANGE`

    Keyword. Suppresses the write-back; the returned value is bound to `alias` but no property is updated. Use to get the `CHANGE` constraint-filter behavior without the implicit assignment.

- `alias`

    Name of the local parameter that the returned value is bound to. Simple ID. Defaults to the form object's name.

- `NULL` in the `INPUT` / `CHANGE` marker

    Keyword. Allows the user to return `NULL` via the *Drop* system action (`System.formDrop[]`). Enabled by default after `CHANGE`; otherwise disabled by default.

- `propId`

    [ID of the property](IDs.md#propertyid) that the returned value is written to (`INPUT` form), or that the value is also written to in addition to the `CHANGE` target. The property is invoked at no parameters.

- `CONSTRAINTFILTER`

    Keyword. Enables the constraint-respecting filter described under `CHANGE`. After `INPUT` the filter is off by default and `CONSTRAINTFILTER` turns it on. After `CHANGE` the filter is on by default, so the keyword keeps it on — including when `NOCONSTRAINTFILTER` was also given, in which case `CONSTRAINTFILTER` re-enables the filter. The `changeExpr` sets the property used by the filter, and with `CHANGE` this same property also replaces the `CHANGE` target (a single property is shared between the constraint check and the write-back).

- `listExpr`

    Expression that produces a property restricting the rows the user can pick: only rows for which the property's value is not `NULL` are selectable. Evaluated in the extended input context, so it can reference the input aliases introduced by `INPUT` / `CHANGE` on this and earlier objects. The `LIST` option may be specified for at most one object across the whole call.

### Options

- `filterExpr1, ..., filterExprM`

    Non-empty list of expressions that define additional filters applied to the form. Each expression can use both the calling context parameters and the form's own objects.

- `windowType`

    The [form layout](../paradigm/In_an_interactive_view_SHOW_DIALOG.md#location): `FLOAT` shows the form as a floating window, `DOCKED` as a tab in the system forms window, `EMBEDDED` and `POPUP` as in-place editors, and `IN` places the form inside `containerName` — a form-qualified [design component](DESIGN_statement.md#selector) (the form's name followed by the component's path within that form's design) that must be a container. `FLOAT` is the default.

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

    Keyword. If specified, when the user presses the *OK* system action (`System.formOk[]`), the platform first validates the pending session changes (runs the apply pass — constraints, aggregations, event handlers — without committing); the dialog closes only if the validation passes, otherwise it stays open.

### Examples

```lsf
FORM selectSku
    OBJECTS s = Sku
    PROPERTIES(s) id
;

testDialog ()  {
    // open a form for value input; show 'On Init' on opening, react to the chosen value on OK
    DIALOG selectSku OBJECTS s INPUT {
        MESSAGE 'On Init';
    } DO {
        MESSAGE 'Selected sku : ' + id(s);
    }
}

sku = DATA Sku (OrderDetail);

changeSku (OrderDetail d)  {
    // input with write-back: pick a sku, the picker filters out values that would break constraints
    DIALOG selectSku OBJECTS s = sku(d) CHANGE;

    // equivalent to the previous line spelled out via INPUT
    DIALOG selectSku OBJECTS s = sku(d) INPUT NULL CONSTRAINTFILTER DO {
        sku(d) <- s;
    }
}

// list form of the Sku class instead of a named form
selectAnySku () { DIALOG LIST Sku INPUT s DO MESSAGE 'sku selected'; }

quantity = DATA INTEGER (OrderDetail);

// prompting for a value of a built-in class: the object is displayed in the panel
FORM askQuantity 'Quantity'
    OBJECTS q = INTEGER PANEL
    PROPERTIES VALUE(q)
;

changeQuantity (OrderDetail d) {
    DIALOG askQuantity OBJECTS q = quantity(d) INPUT DO
        quantity(d) <- q;
}
```
