---
slug: "/NEW_operator"
title: 'NEW operator'
---

The `NEW` operator creates an [action](../paradigm/Actions.md) that [creates objects](../paradigm/New_object_NEW.md) of the specified [class](../paradigm/Classes.md).

### Syntax

The operator has two forms.

The *bulk* form creates one object per matching set of arguments and optionally writes each created object to a destination property:

```
NEW className WHERE whereExpr [TO propertyId(prm1, ..., prmN)]
```

The *block* form creates exactly one object and runs the following action with read access to it:

```
NEW [alias =] className [AUTOSET] action
```

### Description

In the *bulk* form, an object is created for every set of arguments where `whereExpr` is not `NULL`. The `WHERE` block introduces local parameters used to iterate; these parameters correspond to objects being iterated and are not parameters of the created action. If `TO` is specified, the created object is written into the [data property](../paradigm/Data_properties_DATA.md) `propertyId` at the arguments `prm1, ..., prmN` on each row; if `TO` is omitted, the created object is not written anywhere.

In the *block* form, exactly one object is created. The action that follows the operator reads the new object through the local name `alias` (or the default name `added` if `alias` is omitted). The `AUTOSET` clause optionally enables auto-filling of parent links from the form context.

To create one object per iteration of a loop, use the `NEW` option of the [`FOR` operator](FOR_operator.md): unlike the bulk form, the loop body has read access to each new object via the loop's local alias.

### Parameters

- `className`

    Name of the [custom class](../paradigm/User_classes.md) of the created objects. [Composite ID](IDs.md#cid). The class must be concrete.

- `whereExpr`

    [Expression](Expression.md) whose value is the condition under which an object is created. In this expression you can both reference already declared parameters and declare new local parameters.

- `propertyId`

    [ID](IDs.md#propertyid) of the data property into which the created object is written.

- `prm1, ..., prmN`

    List of [typed parameters](IDs.md#paramid) used as arguments of `propertyId`. Must reference parameters introduced in the `WHERE` block; existing context parameters are not allowed. The number of parameters must equal the number of parameters of `propertyId`.

- `alias`

    [Simple ID](IDs.md#id) of the local parameter that holds the created object. The default value is `added`.

- `AUTOSET`

    Keyword. If specified, after the object is created the platform writes it into the [`AUTOSET`-marked data properties](Property_options.md) whose single argument class is a parent of `className`. For each such property the platform reads the currently active object of the property's value class from the form context and writes it as the property's value at the new object.

- `action`

    [Context-dependent action operator](Action_operators.md#contextdependent) describing the action that runs with read access to the created object via `alias`.

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

// block form with the default alias
addOrder ()  {
    NEW Order {
        date(added) <- currentDate();
    }
}
```
