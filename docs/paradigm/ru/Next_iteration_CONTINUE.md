---
slug: "/Next_iteration_CONTINUE"
title: 'Следующая итерация (CONTINUE)'
---

Оператор *перехода к следующей итерации* создает [действие](Actions.md), которое пропускает выполнение оставшегося кода в текущей итерации и переходит на следующую итерацию наиболее вложенного цикла ([обычного](Loop_FOR.md) или [рекурсивного](Recursive_loop_WHILE.md)). Если созданное действие не находится внутри цикла, оно выходит из наиболее вложенного [вызова действия](Call_EXEC.md), как и [оператор выхода](Exit_RETURN.md).

### Язык

Синтаксис оператора перехода к следующей итерации описывается [оператором `CONTINUE`](../language/CONTINUE_operator.md).

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
