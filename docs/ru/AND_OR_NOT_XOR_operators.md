---
title: 'Операторы AND, OR, NOT, XOR'
---

Операторы `AND`, `OR`, `NOT`, `XOR` - создание [свойств](Properties.md), реализующих [логические операции](Logical_operators_AND_OR_NOT_XOR.md).

### Синтаксис

    expression1 AND expression2
    expression1 OR expression2
    expression1 XOR expression2
    NOT expression1

### Параметры

- `expression1, expression2`

    [Выражения](Expression.md), значения которых будут являться аргументами операторов. Значения выражений будут рассматриваться как значения [класса `BOOLEAN`](Built-in_classes.md) в зависимости от того, равны они `NULL` или нет.

### Примеры

```lsf
likes = DATA BOOLEAN (Person, Person);
likes(Person a, Person b, Person c) = likes(a, b) AND likes(a, c);
outOfInterval1(value, left, right) = value < left OR value > right;
outOfInterval2(value, left, right) = NOT (value >= left AND value <= right);
```
