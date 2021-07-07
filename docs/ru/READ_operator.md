---
title: 'Оператор READ'
---

Оператор `READ` - создание [действия](Actions.md), [считывающего файл](Read_file_READ.md) из внешнего ресурса в [свойство](Properties.md).

### Синтаксис

    READ [CLIENT [DIALOG]] urlExpr [TO propertyId]

### Описание

Оператор `READ` создает действие, которое читает файл из внешнего ресурса по заданному URL, после чего записывает полученный файл в заданное свойство.

Поддерживаются следующие типы URL: 

    [file://]path_to_file
    [s]ftp://username:password[;charset]@host:port[/path_to_file][?passivemode=true|false]

Если значение свойства, в которое записывается файл принадлежит классу `FILE`, то в его значение вместе с файлом также записывается расширение файла из URL.

### Параметры

- `CLIENT`

    Ключевое слово. Если указывается, то действие будет выполнено на клиенте. По умолчанию действие выполняется на сервере.

- `DIALOG`

    Ключевое слово. Если указывается, то перед записью файла, будет показан диалог, в котором пользователь может изменить заданный URL. Можно использовать только при записи на диск (тип URL - file) . По умолчанию диалог не показывается. 

- `urlExpr`

    [Выражение](Expression.md), значением которого является URL, из которого следует произвести чтение. Значение выражения должно быть строкового типа.

- `propertyId`

    [Идентификатор свойства](IDs.md#propertyid), в которое будет производиться запись считанных данных. У этого свойства не должно быть параметров и класс его значения должен быть файловым (`FILE`, `RAWFILE`, `JSONFILE` и т.д.). Если свойство не указано, оно автоматически устанавливается равным `System.readFile`.

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
