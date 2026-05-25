---
slug: "/Call_EXEC"
title: 'Call (EXEC)'
---

The *action call* operator creates an [action](Actions.md) that executes another action, passing it specified argument values for its parameters.

If the called action has a [result](Actions.md), that result can be written into a property at the call site. When the result is itself a property of additional parameters, this property must have those same parameter classes.

Actions that return a result can also be used as a value — the result is substituted at the position of the call.

### Language

The syntax of the action call operator is described by the [`EXEC` operator](../language/EXEC_operator.md).

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
