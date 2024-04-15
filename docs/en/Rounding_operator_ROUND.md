---
title: 'Rounding operator (ROUND)'
---

The *rounding operator* creates a [property](Properties.md) whose value is the result of rounding a number to the specified number of digits. The arguments of this operator must be properties that determine the number and the rounding precision. Negative precision values indicate the number of least significant digits to be rounded in the integer part of the number, allowing you to round the number to tens, hundreds, etc.

### Language

To create a property that rounds a number, the [`ROUND` operator](ROUND_operator.md) is used.

### Examples

```lsf
number = DATA NUMERIC[10,3]();  //12345.678
rounded = ROUND(number());      //12346
rounded1 = ROUND(number(), 2);  //12345.68
rounded2 = ROUND(number(), -2); //12300.00

FORM roundTest
PROPERTIES() number, rounded, rounded1, rounded2;
```