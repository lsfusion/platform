---
title: 'How-to: Экспорт данных'
---

## Пример 1

### Условие

Есть заказы на продажу покупателям некоторых книг.

```lsf
REQUIRE Time;

CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[100] (Book) IN id;

CLASS Customer 'Покупатель';
name 'Наименование' = DATA ISTRING[50] (Customer) IN id;
address 'Адрес' = DATA ISTRING[50] (Customer) IN base;

CLASS Currency 'Валюта';
name 'Наименование' = DATA ISTRING[50] (Currency) IN id;

CLASS Order 'Заказ';
date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA STRING[10] (Order);

customer 'Покупатель' = DATA Customer (Order);
nameCustomer 'Покупатель' (Order o) = name(customer(o));

CLASS OrderDetail 'Строка заказа';
order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;

book 'Книга' = DATA Book (OrderDetail) NONULL;
nameBook 'Книга' (OrderDetail d) = name(book(d));

quantity 'Количество' = DATA INTEGER (OrderDetail);
price 'Цена' = DATA NUMERIC[14,2] (OrderDetail);

currency 'Валюта' = DATA Currency (OrderDetail);
nameCurrency 'Валюта' (OrderDetail d) = name(currency(d));

FORM order 'Заказ'
    OBJECTS o = Order PANEL
    PROPERTIES(o) date, number, nameCustomer

    OBJECTS d = OrderDetail
    PROPERTIES(d) nameBook, quantity, nameCurrency, price, NEW, DELETE
    FILTERS order(d) == o

    EDIT Order OBJECT o
;

FORM orders 'Заказы'
    OBJECTS o = Order
    PROPERTIES(o) READONLY date, number, nameCustomer
    PROPERTIES(o) NEWSESSION NEW, EDIT, DELETE
;

NAVIGATOR {
    NEW orders;
}
```

Нужно сделать кнопку, которая выгрузит содержимое заказа в XML-формат.

### Решение

```lsf
GROUP Info;
GROUP Customer : Info;

GROUP Specification;

GROUP price;

FORM Order
    PROPERTIES timeStamp = currentDateTime() ATTR

    OBJECTS order = Order
    PROPERTIES(order) IN Info date, number
    PROPERTIES IN Customer nameCustomer(order) EXTID 'name', =address(customer(order)) EXTID 'address'

    PROPERTIES IN Specification count = [GROUP SUM 1 BY order(OrderDetail d)](order) ATTR

    OBJECTS Detail = OrderDetail IN Specification
    PROPERTIES(Detail) nameBook, quantity,
                       nameCurrency IN price EXTID 'currency' ATTR, price IN price EXTID 'value'
    FILTERS order(Detail) = order
;

exportToXML 'Экспорт в XML' (Order o) {
    EXPORT Order OBJECTS order = o XML;
    open(exportFile());
}

EXTEND FORM orders
    PROPERTIES(o) exportToXML TOOLBAR
;
```

Для выгрузки в XML формат, сначала создается форма [соответствующей структуры](Structured_view.md), а затем вызывается [оператор `EXPORT`](EXPORT_operator.md). Он на основе формы формирует файл и складывает его в свойство `exportFile`, которое затем открывается на клиенте при помощи действия `open`. Оно показывает файл в приложении, ассоциированном с его расширением (в данном случае .xml).

Результирующий xml будет выглядеть следующим образом :
```xml
<Order timeStamp="13.11.18 12:28:58">
   <Info>
      <date>13.11.18</date>
      <number>12</number>
      <Customer>
         <name>Покупатель 2</name>
         <address>Адрес 2</address>
      </Customer>
   </Info>
   <Specification count="2">
      <Detail>
         <nameBook>Книга 2</nameBook>
         <quantity>1</quantity>
         <price currency="USD">3.99</price>
      </Detail>
      <Detail>
         <nameBook>Книга 1</nameBook>
         <quantity>2</quantity>
         <price currency="EUR">4.99</price>
      </Detail>
   </Specification>
</Order>
```

Верхний тэг `Order` совпадает с именем формы. Все остальные имена тэгов определяются либо именем свойством на форме (например, `date`, `number` или `count`), либо при помощи специального параметра `EXTID` (например, `name` и `address`). Использование `EXTID` предпочтительнее в тех случаях, когда на форме будут свойства с одним именем, но для разных объектов.

Для создания "промежуточных" тэгов (например, `Info`, `Customer` или `Specification`) создаются группы, соответствующим образом вложенные друг в друга, к которым затем привязываются нужные свойства.

Использование атрибута `ATTR` позволяет поместить значение не в отдельный тэг, а в атрибут верхнего "родительского" тэга (например, `timeStamp` или `count`).

Для того, чтобы поместить в тэг `price` атрибут валюты, создается группа с соответствующим именем, в которую затем помещаются два свойства : `nameCurrency` (с валютой) и `price` (сама цена). Для валюты задается атрибут `ATTR` - поэтому она попадает в тэг `price` атрибутом. Для цены же задается предопределенный `EXTID 'value'`, благодаря чему значение пишется непосредственно в тэг `price`. Без указания всех этих параметров результат бы выглядел следующим образом:

```xml
<price><currency>USD</currency><price>4.99</price></price>
```

Если есть формат требуемого файла XML, в который необходимо сделать выгрузку, то можно пользоваться следующими правилами.

Любой тэг в результирующем файле генерируется одним из четырех элементов:

1.  *Свойством* (объявляется при помощи `PROPERTIES`).
2.  *Объектом* (объявляется при помощи `OBJECTS`).
3.  *Группой свойств* (объявляется при помощи `GROUP` за пределами формы).
4.  *Формой* (самый верхний тэг)

-   Если тэг повторяется несколько раз подряд (например, `Detail`), то должен быть объявлен *объект* соответствующим именем.

-   Если тэг содержит вложенные тэги, но при этом сам встречается ровно один раз (например, `Specification`), то для него должна быть объявлена *группа свойств*.

-   Если тэг содержит атрибуты (например, `count = "2"`), то он должен генерироваться либо *группой свойств*, либо *объектом* (как правило, в зависимости от того один или несколько раз встречается этот тэг).

-   Если тэг содержит значение, то он генерируется либо *свойством* (например, `nameBook` или `quantity`) с соответствующим именем, либо группой свойств / объектом с единственным "вложенным" свойством с `EXTID 'value'`).

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1).

Нужно сделать кнопку, которая выгрузит содержимое заказа в JSON-формат.

### Решение

```lsf
exportToJSON 'Экспорт в JSON' (Order o) {
    EXPORT Order OBJECTS order = o JSON;
    open(exportFile());
}

EXTEND FORM orders
    PROPERTIES(o) exportToJSON TOOLBAR
;
```

Результирующий файл будет выглядеть следующим образом:
```json
{  
   "timeStamp":"13.11.18 15:11:45",
   "Info":{  
      "date":"13.11.18",
      "number":"12",
      "Customer":{  
         "address":"Адрес 2",
         "name":"Покупатель 2"
      }
   },
   "Specification":{  
      "count":2,
      "Detail":[  
         {  
            "quantity":1,
            "price":3.99,
            "nameBook":"Книга 2"
         },
         {  
            "quantity":2,
            "price":4.99,
            "nameBook":"Книга 1"
         }
      ]
   }
}
```

Следует отметить, что в случае выгрузки в формат JSON опция `ATTR` игнорируется.

## Пример 3

### Условие

Аналогично [**Примеру 1**](#пример-1).

Нужно сделать кнопку, которая выгрузит все заказы за определенную дату в CSV формат.

### Решение

```lsf
exportToCSV (DATE date) {
    LOCAL file = FILE();
    EXPORT CSV HEADER FROM number = number(order(OrderDetail d)),
                    customer = nameCustomer(order(d)),
                    book = nameBook(d),
                    quantity(d),
                    price(d)
           WHERE date(order(d)) = date TO file;
    WRITE CLIENT DIALOG file() TO 'orders';
}

FORM exportParameters 'Параметры'
    OBJECTS d = DATE PANEL
    PROPERTIES(d) 'Дата' = VALUE
;
exportToCSV 'Экспорт в CSV' () {
    DIALOG exportParameters OBJECTS d INPUT DO
        exportToCSV(d);
}

EXTEND FORM orders
    PROPERTIES() exportToCSV DRAW o TOOLBAR
;
```

Первое действие принимает на вход дату и, при помощи оператора `EXPORT`, формирует плоский файл CSV с разделителем точка с запятой. Файл будет выглядеть следующим образом:

```csv
number;customer;book;quantity;price
14;Покупатель 1;Книга 1;2;8.99
12;Покупатель 2;Книга 2;1;3.99
12;Покупатель 2;Книга 1;2;4.99
```

Для первых трех параметров переопределено имя колонки, а остальные два используют имя соответствующих свойств. Результат сначала записывается в локальное свойство `file`, из которого затем, при помощи [оператора `WRITE`](WRITE_operator.md), пользователю будет предложено сохранить его на рабочий компьютер.

Второе действие открывает форму, запрашивает у пользователя дату и вызывает первое действие по экспорту данных.
