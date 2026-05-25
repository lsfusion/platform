---
slug: "/BREAK_operator"
title: 'Оператор BREAK'
---

Оператор `BREAK` создает [действие](../paradigm/Actions.md), реализующее [прерывание цикла](../paradigm/Interruption_BREAK.md).

### Синтаксис

```
BREAK
```

### Описание

Оператор `BREAK` создает действие, которое выходит из наиболее вложенного цикла.

### Примеры

```lsf
testBreak ()  {
    FOR iterate(INTEGER i, 1, 100) DO {
        IF i == 50 THEN BREAK; // дойдет только до 50
    }
}
```
