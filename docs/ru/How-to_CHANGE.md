---
title: 'How-to: CHANGE'
---

## Пример 1

### Условие

Есть заказ на продажу книг.

```lsf
CLASS Order 'Заказ';

CLASS Customer 'Покупатель';
name = DATA ISTRING[50] (Customer);

date 'Дата' = DATA DATE (Order);

customer 'Покупатель' = DATA Customer (Order);
nameCustomer 'Покупатель' (Order o) = name(customer(o));

discount 'Скидка, %' = DATA NUMERIC[5,2] (Order);

FORM order
    OBJECTS o = Order PANEL
    PROPERTIES(o) date, nameCustomer, discount
;
```

Нужно создать действие, при выполнении которого, заказ будет выставлен на 30 дней вперед, начиная с текущей датой, и применена скидка 5%.

### Решение

```lsf
setDateDiscount 'Применить скидку (поздняя поставка)' (Order o)  {
    date(o) <- sum(currentDate(), 30);
    discount(o) <- 5.0;
}

EXTEND FORM order
    PROPERTIES(o) setDateDiscount
;
```

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1), только добавлена спецификация к заказу. Также для каждой книги указана текущая цена.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book);
price 'Текущая цена' (Book b) = DATA NUMERIC[14,2] (Book);

CLASS OrderDetail 'Строка заказа';
order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;
book 'Книга' = DATA Book (OrderDetail);
nameBook 'Книга' (OrderDetail d) = name(book(d));

price 'Цена' = DATA NUMERIC[14,2] (OrderDetail);

EXTEND FORM order
    OBJECTS d = OrderDetail
    PROPERTIES(d) nameBook, price
;
```

Нужно создать действие, при выполнении которого, для всех строк заказа будет проставлена текущая цена для соответствующих книг.

### Решение

```lsf
fillPrice 'Установить текущие цены' (Order o)  {
    price(OrderDetail d) <- price(book(d)) WHERE order(d) == o;
}

EXTEND FORM order
    PROPERTIES(o) fillPrice
;
```

Важно не забыть указать в действии `WHERE`, так как иначе цена будет установлена для всех строк заказов в базе данных.
