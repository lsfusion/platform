---
slug: "/BREAK_operator"
title: 'BREAK operator'
---

The `BREAK` operator creates an [action](../paradigm/Actions.md) that implements a [loop interruption](../paradigm/Interruption_BREAK.md).

### Syntax

```
BREAK
```

### Description

The `BREAK` operator creates an action that exits the innermost enclosing loop.

### Examples

```lsf
testBreak ()  {
    FOR iterate(INTEGER i, 1, 100) DO {
        IF i == 50 THEN BREAK; // will only come up to 50
    }
}
```
