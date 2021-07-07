---
title: 'Оператор MAX'
---

Оператор `MAX` - создание [свойства](Properties.md), реализующего нахождение [максимального](Extremum_MAX_MIN.md) значения.

### Синтаксис 

    MAX expr1, ..., exprN

### Описание

Оператор `MAX` создает свойство, которое возвращает максимальное значение среди значений заданных свойств.

### Параметры

- `expr1, ..., exprN`

    Список [выражений](Expression.md), среди значений которых выбирается максимальное.

### Примеры

```lsf
date1 = DATA DATE(INTEGER);
date2 = DATA DATE(INTEGER);
maxDate (INTEGER i) = MAX date1(i), date2(i);

balance = DATA INTEGER (Item);
outcome 'Остаток (неотрицательный)' (Item i) = MAX balance(i), 0;
```
