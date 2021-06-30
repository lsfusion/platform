---
title: 'Вычисляемые события'
---

*Вычисляемые* события - события, которые при изменении значения свойства (*условия*) на не `NULL`, изменяют значение одного свойства на другое. При этом, в отличии от [простых](Simple_event.md) событий, это изменение происходит, не в момент изменения условия, а вычисляется каждый раз при обращении к изменяемому свойству. Если в сессии уже есть [изменение](Property_change_CHANGE.md) этого свойства, это изменение считается более приоритетным, чем изменение в вычисляемом событии.

Для каждого свойства может быть только одно вычисляемое событие, которое изменяет это свойство.  

### Язык

Для задания вычисляемых событий используется [инструкция `<- WHEN`](lt-_WHEN_statement.md).

### Примеры

```lsf
// при добавлении клиента, по умолчанию, предоставить ему заданную скидку
defaultDiscount = DATA NUMERIC[6,2] ();
discount = DATA NUMERIC[6,2] (Customer);
discount(Customer c) <- defaultDiscount() WHEN SET(c IS Customer);

quantity = DATA NUMERIC[10,2] (OrderDetail);
price = DATA NUMERIC[10,2] (OrderDetail);
sum = DATA NUMERIC[10,2] (OrderDetail);

sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));
```
