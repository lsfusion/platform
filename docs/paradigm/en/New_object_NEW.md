---
slug: "/New_object_NEW"
title: 'New object (NEW)'
---

The *new object* operator creates an [action](Actions.md) that creates objects of a specified [custom class](User_classes.md) and optionally writes each created object to a [data property](Data_properties_DATA.md). The target class must be a concrete custom class — an object cannot be added to the system without a known concrete class.

The operator has two forms.

-   In the *bulk* form, an object is created for every set of arguments where some expression (*condition*) is not `NULL`. The created object can be written to a specified target data property on each row; if no target is specified, the created object is not written anywhere.
-   In the *block* form, exactly one object is created and a body that follows the action has read access to the new object through a local name. This form is the natural way to create one object and initialize its properties in the same action.

Objects can also be created inside the [loop](Loop_FOR.md) action, which creates one object per loop iteration and exposes it to the loop body — see the [loop](Loop_FOR.md#addobject) article for the corresponding option.

### Language

To declare an action that creates objects, use the [`NEW` operator](../language/NEW_operator.md). For loop-driven creation, see the `NEW` option of the [`FOR` operator](../language/FOR_operator.md).

### Examples

```lsf
// bulk form: create three Sku objects and write each one into addedSkus(i)
newSku ()  {
    LOCAL addedSkus = Sku (INTEGER);
    NEW Sku WHERE iterate(i, 1, 3) TO addedSkus(i);
    FOR Sku s = addedSkus(i) DO {
        id(s) <- 425;
        name(s) <- 'New Sku';
    }
}

// block form: create one Sku and initialize its properties
newSku ()  {
    NEW s = Sku {
        id(s) <- 425;
        name(s) <- 'New Sku';
    }
}

// block form: create a copy of a Sku, sharing the same id and name
copy (Sku old)  {
    NEW new = Sku {
        id(new) <- id(old);
        name(new) <- name(old);
    }
}

// loop-driven creation: one OrderDetail per Sku where in(s) holds
createDetails (Order o)  {
    FOR in(Sku s) NEW d = OrderDetail DO {
        order(d) <- o;
        sku(d) <- s;
    }
}
```
