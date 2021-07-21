---
title: 'How-to: PARTITION'
---

## Пример 1

### Условие

Есть заказ вместе со строками.

```lsf
CLASS Order 'Заказ';
CLASS OrderDetail 'Строка заказа';

order 'Заказ' = DATA Order (OrderDetail) NONULL DELETE;
```

Необходимо пронумеровать строки от 1 в данном заказе в порядке их добавления.

### Решение

```lsf
index 'Номер строки' (OrderDetail d) = PARTITION SUM 1 ORDER d BY order(d) CHARWIDTH 4;
```

В данном случае, сортируем по внутреннему идентификатору строки заказа, так как гарантируется, что он возрастает при создании новых строк.

## Пример 2

### Условие

Есть список заказов покупателей с указанной датой.

```lsf
date 'Дата' = DATA DATE (Order);

CLASS Customer 'Покупатель';
customer 'Покупатель' = DATA Customer (Order);
```

Необходимо для заказа найти дату предыдущего заказа этого же покупателя.

### Решение

```lsf
prevOrderDate 'Предыдущий заказ' (Order o) = PARTITION PREV date(o) ORDER date(o), o BY customer(o);
```

Как и в случае с [How-to: `GROUP CONCAT`](How-to_GROUP_CONCAT.md), порядок должен быть полностью детерминирован. Поэтому в `ORDER` последним параметром добавляется сам заказ (де-факто, его внутренний идентификатор).

## Пример 3

### Условие

Есть текущие остатки книг по партиям на складе.

```lsf
CLASS Book 'Книга';

CLASS Batch 'Партия';
book 'Книга' = DATA Book (Batch);
date 'Дата прихода' = DATA DATE (Batch);

CLASS Stock 'Склад';
// Остаток сделан первичным для примера. Обычно это вычисляемое свойство.
currentBalance 'Остаток' = DATA INTEGER (Batch, Stock); 
```

Необходимо для заданного количества одной книги расписать это количество по партиям по принципу FIFO.

### Решение

```lsf
quantity = DATA LOCAL INTEGER (Book);

quantityFIFO 'Кол-во по партии FIFO' (Batch b, Stock s) = PARTITION UNGROUP quantity
                                                                    LIMIT STRICT currentBalance(b, s)
                                                                    ORDER date(b), b
                                                                    BY book(b);
```

Параметр `STRICT` обозначает, что если количество будет больше остатка по всем партиям, то вся разница будет добавлена к последней партии.  
  
