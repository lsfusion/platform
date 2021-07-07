---
title: 'Interruption (BREAK)'
---

The *interrupt operator* creates an [action](Actions.md) that exits the most nested loop ([normal](Loop_FOR.md) or [recursive](Recursive_loop_WHILE.md)) within which this action is located. Control is transferred to the first action following the loop. If the created action is not inside a loop, its behavior becomes similar to the action created by the [exit operator](Exit_RETURN.md). 

### Language

The interrupt operator syntax is described by the [`BREAK` operator](BREAK_operator.md). 

### Examples

```lsf
testBreak ()  {
    FOR iterate(INTEGER i, 1, 100) DO {
        IF i == 50 THEN BREAK; // will only come up to 50
    }
}
```
