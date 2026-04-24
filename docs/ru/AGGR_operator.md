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

`eventClause` задаёт базовое [событие](Events.md) проверки; `NEW` и `DELETE` — события разрешения для создания и удаления агрегируемых объектов соответственно.

В отличие от других контекстно-зависимых операторов, оператор `AGGR` нельзя использовать в [выражениях](Expression.md) внутри других операторов (в этом смысле он больше похож на контекстно-независимые операторы), а также в [операторе `JOIN`](JOIN_operator.md) (внутри `[= ]`)

### Параметры

- `eventClause`

    [Блок описания события](Event_description_block.md). Базовое событие проверки. По умолчанию — глобальное `APPLY`.

- `aggrClass`

    Класс значения агрегируемого объекта. [Составной идентификатор](IDs.md#cid). Должен быть пользовательским [классом](Classes.md); встроенные классы не допускаются.

- `aggrExpr`

    [Выражение](Expression.md), не-`NULL` значения которого задают агрегацию; его типизированные параметры определяют параметры свойства-результата и автоматически создаваемых свойств для каждого параметра.

- `NEW`

    Ключевое слово. Задаёт событие разрешения для создания агрегируемых объектов.

- `newEventClause`

    [Блок описания события](Event_description_block.md). Если `NEW` не указан, событие разрешения наследует от `eventClause` только область видимости (`GLOBAL`/`LOCAL`); его `FORMS`, `AFTER`/`GOAFTER` и имя события не переносятся. Если `NEW` указан без блока — используется глобальное `APPLY`.

- `DELETE`

    Ключевое слово. Задаёт событие разрешения для удаления агрегируемых объектов.

- `deleteEventClause`

    [Блок описания события](Event_description_block.md). Если `DELETE` не указан, событие разрешения наследует от `eventClause` только область видимости (`GLOBAL`/`LOCAL`); его `FORMS`, `AFTER`/`GOAFTER` и имя события не переносятся. Если `DELETE` указан без блока — используется глобальное `APPLY`.

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

// базовое LOCAL-событие: все три события выполняются в рамках сессии
sessionAggr(Invoice invoice) = AGGR LOCAL ShipmentInvoice WHERE createShipment(invoice);

// явный блок события NEW: создание выполняется локально, удаление наследует базовое (глобальное) событие
newLocalAggr(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice) NEW LOCAL;

// раздельные события: создание — глобально (пустой блок NEW трактуется как APPLY), удаление — локально
splitAggr(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice) NEW DELETE LOCAL;
```
