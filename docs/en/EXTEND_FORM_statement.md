---
title: 'EXTEND FORM statement'
---

The `EXTEND FORM` statement [extends](Form_extension.md) an existing [form](Forms.md).

### Syntax

    EXTEND FORM formName 
        formBlock1
        ...
        formBlockN
    ;

### Description

The `EXTEND FORM` statement allows you to extend an existing form with additional [form blocks](FORM_statement.md#blocks).

### Parameters

- `formName`

    The name of the form being extended. [Composite ID](IDs.md#cid).

- `formBlock1 ... formBlockN`

    Form blocks.

### Example

```lsf
CLASS ItemGroup;
name = DATA ISTRING[100] (ItemGroup);

itemGroup = DATA ItemGroup (Item);

EXTEND FORM items
    PROPERTIES(i) NEWSESSION DELETE // adding a delete button to the form

    OBJECTS g = ItemGroup BEFORE i // adding a product group object to the form before the product
    PROPERTIES(g) READONLY name
    // if the object was added after the object with products, then filtering would go by the group of products, 
    // and not by products
    FILTERS itemGroup(i) == g 
;
```
