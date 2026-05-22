---
slug: "/Brackets_operator"
title: 'Оператор []'
---

Оператор `[]` - создание [свойства](../paradigm/Properties.md), возвращающего объект из [структуры](../paradigm/Structure_operators_STRUCT.md).

### Синтаксис

```
expr [ number ]
```

Где, `[` и `]` - это обычные квадратные скобки.

### Описание

Оператор `[]` создает свойство, принимающее на вход структуру, и возвращающее один из объектов этой структуры. Доступ к объектам происходит с помощью порядкового номера объекта. 

### Параметры

- `expr`

    [Выражение](Expression.md), значением которого должна являться структура.

- `number`

    Порядковый номер объекта. [Числовой литерал](Literals.md#intliteral). Должен быть в интервале `[1..N]`, где `N` - количество объектов в структуре.

### Примеры

```lsf
CLASS Letter;
attachment1 = DATA FILE (Letter);
attachment2 = DATA FILE (Letter);
letterAttachments (Letter l) = STRUCT(attachment1(l), attachment2(l));
secondAttachment(Letter l) = letterAttachments(l)[2]; // возвращает attachment2
```
