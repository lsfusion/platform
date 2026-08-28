---
slug: "/Brief_export"
title: 'Brief: data export (EXPORT)'
---

## Exporting properties and forms

The [`EXPORT` operator](../language/EXPORT_operator.md) creates an action that exports data to a file — either from a list of properties or from a form [opened in the structured view](../paradigm/In_a_structured_view_EXPORT_IMPORT.md):

```
EXPORT [exportFormat] FROM [columnId1 =] propertyExpr1, ..., [columnIdN = ] propertyExprN
  [WHERE whereExpr] [ORDER orderExpr1 [DESC], ..., orderExprL [DESC]]
  [TOP topExpr] [OFFSET offsetExpr] [TO propertyId]
EXPORT formName [OBJECTS objName1 = expr1, ..., objNameK = exprK] [exportFormat]
  [TOP topSelect]
  [OFFSET offsetSelect]
  [TO exportTo]
```

In the first form each listed expression becomes a column of the result, `WHERE` sets the rows, `ORDER` their order. In the second form the structure of the export is set by the form itself: its object groups, the properties shown for them, its filters; the objects fixed in the `OBJECTS` block act as additional filters. The mechanism is described in [data export](../paradigm/Data_export_EXPORT.md).

```lsf
exportSkus (Store store) {
    EXPORT CSV FROM id = id(Sku s), name = name(s) WHERE in(store, s) ORDER name(s);
}
```

## Flat and hierarchical structure

Exporting a list of properties always gives a flat result — a single table of rows.

Exporting a form carries the hierarchy of its object groups into the result, but only in the **JSON** and **XML** formats. In the flat formats (**CSV**, **XLS**, **XLSX**, **DBF**, **TABLE**) each object group is exported to a separate file, and exporting a form to a single file in a flat format is not supported. The objects fixed in the `OBJECTS` block do not participate in building the group hierarchy.

How a form turns into a data structure is described in the [structured view](../paradigm/Structured_view.md).

## Formats and options

The format is written before the list of exported data as one of the variants of the [`EXPORT` operator](../language/EXPORT_operator.md):

```
JSON [CHARSET charsetStr]
XML [HEADER | NOHEADER] [ROOT rootExpr] [TAG tagExpr] [ATTR] [CHARSET charsetStr]
CSV [separator] [HEADER | NOHEADER] [ESCAPE | NOESCAPE] [CHARSET charsetStr]
XLS [SHEET sheetExpr] [HEADER | NOHEADER]
XLSX [SHEET sheetExpr] [HEADER | NOHEADER]
DBF [CHARSET charsetStr]
TABLE
```

| Format            | Options and their defaults                                                     |
| ----------------- | ------------------------------------------------------------------------------ |
| **JSON**          | `CHARSET` — `UTF-8`                                                             |
| **XML**           | `HEADER` — the `<?xml ...?>` line; `ROOT` — the root element; `TAG` — the record element; `ATTR` — values as attributes; `CHARSET` — `UTF-8` |
| **CSV**           | separator — `;`; `NOHEADER`; `ESCAPE`; `CHARSET` — `UTF-8`                       |
| **XLS**, **XLSX** | `SHEET` — the sheet name; `NOHEADER`                                             |
| **DBF**           | `CHARSET` — `CP1251`                                                             |
| **TABLE**         | none                                                                             |

## Where the result goes

The `TO` block sets the property without parameters, of a file class (`FILE`, `RAWFILE`, `JSONFILE` and so on), that the result is written to. When a list of properties is exported, and when a form is exported to a hierarchical format (**JSON**, **XML**), a single file is produced, and without `TO` it goes into the `System.exportFile` property. When the value class of the destination is `FILE`, the file extension matches the name of the format in lower case (`json`, `xml`, `csv`, `xls`, `xlsx`, `dbf`, `table`).

When a form is exported to a flat format, each object group produces its own file, so the destination is set separately for each group — `TO groupId1 = propertyId1, ...`. Groups not listed are not exported: there is no `System.exportFile` fallback here. The [empty object group](../paradigm/Static_view.md#empty) is named `root`.

```lsf
exportSku (Store store) {
    LOCAL exportedFile = FILE ();
    // flat format: the destination is set for the object group s of the exportSku form
    EXPORT exportSku OBJECTS st = store DBF CHARSET 'CP866' TO s = exportedFile;
}
```

## Defaults that shape the result

- The format, if not specified, is **JSON**.
- `WHERE`, if not specified, is the disjunction of all exported properties: the exported object sets are those for which at least one of them is not `NULL`.
- Column names, if not set, are `expr1`, ..., `exprN` by the position of the expression in the list.
- A `NULL` value is omitted from the record in **JSON** and **XML** (the key or the element is absent) and is written as an empty cell in the flat formats; the record itself remains while the `WHERE` condition holds.
- `ORDER` takes arbitrary expressions: an expression that is not among the exported ones is added to the internal query as a hidden column and does not appear in the result.
- Exporting a single value without a column name gives the value itself in **JSON**, not an object with a field.
