---
title: 'RETURN operator'
---

The `RETURN` operator creates an [action](Actions.md) that implements [exit](Exit_RETURN.md) from an action created by the [`EXEC` operator](Call_EXEC.md).

### Syntax

    RETURN

### Description

The `RETURN` operator creates an action that exits from the most nested [action call](Call_EXEC.md). 

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
