---
title: 'Запись файла (WRITE)'
---

Оператор *записи файла*, создает [действие](Actions.md), которое читает файл (либо JSON-значение, которое сериализуется в файл с расширением `.json`) из значения некоторого свойства и сохраняет его по заданному URL.

URL для записи задается строковым [выражением](Expression.md). Поддерживаются следующие типы URL: **FILE**, **FTP**, **FTPS**, **SFTP**.

### Язык

Для объявления действия, выполняющего запись файла, используется [оператор `WRITE`](WRITE_operator.md).

### Примеры


```lsf
loadAndWrite ()  {
    INPUT f = FILE DO {
        WRITE f TO 'file:///home/user/loadedfile.csv' APPEND;
        WRITE CLIENT f TO '/home/user/loadedfile.txt';
        WRITE CLIENT DIALOG f TO 'loadedfile';
    }
}
```
