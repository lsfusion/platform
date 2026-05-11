---
title: 'Чтение файла (READ)'
---

Оператор *чтения файла*, создает [действие](Actions.md), которое читает файл из заданного источника и [записывает](Property_change_CHANGE.md) этот файл в заданное свойство без параметров.

Источник задается строковым [выражением](Expression.md), значение которого является URL для чтения. Поддерживаются следующие типы источников данных (URL): FILE, HTTP, HTTPS, FTP, FTPS, SFTP.

### Язык

Для объявления действия, выполняющего чтение файла, используется [оператор `READ`](READ_operator.md).

### Примеры

```lsf
readFiles()  {

    LOCAL importFile = FILE ();

    //чтение из HTTP
    READ 'http://www.lsfusion.org/file.xlsx' TO importFile;
    //чтение из HTTPS
    READ 'https://www.lsfusion.org/file.xlsx' TO importFile;
    //чтение из FTP
    READ 'ftp://ftp.lsfusion.org/file.xlsx' TO importFile;
    //чтение из FTPS
    READ 'ftps://ftps.lsfusion.org/file.xlsx' TO importFile;
    //чтение из SFTP
    READ 'sftp://sftp.lsfusion.org/file.xlsx' TO importFile;
    //чтение из FILE
    READ 'D://lsfusion/file.xlsx' TO importFile;
    READ 'file://D://lsfusion/file.xlsx' TO importFile;
}
```
