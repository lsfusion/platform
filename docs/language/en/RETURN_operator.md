---
slug: "/RETURN_operator"
title: 'RETURN operator'
---

The `RETURN` operator creates an [action](../paradigm/Actions.md) that implements [exit](../paradigm/Exit_RETURN.md) from an [action call](../paradigm/Call_EXEC.md).

### Syntax

```
RETURN [resultExpr]
```

### Description

The `RETURN` operator creates an action that exits from the innermost enclosing [action call](../paradigm/Call_EXEC.md). If `resultExpr` is specified, its value becomes the result of that call. Otherwise the call simply exits without producing a value.

The result class of the surrounding action is determined in one of two ways:

- For [abstract actions](../paradigm/Action_extension.md), the result class is declared in the [`ABSTRACT` operator](ABSTRACT_action_operator.md); the value class of `resultExpr` must conform to the declared class.
- For other actions, the result class is inferred from `resultExpr` of all `RETURN` operators in the body. If several `RETURN` operators are present, the resulting class is the common ancestor of the classes of their expression values.

If the platform cannot determine the value class of `resultExpr`, such a `RETURN` is invalid. For example, the class cannot be inferred from an untyped `NULL` value.

If `resultExpr` declares new local parameters, they become additional parameters of the result. In this case the action returns a set of objects depending on the values of those parameters.

A bare `RETURN` is allowed in any action regardless of whether the action declares a result.

### Parameters

- `resultExpr`

    Optional [expression](Expression.md). Its value is returned to the caller as the result of the surrounding action. For abstract actions, the value class of this expression must conform to the declared result class. For other actions, the platform must be able to infer this class.

### Examples

```lsf
// bare RETURN — exits the surrounding action without producing a value
importFile  {
    LOCAL file = FILE ();
    INPUT f = FILE DO {
        file () <- f;
    }

    IF NOT file() THEN RETURN;
}

// RETURN with an expression — result class is inferred from the expressions
priceBucket (INTEGER price)  {
    IF price > 1000 THEN RETURN 'high';
    IF price > 100 THEN RETURN 'mid';
    RETURN 'low';
}

// an extra parameter in the expression — the action returns a set of values indexed by that parameter
allCaptions ()  {
    LOCAL caption = STRING[100] (STRING);
    caption('A') <- 'Alpha';
    caption('B') <- 'Beta';
    RETURN caption(STRING s);
}

// RETURN with an expression in an abstract action with a declared result class
getLocalizedTitle(Issue issue) ABSTRACT STRING[100] (Language);
getLocalizedTitle (Issue issue) + {
    RETURN localizedTitle(issue, Language l);
}
```
