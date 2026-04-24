---
title: 'AGGR operator'
---

The `AGGR` operator creates an [aggregation](Aggregations.md).

### Syntax  

```
AGGR [eventClause] aggrClass WHERE aggrExpr [NEW [newEventClause]] [DELETE [deleteEventClause]]
```

### Description

In addition to the property that is the result of this operator and contains the value of the aggregated object, for each parameter the `AGGR` operator also creates a [data property](Data_properties_DATA.md) with one parameter of type `aggrClass`. The value class and name of this property match the class and name of the corresponding parameter; when the aggregated object is created, the value of the parameter is automatically written to this property.

`eventClause` specifies the base check event; `NEW` and `DELETE` specify resolution events for creating and deleting aggregated objects respectively.

Unlike other context-dependent operators, the `AGGR` operator cannot be used in [expressions](Expression.md) inside other operators (in this sense it is more like context-independent operators), or in the [`JOIN` operator](JOIN_operator.md) (inside `[= ]`)

### Parameters

- `eventClause`

    [Event description block](Event_description_block.md). The base check event. Default — global `APPLY`.

- `aggrClass`

    The value class of the aggregated object. Must be a user-defined [class](Classes.md); built-in classes are not allowed.

- `aggrExpr`

    An [expression](Expression.md) whose non-`NULL` values drive the aggregation; its typed parameters determine the parameters of the result property and of the auto-created properties for each parameter.

- `NEW`

    Keyword. Specifies the resolution event for creating aggregated objects.

- `newEventClause`

    [Event description block](Event_description_block.md). If `NEW` is absent, the resolution event inherits only the scope (`GLOBAL`/`LOCAL`) of `eventClause`; its `FORMS`, `AFTER`/`GOAFTER`, and event name are not carried over. If `NEW` is specified but `newEventClause` is omitted, the default global `APPLY` event is used.

- `DELETE`

    Keyword. Specifies the resolution event for deleting aggregated objects.

- `deleteEventClause`

    [Event description block](Event_description_block.md). If `DELETE` is absent, the resolution event inherits only the scope (`GLOBAL`/`LOCAL`) of `eventClause`; its `FORMS`, `AFTER`/`GOAFTER`, and event name are not carried over. If `DELETE` is specified but `deleteEventClause` is omitted, the default global `APPLY` event is used.

### Examples

```lsf
CLASS A; CLASS B; CLASS C;
f = DATA INTEGER (A, B);
c = AGGR C WHERE f(A a, B b) MATERIALIZED INDEXED;

CLASS AB;
ab = AGGR AB WHERE A a IS A AND B b IS B; // for each A B pair creates an object AB

CLASS Shipment 'Delivery';
date = ABSTRACT DATE (Shipment);
CLASS Invoice 'Invoice';
createShipment 'Create delivery' = DATA BOOLEAN (Invoice);
date 'Shipment date' = DATA DATE (Invoice);
CLASS ShipmentInvoice 'Delivery by invoice' : Shipment;
// creating a delivery by invoice, if the option for delivery creation is defined for the invoice
shipment(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice);
date(ShipmentInvoice si) += sum(date(invoice(si)),1); // delivery date = invoice date + 1

// LOCAL base event: all three events run within the session
sessionAggr(Invoice invoice) = AGGR LOCAL ShipmentInvoice WHERE createShipment(invoice);

// explicit NEW event clause: creation runs locally, deletion inherits the base (global) event
newLocalAggr(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice) NEW LOCAL;

// separate events: creation — global (empty NEW clause defaults to APPLY), deletion — local
splitAggr(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice) NEW DELETE LOCAL;
```
