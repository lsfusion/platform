---
title: 'How-to: DELETE'
---

## Пример 1

### Условие

Есть заказ, с заданной датой и покупателем и строками заказов, которые ссылаются на книги.

```lsf
CLASS Order 'Заказ';

CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book);

CLASS OrderDetail 'Строка заказа';
order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;
book 'Книга' = DATA Book (OrderDetail);
nameBook 'Книга' (OrderDetail d) = name(book(d));
```

Нужно создать действие, которое удалит книгу, если по ней нету заказов.

### Решение

```lsf
delete (Book b)  {
    IF NOT [ GROUP SUM 1 BY book(OrderDetail d)](b) THEN
        DELETE b;
    ELSE
        MESSAGE 'Запрещено удалять книгу, так как по ней есть заказ';
}
```

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1).

Нужно создать действие, которое очистит заказ, удалив все его строки.

### Решение

```lsf
clear (Order o)  {

    // Вариант 1
    DELETE OrderDetail d WHERE order(d) == o;

    // Вариант 2
    FOR order(OrderDetail d) == o DO
        DELETE d;
}
```

Оба варианта равносильны с точки зрения выполнения.
