---
title: 'Вызов (EXEC)'
---

Оператор `EXEC` создает [действие](Actions.md), которое выполняет другое действие, передавая ему на вход заданные свойства (*аргументы*).

### Язык

[Cинтаксис оператора `EXEC`](EXEC_operator.md).

### Примеры

```lsf
// объявление действие importData с двумя параметрами
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}

order = DATA Order (OrderDetail) NONULL DELETE;
// объявление действия runImport, которое будет вызывать importData
runImport(OrderDetail d) { importData(sku(d), order(d)); } 
```


 
