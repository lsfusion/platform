---
title: 'IS, AS operators'
---

`IS`, `AS` operators create an [action](Properties.md) that implements [classification](Classification_IS_AS.md).

### Syntax

    expression IS className
    expression AS className

### Description

The `IS` operator creates an action which returns `TRUE` if the value of the [expression](Expression.md) belongs to the specified class.

The `AS` operator creates a property which returns the expression value if this value belongs to the specified class.

### Parameters

- `expression`

    An expression which value is checked for belonging to the class.

- `className`

    Class name. [Class ID](IDs.md#classid).

### Examples 

```lsf
asOrder(object) = object AS Order;

person = DATA Human (Order);
isMale (Order o) = person(o) IS Male;
```
