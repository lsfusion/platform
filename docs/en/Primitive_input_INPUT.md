---
title: 'Primitive input (INPUT)'
---

The  *primitive input* operator creates an [action](Actions.md) that requests the client to [input a value](Value_input.md) [of a builtin class](Built-in_classes.md). The user can [cancel the input](Value_input.md#result), for example by pressing the `Esc` key on the keyboard.

As with other value input operators, this operator allows to:

-   specify [initial object values](Value_input.md#initial)
-   specify [main and alternative](Value_input.md#result) actions. The first is called if the input was successfully completed, the second if not (i.e. if the input was canceled).
-   [change](Value_input.md#initial) a specified property

This operator can only be used in property [change event](Form_events.md#property) handlers on a form.

### Language

The syntax of the primitive input operator is described by the [`INPUT` operator](INPUT_operator.md).

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
