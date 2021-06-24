---
title: 'How-to: GROUP LAST'
---

## Пример 1

### Условие

Есть набор книг, привязанных к определенной категории, и даты их прихода.

```lsf
CLASS Book 'Книга';
CLASS Category 'Категория';

category 'Категория' = DATA Category (Book);
date 'Дата прихода' = DATA DATE (Book);
```

Необходимо найти последнюю приходившую книгу по выбранной категории.

### Решение

```lsf
book 'Последняя книга' (Category c) = GROUP LAST Book b ORDER date(b), b BY category(b);
```

Важно помнить, что в `ORDER` должен указываться однозначно определяемый порядок. Для этого вторым параметром там добавлена сама книга (точнее ее внутренний идентификатор), так как у нескольких книг может совпадать дата прихода.

## Пример 2 

### Условие

Аналогичен [**Примеру 1**](#пример-1), но для каждой книги задан автор и список жанров.

```lsf
CLASS Author 'Автор';
CLASS Genre 'Жанр';

author 'Автор' = DATA Author (Book);
genre 'Жанр' = DATA Genre (Book);
in 'Вкл' = DATA BOOLEAN (Book, Genre);
```

Нужно найти наиболее часто встречающуюся категорию по автору и жанру.

### Решение

```lsf
countBooks 'Кол-во книг' (Category c, Author a, Genre g) = GROUP SUM 1 IF in(Book b, g) BY category(b), author(b);

category (Author a, Genre g) = GROUP LAST Category c ORDER countBooks(c, a, g), c WHERE countBooks(c, a, g);
```

## Пример 3

### Условие

Задан некоторый набор книг, и есть информация об изменении цен для книги и склада. Каждый объект класса `Ledger` отражает одно изменение цены, начиная с определенной даты.

```lsf
CLASS Stock 'Склад';

CLASS Ledger 'Изменение цены';
date = DATA DATE (Ledger);
stock = DATA Stock (Ledger);
book = DATA Book (Ledger);

price = DATA NUMERIC[10,2] (Ledger);
```

Нужно определить текущую цену, действующую на книгу для склада.

### Решение

```lsf
currentPrice (Book b, Stock s) = GROUP LAST price(Ledger l) ORDER date(l), l BY book(l), stock(l);//#solution3
```

## Пример 4

### Условие

Аналогично [**Примеру 3**](#пример-3).

Нужно определить цену, действующую на определенную дату для книги и склада.

### Решение

```lsf
price (Book b, Stock s, DATE d) = GROUP LAST price(Ledger l) ORDER date(l), l WHERE date(l) <= d BY book(l), stock(l);
```

## Пример 5

### Условие

Аналогично [**Примеру 4**](#пример-4), только для изменения цены есть срок окончания действия. Если он не задан, то считается, что цена действует бесконечно.

```lsf
dateTo 'Действует по (включительно)' = DATA DATE (Ledger);
```

### Решение

```lsf
currentPriceDate (Book b, Stock s) = GROUP LAST price(Ledger l) ORDER date(l), l WHERE NOT dateTo(l) < currentDate() BY book(l), stock(l);
priceDate(Book b, Stock s, DATE d) = GROUP LAST price(Ledger l) ORDER date(l), l WHERE date(l) <= d AND NOT dateTo(l) < d BY book(l), stock(l);
```

Важно учитывать, что выражение `NOT dateTo(l) < date` не эквивалентно `dateTo(l) >= date`. Отличие возникает, когда значение `dateTo(l)` равно `NULL`. В первом случае `dateTo(l) < date` будет равно `NULL`(то есть ложь), а `NOT NULL` равно `TRUE`. Во втором случае выражение будет сразу равно `NULL` (то есть ложь).
