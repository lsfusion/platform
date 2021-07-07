---
title: 'New object (NEW)'
---

The *new object* operator creates an [action](Actions.md) that creates objects of a specified [custom class](User_classes.md) for object collections where the value of some [property](Properties.md) (*condition*) is not `NULL`. The condition can be omitted, in which case it is considered to be equal to `TRUE`.

This operator also allows you to set a [primary property](Data_properties_DATA.md), to whose values the added objects will be written. If no condition is specified, by default this property is considered to be `addedObject`.

The custom class whose objects will be created by this operator must be concrete.

You can also create objects using the corresponding [option](Loop_FOR.md#addobject) in the [loop](Loop_FOR.md) operator.

### Language

To declare an action that implements objects creation, use the [`NEW` operator](NEW_operator.md). The `NEW` option in the [`FOR` operator](FOR_operator.md) is also used to implement similar functionality.

### Examples

```lsf

newSku ()  {
    LOCAL addedSkus = Sku (INTEGER);
    NEW Sku WHERE iterate(i, 1, 3) TO addedSkus(i);
    FOR Sku s = addedSkus(i) DO {
        id(s) <- 425;
        name(s) <- 'New Sku';
    }
}
```

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
