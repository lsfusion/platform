---
title: 'MIN operator'
---

The `MIN` operator creates a [property](Properties.md) that implements finding the [minimum](Extremum_MAX_MIN.md) value.

### Syntax 

    MIN expr1, ..., exprN

### Description

The `MIN` operator creates a property that returns the minimum value of the values of the specified properties.

### Parameters

- `expr1, ..., exprN`

    A list of [expressions](Expression.md) of which values the minimum is selected.

### Examples

```lsf
minPrice(Book b) = MIN price1(b), price2(b);

date (INTEGER i) = DATA DATE (INTEGER);
minDate (INTEGER i) = MIN date(i), 2001_01_01;
```
