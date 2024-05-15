---
title: 'Оператор CONTINUE'
---

Оператор `CONTINUE` - создание [действия](Actions.md), реализующего переход [к следующей итерации цикла](Next_iteration_CONTINUE.md).

### Синтаксис

```
CONTINUE
```

### Описание

Оператор `CONTINUE` создает действие, которое реализует переход к следующей итерации цикла.

### Пример

```lsf
testContinue ()  {
    FOR iterate(INTEGER i, 1, 5) DO {
        MESSAGE 'before';
        IF i == 3 THEN CONTINUE; // no message 'after' for i == 3
        MESSAGE 'after';
    }
}
```
