---
slug: "/JOIN_operator"
title: 'Оператор JOIN'
---

Оператор `JOIN` - создание [свойства](../paradigm/Properties.md), реализующего [композицию](../paradigm/Composition_JOIN.md).

### Синтаксис

```
[JOIN] mainProperty(expr1, ..., exprN)
```

Где `mainProperty` определяется как:

```
propertyId

"[" operator "]"

"[" expression "]"
```

Где `"["` и `"]"` — это обычные квадратные скобки.

### Описание

Когда главное свойство задано в скобках — как [контексто-независимый](Property_operators.md#contextindependent) оператор-свойство или [выражение](Expression.md) — свойство создается анонимно прямо по месту использования, и нет необходимости объявлять промежуточное свойство с помощью [инструкции `=`](=_statement.md).

Оператор или выражение в скобках могут использовать внешние параметры — параметры, уже объявленные в контексте, где используется оператор `JOIN`. Они передаются анонимному свойству автоматически; остальные его параметры определяются по тем же правилам, что и у свойства, заданного через `=` без явных параметров.

### Параметры

- `propertyId`

    [Идентификатор](IDs.md#propertyid) существующего свойства.

- `operator`

    Контексто-независимый оператор-свойство в скобках.

- `expression`

    Выражение в скобках.

- `expr1, ..., exprN`

    Список выражений, задающих аргументы главного свойства. Количество выражений должно быть равно количеству параметров главного свойства; список пуст, когда у главного свойства нет параметров. Внешние параметры, использованные в скобках, в это количество не входят и среди аргументов не указываются.

### Примеры

```lsf
f = DATA INTEGER (INTEGER, INTEGER, INTEGER);
g = DATA INTEGER (INTEGER, INTEGER);
h = DATA INTEGER (INTEGER, INTEGER);
c(a, b) = f(g(a, b), h(b, 3), a);

count = DATA BPSTRING[255] (INTEGER);
name = DATA BPSTRING[255] (INTEGER);
formatted(INTEGER a, INTEGER b) = [FORMULA BPSTRING[255] ' CAST($1 AS TEXT) || \' / \' || CAST($2 AS TEXT)'](count(a), name(b));
```

Иногда удобно задавать главное свойство с помощью выражения, чтобы упростить исходный код и сделать его более понятным.

```lsf
CLASS Triangle;
cathetus1 = DATA DOUBLE(Triangle);
cathetus2 = DATA DOUBLE(Triangle);

hypotenuseSq(triangle) = cathetus1(triangle)*cathetus1(triangle) + cathetus2(triangle)*cathetus2(triangle);

// аналогичное свойство, заданное с помощью композиции
hypotenuseSq2(triangle) = [ x*x + y*y](cathetus1(triangle), cathetus2(triangle)); 
```

Внешние параметры, использованные в скобках, среди аргументов не указываются.

```lsf
CLASS Book;
name = DATA STRING[100] (Book);
CLASS Order;
CLASS OrderDetail;
order = DATA Order (OrderDetail);
book = DATA Book (OrderDetail);
quantity = DATA INTEGER (OrderDetail);

// k - внешний параметр, поэтому единственный аргумент - значение x
scaledQuantity(OrderDetail d, INTEGER k) = [x*k](quantity(d));

showQuantities (Order o)  {
    // o - внешний параметр, поэтому единственный аргумент соответствует выражению BY;
    // этот аргумент объявляет новый параметр b, и цикл перебирает книги заказа
    FOR INTEGER q = [GROUP SUM quantity(OrderDetail d) IF order(d) = o BY book(d)](Book b) DO
        MESSAGE name(b) + ': ' + q;
}
```
