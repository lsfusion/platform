---
title: 'Aggregations'
---

Aggregation is the creation of a unique (*aggregate*) of the object corresponding to each non-`NULL` value of some *aggregated* property. Each such object is assumed to have properties that map this object to each aggregated property parameter, and, conversely, a property that maps the aggregated property parameters to this object. 

The aggregated object and each aggregated property parameter must belong to a specified [class](Classes.md).

The aggregation mechanism is implemented using two [consequences](Simple_constraints.md) with automatic resolution and an [aggregation](Grouping_GROUP.md) operator. With the help of the aggregation operator, the first consequence creates an object when the aggregated property becomes non-`NULL`, and writes the necessary values to all its properties. The second consequence deletes the object when the aggregated property becomes `NULL`.

### Language

To create aggregations, use the [operator`AGGR`](AGGR_operator.md).

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
```
