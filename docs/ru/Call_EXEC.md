---
title: 'Вызов (EXEC)'
---

Оператор `EXEC` создает [действие](Actions.md), которое выполняет другое действие, передавая ему на вход заданные свойства (*аргументы*).

### Язык

[Cинтаксис оператора `EXEC`](EXEC_operator.md).

### Примеры

```lsf
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}                                    // объявленное выше действие importData с двумя параметрами

order = DATA Order (OrderDetail) NONULL DELETE;
runImport(OrderDetail d)  { importData(sku(d), order(d)); } // объявление действия runImport, которое будет вызывать importData
```


 
