---
slug: "/Primitive_input_INPUT"
title: 'Value input (INPUT)'
---

The *value input* operator creates an [action](Actions.md) that requests the user to [input a value](Value_input.md). The requested value can be an object of a [built-in class](Built-in_classes.md) (for example, a string, a number, a date, or a file) or an object of a custom class. The user can [cancel the input](Value_input.md#result), for example by pressing the `Esc` key on the keyboard.

This operator can only be used in property [change event](Form_events.md#property) handlers on a form.

### Requested value {#value}

If an object of a built-in class is requested, the platform shows the user an [input control](Value_input.md#execution) matching that class, and the user enters it directly.

If an object of a custom class is requested, the user does not enter it directly but selects it from the class's [selection form](Interactive_view.md#edtClass).

### Offered values {#list}

The values offered to the user can be limited to a specific set, and that set can be narrowed further by a condition. For a built-in class, such a set is shown to the user as a list of suggestions during input; for a custom class, it limits the objects available for selection.

In addition, if the input is used to [change](Value_input.md#initial) a property, the offered values are by default further limited to those whose selection will not break any [constraint](Constraints.md) existing in the system — the same way as in the [dialog form](In_an_interactive_view_SHOW_DIALOG.md#dialog) of form opening. This limitation can be disabled if needed.

### Common input capabilities

As with other value input operators, this operator allows to:

-   specify [initial object values](Value_input.md#initial)
-   specify [main and alternative](Value_input.md#result) actions. The first is called if the input was successfully completed, the second if not (i.e. if the input was canceled).
-   [change](Value_input.md#initial) a specified property

### Language

The syntax of the value input operator is described by the [`INPUT` operator](../language/INPUT_operator.md).

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

// selecting an object of a custom class: offering skus by their name and keeping only those in stock
changeSku (OrderDetail d)  {
    INPUT s = sku(d) CHANGE LIST name(s) WHERE inStock(s);
}
```
