---
title: 'Constant'
---

The *constant* operator is used to create properties without parameters which always return the same value. This value can be [static objects](Static_objects.md) of [custom](User_classes.md) and [built-in](Built-in_classes.md) classes as well as the special `NULL` value. 

### Language

Static objects of custom classes are specified as `<class name>.<object name>`.

Static objects of built-in classes are specified by special [literals](Literals.md).

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

  
