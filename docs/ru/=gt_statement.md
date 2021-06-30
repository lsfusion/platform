---
title: 'Инструкция =>'
---

Инструкция `=>` - создание [следствия](Simple_constraints.md).

### Синтаксис

    leftPropertyId(param1, ..., paramN) => eventClause rightExpr [RESOLVE resolveType];

### Описание

Инструкция `=>` создает следствие. Этот оператор может объявлять свои локальные параметры при задании свойства посылки следствия. Затем эти параметры могут быть использованы в выражении следствия.

При создании следствия будет создано [ограничение](Constraints.md), которое во многом будет эквивалентно следующей инструкции

    CONSTRAINT eventClause leftPropertyId(param1, ..., paramN) AND NOT rightExpr MESSAGE 'Нарушено следствие';

но при этом позволяет автоматически разрешать ситуации нарушения этого ограничения. Так тип разрешения `RESOLVE LEFT` будет эквивалентен созданию [простого события](Simple_event.md):

    WHEN eventClause SET(leftPropertyId(param1, ..., paramN)) DO 
        SETACTION(rightExpr);

А `RESOLVE RIGHT` соответственно:

    WHEN eventClause DROPPED(rightExpr) DO
        DROPACTION(leftPropertyId(param1, ..., paramN));

### Параметры

- `leftPropertyId`

    [Идентификатор свойства](IDs.md#propertyid), задающего посылку следствия.

- `param1, ..., paramN`

    Список [параметров](IDs.md#paramid) свойства, задающего посылку следствия. Количество этих параметров должно совпадать с количеством параметров свойства.

- `rightExpr`

    [Выражение](Expression.md), значение которого определяет следствие.

- `resolveType`

    Тип [автоматического разрешения](Simple_event.md) при нарушении следствия. Задается одним из следующих вариантов:

    - `LEFT` - если посылка (левая часть инструкции) изменяется на не `NULL`, то следствие изменяется на не `NULL`.
    - `RIGHT` -  если следствие (правая часть инструкции) изменяется на `NULL`, то посылка изменяется на `NULL`.
    - `LEFT RIGHT` - аналогично `LEFT` и `RIGHT` вместе. 

- `eventClause`

    [Блок описания события](Event_description_block.md). Описывает [событие](Events.md), при наступлении которого будет проверяться создаваемое следствие и выполняться операции автоматического разрешения.

### Примеры


```lsf
is(Sku s) = s IS Sku;
// для товара должны быть заданы штрих-код и наименование
is(Sku s) => barcode(s);
is(Sku s) => name(s);


CLASS Invoice;
CLASS InvoiceLine;
invoice = DATA Invoice (InvoiceLine);
is(InvoiceLine l) = l IS InvoiceLine;
// для строки документа должен быть задан документ, и при удалении документа, чтобы удалялись строки этого документа
is(InvoiceLine l) => invoice(l) RESOLVE RIGHT;
// равносильно объявлению document = DATA Invoice (InvoiceLine) NONULL DELETE;

// агрегация для f(a,b) создавать объект класса x, у которого свойство a(x) равняется a, а свойство b(x) равняется b
CLASS A;
CLASS B;
f = DATA BOOLEAN (A, B);

CLASS X;
a = DATA A(X);
b = DATA B(X);
is (X x) = x IS X;

f(a,b) => [ GROUP AGGR X x WHERE x IS X BY a(x), b(x)](a,b) RESOLVE LEFT;
is(X x) => f(a(x), b(x)) RESOLVE RIGHT;
```

