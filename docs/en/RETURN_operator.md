---
title: 'RETURN operator'
---

The `RETURN` operator creates an [action](Actions.md) that implements [exit](Exit_RETURN.md) from an action created by the [`EXEC` operator](Call_EXEC.md).

### Syntax

```
RETURN [resultExpr]
```

### Description

The `RETURN` operator creates an action that exits from the most nested [action call](Call_EXEC.md). When `resultExpr` is supplied, the value of that expression becomes the result of the call; this form is used in actions whose result class is declared (for example, in [abstract actions](Action_extension.md) with a return class). Without `resultExpr` the call simply exits and produces no value.

### Parameters

- `resultExpr`

    Optional [expression](Expression.md). Its value is returned to the caller as the result of the surrounding action. The expression's class must match the result class declared for that action.

### Examples

```lsf
// bare RETURN — exits the surrounding action without producing a value
importFile  {
    LOCAL file = FILE ();
    INPUT f = FILE DO {
        file () <- f;
    }

    IF NOT file() THEN RETURN;
}

// RETURN with an expression — provides a value as the result of an abstract action with a return class
getLocalizedTitle(Issue issue) ABSTRACT STRING[100] (Language);
getLocalizedTitle (Issue issue) + {
    FOR Language l IS Language DO
        RETURN localizedTitle(issue, l);
}
```
