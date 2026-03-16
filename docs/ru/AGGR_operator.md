---
title: 'Оператор AGGR'
---

Оператор `AGGR` - создание [агрегации](Aggregations.md).

### Синтаксис  

```
AGGR [eventClause] aggrClass WHERE aggrExpr [NEW [newEventClause]] [DELETE [deleteEventClause]]
```

### Описание

Помимо свойства — результата оператора, содержащего значение агрегируемого объекта, — оператор `AGGR` для каждого параметра создаёт [первичное свойство](Data_properties_DATA.md) с одним параметром типа `aggrClass`. Имя и класс значения этого свойства совпадают с именем и классом соответствующего параметра; при создании агрегируемого объекта в него автоматически записывается значение параметра.

`eventClause` задаёт базовое [событие](Events.md) проверки; `NEW` и `DELETE` — события разрешения для создания и удаления агрегируемых объектов соответственно. Если `NEW` или `DELETE` не указан, соответствующее событие разрешения наследует область видимости базового события. Если ключевое слово указано без блока описания события, используется глобальное событие `APPLY`.

:::info
Создание агрегации во многом аналогично следующим инструкциям (пример для 2 параметров):

```lsf
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
```

но является более декларативной и читабельной инструкцией, поэтому рекомендуется использовать именно ее
:::

В отличие от других контекстно-зависимых операторов, оператор `AGGR` нельзя использовать в [выражениях](Expression.md) внутри других операторов (в этом смысле он больше похож на контекстно-независимые операторы), а также в [операторе `JOIN`](JOIN_operator.md) (внутри `[= ]`)

### Параметры

- `eventClause`

    [Блок описания события](Event_description_block.md). Базовое событие проверки. По умолчанию — глобальное `APPLY`.

- `aggrClass`

    Класс значения агрегируемого объекта. [Составной идентификатор](IDs.md#cid).

- `aggrExpr`

    [Выражение](Expression.md), значение которого определяет агрегируемое свойство.

- `NEW`

    Ключевое слово. Задаёт событие разрешения для создания агрегируемых объектов.

- `newEventClause`

    [Блок описания события](Event_description_block.md). Если `NEW` не указан — наследуется от `eventClause`. Если `NEW` указан без блока — используется глобальное `APPLY`.

- `DELETE`

    Ключевое слово. Задаёт событие разрешения для удаления агрегируемых объектов.

- `deleteEventClause`

    [Блок описания события](Event_description_block.md). Если `DELETE` не указан — наследуется от `eventClause`. Если `DELETE` указан без блока — используется глобальное `APPLY`.

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

// базовое LOCAL-событие: создание/удаление обрабатывается в рамках сессии
sessionAggr(Invoice invoice) = AGGR ShipmentInvoice LOCAL WHERE createShipment(invoice);

// раздельные события: создание — глобально (по умолчанию), удаление — локально
splitAggr(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice) NEW DELETE LOCAL;
```
