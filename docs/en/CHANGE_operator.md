---
title: 'CHANGE operator'
---

The `CHANGE` operator creates an [action](Actions.md) that [changes properties](Property_change_CHANGE.md).

### Syntax

    [CHANGE] propertyId(expr1, ..., exprN) <- valueExpr [WHERE whereExpr]

### Description

The `CHANGE` operator creates an action that changes a property when a condition is met. This operator  can add its own local parameters when specifying the property whose value is to be changed. These parameters correspond to the objects being iterated and are not parameters of the created action. 

The condition is defined by the `WHERE` block. If this block is not specified, it is assumed that the condition is always met. 

The keyword `CHANGE`, which defines an operator, may be omitted.

### Parameters

- `propertyId`

    [ID of the property](IDs.md#propertyid) whose value is being changed. The property must be created by certain operators so that its value can be changed. You can change the values of properties created using the operators [`DATA`](DATA_operator.md) , [`ABSTRACT`](ABSTRACT_operator.md), and `LOCAL`.

- `expr1, ..., exprN`

    A list of [expressions](Expression.md) or [typed parameters](IDs.md#paramid) defining arguments to the property being changed. When using typed parameters, you can both access already declared parameters and declare new local parameters. When using expressions, new local parameters cannot be added. The number of expressions in this list must equal to the number of parameters of the property being changed. 

- `valueExpr`

    The expression to whose value the property value must be changed.

- `whereExpr`

    An expression whose value is a condition for the change being created. If not specified, it is considered equal to `TRUE`.

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
