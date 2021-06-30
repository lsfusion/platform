---
title: 'AFTER statement'
---

The `AFTER` statement calls an [action](Actions.md) after calling another action. 

### Syntax

    AFTER action(param1, ..., paramN) DO aspectAction;

### Description

The `AFTER` statement defines an action (let's call it an *aspect*) that will be called after the specified action.

### Parameters

- `action`

    The [ID](IDs.md#propertyid) of the action after which the aspect will be called.

- `param1, ..., paramN`

    List of action parameter names. Each name is defined [by a simple ID](IDs.md#id). These parameters can be accessed when defining an aspect.

- `aspectAction`

    A [context-dependent action operator](Action_operators.md#contextdependent) describing the aspect.

### Examples

```lsf
changePrice(Sku s, DATE d, NUMERIC[10,2] price)  { price(s, d) <- price; }
AFTER changePrice(Sku s, DATE d, NUMERIC[10,2] price) DO MESSAGE 'Price was changed'; // A message will be shown after each call to changePrice
```
