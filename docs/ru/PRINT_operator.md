---
title: 'Оператор PRINT'
---

Оператор `PRINT` - создание [действия](Actions.md), [открывающего форму](In_a_print_view_PRINT.md) в печатном представлении. 

### Синтаксис

```
PRINT name 
[OBJECTS objName1 = expr1, ..., objNameN = exprN]
[formActionOptions] 
```

`formActionOptions` - дополнительные опции этого действия. Существует несколько вариантов синтаксиса опций в этом операторе:

```
printFormat [SHEET sheetProperty] [PASSWORD passwordExpr] [TO propertyId]
[PREVIEW | NOPREVIEW] [syncType] [TO printerExpr]
MESSAGE [syncType]
[TOP (topExpr | (topGroupId1 = topPropertyExpr1, ..., topGroupIdT = topPropertyExprT))]
[OFFSET (offsetExpr | (offsetGroupId1 = offsetPropertyExpr1, ..., offsetGroupIdF = offsetPropertyExprF))]
```

### Описание

Оператор `PRINT` создает действие, которое печатает указанную форму. При печати формы в блоке `OBJECTS` можно добавлять объектам формы [дополнительные фильтры](Open_form.md#params) на равенство этих объектов переданным значениям.

### Параметры

- `name`

    Имя формы. [Составной идентификатор](IDs.md#cid).

- `objName1 ... objNameN`

    Имена объектов формы, для которых задаются дополнительные фильтры. [Простые идентификаторы](IDs.md#id).

- `expr1 ... exprN`

    [Выражения](Expression.md), значения которых определяют фильтруемые (фиксированные) значения для объектов формы.

#### Дополнительные опции

- `printFormat`

    [Формат печати](In_a_print_view_PRINT.md#format), задается одним из ключевых слов:

    - `PDF` - форма будет выгружена в файл формата PDF.
    - `XLS`, `XLSX` - форма будет выгружена в файл в одном из указанных форматов EXCEL.
    - `DOC`, `DOCX` - форма будет выгружена в файл в одном из указанных форматов WORD.
    - `RTF` - форма будет выгружена в файл формата RTF.
    - `HTML` - форма будет выгружена в файл формата HTML.

- `sheetProperty`

    [Идентификатор свойства](IDs.md#propertyid), значение которого применяется в качестве названия листа в выгружаемом файле. У свойства не должно быть параметров. Используется для форматов печати `XLS`, `XLSX`.

- `passwordExpr`

    Выражение, значение которого указывает на пароль для выгружаемого файла, устанавливающий режим read-only. Используется для форматов печати `XLS`, `XLSX`.

- `propertyId`

    [Идентификатор свойства](IDs.md#propertyid), в которое будет записан сформированный файл. У свойства не должно быть параметров. Если свойство не указано, сформированный файл передается клиенту и открывается у него средствами операционной системы

- `PREVIEW`

    Ключевое слово. Если указывается, то форма показывается в режиме [предварительного просмотра](In_a_print_view_PRINT.md#interactive). Этот режим используется по умолчанию, если другие режимы / форматы не заданы.

- `NOPREVIEW`

    Ключевое слово. Если указывается, то форма сразу (без предварительного просмотра) отправляется на печать.

- `printerExpr`

    Выражение, значение которого указывает на имя принтера, на который будет послана печать. Если принтер с указанным именем не найден (или не указан), выбирается принтер по умолчанию.

- `MESSAGE`

    Ключевое слово. Если указывается, то форма выдает данные пользователю в режиме [сообщения](In_a_print_view_PRINT.md#interactive).

- `syncType`

    Определяет, когда продолжить выполнение созданного действия:

    - `WAIT` - после завершения действия клиентом (закрытия формы предпросмотра / сообщения). Используется по умолчанию.
    - `NOWAIT` -  после подготовки информации для передачи клиенту (чтения данных формы).

- `TOP (topExpr | (topGroupId1 = topPropertyExpr1, ..., topGroupIdT = topPropertyExprT))`

    Печать только первых `n` записей, где `n` - значение выражения `topExpr` или `topPropertyExprT` для группы объектов `topGroupIdT`.

- `OFFSET (offsetExpr | (offsetGroupId1 = offsetPropertyExpr1, ..., offsetGroupIdF = offsetPropertyExprF))`

    Печать только записей со смещением `m`, где `m` - значение выражения `offsetExpr` или `offsetPropertyExprF` для группы объектов `offsetGroupIdF`.

### Примеры

```lsf
FORM printOrder
    OBJECTS o = Order
    PROPERTIES(o) currency, customer

    OBJECTS d = OrderDetail
    PROPERTIES(d) idSku, price
    FILTERS order(d) == o
;

print (Order o)  {
    PRINT printOrder OBJECTS o = o; // выводим на печать

    LOCAL file = FILE ();
    PRINT printOrder OBJECTS o = o DOCX TO file;
    open(file());

    //v 2.0-2.1 syntax
    LOCAL sheetName = STRING[255]();
    sheetName() <- 'enctypted';
    PRINT printOrder OBJECTS o = o XLS SHEET sheetName PASSWORD 'pass';

    //v 2.2 syntax
    //PRINT printOrder OBJECTS o = o XLS SHEET 'enctypted' PASSWORD 'pass';
}
```
