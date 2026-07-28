---
slug: "/FILTER_ORDER_operators"
title: 'Операторы фильтров и порядков формы'
---

Операторы `ORDER`, `FILTER`, `FILTERGROUP`, `FILTER PROPERTY` (и их варианты `ORDERS`, `FILTERS`, `FILTERGROUPS`, `FILTERS PROPERTY`) - создание [действий](../paradigm/Actions.md), которые применяют или читают текущие пользовательские фильтры и порядки элементов открытой формы.

### Синтаксис

```
ORDER            groupObjectId   [FROM expr]
FILTER           groupObjectId   [FROM expr]
FILTERGROUP      filterGroupId   [FROM expr]
FILTER PROPERTY  formPropertyId  [FROM expr]

ORDERS           groupObjectId   [TO propId]
FILTERS          groupObjectId   [TO propId]
FILTERGROUPS     filterGroupId   [TO propId]
FILTERS PROPERTY formPropertyId  [TO propId]
```

### Описание

Операторы работают с [интерактивным представлением](../paradigm/Interactive_view.md) открытой формы и делятся на две группы:

- операторы в единственном числе (`ORDER`, `FILTER`, `FILTERGROUP`, `FILTER PROPERTY`) **применяют** значение из блока `FROM` к элементу формы — как если бы пользователь сам задал этот порядок или фильтр;
- операторы во множественном числе (`ORDERS`, `FILTERS`, `FILTERGROUPS`, `FILTERS PROPERTY`) **читают** текущее значение элемента формы в свойство из блока `TO`.

Элемент формы задаётся его именем: `groupObjectId` — группа объектов (её порядки или фильтры), `filterGroupId` — группа фильтров, `formPropertyId` — свойство на форме (его фильтр).

Значение представлено в сериализованном виде, зависящем от элемента:

- для порядков группы объектов (`ORDER` / `ORDERS`) — `JSON`: список порядков (имя свойства и признак убывания);
- для фильтров группы объектов (`FILTER` / `FILTERS`) — `JSON`: список условий фильтрации (имя свойства, сравнение, отрицание, значение, объединение через `OR`);
- для группы фильтров (`FILTERGROUP` / `FILTERGROUPS`) — `INTEGER`: номер активного фильтра в группе;
- для фильтра свойства (`FILTER PROPERTY` / `FILTERS PROPERTY`) — `STRING`: значение фильтра свойства.

Если блок `FROM` или `TO` не указан, по умолчанию используется соответствующее свойство системного модуля [`UserEvents`](../paradigm/System_UserEvents.md) (`orders`, `filters`, `filterGroups`, `filtersProperty`), через который эти операторы обычно и вызываются.

### Параметры

- `groupObjectId`

    [Идентификатор группы объектов](IDs.md#groupobjectid) на форме.

- `filterGroupId`

    Имя [группы фильтров](../paradigm/Interactive_view.md#filtergroup) на форме, квалифицированное формой.

- `formPropertyId`

    [Идентификатор свойства или действия на форме](IDs.md#formpropertyid).

- `expr`

    [Выражение](Expression.md), значение которого применяется к элементу формы. Его класс определяет вид сериализации (см. «Описание»).

- `propId`

    [Идентификатор свойства](IDs.md#propertyid) без параметров, в которое записывается прочитанное значение.

### Примеры

```lsf
FORM orders
    OBJECTS o = Order
    PROPERTIES(o) number, date, customer

    FILTERGROUP amount
        FILTER 'Крупные' number(o) > 1000
        FILTER 'Мелкие' number(o) <= 1000
;

savedFilters = DATA JSON ();
currentOrders = DATA JSON ();

// сохранить текущие фильтры группы объектов o и позже применить их обратно;
// savedFilters получит JSON вида [{"property": "number", "compare": ">", "value": 1000, "negation": false, "or": false}]
saveFilters ()  { FILTERS orders.o TO savedFilters; }
restoreFilters ()  { FILTER orders.o FROM savedFilters; }

// прочитать текущий порядок группы объектов o;
// currentOrders получит JSON вида [{"property": "date", "desc": true}, {"property": "number", "desc": false}]
readOrders ()  { ORDERS orders.o TO currentOrders; }

// активировать второй фильтр («Мелкие») в группе фильтров amount (нумерация с нуля)
showSmall ()  { FILTERGROUP orders.amount FROM 1; }

// отфильтровать свойство customer по значению 'Acme'
filterAcme ()  { FILTER PROPERTY orders.customer FROM 'Acme'; }
```