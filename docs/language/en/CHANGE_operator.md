---
slug: "/CHANGE_operator"
title: 'CHANGE operator'
---

The `CHANGE` operator creates an [action](../paradigm/Actions.md) that [changes properties](../paradigm/Property_change_CHANGE.md).

### Syntax

```
[CHANGE] propertyId(expr1, ..., exprN) <- valueExpr [WHERE whereExpr]
```

### Description

The `CHANGE` operator creates an action that writes the value of `valueExpr` into the property at the arguments `expr1, ..., exprN` for every set of arguments where `whereExpr` is not `NULL`. The argument list may introduce new local parameters; such parameters correspond to objects being iterated and are not parameters of the created action.

### Parameters

- `propertyId`

    [ID](IDs.md#propertyid) of the property whose value is changed. The property must be [mutable](../paradigm/Property_change_CHANGE.md#changeable).

- `expr1, ..., exprN`

    A list of [expressions](Expression.md) or [typed parameters](IDs.md#paramid) defining arguments of the property being changed. When using typed parameters, you can both reference already declared parameters and declare new local parameters; when using expressions, new local parameters cannot be added. A new parameter is declared in this list as a separate argument (`discount(Customer c) <- 15`), while an argument expression can use only parameters declared already — parameters of the action or of an enclosing operator, or typed parameters declared to the left in the same list (`discount(customer(o)) <- 15`): writing into a computed key such as `byId(LONG(Item i)) <- ...` is rejected when the module is loaded, and such a parameter is introduced outside — for example, by the [`FOR` operator](FOR_operator.md) (see the last example). The number of items in this list must equal the number of parameters of the property being changed.

- `valueExpr`

    Expression whose value is written into the property.

- `whereExpr`

    Expression whose value is the condition under which the value is written. If not specified, it is considered equal to `TRUE`.

### Examples

```lsf
// set a 15 percent discount for all customers who have an order amount over 100
CLASS Customer;
discount = DATA NUMERIC[5,2] (Customer);
totalOrders = DATA NUMERIC[14,2] (Customer);
setDiscount  {
    discount(Customer c) <- 15 WHERE totalOrders(c) > 100;
}

CLASS Item;
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

// an argument expression over the action's parameter: the discount for the order's customer
CLASS Order;
customer = DATA Customer (Order);
setOrderDiscount (Order o)  {
    discount(customer(o)) <- 15;
}

// the key is an expression over a new parameter: it cannot be declared in the argument list, so i is introduced by the FOR operator
inById = DATA LOCAL BOOLEAN (LONG);
fillInById  {
    FOR in(Item i) DO inById(LONG(i)) <- TRUE;
}
```
