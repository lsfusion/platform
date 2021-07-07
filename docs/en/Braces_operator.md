---
title: '{...} operator'
---

The `{...}` operator creates [actions](Actions.md) that executes a [sequence of other actions](Sequence.md). 

### Syntax

    {
        operator1
        ...
        operatorN
    }

Operators can be of two types:

    actionOperator
    LOCAL [NESTED] name1, ..., nameN = returnClass (paramClass1, ..., paramClassN)

### Description

A sequence of [action operators](Action_operators.md) and `LOCAL` operators enclosed in braces creates a new action that sequentially executes specified actions and creates specified [local properties](Data_properties_DATA.md). The area of visibility of the local properties created inside the `{...}` operator ends at the end of this operator.

### Parameters

- `actionOperator`

    A [context-dependent action operator](Action_operators.md#contextdependent). Each operator is followed by a semicolon, except for operators ending in a closing brace. Extra semicolons are not an error.

- `NESTED`

    A keyword that, when specified, marks the local property as [nested](Session_management.md#nested); that is, all of its changes will be visible in new sessions, and when these sessions are closed, changes to this property will get to the current session. Note that this behavior is similar to the behavior of a regular local property (not `NESTED`) when using the [`NEWSESSION` operator](NEWSESSION_operator.md) with the specified keyword `NESTED LOCAL` (or just `NESTED` if this local property is explicitly specified in the property list)

- `name1, ..., nameN`

    A list of names of the local properties being created. The names are defined by [simple ID's](IDs.md#id).

- `returnClass`

    The [class ID](IDs.md#classid) of the returned value of the local property. 

- `argumentClass1, ..., argumentClassN`

    A list of argument class ID's of the local property.

### Examples

```lsf
CLASS Currency;
name = DATA STRING[30] (Currency);
code = DATA INTEGER (Currency);

CLASS Order;
currency = DATA Currency (Order);
customer = DATA STRING[100] (Order);
copy 'Copy' (Order old)  {
    NEW new = Order { // an action is created that consists of the sequential execution of two actions
        currency(new) <- currency(old); // a semicolon is put after each statement
        customer(new) <- customer(old);
    } // there is no semicolon in this line, because the operator ends in }
}

loadDefaultCurrency(ISTRING[30] name, INTEGER code)  {
    NEW c = Currency {
        name(c) <- name;
        code(c) <- code;
    }
}
run ()  {
    loadDefaultCurrency('USD', 866);
    loadDefaultCurrency('EUR', 1251);
}
```
