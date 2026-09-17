---
slug: "/BEFORE_statement"
title: 'BEFORE statement'
---

The `BEFORE` statement calls an [action](../paradigm/Actions.md) before calling another action. 

### Syntax

```
BEFORE action(param1, ..., paramN) DO aspectAction;
```

### Description

The `BEFORE` statement defines an action (let's call it an *aspect*) that will be called before the specified one.

The aspect receives the same parameter values as the main action and runs before its body on every call of that action. If the aspect ends with the [`RETURN` operator](RETURN_operator.md), the body of the main action is not executed, nor are its remaining aspects, including the aspects of the [`AFTER` statement](AFTER_statement.md) - this is how an aspect cancels the call, for example after checking a condition; for the calling action the call still completes normally, and it continues with the next action. Several aspects of one action run in the order of their declaration.

### Parameters

- `action`

    The [ID](IDs.md#propertyid) of the action before which the aspect will be called.

- `param1, ..., paramN`

    List of action parameter names. Each name is defined [by a simple ID](IDs.md#id). These parameters can be accessed while defining an aspect.

- `aspectAction`

    A [context-dependent action operator](Action_operators.md#contextdependent) describing the aspect.

### Examples

```lsf
changeName(Sku s, STRING[100] name)  { name(s) <- name; }

// The message will be shown before each call to changeName
BEFORE changeName(Sku s, STRING[100] name) DO MESSAGE 'Changing user name'; 

quantity = DATA NUMERIC[16,3] (Sku);
deleteSku(Sku s)  { DELETE s; }

// A check before deletion: with a positive balance the deletion is not performed, and the calling action goes on
BEFORE deleteSku(Sku s) DO {
    IF quantity(s) > 0 THEN {
        MESSAGE 'Sku has a stock balance';
        RETURN;
    }
}
```
