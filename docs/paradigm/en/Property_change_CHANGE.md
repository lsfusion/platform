---
slug: "/Property_change_CHANGE"
title: 'Property change (CHANGE)'
---

The *property change* operator creates an [action](Actions.md) that writes the value of an expression (*source*) into a property (*destination*) for every set of arguments where a third expression (*condition*) is not `NULL`. The condition may be omitted; in that case it is considered to always hold.

The source and the condition share the same arguments as the destination property. If the source evaluates to `NULL` for a set of arguments matched by the condition, `NULL` is written for that set, which erases the previously stored value.

The change is performed as one [set operation](Set_operations.md): the source and the condition are computed for all argument sets at once, over the values before the write, after which the write happens — the value written for one argument set does not affect the values computed for the others.

### Changeable properties {#changeable}

The destination property must be a *mutable* property. Mutable properties are:

-   [data properties](Data_properties_DATA.md), including local data properties;
-   properties created by the [selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) — the write is distributed over the conditions in the order they are listed: each result property receives the part of the write for which its condition holds, and the untaken remainder passes to the following conditions. For the [polymorphic form](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#poly) in which the condition is the result property itself, the condition of the write is the result's ability to accept the write as a mutable property, not its current value: a non-mutable result (for example, a constant) accepts nothing. So for a selection property with a mutable result and a constant as the default value, the write goes to the mutable result even while the current value comes from the constant;
-   [composition](Composition_JOIN.md) of a mutable property whose arguments are only its own parameters — a renaming or reordering of parameters (`h(a, b) = f(b, a)`): the write goes into the main property at the rearranged arguments. A composition through another property (`nameCustomer(o) = name(customer(o))`) is not mutable for this operator: a write into it is silently not performed — nothing changes and no error is reported (the IDE plugin marks such an assignment as an error). On a form such a property is nevertheless editable — the write goes into the link: see the [interactive view](Interactive_view.md).

:::info
In addition to the above, mutable properties are also properties created using the [extremum operator](Extremum_MAX_MIN.md) and [logical operators](Logical_operators_AND_OR_NOT_XOR.md) (which are basically varieties of the selection operator).
:::

### Language

To declare an action that implements property change, use the [`CHANGE` operator](../language/CHANGE_operator.md).

### Examples

```lsf
// set a 15 percent discount for all customers who have an order amount over 100
CLASS Customer;
discount = DATA NUMERIC[5,2] (Customer);
totalOrders = DATA NUMERIC[14,2] (Customer);
setDiscount  {
    discount(Customer c) <- 15 WHERE totalOrders(c) > 100;
}

discount = DATA NUMERIC[5,2] (Customer, Item);
in = DATA BOOLEAN (Item);
// change the discount for selected products for a customer
setDiscount (Customer c)  {
    discount(c, Item i) <- 15 WHERE in(i);
}

// copy property g to property f
f = DATA INTEGER (INTEGER);
g = DATA INTEGER (INTEGER);
copyFG  {
    f(a) <- g(a);
}
```
