---
title: 'Change operators (SET, CHANGED, ...)'
---

*Change operators* determine whether some types of changes have occurred for a certain property in the current session. All these operators are derived from the [previous value operator (`PREV`)](Previous_value_PREV.md), however, it is recommended to use them to improve readability and performance. The following table shows the supported types of changes and their description:

|Operator     |Value                                                            |Description                   |
|-------------|-----------------------------------------------------------------|------------------------------|
|`SET`        |`f AND NOT PREV(f)`                                              |Value is set                  |
|`DROPPED`    |`NOT f AND PREV(f)`                                              |Value is reset                |
|`CHANGED`    |`(f OR PREV(f)) AND NOT f==PREV(f)`                              |Value is changed              |
|`SETCHANGED` |`f AND NOT f==PREV(f)`<br/>or<br/>`CHANGED(f) AND NOT DROPPED(f)`|Value is changed to non-`NULL`|
|`DROPCHANGED`|`CHANGED(f) AND NOT SET(f)`                                      |Value is either reset or changed from one non-`NULL` to another non-`NULL`|
|`SETDROPPED` |`SET(f) OR DROPPED(f)`                                           |Value is either reset or changed from `NULL` to non-`NULL`|

:::caution
These operators are computed differently inside the [event](Events.md#change) handler: in this case, they return changes from the point of the previous occurrence of this event, or rather, from the point at which all its handlers are completed.
:::

### Language

To declare a property using change operators, the following [syntax constructs](Change_operators.md) are used. 

### Examples

```lsf
quantity = DATA NUMERIC[14,2] (OrderDetail);
price = DATA NUMERIC[14,2] (OrderDetail);
sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));

createdUser = DATA CustomUser (Order);
createdUser (Order o) <- currentUser() WHEN SET(o IS Order);

numerator = DATA Numerator (Order);
number = DATA STRING[28] (Order);
series = DATA BPSTRING[2] (Order);
WHEN SETCHANGED(numerator(Order o)) AND
     NOT CHANGED(number(o)) AND
     NOT CHANGED(series(o))
     DO {
        number(o) <- curStringValue(numerator(o));
        series(o) <- series(numerator(o));
        incrementValueSession(numerator(o));
     }
;
```
