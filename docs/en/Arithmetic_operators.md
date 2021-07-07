---
title: 'Arithmetic operators'
---

`+`, `-`, `*`, `/`, `(+)`, `(-)` operators create [properties](Properties.md) responsible for [arithmetic operations](Arithmetic_operators_+_-_etc.md).

### Syntax

    expression1 + expression2  
    expression1 - expression2  
    expression1 / expression2  
    expression1 * expression2  
    -expression1
    expression1 (+) expression2  
    expression1 (-) expression2  

### Parameters

- `expression1, expression2`

    [Expressions](Expression.md) whose values will be arguments for arithmetic operators.

### Examples

```lsf
sum(a, b) = a + b;
transform(a, b, c) = -a * (b (+) c);
```
