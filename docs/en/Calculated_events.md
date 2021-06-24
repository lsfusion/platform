---
title: 'Calculated events'
---

*Calculated* events are events that change the value of a property when the value of some other property (*condition*) changes to a non-`NULL` value. Moreover, unlike [simple](Simple_event.md) events, this change is not made at the moment the condition is changed but is calculated each time the changed property is accessed. If the property has already been [changed](Property_change_CHANGE.md) in the same session, this change is considered higher priority than the change in the calculated event.

For each property, there can only be one calculated event that changes this property.  

### Language

To define calculated events, use the [`<- WHEN` statement](lt-_WHEN_statement.md).

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
