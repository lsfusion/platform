---
slug: "/Constraints"
title: 'Constraints'
---

Constraints in the platform determine which values the [data properties](Data_properties_DATA.md) can have and which cannot. In general, a constraint is defined as a property which value should always be `NULL`.

In fact, a constraint is a [simple event](Simple_event.md), where condition is the constrained property and handling is [showing](#message) all the non-`NULL` values (in the [asynchronous message](In_a_print_view_PRINT.md#interactive) mode) and [canceling](Cancel_changes_CANCEL.md) all the changes. As well as for a simple event, you need to specify a base [event](Events.md), which determines when the defined constraint is checked. 

Compared to implementation via simple events, constraints have a set of additional advantages:

-   There is a global checking procedure upon a working database for constraints (similarly to "recalculation" technique in simple events which is not applicable here as long as the handler contains the cancel changes operator)
-   Constraints are more understandable and readable since, unlike simple events, they emphasize the static/declarative nature of these rules, i.e. their independence from the moment in time.
-   You can use the created constraint when showing dialogs for changing properties used in this constraint. In this case, an additional filter will be set in the dialog so that, when the property value changes to the selected one, the constraint is not violated.

Note that in some cases, instead of showing a message to the user and canceling the transaction, it is necessary, for example, to automatically resolve the violated constraint. In that case, it is recommended to use [simple constraints](Simple_constraints.md), or, if it is impossible, simple events.

The set of rows checked is determined by the change of the constrained property, not by the objects edited in the session: the changes are canceled when the constrained property became non-`NULL` on at least one row as a result of the applied changes. A change of any property it depends on — including a property with no parameters, such as a global threshold setting — forces this check to compare the new value of the constrained property with the pre-change one on all rows where its other operands are defined. So when the constrained property compares a large stored property with such a threshold, applying a change of the threshold alone evaluates the condition over all rows of the stored property, which on a large table can take minutes. When only the rows changed in the session must be checked, add to the condition an explicit [change condition](Change_operators_SET_CHANGED_etc.md) on the stored operand: a change of the threshold alone then triggers no check at all — but rows that violate the new threshold without being edited in the session are deliberately left unchecked.

Like any event condition, the constrained property is computed incrementally over the changes being applied. If it contains heavy aggregations over large tables (especially nested non-[materialized](Materializations.md) ones), the query built by this incremental computation can grow impractically large — even with computation hints on the properties involved. For such expensive checks, use a simple event instead: make its condition a cheap detector of the relevant changes, and in its handler read the heavy values into [local properties](Data_properties_DATA.md#local), check them, and show the message and [cancel](Cancel_changes_CANCEL.md) the changes explicitly.

### Show message {#message}

For any non-`NULL` value [output](In_a_print_view_PRINT.md) the platform uses an automatically generated [form](Forms.md), consisting of:

-   one [group of objects](Form_structure.md#objects) with the objects corresponding to the parameters of the constrained property.
-   properties with the matching classes and either belonging to [property group](Groups_of_properties_and_actions.md) `System.id` or explicitly specified when creating the constraint.
-   a [filter](Form_structure.md#filters) equal to the constrained property.
-   a global message defined by the developer when creating the constraint.

### Language

Constraints are created using the [`CONSTRAINT` statement](../language/CONSTRAINT_statement.md). 

### Examples

```lsf
// balance not less than 0
CONSTRAINT balance(Sku s, Stock st) < 0 MESSAGE 'The balance cannot be negative for ' + 
    (GROUP CONCAT 'Product: ' + name(Sku ss) + ' Warehouse: ' + name(Stock sst), '\n' IF SET(balance(ss, sst) < 0) ORDER sst);

limit 'Maximum balance' = DATA NUMERIC[16,3] ();
// check only the rows changed in the session: changing limit() alone does not re-evaluate all balances
CONSTRAINT CHANGED(balance(Sku s, Stock st)) AND balance(s, st) > limit()
    MESSAGE 'The balance exceeds the allowed maximum';

barcode = DATA STRING[15] (Sku);
// "emulation" security policy
CONSTRAINT DROPCHANGED(barcode(Sku s)) AND name(currentUser()) != 'admin'
    MESSAGE 'Only the administrator is allowed to change the barcode for an already created product';

sku = DATA Sku (OrderDetail);
in = DATA BOOLEAN (Sku, Customer);

CONSTRAINT sku(OrderDetail d) AND NOT in(sku(d), customer(order(d)))
    CHECKED BY sku[OrderDetail] // a filter by available sku when selecting an item for an order line will be applied
    MESSAGE 'In the order, a product unavailable to the user is selected for the selected customer';
```
