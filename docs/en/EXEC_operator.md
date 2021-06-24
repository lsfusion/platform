---
title: 'EXEC operator'
---

The `EXEC` operator creates an [action](Actions.md) that [executes](Call_EXEC.md) another action.

### Syntax

    [EXEC] actionId(expression1, ..., expressionN)

### Description

The `EXEC` operator creates an action that executes another action, passing it the values of [expressions](Expression.md) as parameters.

### Parameters

- `actionId`

    [Action ID](IDs.md#propertyid). 

- `expression1, ..., expressionN`

    A list of expressions whose values will be passed to the action being executed as arguments. The number of expressions must be equal to the number of parameters of the action being executed.

- `operator`

    An operator that creates the action being executed.

### Examples

```lsf
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}                                    // declared above action importData with two parameters

order = DATA Order (OrderDetail) NONULL DELETE;
runImport(OrderDetail d)  { importData(sku(d), order(d)); } // declaration of the action runImport that will call importData
```
