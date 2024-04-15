---
id: CLASS_operator
title: 'ISCLASS operator'
---

The `ISCLASS` operator creates a property that implements a [signature matching operator](Property_signature_ISCLASS.md).

### Syntax

```
ISCLASS(expr) 
```

### Description

The `ISCLASS` operator creates a property that determines, from a class perspective, whether the expression specified in the operator can have a non-`NULL` value for the given arguments.

### Parameters

- `expr`

    [Expression](Expression.md) that describes and creates a property, for which a set of parameter classes - a signature - is inferred. Matching this signature will be checked.

### Example

```lsf
CLASS Person;
name = ABSTRACT CASE STRING[100] (Person);

CLASS Student : Person;
studentName = DATA STRING[100] (Student);

name(s) += WHEN ISCLASS(studentName(s)) THEN studentName(s); // is equivalent to WHEN s IS Student THEN studentName(s)
```
