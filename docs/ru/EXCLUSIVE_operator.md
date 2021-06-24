---
title: 'Оператор EXCLUSIVE'
---

Оператор `EXCLUSIVE` - создание [свойства](Properties.md), реализующего [выбор](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#exclusive) одного из значений (полиморфная форма).

### Синтаксис

    EXCLUSIVE expr1, ..., exprN

### Описание

Оператор `EXCLUSIVE` создает свойство, значением которого будет значение одного из указанных в операторе свойств. При этом предполагается, что при любом наборе параметров максимум одно из свойств не будет равняться `NULL`. Значением свойства будет значение этого единственного не равного `NULL` свойства, либо `NULL`, если таких свойств нет.

### Параметры

- `expr1, ..., exprN`

    Список [выражений](Expression.md), значения которых будут определять значение свойства.

### Примеры

```lsf
background 'Цвет' (INTEGER i) = EXCLUSIVE RGB(255,238,165) IF i <= 5,
                                                   RGB(255,160,160) IF i > 5;

CLASS Human;

CLASS Male : Human;
CLASS Female : Human;

name(Human h) = EXCLUSIVE 'Male' IF h IS Male, 'Female' IF h IS Female;
```

