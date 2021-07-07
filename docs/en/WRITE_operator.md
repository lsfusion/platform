---
title: 'WRITE operator'
---

The `WRITE` operator creates an [action](Actions.md) that [writes a file](Write_file_WRITE.md) from a property to an external resource. 

### Syntax

    WRITE [CLIENT [DIALOG]] fileExpr TO urlExpr [APPEND]

### Description

The `WRITE` operator creates an action that writes a file from the property to an external resource located at the specified URL.

The following URL types are supported:

    [file://]path_to_file
    [s]ftp://username:password[;charset]@host:port[/path_to_file][?passivemode=true|false]

It is assumed that the file extension is not specified in the URL (that is, the period `.` is also considered a part of the file name). This extension is determined automatically based on the class of the file being written:

|Extension                                  |Class      |
|-------------------------------------------|-----------|
|read from the passed object                |`FILE`     |
|json                                       |`JSONFILE` |
|xml                                        |`XMLFILE`  |
|csv                                        |`CSVFILE`  |
|xls or xlsx, depending on the file content |`EXCELFILE`|
|dbf                                        |`DBFFILE`  |
|table                                      |`TABLEFILE`|
|html                                       |`HTMLFILE` |
|doc or docx, depending on the file content |`WORDFILE` |
|jpg                                        |`IMAGEFILE`|
|pdf                                        |`PDFFILE`  |

The `Downloads` folder in the user folder is considered to be the current folder on the client side.

### Parameters

- `CLIENT`

    Keyword. If specified, the action will be executed on the client side. By default, the action is executed on the server.

- `DIALOG`

    Keyword. If specified, before writing the file a dialog will be shown in which the user can change the specified URL. This can be used only when writing to the disk (the URL type is file). By default, the dialog is not shown. 

- `ulrExpr`

    An [expression](Expression.md) whose value equals to the URL.

- `fileExpr`

    An [expression](Expression.md) whose value equals to the file that will be written to an external resource. 

- `APPEND`

    Keyword. If specified, the file is re-read from `fileExpr` and appended to the file at `urlExpr`. For the **csv** extension, data is added to the end of the file. For **xls** and **xlsx**, all sheets from the `fileExpr` file are copied to the file at the specified location `urlExpr`. Not supported for other extensions. By default, the file is rewritten.

### Examples

```lsf
loadAndWrite ()  {
    INPUT f = FILE DO {
        WRITE f TO 'file:///home/user/loadedfile.csv' APPEND;
        WRITE CLIENT f TO '/home/user/loadedfile.txt';
        WRITE CLIENT DIALOG f TO 'loadedfile';
    }
}
```
