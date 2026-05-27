---
slug: "/Rounding_operator_ROUND"
title: 'Rounding operator (ROUND)'
---

The *rounding operator* creates a [property](Properties.md) whose value is the result of rounding a number to the specified number of digits. The arguments of this operator are properties that determine the number to be rounded and, optionally, the rounding precision. The number must belong to one of the numeric [built-in classes](Built-in_classes.md), and the precision must belong to the `INTEGER` class. When no precision is given, the number is rounded to the nearest integer. Negative precision values indicate the number of least significant digits to be rounded in the integer part of the number, allowing you to round the number to tens, hundreds, etc.

The operator returns `NULL` if the number being rounded or the precision is `NULL`.

How values exactly halfway between two possible results are rounded is determined by the database, not fixed by the platform.

### Determining the result class

The result class depends on whether the precision is given as a constant integer literal:

|Precision|Result|
|---------|---|
|Literal `s` (or absent, which is equivalent to `0`)|`NUMERIC[number.IntegerPart + s, s]`|
|Computed (not a literal)|Class of the number being rounded|

When the precision is a constant literal, the result is a `NUMERIC` whose scale equals that precision and whose integer part is the integer part of the number being rounded; this holds for any literal `s`, so a precision of `0` (or no precision) rounds to the nearest integer, and a negative precision rounds within the integer part. When the precision is computed rather than a literal, the result keeps the class of the number being rounded.

### Language

To create a property that rounds a number, the [`ROUND` operator](../language/ROUND_operator.md) is used.

### Examples

```lsf
number = DATA NUMERIC[10,3]();  //12345.678
rounded = ROUND(number());      //12346
rounded1 = ROUND(number(), 2);  //12345.68
rounded2 = ROUND(number(), -2); //12300

FORM roundTest
PROPERTIES() number, rounded, rounded1, rounded2;
```
