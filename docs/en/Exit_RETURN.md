---
title: 'Exit (RETURN)'
---

The *exit operator* creates an [action](Actions.md) that exits from the inmost [action call](Call_EXEC.md). Control is passed to the first action following that call operator.

The exit operator can also specify the [result](Actions.md) of the surrounding [action call](Call_EXEC.md).

### Language

The syntax of the exit operator is described by the [`RETURN` operator](RETURN_operator.md). 

### Examples


```lsf
// bare exit
importFile  {
    LOCAL file = FILE ();
    INPUT f = FILE DO {
        file () <- f;
    }

    IF NOT file() THEN RETURN;
}

// exit with a value — the value becomes the result of the surrounding action call
priceBucket (INTEGER price)  {
    IF price > 1000 THEN RETURN 'high';
    IF price > 100 THEN RETURN 'mid';
    RETURN 'low';
}
```
