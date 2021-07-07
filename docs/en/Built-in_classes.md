---
title: 'Built-in classes'
---

*Built-in classes* are [classes](Classes.md) whose instances are objects belonging to primitive data types such as integers, strings, etc. 

|Class name           |Description                   |lsFusion literals|
|---------------------|------------------------------|-----------------|
|`INTEGER`            |32-bit integer                |`5`, `23`, `1000000000`|
|`LONG`               |64-bit integer                |`5l`, `23L`, `10000000000000L`|
|`DOUBLE`             |64-bit floating point number  |`5.0d`, `2.35D`|
|`NUMERIC[ , ]`       |Number with fixed width and precision|`5.0`, `2.35`|
|`BOOLEAN`            |The logical data type         |`TRUE`, `NULL`|
|`DATE`               |Date                          |`13_07_1982`|
|`DATETIME`           |Date and time                 |`13_07_1982_18:00`|
|`TIME`               |Time                          |`18:00`|
|`YEAR`               |Year                          ||
|`STRING`, `STRING[ ]`|String data type with optional maximum length, case-sensitive||
|`ISTRING`, `ISTRING[ ]`|String data type with optional maximum length, case-insensitive||
|`BPSTRING[]`         |String data type with maximum length, case-sensitive, padded at the end with spaces|`'text'`, `'text with\nbreak'`|
|`BPISTRING[]`        |String data type with maximum length, case-insensitive, padded at the end with spaces||
|`TEXT`               |String data type of arbitrary length, case-sensitive||
|`RICHTEXT`           |String data type of arbitrary length with formatting||
|`COLOR`              |Color                         |`#00ccff`, `#AA55CC`, `RGB(0, 255, 0)`|
|`FILE`               |File of dynamic type (file content together with extension)||
|`RAWFILE`, `WORDFILE`, `IMAGEFILE`, `PDFFILE`, `EXCELFILE`, `CSVFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|Files of specific type (`RAWFILE`: file with no extension or with unknown extension)||
|`LINK`               |Link to a file (URI)          ||
|`RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `EXCELLINK`, `CSVLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`|Link to a file of a specific type (`RAWLINK`: link to a file with no extension or an unknown extension)||

## Inheritance {#inheritance}

The builtin classes can be divided into four class *families* (assuming that each of the remaining classes forms its own class family)

|Class family                     |Description                                                                                              |
|---------------------------------|---------------------------------------------------------------------------------------------------------|
|Numbers                          |`INTEGER`, `LONG`, `DOUBLE`, `NUMERIC[ , ]`|
|Strings                          |`STRING`, `STRING[ ]`, `ISTRING`, `ISTRING[]`, `BPSTRING[ ]`, `BPISTRING[ ]`, `TEXT`|
|Files of a specific type         |`RAWFILE`, `WORDFILE`, `IMAGEFILE`, `PDFFILE`, `EXCELFILE`, `CSVFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|
|Links to files of a specific type|`RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `EXCELLINK`, `CSVLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`|

The builtin classes inherit only from one another within a single family, and cannot inherit from or be inherited by user classes. Inheritance within each family works on the principle that the narrower class inherits from the broader one.

## Common ancestor {#commonparentclass}

According to this inheritance mechanism, the common ancestor of two builtin classes (e.g. for the [selection](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) operation) is determined as follows:

### Strings

    result = STRING[blankPadded = s1.blankPadded OR s2.blankPadded, 
                    caseInsensitive = s1.caseInsensitive OR s2.caseInsensitive, 
                    length = MAX(s1.length, s2.length)]
                
where `blankPadded`, `caseInsensitive` and `length` are in turn determined as:

|Class name    |blankPadded|caseInsensitive|length|
|--------------|-----------|---------------|------|
|`STRING[n]`   |false      |false          |n     |
|`ISTRING[n]`  |false      |true           |n     |
|`BPSTRING[n]` |true       |false          |n     |
|`BPISTRING[n]`|true       |true           |n     |
|`TEXT`        |false      |false          |infinite|

### Numbers

    IF p1.integerPart >= p2.integerPart AND p1.precision >= p2.precision
        result = p1 
    ELSE IF p1.integerPart >= p2.integerPart AND p1.precision >= p2.precision
        result = p2 
    ELSE IF p1.integerPart > p2.integerPart  
        result = NUMERIC[p1.integerPart+p2.precision, p2.precision]
    ELSE  
        result = NUMERIC[p2.integerPart+p1.precision, p1.precision]

where `integerPart` and `precision`, in turn, are determined as:

|Class name    |integerPart|precision|
|--------------|-----------|---------|
|`INTEGER`     |10         |0        |
|`DOUBLE`      |99999      |99999    |
|`LONG`        |20         |0        |
|`NUMERIC[l,p]`|length-precision|precision|

### Files of a specific type

    IF p1 = p2
        result = p1
    ELSE
        result = RAWFILE
  
### Links to files of a specific type

    IF p1 = p2
        result = p1
    ELSE
        result = RAWLINK
  
Note that sometimes in programming the definition of a common parent class is associated with *implicit typecasting*.

## Default value {#defaultvalue}

It is sometimes necessary to use some value for a built-in class which will differ from `NULL` (for example, in an import condition with [data import](Data_import_IMPORT.md)). Let's call this value the *default value*. It is defined as follows:

|Class name                |Default value|
|--------------------------|-------------|
|Numerical classes         |0            |
|Strings                   |The empty string|
|`DATE, TIME, DATETIME`    |The current date / time / date and time|
|`BOOLEAN`                 |TRUE         |
|`COLOR`                   |White        |
|Files of a specific type  |Empty file   |
|`FILE`                    |Empty file with empty extension|

## Extensions of specific type files {#extension}

When files of a specific type (`JSONFILE`, `XMLFILE`, ...) are cast into a file of dynamic type (`FILE`), whether explicitly or implicitly (e.g. with [data import](Data_import_IMPORT.md) without specifying a format or when [working with external systems](Access_to_an_external_system_EXTERNAL.md)), the extension of the result file is determined as follows:

|Class name |Extension       |
|-----------|----------------|
|`RAWFILE`  |The empty string|
|`JSONFILE` |json            |
|`XMLFILE`  |xml             |
|`CSVFILE`  |csv             |
|`WORDFILE` |doc             |
|`EXCELFILE`|xls             |
|`HTMLFILE` |html            |
|`PDFFILE`  |pdf             |
|`IMAGEFILE`|jpg             |
|`TABLEFILE`|table           |

## The order of determining the result property when [accessing from an external system](Access_from_an_external_system.md#httpresult) {#export}

|Class name|Property name|
|---|---|
|`FILE`, `RAWFILE`, `WORDFILE`, `IMAGEFILE`, `PDFFILE`, `EXCELFILE`, `CSVFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|`exportFile`, `exportRawFile`, `exportWordFile`, `exportImageFile`, `exportPdfFile`, `exportExcelFile`, `exportCsvFile`, `exportHtmlFile`, `exportJsonFile`, `exportXmlFile`|
|`TEXT`, `STRING`, `BPSTRING`|`exportText`, `exportString`, `exportBPString`|
|`NUMERIC`, `LONG`, `INTEGER`, `DOUBLE`|`exportNumeric`, `exportLong`, `exportInteger`, `exportDouble`|
|`DATETIME`, `DATE`, `TIME`, `YEAR`|`exportDateTime`, `exportDate`, `exportTime`, `exportYear`|
|`LINK`, `RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `EXCELLINK`, `CSVLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`|`exportFile`, `exportRawFile`, `exportWordFile`, `exportImageFile`, `exportPdfFile`, `exportExcelFile`, `exportCsvFile`, `exportHtmlFile`, `exportJsonFile`, `exportXmlFile`|
|`BOOLEAN`, `COLOR`|`exportBoolean`, `exportColor`|
|[User classes](User_classes.md)|`exportObject`|
