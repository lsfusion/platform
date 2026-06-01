---
slug: "/How-to_JSON_export"
title: 'How-to: Экспорт JSON'
---

## Пример 1

### Условие

Нужно собрать JSON-значение из набора литералов — например, ответ API: `{"code":"OK","message":"привет"}`.

### Решение

```lsf
respond () {
    LOCAL f = FILE ();
    EXPORT JSON FROM code = 'OK', message = 'привет' TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

[Оператор `EXPORT`](../language/EXPORT_operator.md) с форматом `JSON` принимает список именованных колонок и пишет результат в файл, указанный после `TO`. У выражений в `FROM` нет параметров строки, поэтому платформа создаёт ровно один JSON-объект — `{"code":"OK","message":"привет"}`. Имена ключей берутся слева от `=`; если их не указать, столбец называется `exprN`.

Чтение результата: `fileToString(f(), 'UTF-8')` (из модуля [`Utils`](../paradigm/System_Utils.md)) заполняет системное локальное свойство `resultString[]`, его и выводит `MESSAGE`.

## Пример 2

### Условие

Есть класс `Book`:

```lsf
CLASS Book 'Книга';
name 'Название' = DATA ISTRING[100] (Book);
year 'Год' = DATA INTEGER (Book);
price 'Цена' = DATA NUMERIC[14,2] (Book);
```

Нужно выгрузить все книги в JSON-массив объектов с полями `name`, `year`, `price`, отсортированных по году.

### Решение

```lsf
exportBooks () {
    LOCAL f = FILE ();
    EXPORT JSON FROM name = name(Book b), year = year(b), price = price(b)
        ORDER year(b)
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

Когда хотя бы одно выражение в `FROM` содержит параметр (`b` объявлен как типизированный параметр `Book`), `EXPORT JSON FROM` итерирует все его значения, для которых хотя бы одно из выражений списка не равно `NULL`. На выходе — массив. `ORDER year(b)` фиксирует порядок строк по году.

Результат на трёх книгах:

```json
[{"name":"Записки из подполья","year":1864,"price":6.25},
 {"name":"Преступление и наказание","year":1866,"price":14.5},
 {"name":"Братья Карамазовы","year":1880,"price":18.99}]
```

## Пример 3

### Условие

Те же книги, но нужны только изданные с 1870 года, в порядке убывания года, не больше двух записей.

### Решение

```lsf
exportRecentBooks () {
    LOCAL f = FILE ();
    EXPORT JSON FROM name = name(Book b), year = year(b), price = price(b)
        WHERE year(b) >= 1870
        ORDER year(b) DESC
        TOP 2
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

`WHERE` фильтрует строки до сборки JSON; `ORDER … DESC` задаёт обратный порядок; `TOP 2` ограничивает первыми двумя записями.

На тех же трёх книгах под фильтр `year >= 1870` попадает только «Братья Карамазовы» (1880), поэтому даже при `TOP 2` массив получит один элемент:

```json
[{"name":"Братья Карамазовы","year":1880,"price":18.99}]
```

## Пример 4

### Условие

Нужно собрать составной JSON: вверху объект с метаданными (`title`), внутри — массив книг. Например, для отправки во внешний сервис.

### Решение

```lsf
exportCatalog () {
    LOCAL f = FILE ();
    EXPORT JSON FROM
        title = 'Books catalog',
        books = (JSON FROM name = name(Book b), year = year(b), price = price(b)
                 ORDER year(b))
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

Конструкция `books = (JSON FROM …)` встраивает выражение [оператора `JSON`](../language/JSON_operator.md) как значение колонки `books`. Внешний `EXPORT JSON FROM` строит один объект (`title` без параметров), а внутренний `JSON FROM` итерирует книги и подставляет массив:

```json
{"title":"Books catalog",
 "books":[{"year":1864,"price":6.25,"name":"Записки из подполья"},
          {"year":1866,"price":14.5,"name":"Преступление и наказание"},
          {"year":1880,"price":18.99,"name":"Братья Карамазовы"}]}
```

Так же собираются и более глубокие структуры — несколько параллельных вложенных `JSON FROM` внутри одного `EXPORT`: `items = (JSON FROM …), partners = (JSON FROM …), currencies = (JSON FROM …)`. Все они работают независимо друг от друга и пишутся в свои ключи верхнего уровня.

## Пример 5

### Условие

Двухуровневая иерархия с двумя видами вложенности на одном уровне: на выходе нужен объект с метаданными (`title`), вложенным **объектом** `store` (`{id, name}` без массива) и массивом заказов, у каждого заказа — собственный массив строк. Классы:

```lsf
CLASS Order 'Заказ';
number       'Номер'    = DATA STRING[50]   (Order);
customerName 'Заказчик' = DATA ISTRING[100] (Order);

CLASS OrderLine 'Строка заказа';
order     'Заказ'   = DATA Order        (OrderLine) NONULL DELETE;
item      'Артикул' = DATA STRING[50]   (OrderLine);
quantity  'Кол-во'  = DATA INTEGER      (OrderLine);
linePrice 'Цена'    = DATA NUMERIC[14,2] (OrderLine);
```

### Решение

```lsf
exportOrdersNested () {
    LOCAL f = FILE ();
    EXPORT JSON FROM
        title = 'Orders',
        store = (JSON FROM id = 'S-7', name = 'Main warehouse'),
        orders = (JSON FROM
            number = number(Order o),
            customer = customerName(o),
            lines = (JSON FROM
                item = item(OrderLine l),
                quantity = quantity(l),
                price = linePrice(l)
                WHERE order(l) = o
                ORDER item(l))
            ORDER number(o))
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

Два разных вида вложенности собираются здесь одним и тем же синтаксисом `column = (JSON FROM …)`:

- `store = (JSON FROM id = 'S-7', name = 'Main warehouse')` — у вложенного `JSON FROM` **нет параметров строки**, поэтому он возвращает **один JSON-объект** `{"id":"S-7","name":"Main warehouse"}`, который ложится значением ключа `store`. Никакого массива не появляется.
- `orders = (JSON FROM …)` — у вложенного есть параметр `Order o`, поэтому платформа итерирует его и возвращает **массив**. Тот же приём повторяется на уровне ниже для `lines`: внутренний `JSON FROM` по `OrderLine l` использует параметр `o` из внешнего контекста в `WHERE order(l) = o`. Для каждой внешней строки заново вычисляется вложенное выражение с подставленным `o`.

Результат:

```json
{"title":"Orders",
 "store":{"name":"Main warehouse","id":"S-7"},
 "orders":[
   {"number":"ORD-001","lines":[
     {"item":"SKU-100","quantity":2,"price":99.5},
     {"item":"SKU-200","quantity":1,"price":250}],"customer":"Иванов"},
   {"number":"ORD-002","lines":[
     {"item":"SKU-300","quantity":5,"price":12}],"customer":"Петров"}]}
```

Обратите внимание: внутри объектов ключи `lines` и `name` идут не в декларационном порядке. Сериализатор `JSON FROM` выводит вложенные JSON-значения отдельно от скалярных колонок; в Примере 6 у того же самого результата ключи в форме идут в декларационном порядке. По спецификации JSON это одинаковые структуры — порядок ключей в объекте не значим.

## Пример 6

### Условие

Тот же JSON, что и в [Примере 5](#пример-5), но через [форму](../paradigm/Forms.md) — выгружать конкретную форму, а не композицию `JSON FROM`-выражений. Удобно, когда экспорт и интерактивный показ должны разделять одну и ту же структуру полей.

### Решение

```lsf
GROUP store;

FORM exportOrdersAndLines
    PROPERTIES title = 'Orders'
    PROPERTIES IN store id = 'S-7', name = 'Main warehouse'

    OBJECTS orders = Order
    PROPERTIES(orders) number, customer = customerName
    ORDERS number(orders)

    OBJECTS lines = OrderLine
    PROPERTIES(lines) item, quantity, price = linePrice
    FILTERS order(lines) = orders
    ORDERS item(lines)
;

exportOrdersForm () {
    LOCAL f = FILE ();
    EXPORT exportOrdersAndLines JSON TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

`EXPORT formName JSON` строит JSON по [иерархии формы](../paradigm/In_a_structured_view_EXPORT_IMPORT.md): корнем выступает объект, ключи — имена групп свойств / групп объектов формы / скалярных свойств без параметров, значения — соответственно вложенные объекты, массивы строк или сами скаляры. Здесь:

- `title = 'Orders'` — скалярное свойство без параметров, ложится скаляром в корень;
- `GROUP store;` объявляет [группу свойств](../paradigm/Groups_of_properties_and_actions.md), а `PROPERTIES IN store id = 'S-7', name = 'Main warehouse'` кладёт два скаляра внутрь этой группы — на выходе это **вложенный объект** `"store":{"id":"S-7","name":"Main warehouse"}` (массив здесь не появляется, потому что в группу не входит ни одной группы объектов);
- группа `OBJECTS orders = Order` — **массив** `orders`; ключи внутри элемента — `number` и `customer` (последний переименован из `customerName` через `customer = customerName`);
- вложенная группа `OBJECTS lines = OrderLine` с `FILTERS order(lines) = orders` ложится массивом `lines` внутрь каждого `orders`-элемента.

Структура итогового JSON совпадает с Примером 5 — те же ключи, те же значения, та же вложенность:

```json
{"title":"Orders",
 "store":{"id":"S-7","name":"Main warehouse"},
 "orders":[
   {"number":"ORD-001","customer":"Иванов","lines":[
     {"item":"SKU-100","quantity":2,"price":99.5},
     {"item":"SKU-200","quantity":1,"price":250}]},
   {"number":"ORD-002","customer":"Петров","lines":[
     {"item":"SKU-300","quantity":5,"price":12}]}]}
```

Имена ключей в формовом варианте задаются именами групп объектов и колонок формы; переименовать колонку можно тем же `EXTID`, что и в обратном [импорте через форму](How-to_JSON_parsing.md#пример-5).
