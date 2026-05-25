---
slug: "/CONTINUE_operator"
title: 'CONTINUE operator'
---

The `CONTINUE` operator creates an [action](../paradigm/Actions.md) that implements a move to the [next iteration of the loop](../paradigm/Next_iteration_CONTINUE.md).

### Syntax

```
CONTINUE
```

### Description

The `CONTINUE` operator creates an action that moves to the next iteration of the innermost enclosing loop.

### Example

```lsf
testContinue ()  {
    FOR iterate(INTEGER i, 1, 5) DO {
        MESSAGE 'before';
        IF i == 3 THEN CONTINUE; // no message 'after' for i == 3
        MESSAGE 'after';
    }
}
```
