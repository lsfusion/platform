---
title: 'How-to: NEW'
---

## Пример 1

### Условие

Есть заказ, с заданной датой и покупателем.

```lsf
CLASS Order 'Заказ';

CLASS Customer 'Покупатель';
name = DATA ISTRING[50] (Customer);

date 'Дата' = DATA DATE (Order);

customer 'Покупатель' = DATA Customer (Order);
nameCustomer 'Покупатель' (Order o) = name(customer(o));
```

Нужно создать действие, которое создаст новый заказ на основе заданного.

### Решение

```lsf
copyOrder 'Копировать' (Order o)  {
    NEW n = Order {
        date(n) <- date(o);
        customer(n) <- customer(o);
    }
}
```

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1), но для заказа заданы строки заказов.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book);

CLASS OrderDetail 'Строка заказа';
order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;
book 'Книга' = DATA Book (OrderDetail);
nameBook 'Книга' (OrderDetail d) = name(book(d));

price 'Цена' = DATA NUMERIC[14,2] (OrderDetail);
```

Нужно создать действие, которое создаст новый заказ на основе выбранного, с идентичными строками.

### Решение

```lsf
copyDetail (Order o)  {
    NEW n = Order {
        date(n) <- date(o);
        customer(n) <- customer(o);
        FOR order(OrderDetail od) == o NEW nd = OrderDetail DO {
            order(nd) <- n;
            book(nd) <- book(od);
            price(nd) <- price(od);
        }
    }
}
```
