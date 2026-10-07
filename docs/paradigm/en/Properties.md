---
slug: "/Properties"
title: 'Properties'
---

A *property* is an element of the system that takes an object collection (*parameters*) and returns exactly one object (the *return value*).

The kind and the specifics of how to calculate each property are determined by the [operator](Property_operators_paradigm.md) used to create the property.

### Data and calculated properties {#calculated}

Properties are either *data* or *calculated* properties. The values of a [data property](Data_properties_DATA.md) are stored and are changed by actions. The values of a *calculated* property are obtained by the rule of the operator that created the property — usually from the values of other properties — and are stored only if the property is [materialized](Materializations.md). Some calculated properties can be [changed](Property_change_CHANGE.md#changeable) too.

### Absence of a value {#null}

Instead of an object, a property can return the special value `NULL`, which means there is no value: for example, a data property is `NULL` for every object collection for which nothing has been written to it.

### Value and parameter classes {#type}

There is no class that all objects belong to: strings and numbers, for example, have no common [ancestor](Built-in_classes.md#inheritance). So, for the same parameter classes, the values of a property must belong to classes with a common ancestor: a property that returns a number for some values of a numeric parameter and a string for others, for instance, is rejected by the platform at startup.

The list of a property's parameter classes is called its *signature*. Parameter classes are set when the property is declared, and a class that is not set is inferred from the property's definition. The class of a parameter may also remain undetermined. The signature serves first of all to [find](Search_.md) a property. A parameter's class by itself does not always restrict the objects for which the property has a value: that depends on the operator that created the property. For example, the parameter classes of a data property must be specified, and if at least one object of a collection does not belong to the class of its parameter, the value is `NULL`.

The number of parameters of a property is fixed. A property may also take no parameters at all.

### Language

A property is declared with the [`=` statement](../language/=_statement.md), which sets the property's name, its parameters, and the operator that creates the property.

### Examples

```lsf
CLASS Item;

// a property associates each Item with a value kept in the database
price (Item i) = DATA NUMERIC[14,2] (Item);

// a property can be calculated from other properties
// for an item without a price, price(i) is NULL, so priceWithVAT(i) is NULL too
priceWithVAT (Item i) = price(i) * 1.20;

// a property may take no parameters
defaultVAT = DATA NUMERIC[6,2] ();
```
