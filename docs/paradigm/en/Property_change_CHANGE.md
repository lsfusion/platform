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
-   [composition](Composition_JOIN.md) of mutable properties — the write is routed through the composition to the underlying mutable property at the resolved arguments.

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
