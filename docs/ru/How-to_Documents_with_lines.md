---
title: 'How-to: Документы со строками'
---

## Пример 1

### Условие

Есть заказы и их спецификация в виде строк.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[100] (Book) IN id;

CLASS Order 'Заказ';
date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA STRING[10] (Order);

CLASS OrderDetail 'Строка заказа';
order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;

book 'Книга' = DATA Book (OrderDetail) NONULL;
nameBook 'Книга' (OrderDetail d) = name(book(d));

quantity 'Количество' = DATA INTEGER (OrderDetail);
price 'Цена' = DATA NUMERIC[14,2] (OrderDetail);
```

Необходимо создать форму со списком заказов с возможностью их добавления, редактирования и удаления.

### Решение

```lsf
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

На форме `order` для объекта строки не добавляется ссылка на заказ, так как при добавлении объекта через `NEW`, ссылка будет автоматически проставлена на основании конструкции `FILTERS`.

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1).

Необходимо добавить на форму со списком заказов их спецификацию.

### Решение

```lsf
EXTEND FORM orders
    OBJECTS d = OrderDetail
    PROPERTIES(d) READONLY nameBook, quantity, price
    FILTERS order(d) == o
;
```

Это бывает удобно, чтобы пользователь мог смотреть состав заказа, не редактируя его.

 
