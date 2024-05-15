---
title: 'Next iteration (CONTINUE)'
---

The *next iteration* operator creates an [action](Actions.md) that skips the execution of the remaining code in the current iteration and moves to the next iteration of the loop ([normal](Loop_FOR.md) or [recursive](Recursive_loop_WHILE.md)). If the created action is not inside a loop, its behavior becomes similar to the action created by the [exit operator](Exit_RETURN.md).

### Language

The next iteration operator syntax is described by the [`CONTINUE` operator](CONTINUE_operator.md).

### Example

```lsf
testContinue ()  {
    FOR iterate(INTEGER i, 1, 5) DO {
        MESSAGE 'before';
        IF i == 3 THEN CONTINUE; // no message 'after' for i == 3
        MESSAGE 'after';
}
```
