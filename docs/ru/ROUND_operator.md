---
title: 'Оператор ROUND'
---

Оператор `ROUND` - создание [свойства](Properties.md), реализующего округление.

### Синтаксис 

```
ROUND(num, scale)
```

### Описание

Оператор `ROUND` создает свойство, которое возвращает округлённое до заданной точности число. 

### Параметры

- `num`

    Округляемое [число](Built-in_classes.md).

- `scale`  

    Число знаков после запятой, [целое число](Built-in_classes.md).

### Примеры

```lsf
rounding = DATA NUMERIC[10,3](); //12345.678
rounded1 = ROUND(rounding(), 2); //12345.68
rounded2 = ROUND(rounding(), -2); //12300.00

FORM roundTest
PROPERTIES() rounding, rounded1, rounded2;
```