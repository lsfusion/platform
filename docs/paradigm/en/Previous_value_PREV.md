---
slug: "/Previous_value_PREV"
title: 'Previous value (PREV)'
---

The *previous value* operator creates a [property](Properties.md) that returns the value of the given property at the beginning of the [change session](Change_sessions.md) (that is, the current value in the database, ignoring any changes that have been made in the session). The resulting property has the same value class as the source property. Together with the [change operators](Change_operators_SET_CHANGED_etc.md) — which are derived from it — this operator is what makes session-local change tracking possible: it gives access to the "before" state that the current session is being compared against.

:::info
This operator always uses the start-of-session scope. For how that interacts with [event handling](Events.md#change), see the canonical event-mode section.
:::

If a parameter's class is determined by neither the condition nor the value of a property, restrict the property's domain with a [class membership](Classification_IS_AS.md) condition before using its previous value in a [constraint](Constraints.md). This may be needed, for example, for a [selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) with a branch that returns a constant value for any object. Without this restriction, the current implementation may encounter a server startup error while building the constraint violation message (see the example below).

### Language

To declare a property that returns a previous value, use the [`PREV` operator](../language/PREV_operator.md). 

### Examples

```lsf
CLASS Order;
sum = DATA NUMERIC[10,2] (Order);

// Most common use: read the "before" value inside an event handler reacting to a
// change. PREV(sum(o)) returns the value at the start of the session — the database
// value, ignoring any in-session change. Change predicates (CHANGED, SET, DROPPED,
// ...) are derived from PREV as Boolean comparisons against the current value; PREV
// itself gives access to the actual previous value.
WHEN CHANGED(sum(Order o)) DO
    MESSAGE 'Sum changed: ' + PREV(sum(o)) + ' → ' + sum(o);
```

```lsf
CLASS Item;
noLimit = DATA BOOLEAN ();
maxQuantity = DATA NUMERIC[10,2] (Item);
quantity = DATA NUMERIC[10,2] (Item);

// without the IS condition the property itself does not determine the class of i: when noLimit[] is true, the value 1000000
// holds for any object, and the previous value of such a property in a constraint fails the server start
limit (Item i) = (IF noLimit() THEN 1000000 ELSE maxQuantity(i)) IF i IS Item;
// a quantity changed in the session cannot exceed the limit that was in force at the start of the session
// the change operator is required: without it the condition quantity(i) > PREV(limit(i)) would never be checked
CONSTRAINT CHANGED(quantity(Item i)) AND quantity(i) > PREV(limit(i)) MESSAGE 'Quantity exceeds the limit';
```
