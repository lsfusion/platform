---
title: 'Оператор IF'
---

Оператор `IF` - создание [свойства](Properties.md), реализующего [выбор](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) значения по условию (одиночная форма). 

### Синтаксис

    result IF condition 

### Описание

Оператор `IF` создает свойство, которое возвращает заданное значение при выполнении некоторого условия. Если условие не выполняется, свойство возвращает `NULL`.

### Параметры

- `result`

    [Выражение](Expression.md), значение которого определяет результат.

- `condition`

    Выражение, значение которого определяет условие.

### Примеры

```lsf
name = DATA STRING[100] (Book);
hasName (Book b) = TRUE IF name(b);

background (Book b) = RGB(224, 255, 128) IF b IS Book;

countTags (Book b) = GROUP SUM 1 IF in(b, Tag t);
```
