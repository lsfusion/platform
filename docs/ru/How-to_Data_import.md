---
title: 'How-to: Импорт данных'
---

## Пример 1

### Условие

Есть книги, для которых заданы наименование и цена. Также определена логика заказов.

```lsf
REQUIRE Utils;

CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[100] (Book) IN id;

id 'Код' = DATA STRING[20] (Book) IN id;
book 'Книга' (STRING[20] id) = GROUP AGGR Book b BY id(b);

CLASS Order 'Заказ';
date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA STRING[10] (Order);

CLASS OrderDetail 'Строка заказа';
order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;

book 'Книга' = DATA Book (OrderDetail) NONULL;
nameBook 'Книга' (OrderDetail d) = name(book(d));

quantity 'Количество' = DATA INTEGER (OrderDetail);
price 'Цена' = DATA NUMERIC[14,2] (OrderDetail);

FORM order 'Заказ'
    OBJECTS o = Order PANEL
    PROPERTIES(o) date, number

    OBJECTS d = OrderDetail
    PROPERTIES(d) nameBook, quantity, price, NEW, DELETE
    FILTERS order(d) == o

    EDIT Order OBJECT o
;

FORM orders 'Заказы'
    OBJECTS o = Order
    PROPERTIES(o) READONLY date, number
    PROPERTIES(o) NEWSESSION NEW, EDIT, DELETE
;

NAVIGATOR {
    NEW orders;
}
```

Нужно сделать кнопку, которая загрузит содержимое заказа из Excel-файла, выбранного пользователем на своем компьютере.

### Решение

```lsf
importXlsx 'Импортировать из XLS' (Order o)  {
    INPUT f = EXCELFILE DO {

        LOCAL bookId = STRING[20] (INTEGER);
        LOCAL quantity = INTEGER (INTEGER);
        LOCAL price = NUMERIC[14,2] (INTEGER);

        IMPORT XLS FROM f TO bookId = A, quantity = B, price = C;

        FOR imported(INTEGER i) NEW d = OrderDetail DO {
            order(d) <- o;

            book(d) <- book(bookId(i));
            quantity(d) <- quantity(i);
            price(d) <- price(i);
        }
    }
}

EXTEND FORM order
    PROPERTIES(o) importXlsx
;
```

[Оператор `INPUT`](INPUT_operator.md), который запрашивает файл, вызовет пользователю диалог с выбором файлов с расширениями xls и xlsx. При успешном выборе будет вызвано [действие](Actions.md), которое следует после слова `DO`.

Предполагается, что в файле будет три колонки. В первой `A` будет содержаться код книги, во второй `B` - количество, а в третьей `C` - цена. 

[Оператор `IMPORT`](IMPORT_operator.md) считывает содержимое выбранного файла в локальные свойства, у которых единственным параметром является номер строки. Эти номера начинаются с нуля. В свойстве `imported` будет `TRUE`, если в файле есть строка с соответствующим номером. Затем для каждой такой строки создается соответствующая строка в заказе.

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1). Кроме того, задан директорий, в который некоторая внешняя система складывает заказы. Для каждого заказа формируется отдельный файл в формате CSV, в котором хранятся дата и номер заказа (в денормализованном виде), а также код книги, количество и цена.

```lsf
serverDirectory 'Директорий на сервере, из которого импортировать заказы' = DATA STRING[100] ();

EXTEND FORM orders PROPERTIES() serverDirectory;
```

Необходимо реализовать действие, которое будет импортировать из этой папки заказы в систему.

### Решение

```lsf
importOrders 'Импортировать заказы из директория' ()  {

    listFiles('file://' + serverDirectory());

    FOR ISTRING[255] f = fileName(INTEGER j) AND NOT fileIsDirectory(j) DO NEWSESSION {

        LOCAL file = FILE ();
        READ 'file://' + serverDirectory() + '/' + f TO file;

        LOCAL date = DATE (INTEGER);
        LOCAL number = STRING[10] (INTEGER);

        LOCAL bookId = STRING[20] (INTEGER);
        LOCAL quantity = INTEGER (INTEGER);
        LOCAL price = NUMERIC[14,2] (INTEGER);

        IMPORT CSV '|' NOHEADER CHARSET 'CP1251' FROM file() TO date, number, bookId, quantity, price;

        NEW o = Order {
            date(o) <- date(0);
            number(o) <- number(0);

            FOR imported(INTEGER i) NEW d = OrderDetail DO {
                order(d) <- o;

                book(d) <- book(bookId(i));
                quantity(d) <- quantity(i);
                price(d) <- price(i);
            }
        }

        APPLY;
        move('file://' + serverDirectory() + '/' + f, 'file://' + serverDirectory() + '/' + (IF canceled() THEN 'error/' ELSE 'success/') + f);
    }
}

EXTEND FORM orders PROPERTIES() importOrders;
```

Действие `listFiles` объявлено в системном [модуле](Modules.md) `Utils`. Оно сканирует указанную в параметре папку и считывает все файлы из нее в свойства `fileName` (имя файла) и `fileIsDirectory` (логическое свойство - является ли файл директорием).

[Оператор `READ`](READ_operator.md) читает указанный файл в локальное свойство с типом `FILE`, которое затем обрабатывает оператор `IMPORT`. В его параметрах указывается, что форматом файла является CSV без заголовка в первой строке, с вертикальной чертой в качестве разделителем и кодировкой CP1251.

Предполагается, что дата и номер в каждой из строк будут содержать одинаковое значение. Поэтому их значения читаются из первой строки с номером 0.

Каждый файл обрабатывается в отдельной новой [сессии изменений](Change_sessions.md) с последующим сохранением путем вызова [оператора `APPLY`](APPLY_operator.md). Этот оператор записывает в свойство `canceled` `TRUE`, если при сохранении было нарушено некоторое [ограничение](Constraints.md). Дальше при помощи конструкции `MOVE` оператора `READ` файл перемещается либо в папку `success`, либо в папку `error`. Это нужно, чтобы действие можно было вызывать повторно, не обрабатывая при этом одни и те же заказы повторно.

Так как полученное действие не имеет параметров, то его можно включать в планировщик для автоматического запуска через определенные промежутки времени.

## Пример 3

### Условие

Аналогично [**Примеру 1**](#пример-1).

Во внешней базе данных хранится справочник книг с полями код и наименование.

Нужно сделать действие, которое будет синхронизировать справочник книг со внешней базой данных.

### Решение

```lsf
importBooks 'Импортировать книги' ()  {
    LOCAL file = FILE ();
    READ 'jdbc:sqlserver://localhost;databaseName=books;User=import;Password=password@SELECT id, name FROM books' TO file;

    LOCAL id = STRING[20] (INTEGER);
    LOCAL name = ISTRING[100] (INTEGER);
    IMPORT TABLE FROM file() TO id, name;

    //создаем новые книги
    FOR id(INTEGER i) AND NOT book(id(i)) NEW b = Book DO {
        id(b) <- id(i);
    }

    // меняем значения
    FOR id(Book b) == id(INTEGER i) DO {
        name(b) <- name(i);
    }

    // удаляем книги
    DELETE Book b WHERE b IS Book AND NOT [ GROUP SUM 1 BY id(INTEGER i)](id(b));
}
```

Синхронизация состоит из трех основных действий. Сначала создаются книги, коды которых есть во внешней базе данных, но нету в нашей базе. Затем для всех книг, которые есть в нашей базе данных изменяются значения на новые. И в конце удаляются книги, коды которых отсутствуют во внешней базе данных.

Таким образом, гарантируется, что после запуска действия справочник книг будет абсолютно идентичен справочнику во внешней системе. Эта схема удобна, когда некоторые мастер-данные ведутся в другой системе. Полученное действие можно добавить в планировщик, которое будет срабатывать через определенные относительное небольшие промежутки во времени, тем самым обеспечивая почти онлайновое обновление справочника.

## Пример 4

### Условие

Аналогично [**Примеру 1**](#пример-1).

Для строки заказа добавлена расшифровка этой строки по цветам и размерам.

```lsf
CLASS OrderDetailInfo 'Строка заказа (расшифровка)';

detail = DATA OrderDetail (OrderDetailInfo) NONULL DELETE;
size = DATA STRING[100] (OrderDetailInfo);
color = DATA STRING[100] (OrderDetailInfo);
quantity = DATA INTEGER (OrderDetailInfo);

EXTEND FORM order
    OBJECTS i = OrderDetailInfo
    PROPERTIES(i) size, color, quantity, NEW, DELETE
    FILTERS detail(i) = d
;
```

Необходимо реализовать импорт заказов из JSON файла заданной структуры. Пример JSON-файла :
```json
{
   "version":"v1",
   "order":[
      {
         "date":"03.01.2018",
         "number":"430",
         "detail":[
            {
               "item":{
                  "id":"132",
                  "info":[
                     {
                        "size":"40",
                        "color":"black",
                        "quantity":2
                     },
                     {
                        "size":"41",
                        "color":"white",
                        "quantity":3
                     }
                  ]
               },
               "price":1.99
            },
            {
               "item":{
                  "id":"136",
                  "info":[
                     {
                        "size":"39",
                        "color":"white",
                        "quantity":4
                     },
                     {
                        "size":"43",
                        "color":"red",
                        "quantity":1
                     }
                  ]
               },
               "price":2.99
            }
         ]
      },
      {
         "date":"04.01.2018",
         "number":"435",
         "detail":[
            {
               "item":{
                  "id":"122",
                  "info":[
                     {
                        "size":"L",
                        "color":"black",
                        "quantity":1
                     },
                     {
                        "size":"XL",
                        "color":"white",
                        "quantity":1
                     }
                  ]
               },
               "price":11.99
            },
            {
               "item":{
                  "id":"126",
                  "info":[
                     {
                        "size":"S",
                        "color":"white",
                        "quantity":1
                     },
                     {
                        "size":"M",
                        "color":"red",
                        "quantity":1
                     }
                  ]
               },
               "price":12.99
            }
         ]
      },
   ]
}
```

### Решение

```lsf
version = DATA LOCAL STRING[100]();

GROUP item;
idItem = DATA LOCAL STRING[100] (OrderDetail);

FORM importOrder
    PROPERTIES() version

    OBJECTS order = Order
    PROPERTIES(order) date, number

    OBJECTS detail = OrderDetail
    PROPERTIES(detail) IN item idItem EXTID 'id'
    PROPERTIES(detail) price
    FILTERS order(detail) = order

    OBJECTS detailInfo = OrderDetailInfo IN item EXTID 'info'
    PROPERTIES(detailInfo) size, color, quantity
    FILTERS detail(detailInfo) = detail
;

importOrderFromJSON 'Импорт из JSON' () {
    INPUT f = FILE DO {
        IMPORT importOrder JSON FROM f;
        book(OrderDetail d) <- book(idItem(d)) WHERE idItem(d);
        APPLY;
    }
}

EXTEND FORM orders
    PROPERTIES() importOrderFromJSON DRAW o TOOLBAR
;
```

Для реализации импорта нужно объявить форму [структуры](Structured_view.md), соответствующей структуре JSON-файла.

Тэг `version`, который находится на самом высоком уровне, объявляем без входов и добавляем на форму.

Значением тэга `order` является массив, поэтому на форме объявляем объект с именем этого тэга. Платформа создаст по новом объекту для каждого элемента массива в JSON. Свойства `date` и `number` для заказа будут автоматически импортированы из соответствующий тэгов в JSON.

Аналогичным образом, для тэга `detail` создается объект с таким же именем, который через `FILTERS` связывается с объектом `order`. Система при импорте сама заполнит ссылку строки заказа на заказ на основе этого фильтра исходя из вложенности тэгов друг в друга.

Для того, чтобы импортировать значения из тэгов, вложенных в тэг `item`, создается новая [группа](Groups_of_properties_and_actions.md) `item`, в которую затем помещаются свойства и объекты. В частности, создается локальное свойство `idItem` и добавляется на форму в эту группу. Так как имя свойства не совпадает с именем тэга, то для свойства на форме при помощи ключевого слова `EXTID` указывается соответствующее имя.
