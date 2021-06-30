---
title: 'How-to: Состояние таблиц'
---

## Пример 1

### Условие

Заданы понятия книг, для которых определены наименование, жанр и цена.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book) IN id;
genre 'Жанр' = DATA ISTRING[30] (Book) IN id;

price 'Цена' = DATA NUMERIC[12,2] (Book) IN id;

FORM books 'Книги'
    OBJECTS b = Book
    PROPERTIES(b) READONLY name, genre, price
    PROPERTIES(b) NEWSESSION NEW, EDIT, DELETE
;

NAVIGATOR {
    NEW books;
}
```

Нужно вывести на форму количество книг с учетом отборов, сделанных пользователем.

### Решение

```lsf
filtered 'Книга отфильтрована' (Book b) = FILTER books.b;
filteredCount 'Кол-во книг' = GROUP SUM 1 IF filtered(Book b);

EXTEND FORM books
    PROPERTIES() READONLY filteredCount DRAW b TOOLBAR
;
```

Для решения используется [оператор `FILTER`](Filter_FILTER.md), который возвращает `TRUE`, если объект находится в текущей отборе на форме.

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1).

Нужно вывести в таблицу с книгами порядковый номер книги в текущем отборе и сортировке, сделанными пользователем.

### Решение

```lsf
index 'Порядок' (Book b) = PARTITION SUM 1 IF filtered(b) ORDER [ ORDER books.b](b);

EXTEND FORM books
    PROPERTIES(b) index
;
```

Свойство с текущим порядком, определяемое при помощи [оператора `ORDER`](Order_ORDER.md), не выделяется в отдельное именованное свойство, а используется непосредственно в выражении.

Полученная в обоих примерах форма с заданными пользователем отбором и сортировкой будет выглядеть следующим образом :

![](images/How-to_Table_status.png)
