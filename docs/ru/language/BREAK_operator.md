---
slug: "/BREAK_operator"
title: 'Оператор BREAK'
---

Оператор `BREAK` - создание [действия](../paradigm/Actions.md), реализующего [прерывание цикла](../paradigm/Interruption_BREAK.md).

### Синтаксис

```
BREAK
```

### Описание

Оператор `BREAK` создает действие, которое выходит из наиболее вложенного цикла, внутри которого находится.

### Примеры

```lsf
testBreak ()  {
    FOR iterate(INTEGER i, 1, 100) DO {
        IF i == 50 THEN BREAK; // дойдет только до 50
    }
}
```
