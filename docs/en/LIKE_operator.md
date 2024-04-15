---
title: 'LIKE operator'
---

The `LIKE` operator is the creation of a [property](Properties.md) that implements the operation of [comparing a string with a pattern](Comparison_operators_=_etc.md).

### Syntax

```
stringExpr LIKE patternExpr
```

### Description

The `LIKE` operator creates a property that returns `TRUE` if the specified string matches the given pattern. The pattern can include the following wildcard characters:

- `%` (percent sign) - replaces any number of characters, including zero characters. This wildcard is used when the exact content or number of characters in a part of the string is unknown.
- `_` (underscore) - replaces exactly one character. It is used when the exact location of a single character is required, but the character itself can be anything.

To include the `%` or `_` characters in the pattern as regular characters, they must be escaped using the backslash `\` character. Remember, when specifying the pattern using a [string literal](Literals.md), to represent the backslash itself, it needs to be doubled: `\\`.

### Parameters

- `stringExpr`

    [Expression](Expression.md) whose value determines the string being compared. The value of the expression must belong to one of the [string classes](Built-in_classes.md).

- `patternExpr`

    Expression whose value determines the pattern. The value of the expression must belong to one of the string classes.

### Examples

```lsf
isReportDocument(Document doc) = name(doc) LIKE '%report%'; // checks if the name contains the word 'report'
isPhoneNumber(STRING str) = str LIKE '(___) ___-____'; // checks if the phone number matches the format

startsWith(STRING str, STRING prefix) = str LIKE prefix + '%'; // checks the beginning of the string
contains(STRING content, STRING str) = content LIKE '%' + str + '%'; // checks if the string contains the substring
containsNew(STRING content, STRING str) = content LIKE '%${str}%'; // same as above, but with string interpolation

escapingExample(STRING str) = str LIKE '__\\%'; // Escaping example. Checks for a two-digit percentage.
```