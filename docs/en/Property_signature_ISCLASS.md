---
id: Property_signature_CLASS
title: 'Signature matching (ISCLASS)'
---

The signature matching operator creates a [property](Properties.md) that determines, from a class perspective, whether the property specified in the operator can have a non-`NULL` value for the given arguments. In fact, this operator infers the possible classes of a given property from its semantics, and then, using [logical](Logical_operators_AND_OR_NOT_XOR.md) operators and the [classification](Classification_IS_AS.md) operator, creates the required property.

### Language

To implement this operator, use the [`ISCLASS` operator](ISCLASS_operator.md).

### Example

```lsf
CLASS Person;
name = ABSTRACT CASE STRING[100] (Person);

CLASS Student : Person;
studentName = DATA STRING[100] (Student);

name(s) += WHEN ISCLASS(studentName(s)) THEN studentName(s); // is equivalent to WHEN s IS Student THEN studentName(s)
```
