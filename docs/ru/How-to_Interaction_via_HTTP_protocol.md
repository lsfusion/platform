---
title: 'How-to: Взаимодействие через HTTP-протокол'
---

## Пример 1

### Условие

Задан некоторый набор городов, привязанных к странам.

```lsf
CLASS Country 'Страна';
id 'Код' = DATA STRING[20] (Country) IN id;
name 'Имя' = DATA ISTRING[100] (Country) IN id;

country (STRING[20] id) = GROUP AGGR Country c BY id(c);

CLASS City 'Город';
name 'Имя' = DATA ISTRING[100] (City) IN id;

country 'Страна' = DATA Country (City);
nameCountry 'Страна' (City c) = name(country(c));

FORM cities 'Города'
    OBJECTS c = City
    PROPERTIES(c) name, nameCountry, NEW, DELETE
;

NAVIGATOR {
    NEW cities;
}
```

Нужно отправить на определенный url HTTP-запрос на добавление города в формате JSON.

### Решение

```lsf
postCity 'Отправить' (City c)  {
    EXPORT JSON FROM countryId = id(country(c)), name = name(c);

    LOCAL result = FILE();
    EXTERNAL HTTP 'http://localhost:7651/exec?action=Location.createCity' PARAMS exportFile() TO result;

    LOCAL code = STRING[10]();
    LOCAL message = STRING[100]();
    IMPORT JSON FROM result() TO() code, message;
    IF NOT code() == '0' THEN {
        MESSAGE 'Ошибка: ' + message();
    }
}

EXTEND FORM cities
    PROPERTIES(c) postCity
;
```

[Оператор `EXPORT`](Data_export_EXPORT.md) создаст JSON в формате [`FILE`](Built-in_classes.md) и сохранит его в свойство `exportFile`. Пример сформированного файла:
 
```json
{"countryId":"123","name":"San Francisco"}
```

Дальше вызывается [оператор `EXTERNAL`](Access_to_an_external_system_EXTERNAL.md), который делает запрос на предопределенный url, передавая туда в качестве Body содержимое сформированного файла. В данном случае, так как свойство в блоке `FROM` имеет тип JSON, то в качестве типа контента будет использоваться *application/json*. В url'е закодированы `<пространство имен>.<имя свойства>`. В данном случае пространство именем модуля вызываемого свойства `createCity` является `Location`. Все параметры передаются по порядку с идентификатором `p`. Ответ, который будет получен от сервера, будет записан в свойство `result`. Предположим, что ответ получен в формате JSON и имеет один из следующих видов:

```json
{"code":"0","message":"OK"}

{"code":"1","message":"Некорректный код страны"}
```

Ответ разбирается при помощи [оператора `IMPORT`](Data_import_IMPORT.md), который раскладывает соответствующие параметры в свойства `code` и `message` соответственно. В случае ошибки пользователю выдается сообщение с текстом ошибки.

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1). 

Нужно принять соответствующий HTTP-запрос и создать новый город в базе данных с параметрами запроса.

### Решение

```lsf
createCity (FILE f)  {

    LOCAL cy = STRING[20] ();
    LOCAL ne = STRING[100] ();

    IMPORT JSON FROM f AS FILE TO() cy = countryId, ne = name;

    IF NOT country(cy()) THEN {
        EXPORT JSON FROM code = '1', message = 'Некорректный код страны';
        RETURN;
    }

    NEW c = City {
        name(c) <- ne();
        country(c) <- country(cy());

        APPLY;
    }

    EXPORT JSON FROM code = '0', message = 'OK';
}
```

Так как свойство имеет название `createCity` и расположено в [модуле](Modules.md) с пространством имен `Location`, то url, на котором будет принят запрос, имеет следующий вид:

    http://localhost:7651/exec?action=Location.createCity

Body HTTP-запроса будет передан параметром с типом `FILE`. В локальные свойства `cy` и `ne`, считываются значения из параметров `countryId` и `name` соответственно.

Если не находится страна с соответствующим кодом, то формируется JSON файл с содержанием, описанном в предыдущем примере, и вызывается [оператор `RETURN`](Exit_RETURN.md), чтобы прервать выполнение. В качестве ответа, по умолчанию, используется значение, хранящееся в свойстве `exportFile`.

В случае, если все действия были завершены успешно, то формируется соответствующий ответ с сообщение ОК.

## Пример 3

### Условие

Задана логика заказов книг.

```lsf
CLASS Book 'Книга';
id 'Код' = DATA STRING[10] (Book) IN id;
name 'Наименование' = DATA ISTRING[100] (Book) IN id;

book (STRING[10] id) = GROUP AGGR Book b BY id(b);

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
    OBJECTS i = Order
    PROPERTIES(i) READONLY date, number
    PROPERTIES(i) NEWSESSION NEW, EDIT, DELETE
;

NAVIGATOR {
    NEW orders;
}
```

Нужно отправить на определенный url HTTP-запрос на создание заказа в формате JSON.

### Решение

```lsf
FORM exportOrder
    OBJECTS order = Order PANEL
    PROPERTIES dt = date(order), nm = number(order)

    OBJECTS detail = OrderDetail
    PROPERTIES id = id(book(detail)), qn = quantity(detail), pr = price(detail)
    FILTERS order(detail) == order
;

exportOrder 'Отправить' (Order o)  {
    EXPORT exportOrder OBJECTS order = o JSON;

    LOCAL result = FILE();
    EXTERNAL HTTP 'http://localhost:7651/exec?action=Location.importOrder' PARAMS exportFile() TO result;
}

EXTEND FORM orders
    PROPERTIES(i) exportOrder;
;
```

Для создания JSON с вложенными тэгами нужно создать форму с соответствующими объектами, связанными через конструкцию `FILTERS`. На основе зависимостей между ними создается JSON с соответствующей структурой. В данном случае, JSON будет выглядеть следующим образом:
```json
{
   "dt":"20.08.18",
   "nm":"1",
   "detail":[
      {
         "pr":5.99,
         "id":"b1",
         "qn":3
      },
      {
         "pr":6.99,
         "id":"b2",
         "qn":2
      }
   ]
}
```

Для `order` не создается свой тэг, так как значение объекта передается параметром для оператора `EXPORT`.  
В данном примере, ответ полученный на HTTP-запрос игнорируется.

## Пример 4

### Условие

Аналогично [**Примеру 3**](#пример-3). 

Нужно принять соответствующий HTTP-запрос и создать новый заказ в базе данных с параметрами запроса.

### Решение

```lsf
date = DATA LOCAL DATE();
number = DATA LOCAL STRING[10]();

id = DATA LOCAL STRING[10] (INTEGER);
quantity = DATA LOCAL INTEGER (INTEGER);
price = DATA LOCAL NUMERIC[14,2] (INTEGER);
FORM importOrder
    PROPERTIES dt = date(), nm = number()

    OBJECTS detail = INTEGER
    PROPERTIES id = id(detail), qn = quantity(detail), pr = price(detail)
;

importOrder (FILE f)  {
    IMPORT importOrder JSON FROM f;

    NEW o = Order {
        date(o) <- date();
        number(o) <- number();
        FOR id(INTEGER detail) DO NEW d = OrderDetail {
            order(d) <- o;
            book(d) <- book(id(detail));
            quantity(d) <- quantity(detail);
            price(d) <- price(detail);
        }

        APPLY;
    }
}
```

Для импорта соответствующего файла в формате JSON создается форма аналогичной структуры, только в качестве классов объектов используется тип `INTEGER`. При импорте значения тэгов будут помещены в свойства с соответствующими именами. Свойства `date` и `number` не имеют параметров, так как в JSON значения для них идут на самом верхнем уровне.

## Пример 5

### Условие

Аналогично [**Примеру 4**](#пример-4). 

Нужно отправить на определенный url HTTP-запрос на создание заказа в формате JSON, аналогичный предыдущему примеру, только обернуть все в тэг `order`.

### Решение

```lsf
GROUP order;
FORM exportOrderNew
    OBJECTS o = Order
    PROPERTIES IN order dt = date(o), nm = number(o)

    OBJECTS detail = OrderDetail IN order
    PROPERTIES id = id(book(detail)), qn = quantity(detail), pr = price(detail)
    FILTERS order(detail) == o
;

exportOrderNew 'Отправить (новый)' (Order o)  {
    EXPORT exportOrderNew OBJECTS o = o JSON;

    LOCAL result = FILE();
    EXTERNAL HTTP 'http://localhost:7651/exec?action=Location.importOrderNew' PARAMS exportFile() TO result;
}

EXTEND FORM orders
    PROPERTIES(i) exportOrderNew;
;
```

В отличие от предыдущего примера создаем [группу](Groups_of_properties_and_actions.md) `order`, при помощи [оператора `GROUP`](GROUP_operator.md). При объявлении формы в эту группу записываем все свойства для заказа и объект `detail`. Результирующий JSON будет выглядеть следующим образом:
```json
{
   "order":{
      "dt":"20.08.18",
      "nm":"1",
      "detail":[
         {
            "pr":5.99,
            "id":"b1",
            "qn":3
         },
         {
            "pr":6.99,
            "id":"b2",
            "qn":2
         }
      ]
   }
}
```

## Пример 6

### Условие

Аналогично [**Примеру 5**](#пример-5). 

Нужно принять соответствующий HTTP-запрос и создать новый заказ в базе данных с параметрами запроса.

### Решение

```lsf
FORM importOrderNew
    PROPERTIES IN order dt = date(), nm = number()

    OBJECTS detail = INTEGER IN order
    PROPERTIES id = id(detail), qn = quantity(detail), pr = price(detail)
;

importOrderNew (FILE f)  {
    IMPORT importOrderNew JSON FROM f;

    NEW o = Order {
        date(o) <- date();
        number(o) <- number();
        FOR id(INTEGER detail) DO NEW d = OrderDetail {
            order(d) <- o;
            book(d) <- book(id(detail));
            quantity(d) <- quantity(detail);
            price(d) <- price(detail);
        }

        APPLY;
    }
}
```

Точно так же как и при экспорте, добавляем все свойств и объект `detail` в группу `order` для корректного приема новой версии JSON.

## Пример 7

### Условие

Аналогично [**Примеру 3**](#пример-3). 

Нужно по HTTP GET запросу, в котором задана дата, вернуть список номеров заказов от этой даты.

### Решение

```lsf
FORM exportOrders
    OBJECTS date = DATE PANEL

    OBJECTS order = Order
    PROPERTIES nm = number(order)
    FILTERS date(order) = date
;

getOrdersByDate (DATE d) {
    EXPORT exportOrders OBJECTS date = d JSON;
}
```

Url, на который следует слать HTTP запрос, будет выглядеть следующим образом: `http://localhost:7651/exec?action=Location.getOrdersByDate&p=12.11.2018`.

В ответ будет возвращен, например, следующий JSON :
```json
{
    "order": [
        {
            "nm": "42"
        },
        {
            "nm": "65"
        }
    ]
}
```

## Пример 8

### Условие

Аналогично [**Примеру 3**](#пример-3).

Для каждого заказа существует список приложенных к нему файлов.
```lsf
CLASS Attachment 'Приложение';
order = DATA Order (Attachment) NONULL DELETE;
name 'Имя' = DATA STRING (Attachment);
file = DATA FILE (Attachment);
```
Нужно реализовать HTTP GET запрос, который будет возвращать для переданного внутреннего идентификатора заказа его параметры и список файлов.
Кроме того, нужно отдельным запросом реализовать получение содержимого конкретного файла.  

### Решение
```lsf
FORM orderAttachments
    OBJECTS o = Order
    PROPERTIES(o) number, date

    OBJECTS attachments = Attachment
    PROPERTIES id = VALUE(attachments), name(attachments)
;

getOrderAttachments (LONG orderId) {
    FOR LONG(Order o AS Order) = orderId DO {
        EXPORT orderAttachments OBJECTS o = o JSON;
    }
}
```
Для формирования HTTP запроса нужно использовать следующий url : http://localhost:7651/exec?action=getOrderAttachments&p=32178.
В нем параметр _p_ содержит внутренний идентификатор заказа.

В ответ будет возвращен, например, следующий JSON :
```json
{
    "date": "20.10.2021",
    "number": "12",
    "attachments": [
        {
            "name": "File 1",
            "id": 32180
        },
        {
            "name": "File 2",
            "id": 32183
        }
    ]
}
```
В атрибутах _id_ будут содержаться внутренние идентификаторы файлов. Затем содержимое этих файлов может быть прочитано запросом по следующему url :
http://localhost:7651/exec?action=getOrderAttachment&p=32180. Действие _getOrderAttachment_ объявляется следующим образом :
```lsf
getOrderAttachment (LONG id) {
    FOR LONG(Attachment a AS Attachment) = id DO
        exportFile() <- file(a); 
}
```
Запрос вернет файл с _Content-Type_ соответствующим расширению свойства _file_. 