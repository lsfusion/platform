---
title: 'PREV operator'
---

The `PREV` operator creates a [property](Properties.md) using a [previous value operator](Previous_value_PREV.md).

### Syntax

    PREV(propExpr)

### Description

The `PREV` operator creates a property that returns the value of another property at the start of the current session (or at the time of the previous event in [event](Events.md#change) mode) - i.e., the value that existed before the changes that were made in the current session.


:::info
It's important to understand that `PREV` is not a built-in property with [composition](Composition_JOIN.md) but an operator. Thus, in particular `PREV(f(a))` is not equal to `[PREV(a)](f(a))`.
:::

### Parameters

- `propExpr`

    [Expression](Expression.md) whose value defines the property for which the previous value must be obtained.

### Examples

```lsf
f = DATA INTEGER (A);
// outputs all changes f(a) in the session one by one
messageFChanges  {
    FOR CHANGED(f(A a)) DO
        MESSAGE 'In this session f(a) changed from ' + PREV(f(a)) + ' TO ' + f(a);
}

CLASS Document;
date = DATA DATE (Document);

CLASS Article;
price = DATA NUMERIC[14,2] (Document, Article);
// write in the price of the document the last used price in the database
// PREV is important to ignore the prices entered in this document
// this is especially important if the last used price is materialized, then the platform can simply read this value from the table
setPrice  {
    price(Document d, Article a) <- PREV((GROUP LAST price(d, a) ORDER date(d), d));
}
```
