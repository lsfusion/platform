---
slug: "/Comparison_operators"
title: 'Операторы сравнения'
---

Операторы `==`, `=`, `!=`, `<`, `>`, `<=`, `>=` - создание [свойств](../paradigm/Properties.md), реализующих [операции сравнения](../paradigm/Comparison_operators_=_etc.md).

### Синтаксис

```
expression1 == expression2
expression1 = expression2
expression1 != expression2
expression1 < expression2
expression1 > expression2
expression1 <= expression2
expression1 >= expression2
```

### Параметры

- `expression1, expression2`

    [Выражения](Expression.md), значения которых будут являться аргументами операторов сравнения.

### Примеры

```lsf
equalBarcodes = barcode(a) == barcode(b);
outOfIntervalValue1(value, left, right) = value < left OR value > right;
outOfIntervalValue2(value, left, right) = NOT (value >= left AND value <= right);
```
