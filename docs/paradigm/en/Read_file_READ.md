---
slug: "/Read_file_READ"
title: 'Read file (READ)'
---

The *read file* operator creates an [action](Actions.md) that reads a file from a defined source and [writes](Property_change_CHANGE.md) this file to the specified property without parameters.

The source is defined by a string value — the URL to read from. The following types of data sources (URLs) are supported: FILE, HTTP, HTTPS, FTP, FTPS, SFTP.

The [working parameter](Working_parameters.md) `blockingFileRead` (`false` by default) makes the file be read under an exclusive lock: the file is locked for the duration of the read, and the read itself waits until whoever holds such a lock right now releases it. This protects against reading a file that is being appended to at that moment, but it requires write permission — a read-only file cannot be read at all while the parameter is on. The parameter applies to a read on the server, and to a read on the client side only in the desktop client: the web client reads the file without a lock in any case.

### Language

To declare an action that reads a file, use the [`READ` operator](../language/READ_operator.md).

### Examples

```lsf
readFiles()  {

    LOCAL importFile = FILE ();

    //reading from HTTP
    READ 'http://www.lsfusion.org/file.xlsx' TO importFile;
    //reading from HTTPS
    READ 'https://www.lsfusion.org/file.xlsx' TO importFile;
    //reading from FTP
    READ 'ftp://username:password@ftp.lsfusion.org/file.xlsx' TO importFile;
    //reading from FTPS
    READ 'ftps://username:password@ftps.lsfusion.org/file.xlsx' TO importFile;
    //reading from SFTP
    READ 'sftp://username:password@sftp.lsfusion.org/file.xlsx' TO importFile;
    //reading from FILE
    READ 'D://lsfusion/file.xlsx' TO importFile;
    READ 'file://D://lsfusion/file.xlsx' TO importFile;
}
```
