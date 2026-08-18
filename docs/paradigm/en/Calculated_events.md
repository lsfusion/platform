---
slug: "/Calculated_events"
title: 'Calculated events'
---

*Calculated* events are events that change the value of a [data property](Data_properties_DATA.md) when the value of some other property (*condition*) changes to a non-`NULL` value. Moreover, unlike [simple](Simple_event.md) events, this change is not made at the moment the condition is changed but is calculated each time the changed property is accessed. If the property has already been [changed](Property_change_CHANGE.md) in the same session, this change is considered higher priority than the change in the calculated event. In the absence of such a change, the event writes the calculated value even when it equals `NULL`.

For each property, there can only be one calculated event that changes this property.  

The condition and the value of a calculated event cannot depend on the changed property itself: it would then depend on its own change, and at server startup such a cycle leads to the `Property ... is recursive` error. A guard against overwriting an explicit change is not needed here anyway — an explicit change already takes priority over the event. When the value must be set only while the property is not yet filled, use a [simple](Simple_event.md) event: there the write is performed by the handler action, and no cycle arises (see the example below).

### Language

To define calculated events, use the [`<- WHEN` statement](../language/lt-_WHEN_statement.md).

### Examples

```lsf
// when adding a client, by default, give him the specified discount
defaultDiscount = DATA NUMERIC[6,2] ();
discount = DATA NUMERIC[6,2] (Customer);
discount(Customer c) <- defaultDiscount() WHEN SET(c IS Customer);

quantity = DATA NUMERIC[10,2] (OrderDetail);
price = DATA NUMERIC[10,2] (OrderDetail);
sum = DATA NUMERIC[10,2] (OrderDetail);

sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));
```

```lsf
defaultBonus = DATA NUMERIC[6,2] ();
bonus = DATA NUMERIC[6,2] (Customer);

// wrong: the condition of the calculated event depends on the changed property - the 'is recursive' error at server startup
// bonus(Customer c) <- defaultBonus() WHEN SET(c IS Customer) AND NOT bonus(c);

// to set the bonus only for clients whose bonus is not filled yet, a simple event is used
WHEN SET(Customer c IS Customer) AND NOT bonus(c) DO bonus(c) <- defaultBonus();
```
