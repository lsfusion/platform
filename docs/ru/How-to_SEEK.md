---
title: 'How-to: SEEK'
---

## Пример 1

### Условие

Определена логика книг и категорий. Создана форма со списком книг, разбитых по категориям.

```lsf
REQUIRE Time;

CLASS Category 'Категория';
name 'Наименование' = DATA ISTRING[50] (Category) IN id;

CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book) IN id;

category 'Категория' = DATA Category (Book) NONULL;
nameCategory 'Категория' (Book b) = name(category(b));

FORM book 'Книга'
    OBJECTS b = Book PANEL
    PROPERTIES(b) name, nameCategory

    EDIT Book OBJECT b
;

FORM books 'Книги'
    OBJECTS c = Category
    PROPERTIES(c) READONLY name
    PROPERTIES(c) NEWSESSION NEW, EDIT, DELETE

    OBJECTS b = Book
    PROPERTIES(b) READONLY name
    FILTERS category(b) == c
;

NAVIGATOR {
    NEW books;
}
```

Нужно создать действие, которое создаст новую книгу, автоматически проставит текущую категорию и выберет ее активной после того, как пользователь сохранит и закроет форму.

### Решение

```lsf
createBook 'Создать книгу' (Category c)  {
    NEWSESSION {
        NEW newBook = Book {
            category(newBook) <- c;
            DIALOG book OBJECTS b = newBook INPUT DO {
                SEEK books.b = newBook;
            }
        }
    }
}

EXTEND FORM books
    PROPERTIES(c) createBook DRAW b TOOLBAR
;
```

После закрытия формы вызывается [оператор `SEEK`](SEEK_operator.md), который делает добавленный объект активным.

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1). Также добавлена логика покупателей. Для каждого покупателя и книги можно задать цену в определенной форме.

```lsf
CLASS Customer 'Покупатель';
name 'Наименование' = DATA ISTRING[50] (Customer) IN id;

price 'Цена' = DATA NUMERIC[14,2] (Customer, Book);

FORM prices 'Цены'
    OBJECTS c = Customer PANEL
    PROPERTIES(c) name SELECTOR

    OBJECTS b = Book
    PROPERTIES name(b) READONLY, price(c, b)
;

NAVIGATOR {
    NEW prices;
}
```

Нужно добавить покупателя по умолчанию, который будет проставляться при открытии формы.

### Решение

```lsf
defaultCustomer 'Покупатель по умолчанию' = DATA Customer ();
nameDefaultCustomer 'Покупатель по умолчанию' () = name(defaultCustomer());

EXTEND FORM options PROPERTIES() nameDefaultCustomer;
DESIGN options { commons { MOVE PROPERTY(nameDefaultCustomer()); } }

EXTEND FORM prices
    EVENTS ON INIT { SEEK prices.c = defaultCustomer(); }
;
```

Свойство с покупателем по умолчанию добавляется на форму `'Настройка'` во вкладку `'Общие'`. Текущий объект изменится при входе на форму, так как сработает [событие `ON INIT`](Event_block.md).

## Пример 3

### Условие

Предположим, что есть некоторая форма отчетов, в котором задан интервал дат.

```lsf
FORM report 'Отчет'
    OBJECTS dFrom = DATE PANEL, dTo = DATE PANEL
    PROPERTIES VALUE(dFrom), VALUE(dTo)
;

NAVIGATOR {
    NEW report;
}
```

Нужно сделать кнопки, которые будут изменять интервал на последнюю неделю, текущий месяц и последний месяц.

### Решение

```lsf
setReportLastWeek 'Последняя неделя' ()  {
    SEEK report.dFrom = subtract(currentDate(), 7);
    SEEK report.dTo = subtract(currentDate(), 1);
}
setReportCurrentMonth 'Текущий месяц' ()  {
    SEEK report.dFrom = firstDayOfMonth(currentDate());
    SEEK report.dTo = lastDayOfMonth(currentDate());
}
setReportLastMonth 'Последний месяц' ()  {
    SEEK report.dFrom = firstDayOfMonth(subtract(firstDayOfMonth(currentDate()), 1));
    SEEK report.dTo = subtract(firstDayOfMonth(currentDate()), 1);
}

EXTEND FORM report
    PROPERTIES() setReportLastWeek, setReportCurrentMonth, setReportLastMonth
;
```

Свойства по работе с датами находятся в [системном модуле](Modules.md) `Time`, который подключается в самом начале через инструкцию `REQUIRE`.
