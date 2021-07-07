---
title: 'Tables'
---

For storing and calculating values of the [properties](Properties.md) the **lsFusion** platform uses a relational database. All [data properties](Data_properties_DATA.md), as well as all calculated [properties](Properties.md) marked as [materialized](Materializations.md), are stored in the fields of the *tables* in the database. For each table, there is a set of key fields with the names `key0`, `key1`, ..., `keyN`, where the object IDs are stored. All other fields store property values in such a way that in the corresponding field of each row there is a property value for the objects with IDs from the key fields. Objects of [built-in classes](Built-in_classes.md) can also be used as table keys.

When creating a table, you must specify a list of the object [classes](Classes.md) which will be the keys in this table.

### Property table determining {#property}

For each property, you can specify in which table it should be stored. In this case, the number of the table keys must be equal to the number of property parameters, and the parameter classes must match the table key classes. If the table in which the property should be stored is not set explicitly, the property will be placed automatically to the "nearest" existing table in the system (i.e., which number of keys matches the number of the property parameters and the key classes are the closest to the parameter classes). Also if necessary, you can use the special option (`NODEFAULT`) to specify that when automatically determining property tables, this table should be ignored (i.e., a property can only be put into such a table explicitly using the corresponding option (`TABLE`)).

### Table naming

For each table created in the platform, a corresponding table is created in the database, which name, depending on the selected naming policy, is defined as follows:

| Naming policy                 | Field name            |
| ----------------------------- | --------------------- |
| Full with signature (default) | `NameSpace_TableName` |
| Full without signature        | `NameSpace_TableName` |
| Short                         | `TableName`           |

The naming policy is defined using the [`db.namingPolicy`](Launch_parameters.md#namingpolicy) startup parameter.

### Field naming {#name}

The values of each property are always stored exactly in one field, which name, depending on the selected naming policy, is defined as follows:

| Naming policy                 | Field name                                                   |
| ----------------------------- | ------------------------------------------------------------ |
| Full with signature (default) | `NameSpace_PropertyName_ClassName1_ClassName2_..,ClassNameN` |
| Full without signature        | `NameSpace_PropertyName`                                     |
| Short                         | `PropertyName`                                               |

If necessary, for each property, the developer can explicitly specify the name of the field in which this property will be stored. Also, it is possible to create a custom policy for naming property fields if the above does not suit for some reason.


:::info
Using too short property naming policy (in case the number of materialized properties is large enough) can significantly complicate [naming](Naming.md) these properties (keeping them unique), or, accordingly, lead to the case when you will need to explicitly name the fields in which these properties will be stored too often.
:::

The naming policy is defined using the [`db.namingPolicy`](Launch_parameters.md#namingpolicy) startup parameter.

### Default tables

If the system cannot determine the table in which the property should be put, then a table with a name equal to `auto_<class ID 1 in the property signature>_<class ID 2 in the property signature>_...<class ID n in the property signature>` is automatically created. For example, for a property with class arguments `DATE`, `Item.Item`, `Country.Country`, `INTEGER`, the table `auto_DATE_Item_Item_Country_Country_INTEGER` will be created. However, it is recommended to avoid situations when the default table is used and explicitly specify the tables in which properties will be stored.

Also, it is possible to create a custom policy for naming tables in the platform if the basic policy does not suit for some reason.

### Default indexes

By default, a unique [index](Indexes.md) is built for each table by its key fields `key0`, `key1`, ..., `keyN` named as `pk_<table ID>` where `N` is the number of key fields in the table minus `1`. Also indexes on key fields `keyK`, ..., `keyN` with names like `<table ID>_keyK _..._ keyN_idx` are automatically added for all `K` from `1` to `N`.

### Full tables {#full}

Let's say that the table is *full* if for each of its keys it contains all existing in the system objects of this key class. In general, the fullness of a table is specified explicitly using the special option (`FULL`) and is implemented via an implicit creation of a materialized [classification](Classification_IS_AS.md) property (we will call it the *fullness* property). However, in some cases, the platform may not create this property if it determines that the table already has properties which guarantee that it contains all the necessary objects (for example, the property of belonging to the class).

By default, the fullness property is named `_FULL_TableName`. Also, when defining the field name of this property, the short naming policy is used (since there can be exactly one such field in the table and there is no point in creating bulky names).

It is worth noting that the fullness of the table is important only from the execution optimization perspective (for example, the server knows that a certain table has all objects of the required class, and instead of `LEFT JOIN` uses `INNER JOIN`, which may be critical in some cases), and just as the table mechanism itself, it does not affect the logic of the system.

### Storing belonging to the class

Belonging to the class is basic data similar to [data](Data_properties_DATA.md) properties. Thus, like data properties, this belonging is stored in the field of a certain table. At the same time, this table is determined similar to the table for the other materialized properties (assuming that the belonging to the class is a property with one parameter of this class), with the only difference being that at first only full tables are processed (i.e., they have higher priority), and only if no full tables are found, the remaining (not full) tables are processed.

If a certain table for each of its keys stores the belonging to the class of all descendants of the class of this key, firstly it is automatically marked as full (even if it was not specified explicitly), and secondly, the fullness property is not created for it (it is assumed that the role of this property is fulfilled by the property of belonging to the class itself).

By default, the property of belonging to the class is named `_CLASS_TableName`. Also, when determining the field name of this property, the short naming policy is used (since there can be exactly one such field in the table and there is no point in creating bulky names).

### Language

To create tables, use the [`TABLE` statement](TABLE_statement.md). To specify the table which should store the property, use the [`TABLE` option](Property_options.md) in the property options.

### Examples

```lsf
TABLE book (Book);

in = DATA BOOLEAN (Sku, Stock);
TABLE skuStock (Sku, Stock); // it will store the in property

price = DATA NUMERIC[10,2] (Sku, DATE);
TABLE skuDate (Sku, DATE); // it will store the Sku property

TABLE sku (Sku) FULL;
```
