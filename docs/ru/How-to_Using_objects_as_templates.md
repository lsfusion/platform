---
title: 'How-to: Создание на основе'
---

## Пример 1

### Условие

Есть книга, для которой заданы наименование и цена. Для книги заданы формы просмотра и редактирования.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book) IN id;
price 'Цена' = DATA NUMERIC[14,2] (Book);

FORM book 'Книга'
    OBJECTS b = Book PANEL
    PROPERTIES(b) name, price

    EDIT Book OBJECT b
;

FORM books 'Книги'
    OBJECTS b = Book
    PROPERTIES(b) READONLY name, price
    PROPERTIES(b) NEWSESSION NEW, EDIT, DELETE
;
```

Нужно сделать кнопку, которая на основе текущей книги создаст новую и откроет форму по ее редактированию.

### Решение

```lsf
copy 'Копировать' (Book book)  {
    NEWSESSION {
        NEW newBook = Book {
            name(newBook) <- name(book);
            price(newBook) <- price(book);
            SHOW book OBJECTS b = newBook DOCKED;
        }
    }
}

EXTEND FORM books
    PROPERTIES(b) copy TOOLBAR
;
```

При нажатии кнопки будет создана новая [сессия изменений](Change_sessions.md), в которой создастся книга и будет работать новая форма. Если пользователь закроет форму без сохранения, то новая книга не будет сохранена в базу данных и будет потеряна. Слово `TOOLBAR` указывает на то, что эту кнопку надо отображать в тулбаре таблицы со списком заказов.

## Пример 2

### Условие

Введены понятия документов заказа и счета, а также формы по их просмотру и редактированию. Для каждого из документов заданы строки.

```lsf
// Заказы
CLASS Order 'Заказ';
date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA INTEGER (Order);

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
// Счета
CLASS Invoice 'Счет';
date 'Дата' = DATA DATE (Invoice);
number 'Номер' = DATA INTEGER (Invoice);

CLASS InvoiceDetail 'Строка счета';
invoice 'Счет' = DATA Invoice (InvoiceDetail) NONULL DELETE;

book 'Книга' = DATA Book (InvoiceDetail) NONULL;
nameBook 'Книга' (InvoiceDetail d) = name(book(d));

quantity 'Количество' = DATA INTEGER (InvoiceDetail);
price 'Цена' = DATA NUMERIC[14,2] (InvoiceDetail);

FORM invoice 'Счет'
    OBJECTS i = Invoice PANEL
    PROPERTIES(i) date, number

    OBJECTS d = InvoiceDetail
    PROPERTIES(d) nameBook, quantity, price, NEW, DELETE
    FILTERS invoice(d) == i
;
```

Нужно реализовать [действие](Actions.md), которое создаст на основе заказа счет и откроет форму по его редактированию.

### Решение

```lsf
createInvoice 'Создать счет' (Order o)  {
    NEWSESSION {
        NEW i = Invoice {
            date(i) <- date(o);
            number(i) <- number(o);

            FOR order(OrderDetail od) == o NEW id = InvoiceDetail DO {
                invoice(id) <- i;

                book(id) <- book(od);

                quantity(id) <- quantity(od);
                price(id) <- price(od);
            }
            SHOW invoice OBJECTS i = i DOCKED;
        }
    }
}

EXTEND FORM orders
    PROPERTIES(o) createInvoice TOOLBAR
;
```

## Пример 3

### Условие

Аналогично [**Примеру 2**](#пример-2).

Нужно реализовать [действие](Actions.md), которое для счета вызовет диалог со списком заказов и добавит строки из выбранного.

### Решение

```lsf
fillOrder 'Заполнить по заказу' (Invoice i)  {
    DIALOG orders OBJECTS o INPUT DO {
        date(i) <- date(o);
        number(i) <- number(o);

        FOR order(OrderDetail od) == o NEW id = InvoiceDetail DO {
            invoice(id) <- i;

            book(id) <- book(od);

            quantity(id) <- quantity(od);
            price(id) <- price(od);
        }
    }
}
EXTEND FORM invoice
    PROPERTIES(i) fillOrder
;
```

Никаких манипуляция с сессиями изменений не требуется, так как кнопка будет вызываться из формы редактирования счета, и изменения будут происходить в ее сессии.
