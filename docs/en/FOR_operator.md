---
title: 'FOR operator'
---

The `FOR` operator creates an [action](Actions.md) that implements [loop](Loop_FOR.md).

### Syntax

    FOR expression [ORDER [DESC] orderExpr1, ..., orderExprN]
    [NEW [alias =] className]
    DO action
    [ELSE alternativeAction]

It is possible to include a `NEW` block in the operator but not to specify a condition (considered equal to `TRUE`); in this case, the syntax is as follows:

    NEW [alias =] className
    action

### Description

The `FOR` operator creates an action that implements loop. This operator must add its local parameters when defining a condition. These parameters correspond to the objects being iterated and are not parameters of the created action. You can also use a `NEW` block to specify the name of the [class](Classes.md) of the object that will be created for each object collection that meets the condition. The name of this object needs to be specified. This name will be used as the name of the local parameter that the created object will be written to.

The object iteration order in the `FOR` operator can be specified with an `ORDER` block. If a new parameter is declared in the expressions that define the order (meaning that the parameter not met earlier in the `FOR` option or in the upper context), the condition of all these expressions being non- `NULL` is automatically added.

The main action is specified after the keyword `DO`; an alternative may be specified after the keyword `ELSE`.

In the case when the operator contains a `NEW` block, and no condition is specified, the main action will be called for the created object.

### Parameters

- `expression`

    [Expression](Expression.md) defining a condition. In this expression, you can both access already declared parameters and declare new local parameters.

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprK`

    A list of expressions that define the order in which object collections will be iterated over. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. If the list is undefined, iteration is performed in an arbitrary order.

- `alias`

    The name of the local parameter that will correspond to the object being created. [Simple ID](IDs.md#id).

- `className`

    The name of the class of the object to create. Defined by a [class ID](IDs.md#classid).

- `action`

    [Context-dependent action operator](Action_operators.md#contextdependent) describing the main action.

- `alternativeAction`

    Context-dependent action operator defining an alternative action. Parameters added when defining the condition/creating the object cannot be used as parameters of this action.

### Examples

```lsf
name = DATA STRING[100] (Store);

testFor  {
    LOCAL sum = INTEGER ();
    FOR iterate(i, 1, 100) DO {
        sum() <- sum() (+) i;
    }

    FOR in(Sku s) DO {
        MESSAGE 'Sku ' + id(s) + ' was selected';
    }

    FOR Store st IS Store DO { // iterating over all objects of the Store class
        FOR in(st, Sku s) DO { // iterating over all Sku for which in is set
            MESSAGE 'There is Sku ' + id(s) + ' in store ' + name(st);
        }

    }
}

newSku ()  {
    NEW s = Sku {
        id(s) <- 425;
        name(s) <- 'New Sku';
    }
}

copy (Sku old)  {
    NEW new = Sku {
        id(new) <- id(old);
        name(new) <- name(old);
    }
}

createDetails (Order o)  {
    FOR in(Sku s) NEW d = OrderDetail DO {
        order(d) <- o;
        sku(d) <- s;
    }
}
```
