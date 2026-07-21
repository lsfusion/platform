---
slug: "/How-to_JSON_parsing"
title: 'How-to: Разбор JSON'
---

Далее используются свойства-обёртки над PostgreSQL-функциями `jsonb_*`, которые платформа поставляет в модуле `Utils`: `field`, `fieldText`, `array`, `arrayText`, `map`, `mapText`, `arrayElement`. Их сигнатуры и описание собраны в [`Utils` → `Свойства доступа к JSON`](../paradigm/System_Utils.md#json-access); этот how-to опирается на них и не повторяет их интерфейс.

## Пример 1

### Условие

На вход приходит значение JSON следующей структуры:

```json
{
    "version": "1.0",
    "store": {"id": "S-7", "name": "Главный склад"},
    "orders": [
        {
            "number": "ORD-1001",
            "customer": {"id": "C-21", "name": "Иванов"},
            "lines": [
                {"item": "SKU-100", "quantity": 2, "price":  99.50},
                {"item": "SKU-200", "quantity": 1, "price": 250.00}
            ]
        },
        {
            "number": "ORD-1002",
            "customer": {"id": "C-22", "name": "Петров"},
            "lines": [
                {"item": "SKU-300", "quantity": 5, "price":  12.00}
            ]
        }
    ]
}
```

Нужно достать отдельные значения из верхнего уровня и из глубоко вложенных узлов — без описания полной формы под этот JSON.

### Решение

```lsf
showInfo (JSON j) {
    MESSAGE 'version          = ' + fieldText(j, 'version');
    MESSAGE 'store.name       = ' + fieldText(j, 'store', 'name');
    MESSAGE 'orders[1].number = ' + fieldText(array(field(j, 'orders'), 1), 'number');
    MESSAGE 'orders[1].cust   = ' + fieldText(array(field(j, 'orders'), 1), 'customer', 'name');
    MESSAGE 'orders[2] (raw)  = ' + arrayElement(field(j, 'orders'), 2);
}
```

`fieldText(j, 'version')` достаёт скалярное поле верхнего уровня.

`fieldText(j, 'store', 'name')` спускается на уровень глубже за один вызов — за счёт перегрузки на два строковых аргумента. Перегрузка на три аргумента покрывает три уровня; для четырёх и более — композиция через `field` (см. ниже).

Чтение поля внутри элемента массива собирается через композицию: `field(j, 'orders')` возвращает массив как `JSON`, `array(…, 1)` берёт его первый элемент (тоже `JSON`), `fieldText(…, 'number')` читает у этого элемента поле как `STRING`. Если внутри элемента нужно спуститься ещё на уровень — у `fieldText` берётся перегрузка с двумя ключами: `fieldText(array(field(j, 'orders'), 1), 'customer', 'name')` соответствует пути `orders[0].customer.name` (в lsFusion индексация 1-based).

`arrayElement(field(j, 'orders'), 2)` отличается от `array(…, 2)` тем, что отдаёт элемент сразу как `STRING` — текстовое представление jsonb. Полезно для логирования и отладки.

## Пример 2

### Условие

JSON той же структуры, что и в [примере 1](#пример-1). Нужно построчно обойти все заказы и в каждом — все его строки.

### Решение

```lsf
walkOrders (JSON j) {
    LOCAL report = TEXT ();
    report() <- '';
    FOR JSON ord = array(field(j, 'orders'), INTEGER o) DO {
        report() <- report() + 'order ' + fieldText(ord, 'number')
                              + ' / ' + fieldText(ord, 'customer', 'name') + ':\n';
        FOR JSON line = array(field(ord, 'lines'), INTEGER l) DO
            report() <- report() + '  - ' + fieldText(line, 'item')
                                  + ' x ' + fieldText(line, 'quantity')
                                  + ' @ ' + fieldText(line, 'price') + '\n';
    }
    MESSAGE report();
}
```

`FOR JSON ord = array(field(j, 'orders'), INTEGER o)` — итератор: параметр `o` объявлен на месте как `INTEGER` и пробегает все индексы массива; для каждого `o` свойство `array(…, o)` отдаёт элемент как `JSON`, и `ord` связывается с этим значением на одну итерацию тела цикла. Сам индекс `o` тоже доступен внутри тела — например, для нумерации записей.

Вложенный `FOR JSON line = array(field(ord, 'lines'), INTEGER l) DO` работает по тому же принципу, только теперь массив — это `lines` внутри текущего заказа. `ord` остаётся в области видимости вложенного цикла, поэтому его поля можно использовать в условии или в правой части.

Тот же шаблон без `FOR` работает в любом скалярном выражении. Для подсчёта суммарного количества строк во всех заказах достаточно собрать `(o, l)`-пары и просуммировать единицу:

```lsf
totalLines (JSON j) = GROUP SUM 1
    IF array(field(array(field(j, 'orders'), INTEGER o), 'lines'), INTEGER l);
```

В условии `GROUP SUM` собран весь путь `j → orders → array → lines → array`; `o` и `l` объявлены на месте и пробегают независимо. Считается единица для каждой `(o, l)`-пары, для которой соответствующая строка существует.

Аналогично через `GROUP CONCAT` можно собрать сам отчёт одной декларативной агрегацией, без `LOCAL` и `FOR`:

```lsf
ordersReport (JSON j) =
    GROUP CONCAT
        fieldText(array(field(j, 'orders'), INTEGER o), 'number')
          + ' / ' + fieldText(array(field(j, 'orders'), o), 'customer', 'name')
          + ' :: ' + fieldText(array(field(array(field(j, 'orders'), o), 'lines'), INTEGER l), 'item')
          + ' x ' + fieldText(array(field(array(field(j, 'orders'), o), 'lines'), l), 'quantity')
          + ' @ ' + fieldText(array(field(array(field(j, 'orders'), o), 'lines'), l), 'price'),
        '\n'
        ORDER o, l;
```

Здесь `(o, l)`-пары собираются ровно так же, как в `GROUP SUM`, а тело — выражение, склеиваемое разделителем `'\n'`. `ORDER o, l` фиксирует порядок строк отчёта: сначала по индексу заказа, внутри — по индексу его строки. Результат — единая `STRING`, готовая к выводу через `MESSAGE` или к записи в свойство.

Эта форма строит плоский отчёт — каждая выходная строка относится к одной `(заказ, строка-заказа)`-паре, без сгруппированной шапки заказа. Если шапка важна, либо приходится возвращаться к императивному варианту выше, либо обрамлять `GROUP CONCAT` в более сложную композицию (например, два уровня агрегации через вспомогательное свойство).

Обход словарной структуры (ключ → значение) делается через `map` / `mapText`. Параметр-ключ объявляется на месте — так же, как `INTEGER`-индекс у `array`:

```lsf
listMeta (JSON j) {
    LOCAL out = TEXT ();
    out() <- '';
    FOR STRING v = mapText(field(j, 'store'), STRING k) DO
        out() <- out() + k + ' -> ' + v + '\n';
    MESSAGE out();
}
```

Здесь `mapText(field(j, 'store'), STRING k)` для JSON-объекта `store` отдаёт по строке на каждую пару `(k, v)`: ключ-параметр `k` — строка-имя поля, тело — значение поля как `STRING`.

## Пример 3

### Условие

На вход приходит плоский JSON-массив объектов:

```json
[
    {"name": "Капитанская дочка",     "year": 1836, "price":  8.50},
    {"name": "Евгений Онегин",        "year": 1833, "price": 11.25},
    {"name": "Герой нашего времени",  "year": 1840, "price":  9.75}
]
```

Под него не хочется заводить отдельную форму со staging-свойствами — каждое поле потребляется ровно один раз и сразу пишется в новый объект `Book`.

### Решение

```lsf
importBooksFlat 'Импорт книг' () {
    INPUT f = FILE DO NEWSESSION {
        IMPORT JSON FROM f FIELDS
            ISTRING[100] name, INTEGER year, NUMERIC[14,2] price
        DO NEW b = Book {
            name(b)  <- name;
            year(b)  <- year;
            price(b) <- price;
        }
        APPLY;
    }
}
```

`IMPORT JSON FROM f FIELDS …` ожидает на вход плоский JSON-массив объектов: ключи объектов совпадают с именами полей в списке (`name`, `year`, `price`), их значения приводятся к указанным типам. Тело `DO` выполняется для каждой строки массива по очереди — в нём имена `name`, `year`, `price` доступны как обычные параметры со значениями текущей строки.

В отличие от формового варианта, staging-свойства здесь не нужны; и [`imported[INTEGER]`](../language/IMPORT_operator.md) тоже отсутствует, потому что итерации в явном виде нет — её роль выполняет сама `DO`-часть.

`FIELDS … DO` стоит выбирать, когда значения нужны ровно один раз и валидация не требует нескольких проходов. Если потребуется сначала проверить ссылки, потом пакетно создать объекты, и наконец заполнить их свойства — переключиться на форму или на промежуточные `LOCAL` (см. [пример 5](#пример-5)).

## Пример 4

### Условие

JSON по-прежнему плоский на верхнем уровне, но в каждой строке встречается вложенный объект:

```json
[
    {"number": "ORD-2001", "customer": {"id": "C-101", "name": "Тургенев"}},
    {"number": "ORD-2002", "customer": {"id": "C-102", "name": "Лермонтов"}},
    {"number": "ORD-2003", "customer": {"id": "C-103", "name": "Гоголь"}}
]
```

Нужно для каждого элемента массива создать `Order` и разложить вложенный объект `customer` по двум свойствам — `customerName` и `customerId`.

### Решение

```lsf
CLASS Order 'Заказ';
number       'Номер'         = DATA STRING[50]   (Order);
customerName 'Заказчик'      = DATA ISTRING[100] (Order);
customerId   'Код заказчика' = DATA STRING[50]   (Order);

importOrders 'Импорт заказов' () {
    INPUT f = FILE DO NEWSESSION {
        IMPORT JSON FROM f FIELDS
            STRING[50] number, JSON customer
        DO NEW o = Order {
            number(o)       <- number;
            customerName(o) <- fieldText(customer, 'name');
            customerId(o)   <- fieldText(customer, 'id');
        }
        APPLY;
    }
}
```

Поле `customer` объявлено в `FIELDS` как `JSON` — на каждой итерации в этот параметр приходит весь вложенный объект как `JSON`-значение. Дальше внутри `DO` он раскладывается обычными `fieldText`-обёртками: `fieldText(customer, 'name')`, `fieldText(customer, 'id')`. Для более глубокой вложенности — `fieldText(customer, 'address', 'city')` и т. п.

Этот приём заменяет полноценный формовый импорт, пока вложенность ограничена объектами в строках массива. Как только в JSON появляются вложенные массивы, по которым тоже нужно итерироваться, — без формы или внешнего `FOR JSON … = array(...)` (см. [пример 2](#пример-2)) обойтись уже сложнее.

## Пример 5

### Условие

Есть класс `Book` и его форма.

```lsf
CLASS Book 'Книга';
name 'Название' = DATA ISTRING[100] (Book);
year 'Год' = DATA INTEGER (Book);
price 'Цена' = DATA NUMERIC[14,2] (Book);

FORM books 'Книги'
    OBJECTS b = Book
    PROPERTIES(b) name, year, price, NEW, DELETE
;

NAVIGATOR {
    NEW books;
}
```

Нужно сделать кнопку, которая загрузит список книг из JSON-файла со следующей структурой:

```json
{
    "books": [
        {"name": "Преступление и наказание", "year": 1866, "price": 14.50},
        {"name": "Братья Карамазовы",        "year": 1880, "price": 18.99},
        {"name": "Записки из подполья",      "year": 1864, "price":  6.25}
    ]
}
```

### Решение

```lsf
importBookName  = DATA LOCAL ISTRING[100]   (INTEGER);
importBookYear  = DATA LOCAL INTEGER        (INTEGER);
importBookPrice = DATA LOCAL NUMERIC[14,2]  (INTEGER);

FORM importBooks
    OBJECTS books = INTEGER
    PROPERTIES(books) importBookName  EXTID 'name',
                      importBookYear  EXTID 'year',
                      importBookPrice EXTID 'price'
;

importBooksFromJSON 'Импорт из JSON' () {
    INPUT f = FILE DO NEWSESSION {
        IMPORT importBooks JSON FROM f;

        FOR importBookName(INTEGER i) NEW b = Book DO {
            name(b)  <- importBookName(i);
            year(b)  <- importBookYear(i);
            price(b) <- importBookPrice(i);
        }
        APPLY;
    }
}

EXTEND FORM books
    PROPERTIES() importBooksFromJSON
;
```

Структура формы `importBooks` повторяет структуру JSON: под массив `books` объявлена группа объектов `OBJECTS books = INTEGER`, а под её зонтом — три свойства, имена которых сопоставлены с JSON-ключами через `EXTID`. `INTEGER` здесь — синтетический ключ строки массива, создаваемый платформой.

[`IMPORT … JSON FROM`](../language/IMPORT_operator.md) считывает файл и наполняет локальные свойства: `importBookName(i)`, `importBookYear(i)`, `importBookPrice(i)` для каждой строки `i`.

`FOR importBookName(INTEGER i)` обходит каждую строку, для которой импортированное название не `NULL`, и для каждой создаёт объект класса `Book`. Системное свойство `imported[INTEGER]` для итерации после `IMPORT … JSON FROM` использовать не следует — иначе чем для плоских форматов (`IMPORT XLS`, `IMPORT CSV`), в этом режиме оно не выставляется; роль признака «строка пришла из файла» играет любое непустое staging-свойство.

Следует учитывать, что пустая строка `""` в JSON-файле импортируется как пустая, но не `NULL`, строка (см. [Структурированное представление](../paradigm/Structured_view.md)). Такое значение проходит условия на не-`NULL` — приведённый выше `FOR`, `IF`, агрегации вроде `GROUP LAST`. Если пустые строки должны вести себя как отсутствующие значения, их нужно нормализовать сразу после импорта:

```lsf
importBookName(INTEGER i) <- NULL WHERE importBookName(i) = '';
```

Создание объектов изолировано в `NEWSESSION`, чтобы импорт не зацепил несохранённые правки на самой форме `books`. `APPLY` фиксирует изменения; при нарушении ограничения он сам показывает пользователю текст ошибки.

## Пример 6

### Задача

JSON содержит массивы, вложенные на нескольких уровнях, причём самый внутренний массив состоит из примитивных значений, а не объектов:

```json
{
    "docflows": [
        {
            "number": "DF-1001",
            "events": [
                {"type": "sent",     "participants": ["C-101", "C-102"]},
                {"type": "received", "participants": ["C-103"]}
            ]
        },
        {
            "number": "DF-1002",
            "events": [
                {"type": "sent", "participants": []}
            ]
        }
    ]
}
```

Нужно загрузить все три уровня, сохранив связи между ними.

### Решение

```lsf
docflowNumber = DATA LOCAL STRING[50] (INTEGER);

eventDocflow = DATA LOCAL INTEGER (INTEGER);
eventType    = DATA LOCAL STRING[50] (INTEGER);

participantEvent = DATA LOCAL INTEGER (INTEGER);
participantId    = DATA LOCAL STRING[50] (INTEGER);

FORM importDocflows
    OBJECTS d = INTEGER EXTID 'docflows'
    PROPERTIES(d) docflowNumber EXTID 'number'

    OBJECTS e = INTEGER EXTID 'events'
    PROPERTIES(e) eventType EXTID 'type'
    FILTERS eventDocflow(e) = d

    OBJECTS p = INTEGER EXTID 'participants'
    PROPERTIES(p) participantId EXTID 'value'
    FILTERS participantEvent(p) = e
;

showDocflows (FILE f) {
    IMPORT importDocflows JSON FROM f;

    FOR docflowNumber(INTEGER d) DO {
        MESSAGE 'Документооборот ' + docflowNumber(d);
        FOR eventDocflow(INTEGER e) = d DO
            MESSAGE 'событие ' + eventType(e) + ': ' +
                (GROUP CONCAT participantId(INTEGER p) IF participantEvent(p) = e, ', ' ORDER p);
    }
}
```

Каждый вложенный массив получает собственный блок `OBJECTS` по `INTEGER` — ровно как единственный массив в [примере 5](#пример-5). Связью с родительским уровнем служит `LOCAL`-свойство от дочерней строки к родительской (`eventDocflow`, `participantEvent`), указанное в `FILTERS`: при импорте платформа заполняет его строкой охватывающего элемента массива. Этот же фильтр делает дочернюю группу потомком родительской при построении [иерархии групп объектов](../paradigm/Static_view.md#hierarchy) — именно он направляет чтение массива `events` из ключа `events` внутри каждого элемента `docflows`, а не из корня формы.

Самый внутренний массив состоит из строк, а не объектов; по [преобразованию предопределённого значения `value`](../paradigm/Structured_view.md#value) каждый такой элемент читается как объект `{ "value" : ... }`, поэтому промежуточное свойство отображается через `EXTID 'value'`.

Если вложенный массив лежит не прямо в элементе, а под промежуточным ключом-объектом (скажем, `"status": {"details": [...]}`), объявите [группу свойств](../language/GROUP_statement.md) с этим именем экспорта / импорта и добавьте дочерний блок `OBJECTS` в неё через `IN`. Группа вкладывается под группу объектов итерируемого элемента — по тому же построению иерархии, — а не под корень формы.
