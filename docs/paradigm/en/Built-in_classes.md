---
slug: "/Built-in_classes"
title: 'Built-in classes'
---

*Built-in classes* are [classes](Classes.md) whose instances are objects belonging to primitive data types such as integers, strings, etc. 

|Class name           |Description                   |lsFusion literals|
|---------------------|------------------------------|-----------------|
|`INTEGER`            |32-bit integer                |`5`, `23`, `1000000000`|
|`LONG`               |64-bit integer                |`5l`, `23L`, `10000000000000L`|
|`DOUBLE`             |64-bit floating point number  |`5.0d`, `2.35D`|
|`NUMERIC`, `NUMERIC[ , ]`|Number with fixed width and precision|`5.0`, `2.35`|
|`BOOLEAN`            |Logical data type         |`TRUE`, `NULL`|
|`TBOOLEAN`           |Logical data type with a separate false value|`TTRUE`, `TFALSE`, `NULL`|
|`DATE`               |Date                          |`1982_07_13`|
|`DATETIME`, `DATETIME[ ]`|Date and time, optionally with fractional-seconds precision (0 to 6)|`1982_07_13_18:00`, `1982_07_13_18:00:00`|
|`ZDATETIME`, `ZDATETIME[ ]`|Date and time with time zone, optionally with fractional-seconds precision (0 to 6)||
|`TIME`, `TIME[ ]`    |Time, optionally with fractional-seconds precision (0 to 6)|`18:00`, `18:00:00`|
|`YEAR`               |Year                          ||
|`INTERVAL[DATE]`, `INTERVAL[DATETIME]`, `INTERVAL[TIME]`, `INTERVAL[ZDATETIME]`|Interval — a pair of boundary values (from / to) of the corresponding date / time class||
|`STRING`, `STRING[ ]`|String data type with optional maximum length, case-sensitive|`'text'`, `'text with\nbreak'`|
|`ISTRING`, `ISTRING[ ]`|String data type with optional maximum length, case-insensitive||
|`BPSTRING`, `BPSTRING[ ]`|String data type with optional maximum length, case-sensitive, stored in the database as a fixed-length string||
|`BPISTRING`, `BPISTRING[ ]`|String data type with optional maximum length, case-insensitive, stored in the database as a fixed-length string||
|`TEXT`               |String data type of arbitrary length, case-insensitive||
|`RICHTEXT`           |String data type of arbitrary length with formatting, case-insensitive||
|`HTMLTEXT`           |String data type of arbitrary length with HTML markup, case-insensitive||
|`COLOR`              |Color                         |`#00ccff`, `#AA55CC`, `RGB(0, 255, 0)`|
|`JSON`               |JSON in normalized form (key order, duplicate keys and formatting are not preserved)||
|`JSONTEXT`           |JSON stored as a text string as is||
|`XML`                |XML                           ||
|`HTML`               |HTML markup stored as a string||
|`TSVECTOR`           |Full-text search vector       ||
|`TSQUERY`            |Full-text search query        ||
|`FILE`               |File of dynamic type (file content together with extension)||
|`NAMEDFILE`          |File of dynamic type (file content together with name and extension)||
|`RAWFILE`, `WORDFILE`, `IMAGEFILE`, `PDFFILE`, `VIDEOFILE`, `DBFFILE`, `EXCELFILE`, `CSVFILE`, `TEXTFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|Files of specific type (`RAWFILE`: file with no extension or with unknown extension)||
|`LINK`               |Link to a file (URI)          ||
|`RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `VIDEOLINK`, `DBFLINK`, `EXCELLINK`, `CSVLINK`, `TEXTLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`|Link to a file of a specific type (`RAWLINK`: link to a file with no extension or an unknown extension)||

## Inheritance {#inheritance}

The built-in classes can be divided into seven class *families* (assuming that each of the remaining classes forms its own class family)

|Class family                     |Classes                                                                                                  |
|---------------------------------|---------------------------------------------------------------------------------------------------------|
|Numbers                          |`INTEGER`, `LONG`, `DOUBLE`, `NUMERIC`, `NUMERIC[ , ]`, `YEAR`|
|Strings                          |`STRING`, `STRING[ ]`, `ISTRING`, `ISTRING[ ]`, `BPSTRING`, `BPSTRING[ ]`, `BPISTRING`, `BPISTRING[ ]`, `TEXT`, `RICHTEXT`, `HTMLTEXT`|
|Date and time                    |`DATETIME`, `DATETIME[ ]`|
|Time                             |`TIME`, `TIME[ ]`|
|Date and time with time zone     |`ZDATETIME`, `ZDATETIME[ ]`|
|Files of a specific type         |`RAWFILE`, `WORDFILE`, `IMAGEFILE`, `PDFFILE`, `VIDEOFILE`, `DBFFILE`, `EXCELFILE`, `CSVFILE`, `TEXTFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|
|Links to files of a specific type|`RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `VIDEOLINK`, `DBFLINK`, `EXCELLINK`, `CSVLINK`, `TEXTLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`|

The built-in classes inherit only from one another within a single family, and cannot inherit from or be inherited by user classes. Inheritance within each family works on the principle that the narrower class inherits from the broader one. Where the classes of a family differ in several characteristics at once (blank padding, case insensitivity and maximum length for strings, the integer part and the precision for numbers), a class inherits from another one only when it is not wider in any of them; classes that are wider in one characteristic and narrower in another do not inherit from one another, and their common ancestor is a third class of the same family. In the `DATETIME`, `TIME` and `ZDATETIME` families the fractional-seconds precision is not such a characteristic: any class of such a family inherits from any other class of the same family.

A class that forms its own family is incompatible with the classes of other families and has no common ancestor with them. In particular, the `HTML` class (unlike `HTMLTEXT`, which belongs to the string family) is incompatible with the string classes: a [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) with branches of the `HTML` and `STRING` classes has no common ancestor, so such a property cannot take any values, and the server reports the `property '...' is always NULL` error at startup. Adding (`+`) an `HTML` class value to a string is not an error, but returns a plain string: the result loses the `HTML` class, and the markup is escaped when displayed.

## Common ancestor {#commonparentclass}

According to this inheritance mechanism, the common ancestor of two built-in classes (e.g. for the [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) operation) is determined as follows. If there are more than two classes, they are combined pairwise, one after another. In the rules below the two classes being combined are written in the order in which the platform combines them; that order matters only where each of the two classes inherits from the other, and in such cases the result is one of the two classes and is not determined by the pair of classes itself.

### Strings

```
IF s1 is TEXT, RICHTEXT or HTMLTEXT
    result = s1
ELSE IF s2 is TEXT, RICHTEXT or HTMLTEXT
    result = s2
ELSE
    result = STRING[blankPadded = s1.blankPadded OR s2.blankPadded, 
                    caseInsensitive = s1.caseInsensitive OR s2.caseInsensitive, 
                    length = MAX(s1.length, s2.length)]
```

where `blankPadded`, `caseInsensitive` and `length` are in turn determined as:

|Class name    |blankPadded|caseInsensitive|length|
|--------------|-----------|---------------|------|
|`STRING[n]`   |false      |false          |n     |
|`ISTRING[n]`  |false      |true           |n     |
|`BPSTRING[n]` |true       |false          |n     |
|`BPISTRING[n]`|true       |true           |n     |

A class name written without a length means unlimited length, which is greater than any `n`; if the resulting length is unlimited, the result is the class written without a length (`STRING`, `ISTRING`, `BPSTRING`, `BPISTRING`).

`TEXT`, `RICHTEXT` and `HTMLTEXT` are not blank-padded, are case-insensitive and have unlimited length, and the blank padding, case sensitivity and maximum length of the other class are not taken into account: the common ancestor of `BPSTRING[10]` and `TEXT` is `TEXT`, which is not blank-padded. The order of the two classes matters only when both of them are among `TEXT`, `RICHTEXT` and `HTMLTEXT`: these three inherit from one another in both directions, so the result is the first of the two, and such a combination should be avoided.

### Numbers

```
IF p1.integerPart >= p2.integerPart AND p1.precision >= p2.precision
    result = p1 
ELSE IF p1.integerPart <= p2.integerPart AND p1.precision <= p2.precision
    result = p2 
ELSE IF p1.integerPart > p2.integerPart  
    result = NUMERIC[p1.integerPart+p2.precision, p2.precision]
ELSE  
    result = NUMERIC[p2.integerPart+p1.precision, p1.precision]
```

where `integerPart` and `precision`, in turn, are determined as:

|Class name    |integerPart|precision|
|--------------|-----------|---------|
|`INTEGER`     |10         |0        |
|`YEAR`        |10         |0        |
|`DOUBLE`      |99999      |99999    |
|`LONG`        |20         |0        |
|`NUMERIC[l,p]`|l - p      |p        |
|`NUMERIC`     |95         |32       |

`INTEGER` and `YEAR` have the same integer part and precision, so each of them inherits from the other and the result is the first of the two.

In any `NUMERIC` class the total length (`integerPart` + `precision`) cannot exceed 127, and the precision cannot exceed 32; if the formula gives a larger total length, it is truncated to 127, which reduces the integer part of the result.

### Files of a specific type

```
IF p1 = p2
    result = p1
ELSE
    result = RAWFILE
```
  
### Links to files of a specific type

```
IF p1 = p2
    result = p1
ELSE
    result = RAWLINK
```
  
`FILE`, `NAMEDFILE` and `LINK` belong to none of these families: they are not broader classes for files and links of a specific type and have no common ancestor with them, or with each other. Two built-in classes without a common ancestor cannot be the possible results of one property — of a [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) or an [extremum](Extremum_MAX_MIN.md), for example; a value of one of them is obtained from the other only by [type conversion](Type_conversion.md), written by the developer or applied by the platform itself, as when a file of a specific type is converted to a `FILE` or a `NAMEDFILE` and [receives an extension](#extension).

## Default value {#defaultvalue}

It is sometimes necessary to use some value for a built-in class which will differ from `NULL`. Let's call this value the *default value*. It is used:

-   with [data import](Data_import_IMPORT.md) - in the import condition, and in the columns for which `NULL` values are replaced
-   with [form import](In_a_structured_view_EXPORT_IMPORT.md#importForm) - in the filters of the imported form
-   in the automatic resolution of [simple constraints](Simple_constraints.md)
-   when an object is [added](Interactive_view.md#objectoperators) on a form to which filters are applied
-   when [default objects are selected](Interactive_view.md#defaultobject) in the interactive form view - as the current value of a form object of a built-in class in a panel, when it is not determined otherwise

The default value is defined as follows:

|Class name                |Default value|
|--------------------------|-------------|
|Numerical classes         |`0`          |
|Strings                   |`''` (empty string)|
|`HTML`, `LINK`, links to files of a specific type|`''` (empty string)|
|`DATE`, `TIME`, `DATETIME`, `ZDATETIME`|The current date / time / date and time / date and time with time zone|
|`YEAR`                    |The current year|
|Interval classes          |An interval from the current moment to the current moment|
|`BOOLEAN`                 |`TRUE` (the only value of this class other than `NULL`)|
|`TBOOLEAN`                |`TTRUE`      |
|`COLOR`                   |White        |
|`JSON`, `JSONTEXT`        |`{}`         |
|Files of a specific type  |Empty file   |
|`FILE`                    |Empty file with empty extension|
|`NAMEDFILE`               |Empty file with empty name and extension|
|`XML`, `TSVECTOR`, `TSQUERY`|None       |

## Extensions of specific type files {#extension}

When files of a specific type (`JSONFILE`, `XMLFILE`, ...) are cast into a file of dynamic type (`FILE`, `NAMEDFILE`), whether explicitly or implicitly (e.g. with [data import](Data_import_IMPORT.md) without specifying a format or when [working with external systems](Access_to_an_external_system_EXTERNAL.md)), the extension of the result file is determined as follows:

|Class name  |Extension       |
|------------|----------------|
|`RAWFILE`   |The empty string|
|`JSONFILE`  |json            |
|`XMLFILE`   |xml             |
|`CSVFILE`   |csv             |
|`TEXTFILE`  |txt             |
|`WORDFILE`  |doc             |
|`EXCELFILE` |xls             |
|`HTMLFILE`  |html            |
|`PDFFILE`   |pdf             |
|`VIDEOFILE` |mp4             |
|`DBFFILE`   |dbf             |
|`IMAGEFILE` |jpg             |
|`TABLEFILE` |table           |

For `WORDFILE`, `EXCELFILE` and `IMAGEFILE`, the extension additionally depends on the content of the file:

|Class name  |Extension       |
|------------|----------------|
|`WORDFILE`  |docx, if the file is in the Office Open XML format|
|`EXCELFILE` |xlsx, if the file is in the Office Open XML format|
|`IMAGEFILE` |png / bmp, if the file is in the PNG / BMP format|

The content is taken into account only when the file value itself is passed on - shown to the user on a form, sent to an external system, or attached to an [email message](Send_mail_EMAIL.md). When a file of a specific type is converted to a file of dynamic type by a property, and also when the content does not match the formats listed above, the extension from the first table is used.

Values of classes other than files of a specific type can also be cast into a file of dynamic type; in this case the extension is determined as follows:

|Class name  |Extension       |
|------------|----------------|
|`HTML`      |html            |
|`XML`       |xml             |
|`JSON`, `JSONTEXT`|json      |
|Strings, `LINK`, links to files of a specific type|The empty string|

## Result properties {#export}

The platform declares one *result property* for each built-in class - a property without parameters that holds a value of that class. An action [called from an external system](Access_from_an_external_system.md#httpresult) passes its result to the response through them: unless the request names the properties to return, or the action has a result of its own, the response contains the value of the first property in the list below that is not `NULL`; the list is read from top to bottom. If the values of all these properties are `NULL`, the response is empty.

|Class name|Property name|
|---|---|
|`FILE`|`exportFile[]`|
|`RAWFILE`|`exportRawFile[]`|
|`WORDFILE`|`exportWordFile[]`|
|`IMAGEFILE`|`exportImageFile[]`|
|`PDFFILE`|`exportPdfFile[]`|
|`VIDEOFILE`|`exportVideoFile[]`|
|`DBFFILE`|`exportDbfFile[]`|
|`EXCELFILE`|`exportExcelFile[]`|
|`TEXTFILE`|`exportTextFile[]`|
|`CSVFILE`|`exportCsvFile[]`|
|`HTMLFILE`|`exportHtmlFile[]`|
|`JSONFILE`|`exportJsonFile[]`|
|`XMLFILE`|`exportXmlFile[]`|
|`TABLEFILE`|`exportTableFile[]`|
|`NAMEDFILE`|`exportNamedFile[]`|
|`TEXT`|`exportText[]`|
|`RICHTEXT`|`exportRichText[]`|
|`HTMLTEXT`|`exportHTMLText[]`|
|`STRING`|`exportString[]`|
|`BPSTRING`|`exportBpString[]`|
|`NUMERIC`|`exportNumeric[]`|
|`LONG`|`exportLong[]`|
|`INTEGER`|`exportInteger[]`|
|`DOUBLE`|`exportDouble[]`|
|`DATETIME`|`exportDateTime[]`|
|`ZDATETIME`|`exportZDateTime[]`|
|`INTERVAL[DATE]`|`exportIntervalDate[]`|
|`INTERVAL[DATETIME]`|`exportIntervalDateTime[]`|
|`INTERVAL[TIME]`|`exportIntervalTime[]`|
|`INTERVAL[ZDATETIME]`|`exportIntervalZDateTime[]`|
|`DATE`|`exportDate[]`|
|`TIME`|`exportTime[]`|
|`YEAR`|`exportYear[]`|
|`LINK`|`exportLink[]`|
|`RAWLINK`|`exportRawLink[]`|
|`WORDLINK`|`exportWordLink[]`|
|`IMAGELINK`|`exportImageLink[]`|
|`PDFLINK`|`exportPdfLink[]`|
|`VIDEOLINK`|`exportVideoLink[]`|
|`DBFLINK`|`exportDbfLink[]`|
|`EXCELLINK`|`exportExcelLink[]`|
|`TEXTLINK`|`exportTextLink[]`|
|`CSVLINK`|`exportCsvLink[]`|
|`HTMLLINK`|`exportHtmlLink[]`|
|`JSONLINK`|`exportJsonLink[]`|
|`XMLLINK`|`exportXmlLink[]`|
|`TABLELINK`|`exportTableLink[]`|
|`BOOLEAN`|`exportBoolean[]`|
|`TBOOLEAN`|`exportTBoolean[]`|
|`COLOR`|`exportColor[]`|
|`JSON`|`exportJSON[]`|
|`JSONTEXT`|`exportJSONText[]`|
|`XML`|`exportXML[]`|
|[User classes](User_classes.md)|`exportObject[]`|
|`TSVECTOR`|`exportTSVectorLink[]`|

## Language

A built-in class is written in code as a [class ID](../language/IDs.md#classid) - the keyword that names the class. Values of built-in classes are written as [literals](../language/Literals.md), each with its own form and constraints.
