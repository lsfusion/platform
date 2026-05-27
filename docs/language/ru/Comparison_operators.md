---
slug: "/Comparison_operators"
title: 'Операторы сравнения'
---

Операторы `==`, `=`, `!=`, `<`, `>`, `<=`, `>=` создают [свойства](../paradigm/Properties.md), реализующие [операции сравнения](../paradigm/Comparison_operators_=_etc.md).

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

### Описание

Формы `=` и `==` эквивалентны. Каждый оператор принимает два операнда и не может быть сцеплен — запись `expression1 < expression2 < expression3` недопустима. Порядок вычисления относительно других операторов определяется [приоритетом операторов](Operator_priority.md).

### Параметры

- `expression1, expression2`

    [Выражения](Expression.md), значения которых будут являться аргументами операторов сравнения.

### Примеры

```lsf
equalBarcodes = barcode(a) == barcode(b);
outOfIntervalValue1(value, left, right) = value < left OR value > right;
outOfIntervalValue2(value, left, right) = NOT (value >= left AND value <= right);
```
