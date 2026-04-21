---
title: 'Indexes'
---

An *index* stores the values of one or more table fields in the database in an ordered form. Accordingly, the index is updated whenever these values change. Due to the index, if, for example, you filter by an indexed property, you can find the objects you need very quickly, rather than viewing all the objects that exist in the system.

Only [materialized](Materializations.md) properties can be indexed.

An index can also be built on several fields of one [table](Tables.md) at once (this is effective if, for example, filtering uses several such fields simultaneously). A composite index of this kind can include both materialized properties and parameters referring to the table key fields. Such an index must contain at least one materialized property; all properties in it must be stored in one table and use the same set of parameters.

In addition to the usual index, the platform supports special `LIKE` and `MATCH` index types intended for the operators with the same names. For string fields, `LIKE` adds a specialized index for `LIKE` operations, and `MATCH` adds specialized indexes for `MATCH` and `LIKE` when the current DB adapter has the corresponding trigram/full-text support enabled. The string `MATCH` index uses the current full-text search language. For a single field of type `TSVECTOR`, the `MATCH` index creates only the specialized GIN index by that field itself.

### Language

To create indexes, use the [`INDEX` statement](INDEX_statement.md) for an index by an arbitrary field list of one table or the [`INDEXED` option](Property_options.md#indexed) for an index by one materialized property.

### Examples

```lsf
orderDate = DATA DATE (Order) INDEXED;

INDEX customer(Order o);
number = DATA STRING (Order);
INDEX 'order_number_match' MATCH number(Order o);

date = DATA DATE (Order);
INDEX date(Order o), o;

INDEX supplier(Sku s, DATE d), s, price(s, d), d;
```
