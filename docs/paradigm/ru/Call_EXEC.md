---
slug: "/Call_EXEC"
title: 'Вызов (EXEC)'
---

*Оператор вызова действия* создает [действие](Actions.md), которое выполняет другое действие, передавая ему заданные значения аргументов для его параметров.

Если у вызываемого действия есть [результат](Actions.md), этот результат можно записать в свойство в точке вызова. Если сам результат является свойством от дополнительных параметров, это свойство должно иметь такие же классы параметров.

Действия, возвращающие результат, можно также использовать как значение — результат подставляется в позиции вызова.

### Язык

Синтаксис оператора вызова действия описывается [оператором `EXEC`](../language/EXEC_operator.md).

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
