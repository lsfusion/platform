---
title: 'Чтение файла (READ)'
---

Оператор *чтения файла*, создает [действие](Actions.md), которое читает файл из заданного источника и [записывает](Property_change_CHANGE.md) этот файл в заданное локальное [первичное](Data_properties_DATA.md) свойства без параметров.

Источник задается как некоторое [свойство](Properties.md), значения которого являются экземплярами [строковых классов](Built-in_classes.md). Поддерживаются следующие типы источников данных (URL): FILE, HTTP, HTTPS, FTP, SFTP, JDBC, MDB.

### Язык

Для объявления действия, выполняющего чтение файла, используется [оператор `READ`](READ_operator.md).

### Примеры

```lsf
readFiles()  {

    LOCAL importFile = FILE ();

    //чтение из FTP
    READ 'ftp://ftp.lsfusion.org/file.xlsx' TO importFile;
    //чтение из SFTP
    READ 'sftp://sftp.lsfusion.org/file.xlsx' TO importFile;
    //чтение из FILE
    READ 'D://lsfusion/file.xlsx' TO importFile;
    READ 'file://D://lsfusion/file.xlsx' TO importFile;
}

connectionString = DATA STRING[100]();
importXls 'Импортировать надбавки'()  {
    LOCAL importFile = FILE ();
    READ connectionString() + '@SELECT field1, field2 FROM myTable' TO importFile;

    LOCAL field1 = INTEGER (INTEGER);
    LOCAL field2 = BPSTRING[10] (INTEGER);
    IMPORT TABLE FROM importFile() TO field1, field2;
}
```
