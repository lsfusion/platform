---
slug: "/Brief_export"
title: 'Brief: data export'
---

## Data export map

A detailed map of a single area — exporting data from the system to a file. The overall element map is in [Brief](Brief.md), the full syntax is in the [`EXPORT` operator](../language/EXPORT_operator.md), and the mechanism is described in [Data export](../paradigm/Data_export_EXPORT.md).

This article is not part of the set always passed to the assistant; it is requested when needed.

## Two export sources

### Exporting properties

```
EXPORT [format] FROM [columnId =] expr, ... [WHERE expr] [ORDER expr [DESC], ...] [TOP expr] [OFFSET expr] [TO propertyId]
```

Each listed expression becomes a column of the result. The result is always flat — a single table of rows.

### Exporting a form

```
EXPORT formName [OBJECTS objName = expr, ...] [format] [TOP ...] [OFFSET ...] [TO ...]
```

The form is opened in the [structured view](../paradigm/Structured_view.md), so its object group hierarchy is carried into the result for **JSON** and **XML**. For the flat formats (**CSV**, **XLS**, **XLSX**, **DBF**, **TABLE**) each object group is exported to a separate file. Objects whose values are fixed in the `OBJECTS` block act as filters and do not participate in building the group hierarchy.

## Formats and extensions

If no format is specified, **JSON** is used. The extension of the resulting file is determined by the format when the value class of the destination property is `FILE`.

| Format   | Extension |
| -------- | --------- |
| **JSON** | json      |
| **XML**  | xml       |
| **CSV**  | csv       |
| **XLS**  | xls       |
| **XLSX** | xlsx      |
| **DBF**  | dbf       |
| **TABLE**| table     |

## Format options and their defaults

| Format       | Options                                                     | Defaults                                    |
| ------------ | ----------------------------------------------------------- | ------------------------------------------- |
| **JSON**     | `CHARSET`                                                    | `UTF-8`                                     |
| **XML**      | `HEADER` / `NOHEADER`, `ROOT`, `TAG`, `ATTR`, `CHARSET`      | `HEADER`, `UTF-8`; values in child tags     |
| **CSV**      | separator, `HEADER` / `NOHEADER`, `ESCAPE` / `NOESCAPE`, `CHARSET` | `;`, `NOHEADER`, `ESCAPE`, `UTF-8`    |
| **XLS**, **XLSX** | `SHEET`, `HEADER` / `NOHEADER`                          | `NOHEADER`                                  |
| **DBF**      | `CHARSET`                                                    | `CP1251`                                    |
| **TABLE**    | none                                                         | —                                           |

For **XML**, `HEADER` controls not a table header row but a line such as `<?xml version="1.0" encoding="UTF-8"?>`. The name of the root element is set by `ROOT` (by default the name of the exported form, or `export` when properties are exported), and the name of the element wrapping each record by `TAG` (by default the name of the form's object group).

## Result destination

- `TO propertyId` — a property without parameters whose value class is a file class (`FILE`, `RAWFILE`, `JSONFILE` and so on).
- If `TO` is not specified, the `System.exportFile` property is used.
- When a form is exported to a flat format, the destination is set per object group: `TO (groupId1 = propertyId1, ...)`; the [empty object group](../paradigm/Static_view.md#empty) is named `root`.

## Defaults that shape the result

- The format is **JSON**.
- `WHERE` is the disjunction of all exported properties, so the exported sets of objects are those for which at least one of them is not `NULL`.
- Column names are `expr1`, ..., `exprN` by the position of the expression in the list.
- `ORDER` allows only expressions listed among the exported ones.
- When a single value is exported without a column name, the **JSON** result contains the value itself rather than an object with a field.

## Examples

```lsf
exportSkus (Store store) {
    EXPORT DBF CHARSET 'CP866' FROM id(Sku s), name(s), weight(s) WHERE in(store, s);
    EXPORT CSV NOHEADER NOESCAPE FROM id(Sku s), name(s), weight(s) WHERE in(store, s);
    EXPORT FROM id(Sku s), name(s), weight(s) WHERE in(store, s) ORDER name(s) DESC;
}
```

```lsf
exportSku (Store store) {
    EXPORT exportSku OBJECTS st = store DBF CHARSET 'CP866';
    EXPORT exportSku XML;
}
```
