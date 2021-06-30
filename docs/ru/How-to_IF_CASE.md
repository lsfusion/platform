---
title: 'How-to: IF/CASE'
---

## Пример 1

### Условие

Есть список книг, которые привязаны к заданным категориям. Также для каждой книги задана цена.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book);

CLASS Category 'Категория' {
    novel 'Роман',
    thriller 'Триллер',
    fiction 'Фантастика'
}

category 'Категория' = DATA Category (Book);
price 'Цена' = DATA NUMERIC[14,2] (Book);
```

Нужно создать действие, которое установит определенную цену на книгу, если она относится к заданной категории, и фиксированную цену в противном случае. Если категория не выбрана, то должно быть выдано сообщение об ошибке.

### Решение

```lsf
setPriceIf 'Установить цену' (Book b)  {
    IF NOT category(b) THEN
        MESSAGE 'Не выбрана категория для книги';
    ELSE
        IF category(b) == Category.novel THEN
            price(b) <- 50.0;
        ELSE
            price(b) <- 100.0;
}
```

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1).

Нужно создать действие, которое устанавливает одну из трех цен в зависимости от трех категорий. Во всех остальных случаях цена должна стать равной нулю.

### Решение

```lsf
setPriceCase 'Установить цену' (Book b)  {
    CASE
        WHEN category(b) == Category.novel THEN
            price(b) <- 50.0;
        WHEN category(b) == Category.thriller THEN
            price(b) <- 100.0;
        WHEN category(b) == Category.fiction THEN
            price(b) <- 150.0;
    ELSE
        price(b) <- 0.0;
}
```
