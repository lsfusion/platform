---
slug: "/Brackets_operator"
title: 'Оператор []'
---

Оператор `[]` - создание [свойства](../paradigm/Properties.md), возвращающего объект из [структуры](../paradigm/Structure_operators_STRUCT.md).

### Синтаксис

```
expr[n]
```

Где, `[` и `]` - это обычные квадратные скобки.

### Описание

Оператор `[]` создает свойство, принимающее на вход структуру и возвращающее объект [структуры](../paradigm/Structure_operators_STRUCT.md), стоящий на позиции, заданной числом `n`.

### Параметры

- `expr`

    [Выражение](Expression.md), значением которого должна являться структура.

- `n`

    Позиция объекта внутри структуры. Положительный [числовой литерал](Literals.md#intliteral): должен записываться как константа, а не вычисляться выражением. Нумерация начинается с 1, и значение должно быть в интервале `[1..N]`, где `N` - количество объектов в структуре.

### Примеры

```lsf
CLASS Letter;
attachment1 = DATA FILE (Letter);
attachment2 = DATA FILE (Letter);
letterAttachments (Letter l) = STRUCT(attachment1(l), attachment2(l));
secondAttachment(Letter l) = letterAttachments(l)[2]; // возвращает attachment2
```
