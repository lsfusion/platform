---
title: 'IMPORT operator'
---

The `IMPORT` operator creates an [action](Actions.md) that imports data from a specified file into [specified properties (parameters)](Data_import_IMPORT.md) or, in general, into a [specified form](In_a_structured_view_EXPORT_IMPORT.md#importForm).

## Syntax

    IMPORT [importFormat] FROM fileExpr importDestination [DO actionOperator [ELSE elseActionOperator]]
    IMPORT formName [importFormat] [FROM (fileExpr | (groupId1 = fileExpr1 [, ..., groupIdM = fileExprM])]

`importFormat` can be specified by one of the following options:

    JSON [CHARSET charsetStr]
    XML [ATTR] [CHARSET charsetStr]
    CSV [separator] [HEADER | NOHEADER] [ESCAPE | NOESCAPE] [CHARSET charsetStr]
    XLS [HEADER | NOHEADER] [SHEET (sheetExpr | ALL)]
    DBF [CHARSET charsetStr]
    TABLE

`importDestination` can be specified by one of the following options:

    TO [(objClassId1, objClassId2, ..., objClassIdK)] propertyId1 [= columnId1], ..., propertyIdN [= columnIdN] [WHERE whereId]
    FIELDS [(objClassId1 objAlias1, objClassId2 objAlias1, ..., objClassIdK objAliasK)] propClassId1 [propAlias1 =] columnId1 [NULL], ..., propClassIdN [propAliasN =] columnIdN [NULL]

## Description

The `IMPORT` operator creates an action that imports data from a specified file into the values of specified properties or into a specified form. 

If the format of the imported file is not specified, it is determined automatically depending on the class of the imported file (or on the extension, if the class is `FILE`), in the following way:

|Format   |Extension  |Class      |
|---------|-----------|-----------|
|**JSON** |json       |`JSONFILE` |
|**XML**  |xml        |`XMLFILE`  |
|**CSV**  |csv        |`CSVFILE`  |
|**XLS**  |xls or xlsx|`EXCELFILE`|
|**DBF**  |dbf        |`DBFFILE`  |
|**TABLE**|table      |`TABLEFILE`|


:::info
The first passed file is used to automatically determine a flat file format by its extension
:::

## Parameters

### Source of import

- `fileExpr`

[Expression](Expression.md) whose value is the imported file. The value of the expression must be an object of a file class (`FILE`, `RAWFILE`, `JSONFILE` etc. ). If this expression is not specified when importing a form, then the default expression is `System.importFile()`.

- `groupId1, ..., groupIdM`

    Names of object groups of the imported form for which you want to import data. [Simple IDs](IDs.md#id). Used only for importing a form from flat formats.

- `fileExpr1, ..., fileExprM`

    Expressions whose values are files that need to be imported for the specified object groups. The value of each expression must be an object of a file class (`FILE`, `RAWFILE`, `JSONFILE` etc. ). Used only to import forms from flat formats. For the [empty object group](Static_view.md#empty), the name `root` is used. 

### Import format

- `ATTR`

    A keyword that specifies that values should be read from the attributes of an element. If not specified, then reading happens from child elements. Only applicable for import from **XML**.

- `separator`

    Delimiter in a **CSV** file. [String literal](Literals.md#strliteral). If not specified, then the default delimiter is `;`.

- `HEADER` | `NOHEADER`

    Keywords specifying the presence (`HEADER`) or absence (`NOHEADER`) of a header string in a **CSV**, or **XLS** file. The default is `NOHEADER`.

    When using the `NOHEADER` option:

    - column names are considered to be: `A`, `B`, ..., `Z`, `AA`, ...,  `AE`, ...
    - if a column is not found / does not match the type of the destination property, the value of this column is considered to be `NULL` (in other import formats, in the platform throws an error in these cases).

- `ESCAPE` | `NOESCAPE`

    Keyword specifying the presence (`ESCAPE`) or absence (`NOESCAPE`) of escaping for special characters (`\r`, `\n`, `"` (double quotes)) and the specified delimiter `separator` in a **CSV** file. It makes sense to use `NOESCAPE` only in cases where the specified delimiter is guaranteed not to occur in the data. The default is `ESCAPE`.

- `SHEET (sheetExpr | ALL)`

    An option specifying the import of a specific sheet from an Excel file. If the option is not specified, then sheet number `1` is taken.

    - `sheetExpr`
    
        An expression whose value determines the number of the sheet imported from the Excel file. The value of the expression must be of class `INTEGER` or `LONG`. Numbering starts from `1`.

    - `ALL`
    
        A keyword that means that import will be from all sheets of the excel file.

- `CHARSET charsetStr`

    An option specifying the encoding used for import.

    - `charsetStr`
    
        A string literal that defines the encoding. 

- `actionOperator`

    [Context-dependent action operator](Action_operators.md#contextdependent) describing the action that is executed for each imported record.

- `elseActionOperator`

    A context-dependent action operator describing the action that is executed if no records have been imported. Parameters into which data is imported cannot be used as parameters of this action.

### Import destination

- `formName`

    The name of the form into which data has to be imported. [Composite ID](IDs.md#cid).

- `objClassId1, ..., objClassIdK`

    Classes of the [imported](Data_import_IMPORT.md) objects. Specified by [class IDs](IDs.md#classid). `K <= 1`. By default, it is assumed that what is being imported is one object of class `INTEGER`.

- `objAlias1, ..., objAliasK`

    Names of local parameters into which imported objects are written. [Simple IDs](IDs.md#id). `K <= 1`. By default, it is assumed that one object is being imported with the name `row`.

- `propertyId1, ..., propertyIdN`

    List [property IDs](IDs.md#propertyid) into which columns (fields) of data are imported. Property parameters and their classes must match the imported objects and their classes.

- `columnId1, ..., columnIdN`

    A list of column IDs in the source file from which data will be imported to the corresponding property. Each element of the list is specified either by a simple ID or by a string literal. When the ID of a nonexistent column is specified, or in the absence of an ID, the column corresponding to the property is the column that follows the column specified for the previous property in the list, or the first if the first property is specified. For **DBF** files, column IDs are case-insensitive. 

- `whereId`

    Property ID to which [a default value](Built-in_classes.md#defaultvalue) of the class of this property value will be written for each imported object. Property parameters and classes must match the imported objects and their classes. If the property is not specified and the number of imported objects is greater than `0`, a property with the name `imported` and classes of imported objects (e.g. `System.imported[INTEGER]`) is used.

- `propClassId1, ..., propClassIdN`

    List of names of [builtin classes](Built-in_classes.md) of the imported columns.

- `propAlias1, ..., propAliasN`

    Names of local parameters into which columns (fields) of data are imported. Simple IDs. If the name is not specified, then the name of the column (field) in the source file will be used as the parameter name.

- `NULL`

    Keyword. Specifies that `NULL` values during import (if the imported format supports them) will not be replaced with default values (for example, `0` for numbers, the empty string for strings, etc. ).

## Examples

```lsf
import()  {

    LOCAL xlsFile = EXCELFILE ();

    LOCAL field1 = BPSTRING[50] (INTEGER);
    LOCAL field2 = BPSTRING[50] (INTEGER);
    LOCAL field3 = BPSTRING[50] (INTEGER);
    LOCAL field4 = BPSTRING[50] (INTEGER);

    LOCAL headField1 = BPSTRING[50] ();
    LOCAL headField2 = BPSTRING[50] ();

    INPUT f = EXCELFILE DO {
        IMPORT XLS SHEET 2 FROM f TO field1 = C, field2, field3 = F, field4 = A;
        IMPORT XLS SHEET ALL FROM f TO field1 = C, field2, field3 = F, field4 = A;

        FOR imported(INTEGER i) DO { // imported property - a system property for iterating data
            MESSAGE 'field1 value = ' + field1(i);
            MESSAGE 'field2 value = ' + field2(i);
            MESSAGE 'field3 value = ' + field3(i);
            MESSAGE 'field4 value = ' + field4(i);
       }
    }

    LOCAL t = FILE ();
    EXTERNAL SQL 'jdbc:postgresql://localhost/test?user=postgres&password=12345' EXEC 'SELECT x.a,x.b,x.c,x.d FROM orders x WHERE x.id = $1;' PARAMS '4553' TO t;
    IMPORT FROM t() FIELDS INTEGER a, DATE b, BPSTRING[50] c, BPSTRING[50] d DO        // import with FIELDS option
        NEW o = Order {
            number(o) <- a;
            date(o) <- b;
            customer(o) <- c;
            currency(o) <- GROUP MAX Currency currency IF name(currency) = d; // finding currency with this name
        }


    INPUT f = FILE DO
        IMPORT CSV '*' HEADER CHARSET 'utf-8' FROM f TO field1 = C, field2, field3 = F, field4 = A;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ROOT 'element' ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO() headField1, headField2;

    INPUT f = FILE DO
        INPUT memo = FILE DO
            IMPORT DBF MEMO memo FROM f TO field1 = 'DBFField1', field2 = 'DBFField2';
}
```

```lsf

date = DATA DATE (INTEGER);
sku = DATA BPSTRING[50] (INTEGER);
price = DATA NUMERIC[14,2] (INTEGER);
order = DATA INTEGER (INTEGER);
FORM import
    OBJECTS o = INTEGER // orders
    OBJECTS od = INTEGER // order lines
    PROPERTIES (o) dateOrder = date // importing the date from the dateOrder field
    PROPERTIES (od) sku = sku, price = price // importing product quantity from sku and price fields
    FILTERS order(od) = o // writing the top order to order

;

importForm()  {
    INPUT f = FILE DO {
        IMPORT import JSON FROM f;
        SHOW import; // showing what was imported

        // creating objects in the database
        FOR DATE date = date(INTEGER io) NEW o = Order DO {
            date(o) <- date;
            FOR order(INTEGER iod) = io NEW od = OrderDetail DO {
                price(od) <- price(iod);
                sku(od) <- GROUP MAX Sku sku IF name(sku) = sku(iod); // finding sku with this name
            }
        }
    }
}
```
