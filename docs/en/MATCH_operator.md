---
title: 'MATCH operator'
---

The `MATCH` operator creates a [property](Properties.md) that implements [full-text search](Comparison_operators_=_etc.md). For pattern matching, use the separate [`LIKE` operator](LIKE_operator.md).

### Syntax

```
searchExpr MATCH compareExpr
```

### Description

The `MATCH` operator compares `searchExpr` with a search string or a prepared search query. The `%` and `_` characters in `compareExpr` are not wildcard characters.

- If both `MATCH` operands belong to string classes, the platform converts them using the current full-text search language and returns `TRUE` if either the full-text condition or the case-insensitive substring inclusion check succeeds.
- Prepared full-text values are supported only in their standard roles: `TSVECTOR` in `searchExpr` and `TSQUERY` in `compareExpr`. In these roles, the platform uses the prepared value directly and converts only the string operand, if any; the additional substring inclusion check is not added.
- Values of type `TSVECTOR` and `TSQUERY` are usually obtained with the `toTsVector` and `toTsQuery` functions.

### Parameters

- `searchExpr`

    [Expression](Expression.md) whose value determines the source string or prepared search vector.

    The value of the expression must belong to one of the string classes or have type `TSVECTOR`.

- `compareExpr`

    Expression whose value determines the search string or prepared search query.

    The value of the expression must belong to one of the string classes or have type `TSQUERY`.

### Examples

```lsf
matchesByWords(STRING content, STRING query) = content MATCH query; // full-text search over a string
matchesPrepared(STRING content, TSQUERY query) = content MATCH query; // prepared TSQUERY on the right
matchesVectorByString(TSVECTOR vector, STRING query) = vector MATCH query; // the string on the right is converted to TSQUERY
matchesVector(TSVECTOR vector, TSQUERY query) = vector MATCH query; // direct TSVECTOR-to-TSQUERY match
```
