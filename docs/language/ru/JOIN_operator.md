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

Оператор или выражение в скобках могут использовать внешние параметры; параметры получаемого анонимного свойства определяются по тем же правилам, что и у свойства, заданного через `=` без явных параметров.

### Параметры

- `propertyId`

    [Идентификатор](IDs.md#propertyid) существующего свойства.

- `operator`

    Контексто-независимый оператор-свойство в скобках.

- `expression`

    Выражение в скобках.

- `expr1, ..., exprN`

    Список выражений, задающих аргументы главного свойства. Количество выражений должно быть равно количеству параметров главного свойства; список пуст, когда у главного свойства нет параметров.

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
