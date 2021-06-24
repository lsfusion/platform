---
title: 'How-to: GROUP CONCAT'
---

## Пример 1

### Условие

Есть набор книг, привязанный к определенным тегам с приоритетами.

```lsf
CLASS Book 'Книга';
CLASS Tag 'Тэг';
name 'Наименование' = DATA ISTRING[10] (Tag);

in 'Вкл' = DATA BOOLEAN (Tag, Book);
```

Необходимо получить список тэгов книги через запятую в алфавитном порядке.

### Решение

```lsf
tags 'Тэги' (Book b) = GROUP CONCAT name(Tag t) IF in(t, b), ',' ORDER name(t), t CHARWIDTH 10;
```

Желательно для всех свойств, которые строятся при помощи `GROUP CONCAT`, задавать размеры, которые будут использоваться для вывода их на форму. По умолчанию система работает по "пессимистичному" сценарию и выдает очень много места таким свойствам.

## Пример 2

### Условие

Есть набор книг, привязанный к определенным категориям и авторам.

```lsf
CLASS Category 'Категория';

CLASS Author 'Автор';
name 'Автор' = DATA ISTRING[20] (Author);

category 'Категория' = DATA Category (Book);
author 'Автор' = DATA Author (Book);
```

Необходимо получить список всех авторов по категории через запятую по убыванию количества книг.

### Решение

```lsf
countBooks 'Кол-во книг' (Author a, Category c) = GROUP SUM 1 BY author(Book b), category(b);

authors 'Авторы' (Category c) = GROUP CONCAT name(Author a) IF countBooks(a, c) ORDER DESC countBooks(a, c), a;
```
