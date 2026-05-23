---
slug: "/Type_conversion_operator"
title: 'Оператор преобразования типа'
---

Оператор преобразования типа - создание [свойства](../paradigm/Properties.md), реализующего [преобразование типа](../paradigm/Type_conversion.md).

### Синтаксис

```
typeName(expression) 
```

### Описание

Оператор создаёт свойство, которое преобразует значение некоторого выражения в значение указанного [встроенного класса](../paradigm/Built-in_classes.md). Если преобразование типа невозможно, значением свойства будет `NULL`.

### Параметры

- `typeName`

    Имя [встроенного класса](../paradigm/Built-in_classes.md), в который будут преобразовываться значения.

- `expression`

    [Выражение](Expression.md), значение которого будет преобразовано к значению указанного встроенного класса.

### Примеры

```lsf
itemCount = DATA INTEGER (Store);
itemCountToString(s) = BPSTRING[10](itemCount(s));

barcode = DATA STRING[15] (Item);
longBarcode(Item i) = LONG(barcode(i));
```
