---
title: 'Оператор RETURN'
---

Оператор `RETURN` - создание [действия](Actions.md), реализующего [выход](Exit_RETURN.md) из действия, созданного [оператором `EXEC`](Call_EXEC.md).

### Синтаксис

    RETURN

### Описание

Оператор `RETURN` создает действие, которое выходит из наиболее вложенного [вызова действия](Call_EXEC.md). 

### Примеры

```lsf
importFile  {
    LOCAL file = FILE ();
    INPUT f = FILE DO {
        file () <- f;
    }

    IF NOT file() THEN RETURN;
}
```
