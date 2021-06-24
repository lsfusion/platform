---
title: 'Structure operators (STRUCT, [])'
---

The term *structure* is used in the platform to refer to the [classes](Classes.md) which objects consist of a collection of other objects in a fixed order. The platform supports two operators for working with structures:

|Operator|Name    |Description|Example|Result|
|--------|--------|---|---|---|
|`STRUCT`|Creation|Creates a [property](Properties.md) that takes a list of operands and returns a structure consisting of the objects passed|`STRUCT('a', 1)`|`STRUCT('a', 1)`|
|`[ ]`   |Access  |Creates a property that takes an operand with a fixed integer and returns an object of the structure specified in the first operand with a number equal to the specified integer|`STRUCT('a',1)[2]`|`1`|

Structures support comparison operations which are executed sequentially for each object included in the structure. 

:::info
To better understand how this works, it’s sufficient to say that physically a structure is just a concatenation of the objects included in this structure converted to bit strings
:::

### Language

To create a property that creates a structure the [`STRUCT` operator](STRUCT_operator.md) is used.

To create a property that returns an object from the structure the [`[ ]` operator](Brackets_operator.md) is used.

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

