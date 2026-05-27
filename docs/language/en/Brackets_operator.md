---
slug: "/Brackets_operator"
title: '[] operator'
---

The `[]` operator creates a [property](../paradigm/Properties.md) that returns an object from a [structure](../paradigm/Structure_operators_STRUCT.md).

### Syntax

```
expr[n]
```

Where `[` and `]` are ordinary square brackets.

### Description

The `[]` operator creates a property that takes a structure as input and returns the [structure](../paradigm/Structure_operators_STRUCT.md) object at the position given by `n`.

### Parameters

- `expr`

    An [expression](Expression.md) whose value must be a structure.

- `n`

    The position of the object within the structure. A positive [integer literal](Literals.md#intliteral): it must be written as a constant, not computed from an expression. Numbering is 1-based, and the value must be within the range `[1..N]`, where `N` is the number of objects in the structure.

### Examples

```lsf
CLASS Letter;
attachment1 = DATA FILE (Letter);
attachment2 = DATA FILE (Letter);
letterAttachments (Letter l) = STRUCT(attachment1(l), attachment2(l));
secondAttachment(Letter l) = letterAttachments(l)[2]; // returns attachment2
```
