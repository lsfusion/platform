---
title: 'Change operators'
---

Change operators - a set of operators that determine various types of [property value changes](Change_operators_SET_CHANGED_etc.md). 

### Syntax

    typeChange(propExpr)

### Description

Change operators create [actions](Properties.md) which determine whether some types of changes have been made for a certain property in the current session.

### Parameters

- `typeChange`

    Type of the change operator. It is specified by one of the keywords:

    - `SET`
    - `CHANGED`
    - `DROPPED`
    - `SETCHANGED`
    - `DROPCHANGED`
    - `SETDROPPED`

- `propExpr`

    An [expression](Expression.md) which value defines the property that should be checked for the presence of a change.

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
