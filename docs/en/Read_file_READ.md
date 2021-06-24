---
title: 'Read file (READ)'
---

The *read file* operator creates an [action](Actions.md) that reads a file from a defined source and [writes](Property_change_CHANGE.md) this file to the specified local [data](Data_properties_DATA.md) property without parameters.

The source is defined as a [property](Properties.md) which values are instances of [string classes](Built-in_classes.md). The following types of data sources (URLs) are supported: FILE, HTTP, HTTPS, FTP, SFTP, JDBC, MDB.

### Language

To declare an action that reads a file, use the [`READ` operator](READ_operator.md).

### Examples

```lsf
readFiles()  {

    LOCAL importFile = FILE ();

    //reading from FTP
    READ 'ftp://ftp.lsfusion.org/file.xlsx' TO importFile;
    //reading from SFTP
    READ 'sftp://sftp.lsfusion.org/file.xlsx' TO importFile;
    //reading from FILE
    READ 'D://lsfusion/file.xlsx' TO importFile;
    READ 'file://D://lsfusion/file.xlsx' TO importFile;
}

connectionString = DATA STRING[100]();
importXls 'Import markups'()  {
    LOCAL importFile = FILE ();
    READ connectionString() + '@SELECT field1, field2 FROM myTable' TO importFile;

    LOCAL field1 = INTEGER (INTEGER);
    LOCAL field2 = BPSTRING[10] (INTEGER);
    IMPORT TABLE FROM importFile() TO field1, field2;
}
```
