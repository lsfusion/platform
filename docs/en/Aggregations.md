---
title: 'Aggregations'
---

Aggregation is the creation of a unique (*aggregate*) of the object corresponding to each non-`NULL` value of some *aggregated* property. Each such object is assumed to have properties that map this object to each aggregated property parameter, and, conversely, a property that maps the aggregated property parameters to this object. 

The aggregated object and each aggregated property parameter must belong to a specified [class](Classes.md).

The aggregation mechanism is driven by three events:

- *Base* event — the **check** event: the platform verifies that for every non-`NULL` value of the aggregated property a corresponding aggregated object exists, and vice versa.
- *Create* event — the **resolution** event: when the aggregated property becomes non-`NULL` and no aggregated object exists yet, the platform creates it and fills its properties with the parameter values.
- *Delete* event — the **resolution** event: when the aggregated property becomes `NULL` and the aggregated object still exists, the platform deletes it.

In a particular aggregation all three events may coincide, or the create and delete events may differ both from the base event and from each other.

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
