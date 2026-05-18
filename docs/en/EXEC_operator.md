---
title: 'EXEC operator'
---

The `EXEC` operator creates an [action](Actions.md) that [executes](Call_EXEC.md) another action.

### Syntax

```
[EXEC] actionId(expression1, ..., expressionN) [TO toProperty]
```

### Description

The `EXEC` operator creates an action that executes another action, passing it the values of [expressions](Expression.md) as parameters. If the executed action has a [result](Actions.md), it can be written into the property specified after `TO`.

### Parameters

- `actionId`

    [Action ID](IDs.md#propertyid). 

- `expression1, ..., expressionN`

    A list of expressions whose values will be passed to the action being executed as arguments. The number of expressions must be equal to the number of parameters of the action being executed.

- `toProperty`

    Optional [property ID](IDs.md#propertyid). If specified, the value returned by the executed action is written to this property. The value class and signature of `toProperty` must match the result class and the result parameters of the executed action.

### Examples

```lsf
// declaration of importData action with two parameters
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}

order = DATA Order (OrderDetail) NONULL DELETE;
// declaration of the action runImport that calls importData
runImport(OrderDetail d)  { importData(sku(d), order(d)); }

// calling an action with a result and writing the result via TO
getPrice (Item i) ABSTRACT NUMERIC[10,2];
currentPrice = DATA LOCAL NUMERIC[10,2] ();

showPrice (Item i)  {
    getPrice(i) TO currentPrice;
    MESSAGE 'Price: ' + currentPrice();
}

// writing a result that depends on an extra parameter
captionByLanguage (Item i) ABSTRACT STRING[100] (Language);
caption = DATA LOCAL STRING[100] (Language);

fillCaption (Item i)  {
    captionByLanguage(i) TO caption;
}
```
