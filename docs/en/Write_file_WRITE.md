---
title: 'Write file (WRITE)'
---

The *write file* operator creates an [action](Actions.md) which reads a file from the value of some property and saves it to the defined source.

The source is set as a [property](Properties.md) whose values are instances of [string classes](Built-in_classes.md). The following types of data sources (URLs) are supported: **FILE**, **FTP**, **SFTP**

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
