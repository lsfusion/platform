---
title: 'Call (EXEC)'
---

The `EXEC` operator creates an [action](Actions.md) that executes another action, passing it specified properties (*arguments*).

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
```


  
