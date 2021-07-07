---
title: 'INPUT operator'
---

The `INPUT` operator creates an [action](Actions.md) that [inputs a primitive](Primitive_input_INPUT.md).

### Syntax

    INPUT inputOptions 
    [CHANGE [= changeExpr]]
    [DO actionOperator [ELSE elseActionOperator]]

`inputOptions` - input options. Specified by one of the following syntaxes:

    [alias =] builtInClassName
    [alias] = expr

### Description

The `INPUT` operator creates an action which allows to request the value of one of the [built-in classes](Built-in_classes.md) from the user.

### Parameters

- `builtInClassName`

    The name of one of the [built-in classes](Built-in_classes.md). 

- `expr`

    An [expression](Expression.md), which value determines the [initial value](Value_input.md#initial) of the input.

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
