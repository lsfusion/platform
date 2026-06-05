---
slug: "/INPUT_operator"
title: 'INPUT operator'
---

The `INPUT` operator creates an [action](../paradigm/Actions.md) that [inputs a primitive](../paradigm/Primitive_input_INPUT.md) value or selects an object of a custom class.

### Syntax

```
INPUT inputOptions [LIST listExpr]
[CHANGE [= changeExpr]]
[DO actionOperator [ELSE elseActionOperator]]
```

`inputOptions` - input options. Specified by one of the following syntaxes:

```
[alias =] className
[alias] = expr
```

### Description

The `INPUT` operator creates an action which allows to request the value of one of the [built-in classes](../paradigm/Built-in_classes.md) from the user.

If `className` is a custom class, the operator requests an object of that class. Without an explicit `LIST`, interactive input opens the class selection form, while a programmatically supplied value is interpreted as the object's id. An explicit `LIST` offers the values of `listExpr` for selection (for example `LIST name(o)`); the value class of `listExpr` determines the inline editor.

### Parameters

- `className`

    The name of a [built-in class](../paradigm/Built-in_classes.md) or a custom class. For a custom class, the input requests an object of that class.

- `listExpr`

    An [expression](Expression.md) whose values are offered for selection. Used for a custom-class input to list candidate objects (the value class of `listExpr` determines the inline editor).

- `expr`

    An [expression](Expression.md), which value determines the [initial value](../paradigm/Value_input.md#initial) of the input.

- `alias`

    The name of the local parameter to which the input result is written. [Simple ID](IDs.md#id).

- `CHANGE`

    A keyword specifying that in addition to the value input the result needs to be written to the specified property.

- `changeExpr`

    An [expression](Expression.md) that determines the property to which the input result is written. By default, the property specified as the initial input value is used.

- `actionOperator`

    A [context-dependent action operator](Action_operators.md#contextdependent) that is executed if the input was completed successfully.

- `elseActionOperator`

    A [context-dependent action operator](Action_operators.md#contextdependent) that is executed if the input was cancelled. The input result parameter cannot be used as parameters.

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
```
