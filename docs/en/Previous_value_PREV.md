---
title: 'Previous value (PREV)'
---

The *previous value* operator creates a [property](Properties.md) that returns the value of the specified property at the beginning of the session (that is, the current value in the database ignoring the session changes).

:::caution
This operator is calculated differently inside the [event](Events.md#change) handling: here it returns the value at the time of the previous occurrence of this event (or rather, at the time when all its handling were completed).
:::

### Language

To declare a property that returns a previous value, use the [`PREV` operator](PREV_operator.md). 

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
