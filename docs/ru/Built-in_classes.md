---
title: 'Встроенные классы'
---

*Встроенные классы* - это [классы](Classes.md), экземплярами которых являются объекты примитивных типов данных, такие как целые числа, строки, и т.п. 

|Имя класса           |Описание                  |Литералы lsFusion|
|---------------------|--------------------------|---|
|`INTEGER`            |Четырехбайтное целое число|`5`, `23`, `1000000000`|
|`LONG`               |Восьмибайтное целое число |`5l`, `23L`, `10000000000000L`|
|`DOUBLE`             |Восьмибайтное число с плавающей точкой|`5.0d`, `2.35D`|
|`NUMERIC[ , ]`       |Число с фиксированной разрядностью и точностью|`5.0`, `2.35`|
|`BOOLEAN`            |Логический тип данных     |`TRUE`, `NULL`|
|`TBOOLEAN`            |Логический тип данных (3-значения)    |`TTRUE`, `TFALSE`, `NULL`|
|`DATE`               |Дата                      |`13_07_1982`|
|`DATETIME`           |Дата и время              |`13_07_1982_18:00`|
|`TIME`               |Время                     |`18:00`|
|`YEAR`               |Год                       ||
|`STRING`, `STRING[ ]`|Строковый тип данных, при необходимости с максимальной длиной, зависимый от регистра||
|`ISTRING`, `ISTRING[ ]`|Строковый тип данных, при необходимости с максимальной длиной, независимый от регистра||
|`BPSTRING[]`         |Строковый тип данных с максимальной длиной, зависимый от регистра, с пробелами в конце|`'text'`, `'text with\nbreak'`|
|`BPISTRING[]`        |Строковый тип данных с максимальной длиной, независимый от регистра, с пробелами в конце||
|`TEXT`               |Строковый тип данных произвольной длины, зависимый от регистра||
|`RICHTEXT`           |Строковый тип данных произвольной длины с форматированием||
|`COLOR`              |Цвет|`#00ссff`, `#AA55CC`, `RGB(0, 255, 0)`|
|`FILE`               |Файл динамического типа (содержимое файла вместе с его расширением)||
|`RAWFILE`, `WORDFILE`, `IMAGEFILE`, `PDFFILE`, `EXCELFILE`, `CSVFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|Файлы конкретного типа (`RAWFILE` - файл без расширения / с неизвестным расширением)||
|`LINK`               |Символьный идентификатор-ссылка на файл (URI)||
|`RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `EXCELLINK`, `CSVLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`|Символьный идентификатор-ссылка на файл конкретного типа (`RAWLINK` - ссылка на файл без расширения / с неизвестным расширением)||

## Наследование {#inheritance}

Среди всех встроенных классов можно выделить четыре *семейства* классов (будем считать, что каждый из остальных классов образует свое семейство классов)

|Семейство классов                 |Описание                                    |
|----------------------------------|--------------------------------------------|
|Числа                             |`INTEGER`, `LONG`, `DOUBLE`, `NUMERIC [ , ]`|
|Строки                            |`STRING`, `STRING[ ]`, `ISTRING`, `ISTRING[]`, `BPSTRING[ ]`, `BPISTRING[ ]`, `TEXT`|
|Файлы конкретного типа            |`RAWFILE,` `WORDFILE, IMAGEFILE, PDFFILE,` `EXCELFILE`, `CSVFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|
|Ссылки на файлы конкретного типа  |`RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `EXCELLINK`, `CSVLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`|

Встроенные классы наследуют друг друга только в рамках одного семейства и не могут наследовать / наследоваться от пользовательских классов. Наследование в рамках одного семейства строится по принципу: более узкий класс наследуется от более широкого.

## Общий предок {#commonparentclass}

В соответствии с описанным механизмом наследования, общий предок двух встроенных классов (например для операции [выбора](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md)) определяется следующим образом:

### Строки

    result = STRING[blankPadded = s1.blankPadded OR s2.blankPadded, 
                    caseInsensitive = s1.caseInsensitive OR s2.caseInsensitive, 
                    length = MAX(s1.length, s2.length)]

где `blankPadded`, `caseInsensitive` и `length`, в свою очередь, определяются как:

|Имя класса    |blankPadded|caseInsensitive|length|
|--------------|-----------|---------------|------|
|`STRING[n]`   |false      |false          |n     |
|`ISTRING[n]`  |false      |true           |n     |
|`BPSTRING[n]` |true       |false          |n     |
|`BPISTRING[n]`|true       |true           |n     |
|`TEXT`        |false      |false          |infinite|

### Числа

    IF p1.integerPart >= p2.integerPart AND p1.precision >= p2.precision
        result = p1 
    ELSE IF p1.integerPart <= p2.integerPart AND p1.precision <= p2.precision
        result = p2 
    ELSE IF p1.integerPart > p2.integerPart  
        result = NUMERIC[p1.integerPart+p2.precision, p2.precision]
    ELSE  
        result = NUMERIC[p2.integerPart+p1.precision, p1.precision]

где `integerPart` и `precision`, в свою очередь, определяются как:

|Имя класса    |integerPart|precision|
|--------------|-----------|---------|
|`INTEGER`     |10         |0        |
|`DOUBLE`      |99999      |99999    |
|`LONG`        |20         |0        |
|`NUMERIC[l,p]`|length-precision|precision|

### Файлы конкретного типа

    IF p1 = p2
        result = p1
    ELSE
        result = RAWFILE

### Ссылки на файлы конкретного типа

    IF p1 = p2
        result = p1
    ELSE
        result = RAWLINK

Отметим, что иногда в программировании определение общего родительского класса принято ассоциировать с *неявным приведением типов*.

## Значение по умолчанию {#defaultvalue}

В некоторых случаях для встроенного класса необходимо использовать некоторое значение, которое будет заведомо отличаться от `NULL` (например, в условии импорта при [импорте данных](Data_import_IMPORT.md)). Это значение будем называть *значением по умолчанию*, и определяется оно следующим образом:

|Имя класса            |Значение по умолчанию|
|----------------------|---------------------|
|Числовые классы       |0                    |
|Строки                |Пустая строка        |
|`DATE, TIME, DATETIME`|Текущие дата, время, дата / время|
|`BOOLEAN`             |TRUE                 |
|`COLOR`               |Белый цвет           |
|Файлы конкретного типа|Пустой файл          |
|`FILE`                |Пустой файл с пустым расширением|

## Расширения файлов конкретного типа {#extension}

При преобразовании файлов конкретного типа (`JSONFILE`, `XMLFILE`, ...) к файлу динамического типа (`FILE`), как явном, так и неявном (например при [импорте данных](Data_import_IMPORT.md) без указании формата или при [взаимодействии с внешними системами](Access_to_an_external_system_EXTERNAL.md)) расширение результирующего файла определяется следующим образом:

|Имя класса |Расширение   |
|-----------|-------------|
|`RAWFILE`  |Пустая строка|
|`JSONFILE` |json         |
|`XMLFILE`  |xml          |
|`CSVFILE`  |csv          |
|`WORDFILE` |doc          |
|`EXCELFILE`|xls          |
|`HTMLFILE` |html         |
|`PDFFILE`  |pdf          |
|`IMAGEFILE`|jpg          |
|`TABLEFILE`|table        |

## Порядок определения результирующего свойства при [обращении из внешней системы](Access_from_an_external_system.md#httpresult) {#export}

|Имя класса|Имя свойства|
|----------|------------|
|`FILE`, `RAWFILE`, `WORDFILE`, `IMAGEFILE`, `PDFFILE`, `EXCELFILE`, `CSVFILE`, `HTMLFILE`, `JSONFILE`, `XMLFILE`, `TABLEFILE`|`exportFile`, `exportRawFile`, `exportWordFile`, `exportImageFile`, `exportPdfFile`, `exportExcelFile`, `exportCsvFile`, `exportHtmlFile`, `exportJsonFile`, `exportXmlFile`|
|`TEXT`, `STRING`, `BPSTRING`|`exportText`, `exportString`, `exportBPString`|
|`NUMERIC`, `LONG`, `INTEGER`, `DOUBLE`|`exportNumeric`, `exportLong`, `exportInteger`, `exportDouble`|
|`DATETIME`, `DATE`, `TIME`, `YEAR`| `exportDateTime`, `exportDate`, `exportTime`, `exportYear`|
|`LINK`, `RAWLINK`, `WORDLINK`, `IMAGELINK`, `PDFLINK`, `EXCELLINK`, `CSVLINK`, `HTMLLINK`, `JSONLINK`, `XMLLINK`, `TABLELINK`| `exportFile`, `exportRawFile`, `exportWordFile`, `exportImageFile`, `exportPdfFile`, `exportExcelFile`, `exportCsvFile`, `exportHtmlFile`, `exportJsonFile`, `exportXmlFile`|
|`BOOLEAN`, `COLOR`|`exportBoolean`, `exportColor`|
|[Пользовательские классы](User_classes.md)|`exportObject`|
