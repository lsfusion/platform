---
title: 'Оператор EXEC'
---

Оператор `EXEC` - создание [действия](Actions.md), [выполняющего](Call_EXEC.md) другое действие.

### Синтаксис

    [EXEC] actionId(expression1, ..., expressionN)

### Описание

Оператор `EXEC` создает действие, которое выполняет другое действие, передавая ему значения [выражений](Expression.md) в качестве параметров.

### Параметры

- `actionId`

    [Идентификатор действия](IDs.md#propertyid). 

- `expression1, ..., expressionN`

    Список выражений, значения которых будут передаваться выполняемому действию в качестве аргументов. Количество выражений должно соответствовать количеству параметров выполняемого действия.

- `operator`

    Оператор, создающий выполняемое действие.

### Примеры

```lsf
// объявление действие importData с двумя параметрами
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}

order = DATA Order (OrderDetail) NONULL DELETE;
// объявление действия runImport, которое будет вызывать importData
runImport(OrderDetail d)  { importData(sku(d), order(d)); } 
```
