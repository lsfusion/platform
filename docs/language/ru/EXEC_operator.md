---
slug: "/EXEC_operator"
title: 'Оператор EXEC'
---

Оператор `EXEC` создает [действие](../paradigm/Actions.md), [выполняющее](../paradigm/Call_EXEC.md) другое действие.

### Синтаксис

```
[EXEC] actionId(expression1, ..., expressionN) [TO toProperty]
```

### Описание

Оператор `EXEC` создает действие, которое выполняет другое действие, передавая ему значения [выражений](Expression.md) в качестве параметров. Если у выполняемого действия есть [результат](../paradigm/Actions.md), его можно записать в свойство, указанное после `TO`.

### Параметры

- `actionId`

    [Идентификатор действия](IDs.md#propertyid). 

- `expression1, ..., expressionN`

    Список выражений, значения которых будут передаваться выполняемому действию в качестве аргументов. Количество выражений должно соответствовать количеству параметров выполняемого действия.

- `toProperty`

    Опциональный [идентификатор свойства](IDs.md#propertyid). Если указан, значение, возвращаемое выполняемым действием, записывается в это свойство. Класс возвращаемого значения и сигнатура `toProperty` должны соответствовать классу результата и параметрам результата выполняемого действия.

### Примеры

```lsf
// объявление действия importData с двумя параметрами
importData(Sku sku, Order order)  {
    MESSAGE 'Run import for ' + id(sku) + ' ' + customer(order);
}

order = DATA Order (OrderDetail) NONULL DELETE;
// объявление действия runImport, которое будет вызывать importData
runImport(OrderDetail d)  { importData(sku(d), order(d)); }

// вызов действия с результатом и запись результата через TO
getPrice (Item i) ABSTRACT NUMERIC[10,2];
currentPrice = DATA LOCAL NUMERIC[10,2] ();

showPrice (Item i)  {
    getPrice(i) TO currentPrice;
    MESSAGE 'Price: ' + currentPrice();
}

// запись результата, зависящего от дополнительного параметра
captionByLanguage (Item i) ABSTRACT STRING[100] (Language);
caption = DATA LOCAL STRING[100] (Language);

fillCaption (Item i)  {
    captionByLanguage(i) TO caption;
}
```
