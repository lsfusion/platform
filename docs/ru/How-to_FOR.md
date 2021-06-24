---
title: 'How-to: FOR'
---

## Пример 1

### Условие

Есть список книг с наименованиями.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book);

FORM books 'Книги'
    OBJECTS b = Book
    PROPERTIES(b) name, NEW, DELETE
;

NAVIGATOR {
    NEW books;
}
```

Нужно найти все книги, содержащие определенную строк и выдать сообщение с именем и внутренним кодом.

### Решение

```lsf
findNemo 'Найти книгу' ()  {
    FOR isSubstring(name(Book b), 'Nemo') DO {
        MESSAGE 'Найдена книга ' + name(b) + ' с внутренним кодом ' + b;
    }
}
EXTEND FORM books
    PROPERTIES() findNemo
;
```

Для определения содержит ли одна строка другую используется свойство isSubstring, определенное в системном [модуле](Modules.md) `Utils`.

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1).

Нужно создать действие, которое создаст 100 новых книг с определенными наименованиями.

### Решение

```lsf
add100Books 'Добавить 100 книг' ()  {
    // Вариант 1
    FOR iterate(INTEGER i, 1, 100) NEW b = Book DO {
        name(b) <- 'Книга ' + i;
    }

    // Вариант 2
    FOR iterate(INTEGER i, 1, 100) DO {
        NEW b = Book {
            name(b) <- 'Книга ' + i;
        }
    }
}

EXTEND FORM books
    PROPERTIES() add100Books
;
```

Оба варианта идентичны с точки зрения полученного результата.

Для решения задачи используется свойство `iterate`, определенное в системном модуле `Utils`, которое принимает значение `TRUE` для всех целых чисел в диапазоне.

## Пример 3

### Условие

Аналогично [**Примеру 1**](#пример-1), но добавлена логика заказов. Для каждого заказа заданы строки с указанием книги, цен со скидками.

```lsf
CLASS Order 'Заказ';

CLASS OrderDetail 'Строка заказа';
order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;
book 'Книга' = DATA Book (OrderDetail);
nameBook 'Книга' (OrderDetail d) = name(book(d));

price 'Цена' = DATA NUMERIC[14,2] (OrderDetail);

discount 'Скидка, %' = DATA NUMERIC[8,2] (OrderDetail);
discountPrice 'Цена со скидкой' = DATA NUMERIC[14,2] (OrderDetail);
```

Нужно создать действие, которое проставит скидку всем строкам, у которых цена больше 100.

### Решение

```lsf
makeDiscount 'Сделать скидку' (Order o)  {
    // Вариант 1
    FOR order(OrderDetail d) == o AND price(d) > 100 DO {
        discount(d) <- 10;
        discountPrice(d) <- price(d) * (100.0 - discount(d)) / 100.0;
    }

    // Вариант 2
    discount(OrderDetail d) <- 10 WHERE order(d) == o AND price(d) > 100;
    discountPrice(OrderDetail d) <- price(d) * (100.0 - discount(d)) / 100.0 WHERE order(d) == o AND price(d) > 100;
}
```

Оба варианта идентичны с точки зрения полученного результата.

## Пример 4

### Условие

Аналогично [**Примеру 3**](#пример-3), но для книги добавлена цена по умолчанию.

```lsf
price 'Цена' = DATA NUMERIC[14,2] (Book);
```

Нужно создать действие, которое добавит в заказ все книги с ценой больше 100.

### Решение

```lsf
addSelectedBooks 'Добавить отмеченные книги' (Order o)  {
    // Вариант 1
    FOR price(Book b) > 100 NEW d = OrderDetail DO {
        order(d) <- o;
        book(d) <- b;
        price(d) <- price(b);
    }

    // Вариант 2
    FOR price(Book b) == NUMERIC[14,2] p AND p > 100 NEW d = OrderDetail DO {
        order(d) <- o;
        book(d) <- b;
        price(d) <- p;
    }
}
```

Оба варианта идентичны с точки зрения полученного результата.
