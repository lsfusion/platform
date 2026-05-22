---
slug: "/Call_EXEC"
title: 'Вызов (EXEC)'
---

Оператор `EXEC` создает [действие](Actions.md), которое выполняет другое действие, передавая ему заданные значения аргументов.

Если у выполняемого действия есть [результат](Actions.md), этот результат можно записать в свойство. Если этот результат зависит от дополнительных параметров, целевое свойство должно иметь такие же классы параметров.

Действия, возвращающие результат, можно также использовать [внутри выражений](../language/Expression.md) в теле действия — в этом случае результат подставляется в выражение в позиции вызова.

### Язык

[Cинтаксис оператора `EXEC`](../language/EXEC_operator.md).

### Примеры

```lsf
// объявление действие importData с двумя параметрами
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}

order = DATA Order (OrderDetail) NONULL DELETE;
// объявление действия runImport, которое будет вызывать importData
runImport(OrderDetail d) { importData(sku(d), order(d)); }

// вызов действия с результатом и запись его в свойство
getPrice (Item i) ABSTRACT NUMERIC[10,2];
currentPrice = DATA LOCAL NUMERIC[10,2] ();

showPrice (Item i)  {
    getPrice(i) TO currentPrice;
    MESSAGE 'Price: ' + currentPrice();
}
```


 
