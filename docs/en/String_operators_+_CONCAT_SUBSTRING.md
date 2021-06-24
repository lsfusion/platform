---
title: 'String operators (+, CONCAT, SUBSTRING)'
---

String operators are operators which parameters and result are the properties which values are instances of the string classes. The platform currently supports the following string operators:

|Operator|Name|Description|Example|Result|
|--------|----|-----------|-------|------|
|`+`,&nbsp;[`CONCAT`](CONCAT_operator.md)|Concatenation|Takes two operands and returns a string obtained by concatenating the strings specified in the operands|`'a' + 'b'`|`'ab'`|
|`SUBSTRING`|Substring|Takes three operands and returns a substring obtained from the string specified in the first operand, starting with the character specified in the second operand, and having length specified in the third operand|`SUBSTRING('abc', 2, 2)`|`'bc'`|

The `+` and `SUBSTRING` operators return `NULL` if one of the operands is `NULL`. The `CONCAT` operator treats `NULL` value of the operand as an empty string (however, concatenation of two `NULL` values still returns `NULL`). Also, in the `CONCAT` operator you can optionally specify the third operand (*delimiter*) which will be inserted if and only if both operands are not `NULL`. For example, `CONCAT ' ', 'John', 'Smith'` = `'John Smith'`, but `CONCAT ' ', 'John', NULL` = `'John'`.

### Determining the result class

The result class is defined as:

|Operator|Description|
|---|---|
|`+`, `CONCAT`|`result = STRING[p1.blankPadded AND p2.blankPadded, p1.caseInsensitive OR p2.caseInsensitive, p1.length + p2.length]`|
|`SUBSTRING(p, from, length)`|`result = STRING[p.blankPadded, p.caseInsensitive, length]`|

where `blankPadded`, `caseInsensitive` and `length` are determined similarly to the rules for construction of a common ancestor for two built-in classes (Strings family).

In the `+` operator, operands which classes are other than string are cast to strings in accordance with the following table:

|Class                     |Cast class          |
|--------------------------|--------------------|
|`DATE`, `DATETIME`, `TIME`|`STRING[25]`        |
|`NUMERIC`                 |`STRING[p.length]`  |
|`LOGICAL`                 |`STRING[1]`         |
|`FILE`                    |`TEXT`              |
|[Object](User_classes.md) |`STRING[10]`        |
|Other                     |`STRING[8]`         |

### Examples


```lsf
CLASS Person;
firstName = DATA STRING[100] (Person);
middleName = DATA STRING[100] (Person);
lastName = DATA STRING[100] (Person);

fullName(Person p) = CONCAT ' ', firstName(p), middleName(p), lastName(p);     // if some part of the name is not specified, then this part will be skipped along with a space
```
