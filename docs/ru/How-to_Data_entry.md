---
title: 'How-to: Ввод данных'
---

## Пример 1

### Условие

Есть форма которая отображает список книг. Необходимо реализовать ввод имени, но только большими буквами. Также должны поддерживаться групповая корректировка, PASTE и т.п.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[100] (Book);

FORM books
     OBJECTS b = Book
;

NAVIGATOR {
    NEW books;
}
```

### Решение

```lsf
changeName(Book b)  {
    // вводим ISTRING[100] "в параметр" s 
    // (автоматически оборачивается в REQUEST, то есть доступны групповая корректировки, PASTE и т.п.)
    INPUT s = ISTRING[100] 
        DO // проверка на requestCanceled
            name(b) <- s;
}

EXTEND FORM books
    PROPERTIES (b) name ON CHANGE changeName(b)
;
```

  

## Пример 2

### Условие

Есть форма которая отображает список книг. В этой форме, при необходимости, можно задать жанр, и, соответственно, книги только этого жанра будут отображаться. Также есть форма списка заказов, в которой также можно фильтровать по жанрам. Для книги существует признак запрета, соответственно в заказе могут быть только не запрещенные книги.

```lsf
CLASS Genre 'Жанр';
name 'Наименование' = DATA ISTRING[100] (Genre);

genre 'Жанр' = DATA Genre (Book);
isForbidden 'Запрет' = DATA BOOLEAN (Book);

CLASS Order 'Заказ';
book 'Книга' = DATA Book (Order);
nameBook 'Наименование книги' (Order o) = name(book(o));

date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA STRING[100] (Order);

CONSTRAINT isForbidden(book(Order o))
    CHECKED BY book MESSAGE 'Запрещено выбирать эту книгу';

FORM booksByGenre
     OBJECTS g = Genre PANEL
     PROPERTIES (g) name
     OBJECTS b = Book
     PROPERTIES (b) name
;


FORM orders 'Заказы'
    OBJECTS g = Genre PANEL
    PROPERTIES (g) name

    OBJECTS o = Order
    PROPERTIES(o) READONLY date, number
    FILTERS g == genre(book(o))
;

NAVIGATOR {
    NEW orders;
}
```

Необходимо подменить на форме заказов выбор книги таким образом, чтобы вызывалась форма выбора по жанрам. При этом :

-   По умолчанию, в качестве фильтра по жанру подставлялся жанр формы заказов
-   По умолчанию, подставлялась текущая книга заказа (если она была выбрана до этого)
-   Можно было сбросить книгу заказа (выбрать значение "Не определено")
-   Подставлялись только книги с учетом ограничения запрещенности
-   По полю должны быть доступны групповая корректировка, PASTE и т.п.

### Решение

```lsf
changeNameBook(Genre g, Order o)  {
    // (автоматически оборачивается в REQUEST, то есть доступны групповая корректировки, PASTE и т.п.)
    DIALOG booksByGenre OBJECTS 
        g = g NULL, // разрешен NULL на входе
        b = book(o) NULL INPUT bk NULL CONSTRAINTFILTER
        // book(o) NULL - подставляем book(o) на вход (при необходимости можно не указывать вход,
        //   то есть писать просто b INPUT ..., что в свою очередь эквивалентно b=NULL NULL INPUT ...)
        // INPUT bk NULL - возвращаем значение этого объекта "в параметр" bk (разрешен возврат NULL то есть
        //   будет кнопка сбросить). Так как не указана опция TO, результат будет записан в requestedObject
        // CONSTRAINTFILTER - учитываем для объекта b ограничения из предположения что результат будет записан
        //   в свойство, передаваемое на вход (в данном случае book(o)), при необходимости можно задать 
        //   другое выражение в виде CONSTRAINTFILTER = dataBook(o)
    DO // проверка на requestCanceled
        book(o) <- bk;
}

EXTEND FORM orders
    PROPERTIES (o) nameBook ON CHANGE changeNameBook(g, o)
;
```

  
