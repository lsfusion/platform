---
title: 'Оператор IMPORT'
---

Оператор `IMPORT` - создание [действия](Actions.md), импортирующего данные из заданного файла в [заданные свойства (параметры)](Data_import_IMPORT.md) или, в общем случае, в [заданную форму](In_a_structured_view_EXPORT_IMPORT.md#importForm).

## Синтаксис

    IMPORT [importFormat] FROM fileExpr importDestination [DO actionOperator [ELSE elseActionOperator]]
    IMPORT formName [importFormat] [FROM (fileExpr | (groupId1 = fileExpr1 [, ..., groupIdM = fileExprM])]

`importFormat` может задаваться одним из следующих вариантов:

    JSON [CHARSET charsetStr]
    XML [ATTR] [CHARSET charsetStr]
    CSV [separator] [HEADER | NOHEADER] [ESCAPE | NOESCAPE] [CHARSET charsetStr]
    XLS [HEADER | NOHEADER] [SHEET (sheetExpr | ALL)]
    DBF [CHARSET charsetStr]
    TABLE

`importDestination` может задаваться одним из следующих вариантов:

    TO [(objClassId1, objClassId2, ..., objClassIdK)] propertyId1 [= columnId1], ..., propertyIdN [= columnIdN] [WHERE whereId]
    FIELDS [(objClassId1 objAlias1, objClassId2 objAlias1, ..., objClassIdK objAliasK)] propClassId1 [propAlias1 =] columnId1 [NULL], ..., propClassIdN [propAliasN =] columnIdN [NULL]

## Описание

Оператор `IMPORT `создает действие, которое импортирует данные из файла в значения заданных свойств или в заданную форму. 

Если формат импортируемого файла не задан, то он автоматически определяется, в зависимости от класса импортируемого файла (или от расширения, если этот класс равен `FILE`), следующим образом:

|Формат   |Расширение  |Класс     |
|---------|------------|----------|
|**JSON** |json        |JSONFILE  |
|**XML**  |xml         |XMLFILE   |
|**CSV**  |csv         |CSVFILE   |
|**XLS**  |xls или xlsx|EXCELFILE |
|**DBF**  |dbf         |DBFFILE   |
|**TABLE**|table       |TABLEFILE |

:::info
Для автоматического определения плоского формата файла по его расширению используется первый переданный файл
:::

## Параметры

### Источник импорта

- `fileExpr`

    [Выражение](Expression.md), значением которого является импортируемый файл. Значение выражения должно быть объектом файлового класса (`FILE`, `RAWFILE`, `JSONFILE` и т. д.). Если при импорте формы это выражение не указано, то по умолчанию используется выражение `System.importFile()`.

- `groupId1, ..., groupIdM`

    Имена групп объектов импортируемой формы, для которых необходимо импортировать данные. [Простые идентификаторы](IDs.md#id). Используется только для импорта формы из плоских форматов.

- `fileExpr1 , ..., fileExprM`

    Выражения, значения которых являются файлами, которые необходимо импортировать для заданных групп объектов. Значения выражений должны быть объектами файлового класса (`FILE`, `RAWFILE`, `JSONFILE` и т. д.). Используется только для импорта формы из плоских форматов. Для [пустой группы](Static_view.md#empty) объектов используется имя `root`. 

### Формат импорта

- `ATTR`

    Ключевое слово, указывающее на чтение значений из атрибутов элемента. Если не указывается, то чтение производится из дочерних элементов. Применяется только для импорта **XML**.

- `separator`

    Разделитель в **CSV** файле. [Строковый литерал](Literals.md#strliteral). Если не указывается, то по умолчанию берётся разделитель `;`.

- `HEADER` | `NOHEADER`

    Ключевые слова, указывающие на присутствие (`HEADER`) или отсутствие (`NOHEADER`) в **CSV** / **XLS** файле строки заголовка, в которой содержатся имена колонок. По умолчанию используется `NOHEADER`.

    При использовании опции `NOHEADER`:

    - именами колонок считаются : `A`, `B`, ..., `Z`, `AA`, ...,  `AE`, ...
    - если колонка не найдена / не соответствует типу свойства назначения, значением этой колонки считается значение `NULL` (в остальных форматах импорта в этих случаях платформа выдает ошибку).

- `ESCAPE` | `NOESCAPE`

    Ключевое слово, указывающее на присутствие (`ESCAPE`) или отсутствие (`NOESCAPE`) в **CSV** файле экранирования спецсимволов (`\r`, `\n`, `"` (двойные кавычки)) и указанного разделителя `separator`. `NOESCAPE` имеет смысл использовать, только в случаях, когда в данных гарантировано не будет заданного разделителя. По умолчанию используется `ESCAPE`.

- `SHEET (sheetExpr | ALL)`

    Опция, указывающая на импорт конкретного листа Excel файла. Если опция не указана, то берется лист номер один.

    - `sheetExpr`
    
        Выражение, значение которого определяет номер импортируемого листа Excel файла. Значение выражения должно иметь класс `INTEGER` или `LONG`. Нумерация начинается с единицы.

    - `ALL`
    
        Ключевое слово, которое означает, что импорт будет производится из всех листов excel файла.

- `CHARSET charsetStr`

    Опция, указывающая кодировку файла, используемую при импорте.

    `charsetStr`
    
        Cтроковый литерал, определяющий кодировку. 

- `actionOperator`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий действие, которое выполняется для каждой импортированной записи.

- `elseActionOperator`

    Контекстно-зависимый оператор-действие, описывающий действие, которое выполняется, если ни одной записи импортировано не было. В качестве параметров нельзя использовать параметры, в которые импортируются данные.

### Назначение импорта

- `formName`

    Имя формы, в которую необходимо импортировать данные. [Составной идентификатор](IDs.md#cid).

- `objClassId1, ..., objClassIdK`

    Классы [импортируемых](Data_import_IMPORT.md) объектов. Задаются [идентификаторами класса](IDs.md#classid). `K <= 1`. По умолчанию считается, что импортируется один объект класса `INTEGER`.

- `objAlias1, ..., objAliasK`

    Имена локальных параметров, в которые записываются импортируемые объекты. [Простые идентификаторы](IDs.md#id). `K <= 1`. По умолчанию считается, что импортируется один объект с именем `row`.

- `propertyId1, ..., propertyIdN`

    Список [идентификаторов свойств](IDs.md#propertyid), в которые импортируются колонки (поля) данных. Параметры свойств и их классы должны соответствовать импортируемым объектам и их классам.

- `columnId1, ..., columnIdN`

    Список идентификаторов колонок в исходном файле, из которых будут переноситься данные в соответствующее свойство. Каждый элемент списка задается либо простым идентификатором, либо строковым литералом. При указании идентификатора несуществующей колонки или при отсутствии идентификатора, за колонку, соответствующую свойству, принимается колонка, следующая по порядку за указанной для предыдущего свойства в списке, либо первая, если указывается первое свойство. Для файлов **DBF** идентификаторы колонок являются регистронезависимыми. 

- `whereId`

    Идентификатор свойства, в которое будет записано [значение по умолчанию](Built-in_classes.md#defaultvalue) класса значения этого свойства для каждого импортируемого объекта. Параметры свойства и его классы должны соответствовать импортируемым объектам и их классам. Если это свойство не задано и количество импортируемых объектов больше нуля, по умолчанию осуществляется поиск свойства с именем `imported` и классами импортируемых объектов (например `System.imported[INTEGER]`).

- `propClassId1, ..., *propClassId*N`

    Список имен [встроенных классов](Built-in_classes.md) импортируемых колонок.

- `propAlias1, ..., propAliasN`

    Имена локальных параметров, в которые импортируются колонки (поля) данных. Простые идентификаторы. Если имя не задается, то в качестве имени параметра будет использовано имя колонки (поля) в исходном файле.

- `NULL`

    Ключевое слово. Обозначает, что `NULL` значения при импорте (если импортируемый формат их поддерживает) не будут заменяться на значения по умолчанию (например, `0` - для чисел, пустые строки - для строк и т. п.).

## Примеры

```lsf
import()  {

    LOCAL xlsFile = EXCELFILE ();

    LOCAL field1 = BPSTRING[50] (INTEGER);
    LOCAL field2 = BPSTRING[50] (INTEGER);
    LOCAL field3 = BPSTRING[50] (INTEGER);
    LOCAL field4 = BPSTRING[50] (INTEGER);

    LOCAL headField1 = BPSTRING[50] ();
    LOCAL headField2 = BPSTRING[50] ();

    INPUT f = EXCELFILE DO {
        IMPORT XLS SHEET 2 FROM f TO field1 = C, field2, field3 = F, field4 = A;
        IMPORT XLS SHEET ALL FROM f TO field1 = C, field2, field3 = F, field4 = A;

        // свойство imported - системное свойство, предназначенное для перебора данных
        FOR imported(INTEGER i) DO { 
            MESSAGE 'field1 value = ' + field1(i);
            MESSAGE 'field2 value = ' + field2(i);
            MESSAGE 'field3 value = ' + field3(i);
            MESSAGE 'field4 value = ' + field4(i);
       }
    }

    LOCAL t = FILE ();
    EXTERNAL SQL 'jdbc:postgresql://localhost/test?user=postgres&password=12345' 
             EXEC 'SELECT x.a,x.b,x.c,x.d FROM orders x WHERE x.id = $1;' 
             PARAMS '4553' 
             TO t;
    // импорт с опцией FIELDS
    IMPORT FROM t() FIELDS INTEGER a, DATE b, BPSTRING[50] c, BPSTRING[50] d DO        
        NEW o = Order {
            number(o) <- a;
            date(o) <- b;
            customer(o) <- c;
            // находим currency с данным именем
            currency(o) <- GROUP MAX Currency currency IF name(currency) = d; 
        }


    INPUT f = FILE DO
        IMPORT CSV '*' HEADER CHARSET 'utf-8' FROM f TO field1 = C, field2, field3 = F, field4 = A;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ROOT 'element' ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO() headField1, headField2;

    INPUT f = FILE DO
        INPUT memo = FILE DO
            IMPORT DBF MEMO memo FROM f TO field1 = 'DBFField1', field2 = 'DBFField2';
}
```

```lsf

date = DATA DATE (INTEGER);
sku = DATA BPSTRING[50] (INTEGER);
price = DATA NUMERIC[14,2] (INTEGER);
order = DATA INTEGER (INTEGER);
FORM import
    OBJECTS o = INTEGER // заказы
    OBJECTS od = INTEGER // строки заказов
    PROPERTIES (o) dateOrder = date // импортируем дату из поля dateOrder
    PROPERTIES (od) sku = sku, price = price // импортируем товар количество из полей sku и price
    FILTERS order(od) = o // в order - записываем верхний заказ

;

importForm()  {
    INPUT f = FILE DO {
        IMPORT import JSON FROM f;
        SHOW import; // показываем что импортировалось

        // создаем объекты в базе
        FOR DATE date = date(INTEGER io) NEW o = Order DO {
            date(o) <- date;
            FOR order(INTEGER iod) = io NEW od = OrderDetail DO {
                price(od) <- price(iod);
                sku(od) <- GROUP MAX Sku sku IF name(sku) = sku(iod); // находим sku с данным именем
            }
        }
    }
}
```
