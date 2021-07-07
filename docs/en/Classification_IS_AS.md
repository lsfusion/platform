---
title: 'Classification (IS/AS)'
---

*Classification* operators create [properties](Properties.md) that determine whether an object belongs to the [class](Classes.md) specified. If the property argument does not belong to the class specified in the operator, the property returns `NULL`. Otherwise, the operator `IS` returns `TRUE`, and the operator `AS` returns the object passed as an argument.

### Language

To implement classification operators, [`IS` and `AS` operators](IS_AS_operators.md) are used. 

### Examples 

```lsf
asOrder(object) = object AS Order;

person = DATA Human (Order);
isMale (Order o) = person(o) IS Male;
```
