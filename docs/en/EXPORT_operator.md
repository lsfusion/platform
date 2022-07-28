---
title: 'EXPORT operator'
---

The `EXPORT` operator: creates an [action](Actions.md) that exports [specified properties](Data_export_EXPORT.md) to a file, or, in common case, that [opens a form](In_a_structured_view_EXPORT_IMPORT.md) in a structured view. 

## Syntax

    EXPORT [exportFormat] [TOP n] FROM [columnId1 =] propertyExpr1, ..., [columnIdN = ] propertyExprN [WHERE whereExpr] [ORDER orderExpr1 [DESC], ..., orderExprL [DESC]] [TO propertyId]
    EXPORT formName [OBJECTS objName1 = expr1, ..., objNameK = exprK] [exportFormat] [TOP n] [TO (propertyId | (groupId1 = propertyId1, ..., groupIdN = propertyIdM))]

`exportFormat` can be specified by one of the following options:

    JSON [CHARSET charsetStr]
    XML [ATTR] [CHARSET charsetStr]
    CSV [separator] [HEADER | NOHEADER] [ESCAPE | NOESCAPE] [CHARSET charsetStr]
    XLS [SHEET sheetProperty] [HEADER | NOHEADER]
    XLSX [SHEET sheetProperty] [HEADER | NOHEADER]
    DBF [CHARSET charsetStr]
    TABLE

## Description

The `EXPORT` operator creates an action that exports data from the specified properties or form to a file. The following file formats are supported: **XML**, **JSON**, **CSV**, **XLS**, **XLSX**, **DBF**, **TABLE**. 

If no export file format is specified, it is considered to be `JSON`.

If the property to which the data is exported is of class `FILE`, then the extension of the resulting file is determined depending on the format as follows:

|Format|Extension|
|---------|-----|
|**JSON** |json |
|**XML**  |xml  |
|**CSV**  |csv  |
|**XLS**  |xls  |
|**XLSX** |xlsx |
|**DBF**  |dbf  |
|**TABLE**|table|

When exporting a form in an `OBJECTS` block, it is possible to add extra filters to check for the equality of the objects on the form with [the values passed](Open_form.md#params). These objects [will not participate](Structured_view.md#objects) in building the object group hierarchy.

## Parameters

### Source of export

- `formName`

    The name of the form from which you want to export data. [Composite ID](IDs.md#cid).

- `objName1 ... objNameK`

    Names of form objects for which filtered (fixed) values are specified. [Simple IDs](IDs.md#id).

- `expr1 ... exprK`

    [Expressions](Expression.md) whose values determine the filtered (fixed) values for form objects.

- `propertyExpr1, ..., propertyExprN`

    List of [expressions](Expression.md) from whose values the data is exported. Each property is mapped to a table column of the result file.

- `columnId1, ..., columnIdN`

    A list of column IDs in the resulting file into which data from the corresponding property will be exported. Each list element is either [a simple ID](IDs.md#id) or a [string literal](Literals.md#strliteral). If no ID is specified, it is considered equal to `expr<Column number>` by default.

- `whereExpr`

    An expression whose value is a condition for the export. If not specified, it is considered equal to the [disjunction](Logical_operators_AND_OR_NOT_XOR.md) of all exported properties (that is, at least one of the properties must be non-`NULL`).

- `orderExpr1, ..., orderExprL`

    List of [expressions](Expression.md) by which the exported data is sorted. Only properties present in the list `propertyExpr1, ..., propertyExprN` can be used

- `DESC`

    Keyword. Specifies reverse sort order. By default, ascending sort is used.

### Export format

- `ATTR`

    A keyword that specifies that values should be exported to the attributes of the parent tag. If not specified, the values are exported to child tags. Only applicable for export to **XML**.

- `separator`

    Delimiter in a **CSV** file. [String literal](Literals.md#strliteral). If not specified, then the default delimiter is `;`.

- `HEADER | NOHEADER`

    Keywords specifying the presence (`HEADER`) or absence (`NOHEADER`) of a header string in a **CSV**, **XLS**, or **XLSX** file. The default is `NOHEADER`.

    When using the `NOHEADER` option if the column name is one of the predefined names (`A`, `B`, ..., `Z`, `AA`, ..., `AE`), it is exported to the column with the corresponding number, and the following columns are exported to the columns next in order after this column.

- `ESCAPE | NOESCAPE`

    Keywords specifying the presence (`ESCAPE`) or absence (`NOESCAPE`) of escaping for special characters (`\r`, `\n`, `"` (double quotes) and the specified delimiter (`separator`) in a **CSV** file. It makes sense to use `NOESCAPE` only in cases where the specified delimiter is guaranteed not to occur in the data. The default is `ESCAPE`.

- `CHARSET charsetStr`

    An option specifying the encoding used for export.

    - `charsetStr`
     
        String literal that defines the encoding. 

- `sheetProperty`

  The [ID of the property](IDs.md#propertyid) whose value is used as the name of the sheet in the exported file. The property must not have parameters. It is used for `XLS` and `XLSX` export formats.
     
- `TOP n`

    Exports only the first `n` records. [Integer literal](Literals.md#intliteral).

### Export destination

- `propertyId`

    [Property ID](IDs.md#propertyid) to which the generated file will be written. This property must not have parameters and its value must be of a file class (`FILE`, `RAWFILE`, `JSONFILE`, etc.). If this property is not specified, the `System.exportFile` property is used by default.

- `groupId1, ..., groupIdM`

    Names of object groups from the exported form for which you want to export data. [Simple IDs](IDs.md#id). Used only for exporting forms to flat formats.

- `propertyId1 , ..., propertyIdM`

    [Property IDs](IDs.md#propertyid) to which the generated files for specified object groups will be written. These properties must not have parameters and their value must be of file classes (`FILE`, `RAWFILE`, `JSONFILE`, etc.). Used only for exporting forms to flat formats. For the [empty group](Static_view.md#empty) of objects, the name `root` is used. 

## Examples

```lsf
CLASS Store;

name = DATA STRING[20] (Sku);
weight = DATA NUMERIC[10,2] (Sku);

in = DATA BOOLEAN (Store, Sku);

exportSkus (Store store)  {
    // uploading to DBF all Sku for which in (Store, Sku) is specified for the desired warehouse
    EXPORT DBF CHARSET 'CP866' FROM id(Sku s), name(s), weight(s) WHERE in(store, s); 
    // uploads to CSV without header line and escaping special characters
    EXPORT CSV NOHEADER NOESCAPE FROM id(Sku s), name(s), weight(s) WHERE in(store, s); 
    // uploads JSON, sorting by property name[Sku] in descending order
    EXPORT FROM id(Sku s), name(s), weight(s) WHERE in(store, s) ORDER name(s) DESC; 
    // uploads JSON {"ff":"HI"}, as by default it gets the name value, and the platform
    // gets the object {"value":"HI"} to "HI"
    EXPORT FROM ff='HI'; 
    // uploads JSON "HI", as by default it gets the name value, and the platform
    // automatically converts the object {"value": "HI"} to "HI"
    EXPORT FROM 'HI'; 
}
```

```lsf
FORM exportSku
    OBJECTS st = Store

    OBJECTS s = Sku
    PROPERTIES(s) id, name, weight
    FILTERS in(st, s)
;

exportSku (Store store)  {
    // uploading to DBF all Sku for which in (Store, Sku) is specified for the desired warehouse
    EXPORT exportSku OBJECTS st = store DBF CHARSET 'CP866';
    EXPORT exportSku XML;
    EXPORT exportSku OBJECTS st = store CSV ',';
}
```
