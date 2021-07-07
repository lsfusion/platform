---
title: 'Оператор MIN'
---

Оператор `MIN` - создание [свойства](Properties.md), реализующего нахождение [минимального](Extremum_MAX_MIN.md) значения.

### Синтаксис 

    MIN expr1, ..., exprN

### Описание

Оператор `MIN` создает свойство, которое возвращает минимальное значение среди значений заданных свойств.

### Параметры

- `expr1, ..., exprN`

    Список [выражений](Expression.md), среди значений которых выбирается минимальное.

### Примеры

```lsf
minPrice(Book b) = MIN price1(b), price2(b);

date (INTEGER i) = DATA DATE (INTEGER);
minDate (INTEGER i) = MIN date(i), 2001_01_01;
```
