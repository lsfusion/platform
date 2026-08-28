---
slug: "/Brief_import"
title: 'Brief: data import'
---

## Flat import

The [`IMPORT` operator](../language/IMPORT_operator.md) creates an action that reads a file, splits it into columns (fields), and [writes](../paradigm/Property_change_CHANGE.md) each of them into its own property or parameter.

```
IMPORT [importFormat] FROM fileExpr importDestination
```

The destination is specified in one of two ways ([`IMPORT` operator](../language/IMPORT_operator.md)):

```
TO [(objClassId1, objClassId2, ..., objClassIdK)] propertyId1 [= columnId1], ..., propertyIdN [= columnIdN] [WHERE whereId]
FIELDS [(objClassId1 objAlias1, objClassId2 objAlias2, ..., objClassIdK objAliasK)] propClassId1 [propAlias1 =] columnId1 [NULL], ..., propClassIdN [propAliasN =] columnIdN [NULL] [DO actionOperator [ELSE elseActionOperator]]
```

Rows map onto imported objects: for a numeric class the object is the row number starting from 0, for a concrete [user class](../paradigm/User_classes.md) a new object is created per row. There is at most one such object, `INTEGER` named `row` by default. The mark of an imported row is written into the property from `WHERE whereId`, by default into `System.imported[INTEGER]` ([data import](../paradigm/Data_import_IMPORT.md)).

```lsf
importSkus (FILE f) {
    IMPORT XLS SHEET 2 FROM f TO field1 = C, field2, field3 = F, field4 = A;
}
```

## Structured import and forms

Form import is the operation opposite to opening the form in the [structured view](../paradigm/Structured_view.md): the file's values are written into the form's properties so that exporting the form back recreates the original file ([Brief: data export](Brief_export.md)).

```
IMPORT formName [importFormat] [FROM (fileExpr | groupId1 = fileExpr1 [, ..., groupIdM = fileExprM])]
```

The hierarchical formats (**JSON**, **XML**) are read from one file, the flat ones (**CSV**, **XLS**, **DBF**, **TABLE**) from one file per object group; the empty group is named `root`. Without `FROM`, `System.importFile` is used.

An imported form is restricted: objects of numeric or concrete user classes only, exactly one object per group, properties and filters changeable (as a rule, [data properties](Brief_properties.md)). Flat import is a special case of it, with the form built by the platform itself. The mechanism is [in a structured view](../paradigm/In_a_structured_view_EXPORT_IMPORT.md), and the form's views are in [Brief: forms](Brief_forms.md).

## Formats and field mapping

A format is specified by its keyword and its own options:

```
JSON [ROOT rootExpr] [WHERE whereExpr] [CHARSET charsetStr]
XML [ROOT rootExpr] [ATTR] [WHERE whereExpr] [CHARSET charsetStr]
CSV [separator] [HEADER | NOHEADER] [ESCAPE | NOESCAPE] [WHERE whereExpr] [CHARSET charsetStr]
XLS [HEADER | NOHEADER] [SHEET (sheetExpr | ALL)] [WHERE whereExpr]
DBF [MEMO memoExpr] [WHERE whereExpr] [CHARSET charsetStr]
TABLE [WHERE whereExpr]
```

`XLS` reads both `xls` and `xlsx`; there is no separate `XLSX` keyword on import. Without an explicit format it is determined by the file's class — `JSONFILE`, `XMLFILE`, `CSVFILE`, `EXCELFILE`, `DBFFILE`, `TABLEFILE` — and for the `FILE` class by the extension.

The column for a property is given as `= columnId`, a simple name or a string literal; without it the column following the one given for the previous property is taken. In `FIELDS` without an alias the file's field name becomes the parameter name. `WHERE whereExpr` selects rows by a textual condition of the form `field sign value`.

## Handling imported data

`DO` belongs to the `FIELDS` form: the names listed in `FIELDS` become local parameters, `DO` is executed for each imported record with those parameters in its context, and `ELSE` runs when no record was imported. The `TO` form has no `DO` at all — the values stay in the listed properties, and the imported rows are iterated over by the mark property.

```lsf
importOrders (FILE t) {
    IMPORT FROM t FIELDS INTEGER a, DATE b, BPSTRING[50] c DO
        NEW o = Order {
            number(o) <- a;
            date(o) <- b;
            customer(o) <- c;
        }
}
```

Import writes values as an ordinary property change, so what is written lands in the current [change session](Brief_sessions.md) and is applied together with [events](Brief_events.md) and [constraints](Brief_constraints.md).

The file itself comes from a property value: the [`READ` operator](../language/READ_operator.md) reads it by URL, an [external call](Brief_integration.md) returns it, or the user picks it. Recommendations for writing an import are in [Rules: data import](../rules/Rules_import.md).
