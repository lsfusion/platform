---
title: 'Loop (FOR)'
---

The *loop* operator creates an [action](Actions.md) that iterates all object collections for which the defined *condition* is met, and executes a defined action for each such object collection (let's call it the *main one*). You can also define an *alternative action* that will be executed only if no object collections have been found that meet the condition. The condition itself is defined as a certain [property](Properties.md). Let's say that the condition is *satisfied* if the value of this property is not `NULL`. 

By default, object collections are iterated in arbitrary order. However, the developer can explicitly define this order, if necessary. To do this, you need to specify a list of properties with values in an ascending or descending order that will define the order of object iteration.

As for other [set operations](Set_operations.md), the condition must be such that the operation is [correct](Set_operations.md#correct).

### Adding an object {#addobject}

This operator also allows you to create an object of a specified concrete [custom class](User_classes.md) for each iterated object collection. This object can then be used in the main action as a parameter.

### Language

The syntax of the loop operator is described by the [`FOR` operator](FOR_operator.md).

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
