---
slug: "/Structure_operators_STRUCT"
title: 'Structure operators (STRUCT, [])'
---

The term *structure* is used in the platform to refer to the [classes](Classes.md) whose objects consist of a collection of other objects in a fixed order. The platform supports two operators for working with structures:

|Operator|Name    |Description|Example|Result|
|--------|--------|---|---|---|
|`STRUCT`|Creation|Creates a [property](Properties.md) that takes a list of operands and returns a structure consisting of the objects passed|`STRUCT('a', 1)`|`STRUCT('a', 1)`|
|`[ ]`   |Access  |Creates a property that takes an operand with a fixed integer and returns an object of the structure specified in the first operand with a number equal to the specified integer|`STRUCT('a',1)[2]`|`1`|

Structures support comparison operations which are executed sequentially for each object included in the structure. 

A structure exists only when all of its objects exist: if any of them is `NULL`, the whole structure is `NULL`, and the access operator returns `NULL` for a `NULL` structure.

### Determining the result class

The result class is determined as:

|Operator|Result|
|--------|------|
|`STRUCT`|A structure of the operand classes, in the same order|
|`[ ]`   |The class of the structure object at the specified position|

### Language

To create a property that creates a structure the [`STRUCT` operator](../language/STRUCT_operator.md) is used.

To create a property that returns an object from the structure the [`[ ]` operator](../language/Brackets_operator.md) is used.

### Examples

```lsf
objectStruct(a, b) = STRUCT(a, f(b));
stringStruct() = STRUCT(1, 'two', 3.0);
```


```lsf
CLASS Letter;
attachment1 = DATA FILE (Letter);
attachment2 = DATA FILE (Letter);
letterAttachments (Letter l) = STRUCT(attachment1(l), attachment2(l));
secondAttachment(Letter l) = letterAttachments(l)[2]; // returns attachment2
```

