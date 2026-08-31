---
slug: "/Brief_integration"
title: 'Brief: integration'
---

- [Data import](#data-import)
- [Data export (EXPORT)](#data-export-export)
- [Integration](#integration)

## Data import

### Flat import

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

### Structured import and forms

Form import is the operation opposite to opening the form in the [structured view](../paradigm/Structured_view.md): the file's values are written into the form's properties so that exporting the form back recreates the original file ([Brief: data export](Brief_integration.md#data-export-export)).

```
IMPORT formName [importFormat] [FROM (fileExpr | groupId1 = fileExpr1 [, ..., groupIdM = fileExprM])]
```

The hierarchical formats (**JSON**, **XML**) are read from one file, the flat ones (**CSV**, **XLS**, **DBF**, **TABLE**) from one file per object group; the empty group is named `root`. Without `FROM`, `System.importFile` is used.

An imported form is restricted: objects of numeric or concrete user classes only, exactly one object per group, properties and filters changeable (as a rule, [data properties](Brief_logic.md#properties)). Flat import is a special case of it, with the form built by the platform itself. The mechanism is [in a structured view](../paradigm/In_a_structured_view_EXPORT_IMPORT.md), and the form's views are in [Brief: forms](Brief_view.md#forms).

### Formats and field mapping

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

### Handling imported data

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

Import writes values as an ordinary property change, so what is written lands in the current [change session](Brief_logic.md#change-sessions) and is applied together with [events](Brief_logic.md#events) and [constraints](Brief_logic.md#constraints).

The file itself comes from a property value: the [`READ` operator](../language/READ_operator.md) reads it by URL, an [external call](Brief_integration.md#integration) returns it, or the user picks it. Recommendations for writing an import are in [Rules: data import](../rules/Rules_integration.md).

## Data export (EXPORT)

### Exporting properties and forms

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

### Flat and hierarchical structure

Exporting a list of properties always gives a flat result — a single table of rows.

Exporting a form carries the hierarchy of its object groups into the result, but only in the **JSON** and **XML** formats. In the flat formats (**CSV**, **XLS**, **XLSX**, **DBF**, **TABLE**) each object group is exported to a separate file, and exporting a form to a single file in a flat format is not supported. The objects fixed in the `OBJECTS` block do not participate in building the group hierarchy.

How a form turns into a data structure is described in the [structured view](../paradigm/Structured_view.md).

### Formats and options

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

### Where the result goes

The `TO` block sets the property without parameters, of a file class (`FILE`, `RAWFILE`, `JSONFILE` and so on), that the result is written to. When a list of properties is exported, and when a form is exported to a hierarchical format (**JSON**, **XML**), a single file is produced, and without `TO` it goes into the `System.exportFile` property. When the value class of the destination is `FILE`, the file extension matches the name of the format in lower case (`json`, `xml`, `csv`, `xls`, `xlsx`, `dbf`, `table`).

When a form is exported to a flat format, each object group produces its own file, so the destination is set separately for each group — `TO groupId1 = propertyId1, ...`. Groups not listed are not exported: there is no `System.exportFile` fallback here. The [empty object group](../paradigm/Static_view.md#empty) is named `root`.

```lsf
exportSku (Store store) {
    LOCAL exportedFile = FILE ();
    // flat format: the destination is set for the object group s of the exportSku form
    EXPORT exportSku OBJECTS st = store DBF CHARSET 'CP866' TO s = exportedFile;
}
```

### Defaults that shape the result

- The format, if not specified, is **JSON**.
- `WHERE`, if not specified, is the disjunction of all exported properties: the exported object sets are those for which at least one of them is not `NULL`.
- Column names, if not set, are `expr1`, ..., `exprN` by the position of the expression in the list.
- A `NULL` value is omitted from the record in **JSON** and **XML** (the key or the element is absent) and is written as an empty cell in the flat formats; the record itself remains while the `WHERE` condition holds.
- `ORDER` takes arbitrary expressions: an expression that is not among the exported ones is added to the internal query as a hidden column and does not appear in the result.
- Exporting a single value without a column name gives the value itself in **JSON**, not an object with a field.

## Integration

### Access from outside

From outside, an [action](../paradigm/Actions.md) is called with parameters, and the values of the listed properties without parameters come back as the result. The action is specified in one of three ways: `EXEC` — by name, `EVAL` and `EVAL ACTION` — by supplied lsFusion code (from inside, the same code is run by the [`EVAL` operator](../language/EVAL_operator.md)).

Requests are served by the application server (port `7651`) and by the web server:

- Action API — `/exec`, `/eval`, `/eval/action`, both servers;
- Form API — `/form`, web server only: driving a form in the interactive view;
- File API — `/files/list`, `/files/read`, `/files/search`, web server only: reading the application's classpath.

Whether such a call is accepted at all is set by a working parameter — [`enableAPI`](../paradigm/Working_parameters.md) for the Action and File APIs, `enableUI` for the Form API, which the platform counts as user interface rather than as a program interface. An action that turns out to be interactive is routed to a running client of the user, and needs `enableUI` on top of the `enableAPI` check. The [`@@api`](../language/Action_options.md) action option allows one specific action at `enableAPI=0` (an authenticated user is still required), and `@@noauth` bypasses both the authentication check and `enableAPI`. The annotation marks a named action, so it does not extend to `/eval` and `/eval/action`, which run arbitrary code, nor to the File API, where no action is called. The mechanism is [access from an external system](../paradigm/Access_from_an_external_system.md).

From the same JVM or the same SQL server the elements are reached directly — by Java code ([Java API for integrations](../paradigm/Java_integration_API.md)) or by SQL against the platform's tables ([access from an internal system](../paradigm/Access_from_an_internal_system.md)).

### External calls (EXTERNAL)

The [`EXTERNAL` operator](../language/EXTERNAL_operator.md) creates an action performing a single call to an external system: parameters go in `PARAMS`, results into the properties without parameters listed in `TO`.

```
EXTERNAL externalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

`externalCall`:

```
HTTP [CLIENT] [requestType] connectionStrExpr httpOption1 ... httpOptionN
TCP [CLIENT] connectionStrExpr
UDP [CLIENT] connectionStrExpr
SQL connectionStrExpr EXEC execStrExpr
LSF connectionStrExpr lsfExecType execStrExpr
DBF connectionStrExpr APPEND [CHARSET charsetLiteral]
```

`HTTP` is a request to the given string, `TCP` and `UDP` send a file's bytes to a socket, `SQL` runs a command on a third-party SQL server, `LSF` calls an action on another lsFusion server, `DBF` appends table rows to a `.dbf` file ([access to an external system](../paradigm/Access_to_an_external_system_EXTERNAL.md)). `SQL`, `TCP` and `DBF` connections are reused inside a [`NEWCONNECTION`](../language/NEWCONNECTION_operator.md) block; a file is fetched by URL with the [`READ` operator](../language/READ_operator.md).

```lsf
readRate () {
    EXTERNAL HTTP GET r'https://www.lsfusion.org/rate?cur=$1' PARAMS r'USD' TO exportFile;
}
```

### Internal calls (INTERNAL)

The [`INTERNAL` operator](../language/INTERNAL_operator.md) runs code inside the deployment's own components: Java in the application-server JVM, a JavaScript function or a resource in the user's web client (`CLIENT`), SQL against the platform's own database (`DB`).

```
INTERNAL [CLIENT] [syncType] className [(classId1, ..., classIdN)] [NULL]
INTERNAL [syncType] <{anyTokens}> [NULL]
INTERNAL internalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

The Java target is a class extending `InternalAction`, given by its name or as an inline code fragment in `<{ }>`. Such code writes values straight into lsFusion properties within the same [change session](Brief_logic.md#change-sessions); what it can reach is in [Java API for integrations](../paradigm/Java_integration_API.md). The mechanism of all three types is [internal call (`INTERNAL`)](../paradigm/Internal_call_INTERNAL.md), and the operator's place next to `FORMULA` is [access to an internal system](../paradigm/Access_to_an_internal_system_INTERNAL_FORMULA.md).

```lsf
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>;
```

### Declarative integration (FORMULA, CUSTOM, JSON)

The [`FORMULA` operator](../language/FORMULA_operator.md) creates a property computed by an SQL expression, possibly a different one per DBMS; the table-valued form maps the property onto a whole table ([custom formula](../paradigm/Custom_formula_FORMULA.md)).

```
FORMULA [NULL] [className [valueId]] implList [( paramList )] [NULL]
```

`CUSTOM` hands rendering to a JavaScript function in the client: on an object group as `CUSTOM renderFunction [OPTIONS optionsExpr]` ([object blocks](../language/Object_blocks.md)), on a property as `CUSTOM renderFunction [CHANGE [editFunction]]` ([properties and actions block](../language/Properties_and_actions_block.md)). The function is given the view's own local controller, which reads and changes what this view shows; the form controller — and with it the server calls — is reached from it as `controller.form` ([How-to: Custom Components](../how-to/How-to_Custom_components_objects.md)).

The [`JSON` and `JSONTEXT` operators](../language/JSON_operator.md) create a property building JSON out of a list of properties or out of a form.

```
jsonKeyword FROM [columnId1 =] propertyExpr1, ..., [columnIdN =] propertyExprN
  [WHERE whereExpr]
  [ORDER orderExpr1 [DESC], ..., orderExprL [DESC]]
  [TOP topExpr] [OFFSET offsetExpr]
jsonKeyword ( formName [OBJECTS objName1 = expr1, ..., objNameK = exprK]
  [FILTERS filterExpr1, ..., filterExprP]
  [TOP topSelect] [OFFSET offsetSelect] )
```

`jsonKeyword` is `JSON` or `JSONTEXT`.
