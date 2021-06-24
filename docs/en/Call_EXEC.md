---
title: 'Call (EXEC)'
---

The `EXEC` operator creates an [action](Actions.md) that executes another action, passing it specified properties (*arguments*).

### Language

[Syntax of the `EXEC` operator](EXEC_operator.md).

### Examples

```lsf
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}                                    // declared above action importData with two parameters

order = DATA Order (OrderDetail) NONULL DELETE;
runImport(OrderDetail d)  { importData(sku(d), order(d)); } // declaration of the action runImport that will call importData
```


  
