---
slug: "/CONTINUE_operator"
title: 'Оператор CONTINUE'
---

Оператор `CONTINUE` создает [действие](../paradigm/Actions.md), реализующее переход [к следующей итерации цикла](../paradigm/Next_iteration_CONTINUE.md).

### Синтаксис

```
CONTINUE
```

### Описание

Оператор `CONTINUE` создает действие, которое реализует переход к следующей итерации наиболее вложенного цикла.

### Пример

```lsf
testContinue ()  {
    FOR iterate(INTEGER i, 1, 5) DO {
        MESSAGE 'before';
        IF i == 3 THEN CONTINUE; // нет сообщения 'after' при i == 3
        MESSAGE 'after';
    }
}
```
