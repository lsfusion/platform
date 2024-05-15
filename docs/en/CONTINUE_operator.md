---
title: 'CONTINUE operator'
---

The `CONTINUE` operator creates an [action](Actions.md) that implements move to the [next iteration of the cycle](Next_iteration_CONTINUE.md).

### Syntax

```
CONTINUE
```

### Description

The `CONTINUE` operator creates an action that moves to the next iteration of the cycle.

### Example

```lsf
testContinue ()  {
    FOR iterate(INTEGER i, 1, 10) DO {
        MESSAGE 'before';
        IF i == 5 THEN CONTINUE; // no message 'after' for i == 5
        MESSAGE 'after';
    }
}
```
