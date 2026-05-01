---
title: 'Properties'
---

A *property* is an element of the system that takes a set of objects (*parameters*) and returns exactly one object (the *return value*). 

The type and the specifics of how to calculate each property are determined by the [operator](Property_operators_paradigm.md) used to create the property.

### Type constraint {#type}

Due to implementation features, all non-`NULL` property values returned must be of the same type. That is, a property cannot return, for example, a string for one set of parameters and a number for another.

The same constraint exists for each property parameter: a property cannot have a non-`NULL` value for an object collection in which, for example, the first parameter is a string, and at the same time have a non-`NULL` value for another object collection, in which the first parameter is an object.

### Language

To create properties, use the [`=` statement](=_statement.md). 

### Examples

```lsf
CLASS Item;

// a property associates each Item with a value kept in the database
price (Item i) = DATA NUMERIC[14,2] (Item);

// a property can be computed from other properties
priceWithVAT (Item i) = price(i) * 1.20;

// a property may take no parameters — a single value
defaultVAT = DATA NUMERIC[6,2] ();
```
