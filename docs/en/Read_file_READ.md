---
title: 'Read file (READ)'
---

The *read file* operator creates an [action](Actions.md) that reads a file from a defined source and [writes](Property_change_CHANGE.md) this file to the specified property without parameters.

The source is defined by a string [expression](Expression.md) whose value is the URL to read from. The following types of data sources (URLs) are supported: FILE, HTTP, HTTPS, FTP, FTPS, SFTP.

### Language

To declare an action that reads a file, use the [`READ` operator](READ_operator.md).

### Examples

```lsf
readFiles()  {

    LOCAL importFile = FILE ();

    //reading from HTTP
    READ 'http://www.lsfusion.org/file.xlsx' TO importFile;
    //reading from HTTPS
    READ 'https://www.lsfusion.org/file.xlsx' TO importFile;
    //reading from FTP
    READ 'ftp://ftp.lsfusion.org/file.xlsx' TO importFile;
    //reading from FTPS
    READ 'ftps://ftps.lsfusion.org/file.xlsx' TO importFile;
    //reading from SFTP
    READ 'sftp://sftp.lsfusion.org/file.xlsx' TO importFile;
    //reading from FILE
    READ 'D://lsfusion/file.xlsx' TO importFile;
    READ 'file://D://lsfusion/file.xlsx' TO importFile;
}
```
