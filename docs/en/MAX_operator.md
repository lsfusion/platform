---
title: 'MAX operator'
---

The `MAX` operator creates a [property](Properties.md) that implements finding [maximum](Extremum_MAX_MIN.md) value.

### Syntax 

    MAX expr1, ..., exprN

### Description

The `MAX` operator creates a property that returns the maximum value of the values of the specified properties.

### Parameters

- `expr1, ..., exprN`

    A list of [expressions](Expression.md) of which values the maximum is selected.

### Examples

```lsf
date1 = DATA DATE(INTEGER);
date2 = DATA DATE(INTEGER);
maxDate (INTEGER i) = MAX date1(i), date2(i);

balance = DATA INTEGER (Item);
outcome 'Balance (non-negative)' (Item i) = MAX balance(i), 0;
```
