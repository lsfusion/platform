---
title: 'Экстремумы (MAX, MIN)'
---

*Оператор экстремума* создает [свойство](Properties.md), которое вычисляет максимум или минимум среди нескольких заданных свойств. При этом, если значение какого либо из этих свойств равно `NULL`, это свойство игнорируется. Если значения всех свойств равны `NULL`, значение результата также равно `NULL`.

### Язык

Свойство, определяющее максимум, создается при помощи [оператора `MAX`](MAX_operator.md),  минимум - при помощи [оператора `MIN`](MIN_operator.md).

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

