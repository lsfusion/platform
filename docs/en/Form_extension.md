---
title: 'Form extension'
---

The [form](Forms.md) [extension](Extensions.md) technique allows the developer to extend the [structure](Form_structure.md) and [design](Interactive_view.md) of a form created in another module.

Form extension allows you to extract a specific functionality into a separate module, which when loaded will cause new components to be "embedded" into existing forms. The disadvantage of this approach is that this module must know the precise structure and design of the form which it depends on, and when these are modified the module may become inoperative.

### Language

In order to extend the structure and design of an existing form, the [`EXTEND FORM` statement](EXTEND_FORM_statement.md) must be used.

### Example

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
