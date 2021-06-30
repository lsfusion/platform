---
title: 'Exit (RETURN)'
---

The *exit operator* creates an [action](Actions.md) that exits from the inmost [action call](Call_EXEC.md). Control is passed to the first action following that call operator.

### Language

The syntax of the exit operator is described by the [`RETURN` operator](RETURN_operator.md). 

### Examples


```lsf
importFile  {
    LOCAL file = FILE ();
    INPUT f = FILE DO {
        file () <- f;
    }

    IF NOT file() THEN RETURN;
}
```
