---
title: 'Оператор AGGR'
---

Оператор `AGGR` - создание [агрегации](Aggregations.md).

### Синтаксис  

    AGGR aggrClass WHERE aggrExpr

### Описание

Помимо свойства, которое является результатом этого оператора и содержит значение агрегируемого объекта, оператор `AGGR` также для каждого параметра создает первичное свойство с одним параметром, [класс](User_classes.md) которого равен классу агрегируемого объекта. Класс значения и имя этого свойства равны классу и имени параметра, для которого создается это свойство. Соответственно, при создании агрегируемого объекта в это свойство автоматически записывается значение параметра, для которого создается агрегируемый объект.

:::info
Создание агрегации во многом аналогично следующим инструкциям (пример для 2 параметров):

    prm1 = DATA class1 (aggrClass);
    prm2 = DATA class2 (aggrClass);
    result = GROUP AGGR aggrClass aggrObject BY prm1(aggrObject), prm2(aggrObject);

    // если aggrExpr становится не null, создаем объект класса aggrClass (эквивалентно whereExpr => result(prm1, prm2) RESOLVE LEFT)
    WHEN SET(aggrExpr) AND NOT result(prm1, prm2)
        NEW aggrObject = aggrClass {
            prm1(aggrObject) <- prm1;
            prm2(aggrObject) <- prm2;
        }

    // если aggrExpr становится null, удаляем объект (эквивалентно aggrClass aggrObject IS aggrClass => result(prm1(aggrObject),prm2(aggrObject)) RESOLVE RIGHT)
    WHEN aggrClass aggrObject IS aggrClass AND DROPPED(result(prm1(aggrObject),prm2(aggrObject))) DO
        DELETE aggrObject;

но является более декларативной и читабельной инструкцией, поэтому рекомендуется использовать именно ее
:::

В отличии от других контекстно-зависимых операторов, оператор `AGGR` нельзя использовать в [выражениях](Expression.md) внутри других операторов (в этом смысле он больше похож на контекстно-независимые операторы), а также в [операторе `JOIN`](JOIN_operator.md) (внутри `[= ]`)

### Параметры

- `aggrClass`

    Класс значения агрегируемого объекта.

- `aggrExpr`

    [Выражение](Expression.md), значение которого определяет агрегируемое свойство.

### Примеры

```lsf
CLASS A; CLASS B; CLASS C;
f = DATA INTEGER (A, B);
c = AGGR C WHERE f(A a, B b) MATERIALIZED INDEXED;

CLASS AB;
ab = AGGR AB WHERE A a IS A AND B b IS B; // для каждой пары A B создает объект AB

CLASS Shipment 'Поставка';
date = ABSTRACT DATE (Shipment);
CLASS Invoice 'Инвойс';
createShipment 'Создавать поставку' = DATA BOOLEAN (Invoice);
date 'Дата накладной' = DATA DATE (Invoice);
CLASS ShipmentInvoice 'Поставка по инвойсу' : Shipment;
// создаем поставку по инвойсу, если для инвойса задана опция создавать поставку
shipment(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice); 
date(ShipmentInvoice si) += sum(date(invoice(si)),1); // дата поставки = дата инвойса + 1
```
