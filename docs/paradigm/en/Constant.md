---
slug: "/Constant"
title: 'Constant'
---

The *constant* operator is used to create properties without parameters which always return the same value. This value can be [static objects](Static_objects.md) of [custom](User_classes.md) and [built-in](Built-in_classes.md) classes as well as the special `NULL` value. The value class is fixed by the value itself:

|Constant value|Value class|
|--------------|---|
|Static object of a custom class|the class in whose declaration the object is listed|
|Built-in value|that value's built-in class|
|`NULL`|none — the surrounding context supplies a class where one is required|

There is no `FALSE` constant — the false value is represented by `NULL`.

### Language

Constants are written as references to [static objects](Static_objects.md) for custom-class values, and as [literals](../language/Literals.md) for built-in-class values and `NULL`.

### Examples

```lsf
CLASS Direction {
    north, east, south, west // declaring static objects
}

// A constant property is created from a Direction.north object which is then used 
// by the comparison operator to construct the isNorth property
isNorth (Direction d) = d == Direction.north;  

// A constant property is created from a literal describing the date
defaultDate() = 1982_07_13;                         

CLASS Man;
age 'Age' = DATA INTEGER (Man);
// A constant property is created from an integer (integer literal)
isChild (Man m) = age(m) < 17;                        
```

  
