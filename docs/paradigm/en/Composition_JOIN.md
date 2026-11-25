---
slug: "/Composition_JOIN"
title: 'Composition (JOIN)'
---

The *composition* operator creates a [property](Properties.md) whose value is another property — the *main* property — applied to the values returned by other properties supplied as its *arguments*.

The result class is the value class of the main property.

### `NULL` arguments {#null}

If the value of an argument is `NULL`, the corresponding parameter of the main property is `NULL`, and the composition returns the value that the main property has in this case. For example:

| Main property | Value when the parameter is `NULL` |
|---|---|
| a [data property](Data_properties_DATA.md) | `NULL` |
| a property created by the [grouping](Grouping_GROUP.md) operator | `NULL` if the parameter is part of the group key |
| a property created by the [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) operator | not necessarily `NULL`: if the parameter itself is a condition, for example, this condition is not met, but another condition may be met or an alternative result may be specified |

### Language

To implement composition, use the [`JOIN` operator](../language/JOIN_operator.md).

### Examples


```lsf
f = DATA INTEGER (INTEGER, INTEGER, INTEGER);
g = DATA INTEGER (INTEGER, INTEGER);
h = DATA INTEGER (INTEGER, INTEGER);
c(a, b) = f(g(a, b), h(b, 3), a);

count = DATA BPSTRING[255] (INTEGER);
name = DATA BPSTRING[255] (INTEGER);
formatted(INTEGER a, INTEGER b) = [FORMULA BPSTRING[255] ' CAST($1 AS TEXT) || \' / \' || CAST($2 AS TEXT)'](count(a), name(b));
```


```lsf
CLASS Triangle;
cathetus1 = DATA DOUBLE(Triangle);
cathetus2 = DATA DOUBLE(Triangle);

hypotenuseSq(triangle) = cathetus1(triangle)*cathetus1(triangle) + cathetus2(triangle)*cathetus2(triangle);

// a similar property set using composition
hypotenuseSq2(triangle) = [ x*x + y*y](cathetus1(triangle), cathetus2(triangle)); 
```


```lsf
CLASS Item;
CLASS Batch;
item = DATA Item (Batch);
date = DATA DATE (Batch);
quantity = DATA INTEGER (Batch);
countDate = DATA DATE (Item); // NULL until the item is counted for the first time

// selection: for a NULL date the first condition is not met, and the second result does not depend on the date
dateOrDefault (DATE d) = OVERRIDE d, 2000_01_01;
// for an item that has not been counted the argument is NULL, and the value is 2000_01_01
countDateOrDefault (Item i) = dateOrDefault(countDate(i));
// the same value is returned when the argument is the NULL constant
defaultDate () = dateOrDefault(NULL);

// grouping: the parameter d is part of the group key, so there is no value for a NULL date
receivedSince (Item i, DATE d) = GROUP SUM quantity(Batch b) IF item(b) = i AND date(b) >= d;
// for an item that has not been counted the second argument is NULL, and so is the value.
// With (NOT d OR date(b) >= d) in place of date(b) >= d the value would still be NULL
receivedSinceCount (Item i) = receivedSince(i, countDate(i));
```
