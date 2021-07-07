---
title: 'Запись файла (WRITE)'
---

Оператор *записи файла*, создает [действие](Actions.md), которое читает файл из значения некоторого свойства и сохраняет его в заданный источник.

Источник задается как некоторое [свойство](Properties.md), значения которого являются экземплярами [строковых классов](Built-in_classes.md). Поддерживаются следующие типы источников данных (URL): **FILE**, **FTP**, **SFTP**

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
