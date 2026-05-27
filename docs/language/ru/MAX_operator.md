---
slug: "/MAX_operator"
title: 'Оператор MAX'
---

Оператор `MAX` - создание [свойства](../paradigm/Properties.md), реализующего нахождение [максимального](../paradigm/Extremum_MAX_MIN.md) значения.

### Синтаксис 

```
MAX expr1, ..., exprN
```

### Описание

Оператор `MAX` создает свойство, значением которого является максимальное среди значений заданных операндов. Пропуск `NULL`-операндов и определение класса результата соответствуют [экстремуму](../paradigm/Extremum_MAX_MIN.md).

### Параметры

- `expr1, ..., exprN`

    [Выражения](Expression.md), среди значений которых выбирается максимальное. Должен быть задан хотя бы один операнд.

### Примеры

```lsf
date1 = DATA DATE(INTEGER);
date2 = DATA DATE(INTEGER);
maxDate (INTEGER i) = MAX date1(i), date2(i);

balance = DATA INTEGER (Item);
outcome 'Остаток (неотрицательный)' (Item i) = MAX balance(i), 0;
```
