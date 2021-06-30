---
title: 'Indexes'
---

Building an *index* by property allows storing all the values of this property in the database in an ordered form. Accordingly, the index is updated with every change of the indexed property value. Due to the index, if, for example, you filter by an indexed property, you can find the objects you need very quickly, rather than viewing all the objects that exist in the system.

Only [materialized](Materializations.md) properties can be indexed.

An index can also be built on several properties at once (this is effective if, for example, you need to filter by several properties simultaneously). In addition, property parameters can be included in a composite index of this kind. The built index will be named as following: `<table ID>_<property/parameter name 1>_..._<property/parameter name N>`. If the specified properties are stored in different [tables](Tables.md), then the corresponding error will be thrown when you try to build the index.

### Language

To create indexes, you must use the [`INDEX` statement](INDEX_statement.md) or the [`INDEXED` option](Property_options.md#indexed) in property options.

### Examples

```lsf
INDEX customer(Order o);

date = DATA DATE (Order);
INDEX date(Order o), o;

INDEX name(Sku s), price(s, DATE d), d;
```
