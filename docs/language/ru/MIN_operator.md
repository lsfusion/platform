---
slug: "/MIN_operator"
title: 'Оператор MIN'
---

Оператор `MIN` - создание [свойства](../paradigm/Properties.md), реализующего нахождение [минимального](../paradigm/Extremum_MAX_MIN.md) значения.

### Синтаксис 

```
MIN expr1, ..., exprN
```

### Описание

Оператор `MIN` создает свойство, значением которого является минимальное среди значений заданных операндов. Пропуск `NULL`-операндов и определение класса результата соответствуют [экстремуму](../paradigm/Extremum_MAX_MIN.md).

### Параметры

- `expr1, ..., exprN`

    [Выражения](Expression.md), среди значений которых выбирается минимальное. Должен быть задан хотя бы один операнд.

### Примеры

```lsf
price1 = DATA NUMERIC[10,2] (Book);
price2 = DATA NUMERIC[10,2] (Book);
minPrice (Book b) = MIN price1(b), price2(b);

date = DATA DATE (INTEGER);
minDate (INTEGER i) = MIN date(i), 2001_01_01;
```
