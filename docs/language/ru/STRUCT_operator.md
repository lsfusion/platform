---
slug: "/STRUCT_operator"
title: 'Оператор STRUCT'
---

Оператор `STRUCT` - создание [свойства](../paradigm/Properties.md), которое создает [структуру](../paradigm/Structure_operators_STRUCT.md).

### Синтаксис

```
STRUCT(expr1, ..., exprN)
```

### Описание

Оператор `STRUCT` создает свойство, значением которого является [структура](../paradigm/Structure_operators_STRUCT.md), составленная из значений операндов в порядке их перечисления.

### Параметры

- `expr1, ..., exprN`

    Список [выражений](Expression.md), значения которых становятся элементами структуры. Список не может быть пустым.

### Примеры

```lsf
objectStruct(a, b) = STRUCT(a, f(b));
stringStruct() = STRUCT(1, 'two', 3.0);
```
