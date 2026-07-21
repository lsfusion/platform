---
slug: "/Rules_export"
title: 'Rules: data export'
---

## Scope

Recommendations for exporting data with the [`EXPORT` operator](../language/EXPORT_operator.md). The common mandatory rules are in [Rules](Rules.md); this article is not part of the set always passed to the assistant and is requested when needed.

## Choosing the export source

1. Exporting a list of properties (`EXPORT FROM ...`) is used when the result is a single flat table of columns and its structure matches no existing form.

2. Exporting a form (`EXPORT formName ...`) is used when the export repeats an already existing form or when the result needs the object group hierarchy. The hierarchy is preserved only in **JSON** and **XML**; in the flat formats each object group produces a separate file, so for them the destinations of all groups must be listed in the `TO` block.

3. A form created solely for an export should be declared next to the export action and should not be added to the navigator.

## Stating explicitly what shapes the result

1. The format should be stated explicitly even when **JSON** is intended: relying on the default makes the export depend on the reader remembering that default.

2. The `WHERE` condition should be stated explicitly. Without it the condition is the disjunction of all exported properties, so the export includes every object set with at least one field filled — which almost never matches the intended set of rows.

3. Column identifiers should be given explicitly (`columnId = expr`). The default `expr1`, ..., `exprN` ties the field names in the external format to the order of the expressions, so inserting a column in the middle of the list silently changes the export contract.

4. `ORDER` allows only expressions from the exported list, so a column needed for ordering should be included in the export.

## Format options

1. Options whose default differs between formats should be set explicitly: the presence of a header row (`HEADER` / `NOHEADER`) in **CSV**, **XLS**, **XLSX**, the **CSV** separator (`;` by default) and the encoding (`CHARSET`, `UTF-8` by default and `CP1251` for **DBF**).

2. `NOESCAPE` in **CSV** may be used only when the separator is guaranteed not to occur in the data; otherwise `ESCAPE` should be kept.

3. The encoding should be determined by the receiving side's requirements rather than by the default: recipients of **DBF** files usually expect a single-byte encoding other than `UTF-8`.

## Result destination

1. The destination property in `TO` should be declared local to the export action and of a file class (`FILE`, `RAWFILE`, `JSONFILE`) rather than being a shared property: one property shared by several exports makes the result depend on the execution order.

2. The `System.exportFile` default is acceptable only for debugging and one-off exports.

3. When a form is exported to a flat format, destinations should be listed for every exported object group; the group of objects without a name is called `root`.

## Delivering the result

1. The action should be split into preparing the data, the `EXPORT` itself, and delivering the file to the recipient — writing it to the file system, sending it to an external system, or storing it in a property. This split allows the same export to be reused with different delivery methods.

2. For regular exports, building the file should be done in a separate action with no user interaction, so that it can be called both from a form and on a schedule.

## Examples

```lsf
exportedFile = DATA LOCAL FILE ();

exportShipments (Store store) {
    EXPORT CSV ';' HEADER FROM number = number(Shipment s), date = date(s), sum = sum(s)
        WHERE store(s) = store AND shipped(s)
        ORDER date(s)
        TO exportedFile;
}
```

```lsf
FORM exportOrders
    OBJECTS st = Store
    OBJECTS o = Order
    PROPERTIES(o) number, date
    FILTERS store(o) = st
;

exportOrders (Store store) {
    EXPORT exportOrders OBJECTS st = store JSON TO exportedFile;
}
```
