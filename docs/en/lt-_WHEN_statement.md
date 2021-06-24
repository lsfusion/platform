---
title: '<- WHEN statement'
---

The `<- WHEN` statement creates a [calculated event](Calculated_events.md).

### Syntax

    propertyId(param1, ..., paramN) <- valueExpr WHEN eventExpr;

### Description

The `<- WHEN` statement creates a calculated event for the [property](Data_properties_DATA.md) specified on the left side of the statement. This operator can declare its own local parameters when specifying the property whose value will [change](Property_change_CHANGE.md). These parameters can then be used in expressions of the condition and value to which the property will change.

Only one calculated event can be defined for a property. 

### Parameters

- `propertyId`

    [ID of the property](IDs.md#propertyid) whose value will be changed when the event occurs.

- `param1, ..., paramN`

    [Typed parameters](IDs.md#paramid) properties whose value will be changed when the event occurs. The number of these parameters must be equal to the number of parameters of the property being changed.

- `valueExpr`

    The expression to whose value the property value must be changed.

- `eventExpr`

    An expression whose value is a condition for the generated event.

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

