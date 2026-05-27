---
slug: "/Extremum_MAX_MIN"
title: 'Экстремумы (MAX, MIN)'
---

*Оператор экстремума* создает [свойство](Properties.md), которое вычисляет максимум или минимум среди нескольких заданных свойств. Должно быть задано хотя бы одно свойство. При этом, если значение какого-либо из этих свойств равно `NULL`, это свойство игнорируется. Если значения всех свойств равны `NULL`, значение результата также равно `NULL`.

Операнды должны принадлежать совместимым классам — встроенным классам одного семейства или пользовательским классам, связанным наследованием.

### Определение класса результата

Класс результата — общий предок классов операндов: [встроенный](Built-in_classes.md#commonparentclass) или [пользовательский](User_classes.md#commonparentclass).

### Язык

Свойство, определяющее максимум, создается при помощи [оператора `MAX`](../language/MAX_operator.md),  минимум - при помощи [оператора `MIN`](../language/MIN_operator.md).

### Примеры

```lsf
date1 = DATA DATE(INTEGER);
date2 = DATA DATE(INTEGER);
maxDate (INTEGER i) = MAX date1(i), date2(i);

balance = DATA INTEGER (Item);
outcome 'Остаток (неотрицательный)' (Item i) = MAX balance(i), 0;
```


```lsf
minPrice(Book b) = MIN price1(b), price2(b);

date (INTEGER i) = DATA DATE (INTEGER);
minDate (INTEGER i) = MIN date(i), 2001_01_01;
```

