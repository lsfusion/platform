---
slug: "/Aggregations"
title: 'Aggregations'
---

Aggregation is the creation of a unique (*aggregate*) object for each distinct combination of parameter values at which some *aggregated* property is non-`NULL`. Each such object is assumed to have properties that map this object to each aggregated property parameter, and, conversely, a property that maps the aggregated property parameters to this object.

For each distinct combination of parameter values at which the aggregated property is non-`NULL`, at most one aggregated object exists; this uniqueness is the key invariant that aggregation maintains.

The aggregated object and each aggregated property parameter must belong to a specified [class](Classes.md).

The aggregation mechanism is driven by three events:

- *Base* event — the **check** event: the platform verifies that for every combination of parameter values at which the aggregated property is non-`NULL`, a corresponding aggregated object exists, and vice versa.
- *Create* event — the **resolution** event: when the aggregated property becomes non-`NULL` for some combination of parameter values and no aggregated object yet exists for it, the platform creates such an object and fills its properties with those parameter values.
- *Delete* event — the **resolution** event: when the aggregated property becomes `NULL` for a combination of parameter values whose aggregated object still exists, the platform deletes that object.

In a particular aggregation all three events may coincide, or the create and delete events may differ both from the base event and from each other.

All three events react only to changes made in [change sessions](Change_sessions.md); the correspondence is not established retroactively. In particular, if an aggregation is added when data on which the aggregated property is non-`NULL` already exists in the database, aggregated objects for such existing parameter combinations are not created — neither at server start nor later, as long as the aggregated property merely stays non-`NULL` (the create event fires when the property *becomes* non-`NULL`, not on arbitrary changes of its value). Missing aggregated objects can be created manually by making exactly the changes the create event makes (see the expansion below): for every combination of parameter values on which the aggregated property is non-`NULL` and the property mapping the parameters to the object returns nothing yet, create a new aggregated object and write the parameter values into its properties. The properties mapping the object to the parameters are ordinary [data properties](Data_properties_DATA.md), and objects created this way are indistinguishable for the aggregation from objects created by its own events.

:::info
At its core, aggregation is equivalent to the following explicit combination of properties and events. Unrolled, it expands into:

- for each parameter — a property that remembers what value that parameter had on each aggregated object;
- the reverse direction — a lookup from the parameter values back to the aggregated object (a grouping over those properties gives you exactly that);
- two event handlers: one creates a new aggregated object (and fills in its parameter values) the moment the aggregated property becomes non-`NULL` for a parameter combination that doesn't have an object yet; the other deletes the object once its parameter combination's aggregated property drops back to `NULL`.

Here's the expansion spelled out for two parameters:

```lsf
prm1 = DATA class1 (aggrClass);
prm2 = DATA class2 (aggrClass);
result = GROUP AGGR aggrClass aggrObject BY prm1(aggrObject), prm2(aggrObject);

// if aggrExpr becomes non-null, create an object of class aggrClass (equivalent to aggrExpr => result(prm1, prm2) RESOLVE LEFT)
WHEN SET(aggrExpr) AND NOT result(prm1, prm2)
    NEW aggrObject = aggrClass {
        prm1(aggrObject) <- prm1;
        prm2(aggrObject) <- prm2;
    }

// if aggrExpr becomes null, remove an object (equivalent to aggrClass aggrObject IS aggrClass => result(prm1(aggrObject),prm2(aggrObject)) RESOLVE RIGHT)
WHEN aggrClass aggrObject IS aggrClass AND DROPPED(result(prm1(aggrObject),prm2(aggrObject))) DO
    DELETE aggrObject;
```

Aggregation expresses the same behavior in one declarative line instead of the whole expansion.
:::

### Language

To create aggregations, use the [`AGGR` operator](../language/AGGR_operator.md).

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
