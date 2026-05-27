---
slug: "/IS_AS_operators"
title: 'IS, AS operators'
---

`IS`, `AS` operators create a [property](../paradigm/Properties.md) that implements [classification](../paradigm/Classification_IS_AS.md).

### Syntax

```
expression IS className
expression AS className
```

### Description

The `IS` operator creates a property which returns `TRUE` if the value of the [expression](Expression.md) belongs to the specified class.

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
