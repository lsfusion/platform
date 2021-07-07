---
title: 'Recursive loop (WHILE)'
---

The *recursive loop operator* is similar to a regular [loop](Loop_FOR.md) operator, with the only difference being that iteration is performed recursively until at a certain point the set of objects collections satisfying the condition becomes empty. As a result, there is no sense in defining *alternative* actions with this operator.

### Language

The syntax of the recursive loop operator is described by the [`WHILE` operator](WHILE_operator.md).

### Examples

```lsf
iterateDates (DATE dateFrom, DATE dateTo)  {
    LOCAL dateCur = DATE();

    dateCur() <- dateFrom;
    WHILE dateCur() <= dateTo DO {
        MESSAGE 'I have a date ' + dateCur();
        dateCur() <- sum(dateCur(), 1);
    }
}
```
