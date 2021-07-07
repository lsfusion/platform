---
title: 'IF ... THEN operator'
---

The `IF ... THEN` operator creates a [property](Properties.md) that implements [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) with one condition (single form).

### Syntax

    IF condition 
        THEN value
        [ELSE alternativeValue]

### Description

The `IF ... THEN` operator creates an action that implements conditional selection. The condition is defined using a property. If this condition is met, that is, the value of the property does not equal `NULL`, then the value of the created property will be the value of the property specified in the `THEN` block; otherwise, the value will be the value of the property in the `ELSE` block, or `NULL` if no `ELSE` block was specified.

### Parameters

- `condition`

    [Expression](Expression.md) defining a condition. 

- `value`

    An expression whose value will be the value of the created property if the condition is met.

- `alternativeValue`

    An expression whose value will be the value of the created property if the condition is not met.

### Examples

```lsf
price1 = DATA NUMERIC[10,2] (Book);
price2 = DATA NUMERIC[10,2] (Book);
maxPrice (Book b) = IF price1(b) > price2(b) THEN price1(b) ELSE price2(b);

// if h is of another class, it will be NULL
sex (Human h) = IF h IS Male THEN 'Male' ELSE ('Female' IF h IS Female); 

isDifferent(a, b) = IF a != b THEN TRUE;
```
