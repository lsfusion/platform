---
slug: "/Extremum_MAX_MIN"
title: 'Extremum (MAX, MIN)'
---

The *extremum operator* creates a [property](Properties.md) which calculates the maximum or minimum among several specified properties. At least one property must be specified. If the value of any of these properties is `NULL`, this property is ignored. If the values of all properties are `NULL`, the result value is also `NULL`.

The operands must belong to compatible classes — built-in classes of the same family, or user classes related by inheritance.

### Determining the result class

The result class is the common ancestor — [built-in](Built-in_classes.md#commonparentclass) or [user-defined](User_classes.md#commonparentclass) — of the operand classes.

### Language

The maximum property is created using the [`MAX` operator](../language/MAX_operator.md), the minimum - using the [`MIN` operator](../language/MIN_operator.md).

### Examples

```lsf
date1 = DATA DATE(INTEGER);
date2 = DATA DATE(INTEGER);
maxDate (INTEGER i) = MAX date1(i), date2(i);

balance = DATA INTEGER (Item);
outcome 'Balance (non-negative)' (Item i) = MAX balance(i), 0;
```


```lsf
minPrice(Book b) = MIN price1(b), price2(b);

date (INTEGER i) = DATA DATE (INTEGER);
minDate (INTEGER i) = MIN date(i), 2001_01_01;
```

