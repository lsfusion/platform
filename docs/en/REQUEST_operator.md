---
title: 'REQUEST operator'
---

The `REQUEST` operator creates an [action](Actions.md) that [requests a value](Value_request_REQUEST.md).

### Syntax

    REQUEST requestAction 
    DO doAction [ELSE elseAction]

### Description

The `REQUEST` operator creates an action that allows the separation of a request for a value from its handling.

### Parameters

- `requestAction`

    A [context-dependent action operator](Action_operators.md#contextdependent) that requests a value.

- `doAction`

    A context-dependent action operator that is executed if the input was completed successfully.

- `elseAction`

    A context-dependent action operator that is executed if the input was [cancelled](Value_input.md#result).

### Examples

```lsf
requestCustomer (Order o)  {
    LOCAL resultValue = STRING[100] ();
    REQUEST {
        ASK 'Choose from list?' DO
            DIALOG customers OBJECTS c = resultValue() CHANGE;
        ELSE
            INPUT = resultValue() CHANGE;
    } DO
        customer(o) <- resultValue();
}

FORM request
    OBJECTS o = Order
    PROPERTIES(o) customer ON CHANGE requestCustomer(o) // for example, group adjustment will be performed
;
```
