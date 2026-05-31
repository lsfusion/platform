---
slug: "/TABLE_statement"
title: 'TABLE statement'
---

The `TABLE` statement creates an new [table](../paradigm/Tables.md).

### Syntax

```
TABLE name [dbName] (className1, ..., classNameN) [FULL | NODEFAULT];
```

### Description

The `TABLE` statement declares a new table and adds it to the current [module](../paradigm/Modules.md). 


### Parameters

- `name`

    Table name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](../paradigm/Naming.md#namespace).

- `dbName`

    [String literal](Literals.md#strliteral) that specifies the physical table name in the database. If omitted, the name is generated automatically.

- `className1, ..., classNameN`

    List of class names. Each name is a [class ID](IDs.md#classid). Specifies the classes of the table's key fields. The list may be empty — a table with no keys stores a single row.

- `FULL`

    The keyword that, when specified, marks the table as [full](../paradigm/Tables.md#full) (that is, containing all objects belonging to the classes of the table's key fields).  

- `NODEFAULT`

    The keyword that, when specified, excludes the table from the process of automatic [property table determining](../paradigm/Tables.md#property).

### Examples

```lsf
TABLE book (Book);

in = DATA BOOLEAN (Sku, Stock);
TABLE skuStock (Sku, Stock); // it will store the in property

price = DATA NUMERIC[10,2] (Sku, DATE);
TABLE skuDate (Sku, DATE); // it will store the Sku property

TABLE sku (Sku) FULL;
```
