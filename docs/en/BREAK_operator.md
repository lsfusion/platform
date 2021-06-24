---
title: 'BREAK operator'
---

The `BREAK` operator creates an [action](Actions.md) that implements a [loop interruption](Interruption_BREAK.md).

### Syntax

    BREAK

### Description

The `BREAK` operator creates an action that exits the most nested loop within which it is located.

### Examples

```lsf
testBreak ()  {
    FOR iterate(INTEGER i, 1, 100) DO {
        IF i == 50 THEN BREAK; // will only come up to 50
    }
}
```
