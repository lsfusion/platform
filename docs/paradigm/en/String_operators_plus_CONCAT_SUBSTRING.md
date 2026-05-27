---
slug: "/String_operators_plus_CONCAT_SUBSTRING"
title: 'String operators (+, CONCAT)'
---

String operators are operators which parameters and result are the properties which values are instances of the [string classes](Built-in_classes.md). The platform currently supports the following string operators:

|Operator|Name|Description|Example|Result|
|--------|----|-----------|-------|------|
|`+`,&nbsp;`CONCAT`|Concatenation|Returns a string obtained by concatenating the operand strings|`'a' + 'b'`|`'ab'`|

The `+` operator returns `NULL` if one of the operands is `NULL`. The `CONCAT` operator treats a `NULL` operand as an empty string (however, concatenating values that are all `NULL` still returns `NULL`) and joins the operands with a *delimiter* that is inserted only between operands that are not `NULL`. For example, `CONCAT ' ', 'John', 'Smith'` = `'John Smith'`, but `CONCAT ' ', 'John', NULL` = `'John'`.

### Determining the result class

The result class is defined as:

|Operator|Description|
|---|---|
|`+`, `CONCAT`|`result = STRING[p1.blankPadded AND p2.blankPadded, p1.caseInsensitive OR p2.caseInsensitive, p1.length + p2.length]`|

where `blankPadded`, `caseInsensitive` and `length` are determined similarly to the rules for construction of a [common ancestor](Built-in_classes.md#commonparentclass) for two built-in classes (Strings family). For `CONCAT`, the result length also includes the delimiter inserted between operands.

In the `+` operator, operands which classes are other than string are cast to strings in accordance with the following table:

|Class                     |Cast class          |
|--------------------------|--------------------|
|`DATE`, `DATETIME`, `TIME`|`STRING[25]`        |
|`NUMERIC`                 |`STRING[p.length]`  |
|`LOGICAL`                 |`STRING[1]`         |
|`FILE`                    |`TEXT`              |
|[Object](User_classes.md) |`STRING[10]`        |
|Other                     |`STRING[8]`         |

If any operand belongs to the unlimited-length string class `TEXT` (or to one of its variants such as rich or HTML text), the result is that text class rather than a fixed-length string.

### Language

The summation operator concatenates strings when at least one operand is a string — see the [arithmetic operators](../language/Arithmetic_operators.md).

To concatenate several values with a delimiter inserted only between operands that are not `NULL`, use the [`CONCAT` operator](../language/CONCAT_operator.md).

### Examples


```lsf
CLASS Person;
firstName = DATA STRING[100] (Person);
middleName = DATA STRING[100] (Person);
lastName = DATA STRING[100] (Person);

// if some part of the name is not specified, then this part will be skipped along with a space
fullName(Person p) = CONCAT ' ', firstName(p), middleName(p), lastName(p);     
```
