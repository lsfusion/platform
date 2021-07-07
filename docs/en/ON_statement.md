---
title: 'ON statement'
sidebar_label: Overview
---

The `ON` statement adds an [event](Events.md) handler.

### Syntax 

    ON eventClause eventAction;

### Description

The `ON` statement adds an event handler for the specified event. 

### Parameters

- `eventClause`

    This [event description block](Event_description_block.md) describes an event for which a handler needs to be added.

- `eventAction`

    This [context-dependent action operator](Action_operators.md#contextdependent) describes the event handler.

### Examples

```lsf
CLASS Sku;
name = DATA STRING[100] (Sku);

ON {
    LOCAL changedName = BOOLEAN (Sku);
    changedName(Sku s) <- CHANGED(name(s));
    IF (GROUP SUM 1 IF changedName(Sku s)) THEN {
        MESSAGE 'Changed ' + (GROUP SUM 1 IF changedName(Sku s)) + ' skus!!!';
    }
}

CLASS Order;

CLASS Customer;
name = DATA STRING[50] (Customer);

customer = DATA Customer (Order);
discount = DATA NUMERIC[6,2] (Order);

ON LOCAL {
    FOR CHANGED(customer(Order o)) AND name(customer(o)) == 'Best customer' DO
        discount(o) <- 50;
}
```
