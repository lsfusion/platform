---
title: 'IF operator'
---

The `IF` operator - creating a [property](Properties.md) implementing [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) of a value by condition (single form). 

### Syntax

    result IF condition 

### Description

The `IF` operator creates a property that returns the given value when a certain condition is met. If the condition is not met, the property returns `NULL`.

### Parameters

- `result`

    [Expression](Expression.md) whose value defines the result.

- `condition`

    An expression whose value defines the condition.

### Examples

```lsf
name = DATA STRING[100] (Book);
hasName (Book b) = TRUE IF name(b);

background (Book b) = RGB(224, 255, 128) IF b IS Book;

countTags (Book b) = GROUP SUM 1 IF in(b, Tag t);
```
