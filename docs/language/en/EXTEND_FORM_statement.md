---
slug: "/EXTEND_FORM_statement"
title: 'EXTEND FORM statement'
---

The `EXTEND FORM` statement [extends](../paradigm/Form_extension.md) an existing [form](../paradigm/Forms.md).

## Syntax

```
EXTEND FORM formName 
    [formBlock1
    ...
    formBlockN]
;
```

## Description

The `EXTEND FORM` statement allows you to extend an existing form with additional [form blocks](FORM_statement.md#blocks), written just as in a form declaration. Within these blocks you can refer to the objects, properties, and actions already declared on the form — for example, to place a new element relative to an existing one, or to filter an added object group by an object already on the form.

Besides the blocks that add new elements to the form, the statement can use *extension blocks* that modify the elements already added to the form: the [object extension block](Object_blocks.md#extendobjects), the [object tree extension block](Object_blocks.md#extendtree), the [property and action extension block](Properties_and_actions_block.md#extendproperties), and the [filter group extension block](Filters_and_sortings_block.md#filtergroup).

## Parameters

- `formName`

    The name of the form being extended. [Composite ID](IDs.md#cid). It must refer to a form that already exists, declared in this or another [module](../paradigm/Modules.md); a new form is not created. The form keeps the caption and icon set at its declaration; they are not specified here.

- `formBlock1 ... formBlockN`

    Form blocks.

## Examples

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