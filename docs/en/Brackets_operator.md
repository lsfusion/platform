---
title: '[] operator'
---

The `[]` operator creates a [property](Properties.md) that returns an object from a [structure](Structure_operators_STRUCT.md).

### Syntax

    expr [ number ]

Where `[` and `]` are ordinary square brackets.

### Description

The `[]` operator creates a property that takes a structure as input and returns one of the objects of this structure. Objects are accessed using the sequence number of the object. 

### Parameters

- `expr`

    An [expression](Expression.md) whose value must be a structure.

- `number`

    The sequence number of an object. [Integer literal](Literals.md#intliteral). Must be within the range of `[1..N]`, where `N` is the number of objects in the structure.

### Examples

```lsf
CLASS Letter;
attachment1 = DATA FILE (Letter);
attachment2 = DATA FILE (Letter);
letterAttachments (Letter l) = STRUCT(attachment1(l), attachment2(l));
secondAttachment(Letter l) = letterAttachments(l)[2]; // returns attachment2
```
