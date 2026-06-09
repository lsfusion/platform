---
slug: "/Form_extension"
title: 'Form extension'
---

The [form](Forms.md) [extension](Extensions.md) technique allows the developer to extend the [structure](Form_structure.md) of a form created in another module — adding further objects, the [properties and actions](Form_structure.md#properties) shown for them, [filters](Form_structure.md#filters), and [orders](Form_structure.md#sort), as well as refining elements the form already declares. A module can likewise customize the form's [design](Form_design.md).

Form extension allows you to extract a specific functionality into a separate module, which when loaded will cause new components to be "embedded" into existing forms. The disadvantage of this approach is that this module must know the precise structure and design of the form which it depends on, and when these are modified the module may become inoperative.

A contributed object, property, or action can be given a chosen position relative to the ones already on the form — before or after a specific one, or at the start or end. For objects this position sets their place in the form's [order of object groups](Form_structure.md#objects); that order in turn governs a property's [display group](Form_structure.md#drawgroup) and the [object group a filter applies to](Form_structure.md#filters).

### Language

In order to extend the structure of an existing form instead of creating a new one, the [`EXTEND FORM` statement](../language/EXTEND_FORM_statement.md) must be used.

### Examples

```lsf
CLASS ItemGroup;
name = DATA ISTRING[100] (ItemGroup);

itemGroup = DATA ItemGroup (Item);

EXTEND FORM items
    PROPERTIES(i) NEWSESSION DELETE // adding a delete button to the form

    OBJECTS g = ItemGroup BEFORE i // adding a product group object to the form before the product
    PROPERTIES(g) READONLY name
    // if the object was added after the object with products, then filtering 
    // would go by the group of products, and not by products
    FILTERS itemGroup(i) == g 
;
```
