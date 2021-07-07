---
title: 'TABLE statement'
---

The `TABLE` statement creates an new [table](Tables.md).

### Syntax

    TABLE name(className1, ..., classNameN) [FULL | NODEFAULT];

### Description

The `TABLE` statement declares a new table and adds it to the current [module](Modules.md). 


### Parameters

- `name`

    Table name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](Naming.md#namespace).

- `className1, ..., classNameN`

    List of class names. Each name is a [class ID](IDs.md#classid). Specifies classes for the key fields of the table being created. Cannot be empty,

- `FULL`

    The keyword that, when specified, marks the table as [full](Tables.md#full) (that is, containing all objects belonging to the classes of the table's key fields).  

- `NODEFAULT`

    The keyword that, when specified, excludes the table from the process of automatic [property table determining](Tables.md#property).

### Examples

```lsf
TABLE book (Book);

in = DATA BOOLEAN (Sku, Stock);
TABLE skuStock (Sku, Stock); // it will store the in property

price = DATA NUMERIC[10,2] (Sku, DATE);
TABLE skuDate (Sku, DATE); // it will store the Sku property

TABLE sku (Sku) FULL;
```
