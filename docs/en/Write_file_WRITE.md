---
title: 'Write file (WRITE)'
---

The *write file* operator creates an [action](Actions.md) that reads a file (or a JSON value, which is serialized to a `.json` file) from the value of some property and saves it to the defined destination.

The destination is defined by a string [expression](Expression.md) whose value is the URL to write to. The following URL types are supported: **FILE**, **FTP**, **FTPS**, **SFTP**.

### Language

To declare an action that writes a file, use the [`WRITE` operator](WRITE_operator.md).

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
