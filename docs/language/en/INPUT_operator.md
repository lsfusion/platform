---
slug: "/INPUT_operator"
title: 'INPUT operator'
---

The `INPUT` operator creates an [action](../paradigm/Actions.md) that [inputs a value](../paradigm/Primitive_input_INPUT.md) of a [built-in](../paradigm/Built-in_classes.md) or custom class.

### Syntax

```
INPUT inputValue
      [CHANGE [= changeExpr] [NOCONSTRAINTFILTER] [NOCHANGE]]
      [CUSTOM editorFunction]
      [LIST listSource]
      [WHERE condition]
      [ACTIONS contextAction1, ..., contextActionN]
      [sessionScope]
      [TO propID]
      [DO actionOperator [ELSE elseActionOperator]]
```

Where `inputValue` is a class name, or an expression that gives the initial value:

```
[[alias] =] className
[alias] = expr
```

For a built-in class the `=` may be omitted (`INPUT STRING`); for a custom class the `=` is required (`INPUT o = Customer`).

Where `listSource` supplies the candidate values, either as a property or as an action that returns them:

```
listExpr
listAction
```

Where each `contextAction` has the following syntax:

```
image [KEYPRESS key] [TOOLBAR [quickAccess1 ... quickAccessN]] action
```

### Description

The `INPUT` operator creates an action that requests a value of the given class from the user. For a custom class, without an explicit `LIST`, interactive input opens the class selection form, while a programmatically supplied value is interpreted as the object's id.

The `LIST` block limits the values offered for selection to a specific set. The set is supplied either by a property (`listExpr`), whose values are offered to the user, or by an action (`listAction`), which produces them for the value being entered (allowed for a built-in class only). The `WHERE` block narrows this set further by a given condition.

The `CUSTOM` block replaces the default input editor with a custom one rendered by a given client-side function. The `ACTIONS` block adds extra buttons next to the input control, each running its own action when pressed.

The input result is written to the local parameter `alias` and made available in the `DO` block. It can additionally be written to a property. The `CHANGE` block writes the result to the changed property: by default the one specified as the initial input value (or the `changeExpr` property), and by default it limits the offered values to those whose assignment would not break any existing [constraint](../paradigm/Constraints.md). The `TO` block writes the result to the named property `propID`.

### Parameters

- `className`

    The name of the class of the requested value.

- `expr`

    An [expression](Expression.md) whose value class determines the class of the requested value, and whose value is used as the [initial value](../paradigm/Value_input.md#initial) of the input.

- `alias`

    The name of the local parameter to which the input result is written. [Simple ID](IDs.md#id).

- `changeExpr`

    An expression that determines the property to which the input result is written. By default, the property specified as the initial input value is used.

- `NOCONSTRAINTFILTER`

    A keyword that disables the constraint-respecting filter which `CHANGE` adds by default.

- `NOCHANGE`

    A keyword that suppresses the write-back: the result is still bound to `alias`, but no property is changed. Used to keep the `CHANGE` constraint filter without the assignment.

- `editorFunction`

    A [string literal](Literals.md#strliteral) with the name of a client-side function that renders a custom input editor instead of the default one.

- `listExpr`

    An expression that defines the set of values offered for selection. When inputting a value of a built-in class, it supplies the candidate values shown in the inline list. When inputting a value of a custom class, it maps each candidate object to a displayed value (for example `id(s)`); the value class of `listExpr` determines the inline editor.

- `listAction`

    An [action](Action_operators.md) that returns the candidate values (executed with the value being entered), used instead of `listExpr`. Allowed only when inputting a value of a built-in class.

- `condition`

    An expression that narrows the offered candidates to those for which its value is not `NULL`.

- `image`

    A string literal whose value determines the button's [icon](../paradigm/Icons.md#manual).

- `key`

    A string literal specifying the keyboard key that activates the button.

- `quickAccess`

    A place where the button is additionally shown as a quick-access button. Specified by one of the keywords:

    - `ALL` - on all rows;
    - `SELECTED` - on selected rows;
    - `FOCUSED` - on the focused row.

    A keyword may be followed by `HOVER`, in which case the button is shown only on mouse hover. If no place is specified, the quick-access list is empty.

- `action`

    The action executed when the button is pressed.

- `sessionScope`

    The session the input runs in. Specified by one of the keywords:

    - `NEWSESSION` - a new top-level session;
    - `NESTEDSESSION` - a new [nested](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md) session;
    - `THISSESSION` - the current session. Used by default.

- `propID`

    [ID of a property](IDs.md#propertyid) without parameters that the input result is additionally written to.

- `actionOperator`

    A [context-dependent action operator](Action_operators.md#contextdependent) that is executed if the input was completed successfully.

- `elseActionOperator`

    A context-dependent action operator that is executed if the input was cancelled. The input result parameter cannot be used as parameters.

### Examples

```lsf
changeCustomer (Order o)  {
    INPUT s = STRING[100] DO {
        customer(o) <- s;
        IF s THEN
            MESSAGE 'Customer changed to ' + s;
        ELSE
            MESSAGE 'Customer dropped';
    }
}

FORM order
    OBJECTS o = Order
    PROPERTIES(o) customer ON CHANGE changeCustomer(o)
;

testFile  {
    INPUT f = FILE DO { // requesting a dialog to select a file
        open(f); // opening the selected file
    }
}

sku = DATA Sku (OrderDetail);

// select an object of a custom class, offering skus by their id and keeping only those in stock, and write it back
changeSku (OrderDetail d)  {
    INPUT s = sku(d) CHANGE LIST id(s) WHERE inStock(s);
}
```