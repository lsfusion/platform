---
title: 'Операторы изменений'
---

Операторы изменений - набор операторов реализующих определение различных типов [изменений значений свойств](Change_operators_SET_CHANGED_etc.md). 

### Синтаксис

    typeChange(propExpr)

### Описание

Операторы изменений создают [свойства](Properties.md), которые определяют, произошли ли для некоторого свойства в текущей сессии те или иные виды изменений.

### Параметры

- `typeChange`

    Тип оператора изменений. Задается одним из ключевых слов:

    - `SET`
    - `CHANGED`
    - `DROPPED`
    - `SETCHANGED`
    - `DROPCHANGED`
    - `SETDROPPED`

- `propExpr`

    [Выражения](Expression.md), значение которого определяет свойство, для которого необходимо определить наличие изменения.

### Примеры

```lsf
quantity = DATA NUMERIC[14,2] (OrderDetail);
price = DATA NUMERIC[14,2] (OrderDetail);
sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));

createdUser = DATA CustomUser (Order);
createdUser (Order o) <- currentUser() WHEN SET(o IS Order);

numerator = DATA Numerator (Order);
number = DATA STRING[28] (Order);
series = DATA BPSTRING[2] (Order);
WHEN SETCHANGED(numerator(Order o)) AND
     NOT CHANGED(number(o)) AND
     NOT CHANGED(series(o))
     DO {
        number(o) <- curStringValue(numerator(o));
        series(o) <- series(numerator(o));
        incrementValueSession(numerator(o));
     }
;
```
