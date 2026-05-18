---
title: 'Call (EXEC)'
---

The `EXEC` operator creates an [action](Actions.md) that executes another action, passing it specified argument values.

If the executed action has a [result](Actions.md), that result can be written into a property. If the result depends on additional parameters, the target property must have the same parameter classes.

Actions that return a result can also be used [inside expressions](Expression.md) in an action body — in that case the result is substituted into the expression at the position of the call.

### Language

[Syntax of the `EXEC` operator](EXEC_operator.md).

### Examples

```lsf
// declaration of importData action with two parameters
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}

order = DATA Order (OrderDetail) NONULL DELETE;
// declaration of the action runImport that calls importData
runImport(OrderDetail d)  { importData(sku(d), order(d)); }

// calling an action with a result and writing it into a property
getPrice (Item i) ABSTRACT NUMERIC[10,2];
currentPrice = DATA LOCAL NUMERIC[10,2] ();

showPrice (Item i)  {
    getPrice(i) TO currentPrice;
    MESSAGE 'Price: ' + currentPrice();
}
```


  
