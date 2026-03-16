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

`eventClause` specifies the base check event; `NEW` and `DELETE` specify resolution events for creating and deleting aggregated objects respectively. If `NEW` or `DELETE` is absent, the corresponding resolution event inherits the scope of the base event. If the keyword is specified but its event description block is omitted, the default global `APPLY` event is used.

:::info
Creating an aggregation is in many ways similar to the following statements (example for 2 parameters):

```lsf
prm1 = DATA class1 (aggrClass);
prm2 = DATA class2 (aggrClass);
result = GROUP AGGR aggrClass aggrObject BY prm1(aggrObject), prm2(aggrObject);

// if aggrExpr becomes non-null, create an object of class aggrClass (equivalent to whereExpr => result (prm1, prm2) RESOLVE LEFT)
WHEN SET(aggrExpr) AND NOT result(prm1, prm2)
    NEW aggrObject = aggrClass {
        prm1(aggrObject) <- prm1;
        prm2(aggrObject) <- prm2;
    }

// if aggrExpr becomes null, remove an object (equivalent to aggrClass aggrObject IS aggrClass => result(prm1(aggrObject),prm2(aggrObject)) RESOLVE RIGHT)
WHEN aggrClass aggrObject IS aggrClass AND DROPPED(result(prm1(aggrObject),prm2(aggrObject))) DO
    DELETE aggrObject;
```

but it is a more declarative and readable statement, and therefore using it is recommended
:::

Unlike other context-dependent operators, the `AGGR` operator cannot be used in [expressions](Expression.md) inside other operators (in this sense it is more like context-independent operators), or in the [`JOIN` operator](JOIN_operator.md) (inside `[= ]`)

### Parameters

- `eventClause`

    [Event description block](Event_description_block.md). The base check event. Default — global `APPLY`.

- `aggrClass`

    The value class of the aggregated object.

- `aggrExpr`

    An [expression](Expression.md) whose value defines an aggregated property.

- `NEW`

    Keyword. Specifies the resolution event for creating aggregated objects.

- `newEventClause`

    [Event description block](Event_description_block.md). If `NEW` is absent — `eventClause` is used. If `NEW` is specified but `newEventClause` is omitted, the default global `APPLY` event is used.

- `DELETE`

    Keyword. Specifies the resolution event for deleting aggregated objects.

- `deleteEventClause`

    [Event description block](Event_description_block.md). If `DELETE` is absent — `eventClause` is used. If `DELETE` is specified but `deleteEventClause` is omitted, the default global `APPLY` event is used.

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

// base LOCAL event: creation/deletion handled within the session
sessionAggr(Invoice invoice) = AGGR ShipmentInvoice LOCAL WHERE createShipment(invoice);

// separate events: creation — global (default), deletion — local
splitAggr(Invoice invoice) = AGGR ShipmentInvoice WHERE createShipment(invoice) NEW DELETE LOCAL;
```
