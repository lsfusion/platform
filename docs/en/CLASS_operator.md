---
title: 'CLASS operator'
---

The `CLASS` operator creates a property that implements a [matching signature operator](Property_signature_CLASS.md).

### Syntax

    CLASS(expr) 

### Description

The `CLASS` operator creates a property that determines whether or not, from a class perspective, a specified property can have a non-`NULL` value for passed arguments.

### Parameters

- `expr`

    An [expression](Expression.md) whose result is a property. For this property, a set of parameter classes is inferred, matching which is checked by the result property. 

### Examples

```lsf
CLASS A;
a = ABSTRACT CASE STRING[100] (A);

CLASS B : A;
b = DATA STRING[100] (B);

a(B b) += WHEN CLASS(b(b)) THEN b(b); // is equivalent to WHEN b IS B THEN b(b)
```
