---
slug: "/Type_conversion_operator"
title: 'Оператор преобразования типа'
---

Оператор преобразования типа создаёт [свойство](../paradigm/Properties.md), реализующее [преобразование типа](../paradigm/Type_conversion.md).

### Синтаксис

```
className(expr)
```

### Описание

Оператор создаёт свойство, значением которого является значение `expr`, преобразованное к [встроенному классу](../paradigm/Built-in_classes.md) `className`. Какие преобразования имеют смысл и когда результатом является `NULL`, определяется абстракцией [преобразования типа](../paradigm/Type_conversion.md).

Для пользовательских классов преобразование не определено; значение, суженное до пользовательского класса, возвращает оператор [`AS`](IS_AS_operators.md).

### Параметры

- `className`

    Целевой [встроенный класс](../paradigm/Built-in_classes.md), к которому преобразуется значение. Может быть указан любой встроенный класс, в том числе параметризованный, записанный вместе со своими параметрами (например, `STRING[15]`, `BPSTRING[10]` или `NUMERIC[10,2]`).

- `expr`

    [Выражение](Expression.md), значение которого преобразуется.

### Примеры

```lsf
itemCount = DATA INTEGER (Store);
itemCountToString(s) = BPSTRING[10](itemCount(s));

barcode = DATA STRING[15] (Item);
longBarcode(Item i) = LONG(barcode(i));
```
