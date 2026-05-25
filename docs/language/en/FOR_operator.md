---
slug: "/FOR_operator"
title: 'FOR operator'
---

The `FOR` operator creates an [action](../paradigm/Actions.md) that implements [loop](../paradigm/Loop_FOR.md).

### Syntax

```
FOR expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[TOP topExpr] [OFFSET offsetExpr]
[NEW [alias =] className]
DO action
[ELSE alternativeAction]
```

It is possible to include a `NEW` block in the operator but not to specify a condition (considered equal to `TRUE`); in this case, the syntax is as follows:

```
NEW [alias =] className
action
```

### Description

The `FOR` operator creates an action that implements a loop. This operator can add its local parameters when defining a condition. These parameters correspond to the objects being iterated and are not parameters of the created action. You can also use a `NEW` block to specify the [class](../paradigm/Classes.md) of the object that will be created for each object collection that meets the condition. The created object is bound to the local parameter named by `alias` (or `added` by default), which can be used in the main action.

The object iteration order in the `FOR` operator can be specified with an `ORDER` block. If a new parameter is declared in the expressions that define the order (a parameter not previously encountered in the `FOR` clause or in the upper context), the condition that all these expressions are non-`NULL` is automatically added.

The main action is specified after the keyword `DO`; an alternative may be specified after the keyword `ELSE`.

In the case when the operator contains a `NEW` block, and no condition is specified, the main action will be called for the created object.

### Parameters

- `expression`

    [Expression](Expression.md) defining a condition. In this expression, you can both access already declared parameters and declare new local parameters.

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprN`

    A list of expressions that define the order in which object collections will be iterated over. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. If the list is undefined, iteration is performed in an arbitrary order.

- `TOP topExpr`

    Only first `n` records will participate in the iteration, where `n` is value of expression `topExpr`.

- `OFFSET offsetExpr`

    Only records with offset `m` will participate in the iteration, where `m` is value of expression `offsetExpr`.

- `alias`

    The name of the local parameter that will correspond to the object being created. [Simple ID](IDs.md#id). If omitted, the parameter is named `added`.

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
    FOR in(Sku s) NEW d = OrderDetail TOP 100 DO {
        order(d) <- o;
        sku(d) <- s;
    }
}
```
