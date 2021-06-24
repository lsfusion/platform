---
title: 'Property change (CHANGE)'
---

The *property change* operator allows you to change the values of one property (*write*) to the value of another property (*read*) for all object collections for which the value of a third property (*condition*) is not `NULL`. The condition can be omitted (in which case it is considered to be equal to `TRUE`).

### Changeable properties {#changeable}

In general, the property to be written should be [data](Data_properties_DATA.md), but the platform also allows writing to properties created using the [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) operator. In this case, the platform determines the condition that is met in this selection operator for the created property; the property corresponding to that condition is written to. Accordingly, all properties that can be written to we'll call *mutable*.


:::info
In addition to the above, mutable properties are also properties created using the [extremum operator](Extremum_MAX_MIN.md) and [logical operators](Logical_operators_AND_OR_NOT_XOR.md) (which are basically varieties of the selection operator)
:::

### Language

To declare an action that implements property change, use the [`CHANGE` operator](CHANGE_operator.md).

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
