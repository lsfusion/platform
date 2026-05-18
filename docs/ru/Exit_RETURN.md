---
title: 'Выход (RETURN)'
---

*Оператор выхода* создает [действие](Actions.md), которое выходит из наиболее вложенного [вызова действия](Call_EXEC.md). Управление передается первому действию, следующему за этим оператором вызова.

Оператор выхода может также задавать [результат](Actions.md) окружающего [вызова действия](Call_EXEC.md).

### Язык

Синтаксис оператора выхода описывается [оператором `RETURN`](RETURN_operator.md). 

### Примеры


```lsf
// выход без значения
importFile  {
    LOCAL file = FILE ();
    INPUT f = FILE DO {
        file () <- f;
    }

    IF NOT file() THEN RETURN;
}

// выход со значением — значение становится результатом окружающего вызова действия
priceBucket (INTEGER price)  {
    IF price > 1000 THEN RETURN 'high';
    IF price > 100 THEN RETURN 'mid';
    RETURN 'low';
}
```
