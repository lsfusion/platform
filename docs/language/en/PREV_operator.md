---
slug: "/PREV_operator"
title: 'PREV operator'
---

The `PREV` operator creates a [property](../paradigm/Properties.md) using a [previous value operator](../paradigm/Previous_value_PREV.md).

### Syntax

```
PREV(propExpr)
```

### Description

The `PREV` operator creates a property that returns the value of the given expression at the start of the current [change session](../paradigm/Change_sessions.md) — i.e., the value that existed before the changes made in this session. It always uses the session-start scope (it does not switch modes based on the surrounding context); see the paradigm article and [`Events.md#change`](../paradigm/Events.md#change) for how this relates to event-mode behavior of [change operators](../paradigm/Change_operators_SET_CHANGED_etc.md). It is a [context-dependent](Property_operators.md) property operator and can appear inside [expressions](Expression.md).


:::info
It's important to understand that `PREV` is not a built-in property with [composition](../paradigm/Composition_JOIN.md) but an operator. Thus, in particular `PREV(f(a))` is not equal to `[PREV(a)](f(a))`: the session-start scope applies to the entire operand expression, including its argument sub-expressions. For example, in `PREV(f(g(a)))` the value of `g(a)` is also taken at the start of the session; if `g` is a [local property](../paradigm/Data_properties_DATA.md#local) filled in the current session, inside `PREV` it returns `NULL`, and the whole expression returns `NULL` as well. To read the previous value of a property for arguments computed in the current session, wrap `PREV` in a separate property: with `prevF(x) = PREV(f(x))` the call `prevF(g(a))` evaluates `g(a)` in the current session, and only `f` is read in the session-start state.
:::

### Parameters

- `propExpr`

    The [expression](Expression.md) whose previous value is returned. It must denote a property; a bare parameter cannot be used as the operand.

### Examples

```lsf
CLASS A;
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
