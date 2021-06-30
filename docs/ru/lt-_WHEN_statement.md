---
title: 'Инструкция <- WHEN'
---

Инструкция `<- WHEN` - создание [вычисляемого события](Calculated_events.md).

### Синтаксис

    propertyId(param1, ..., paramN) <- valueExpr WHEN eventExpr;

### Описание

Инструкция `<- WHEN` создает вычисляемое событие для [свойства](Data_properties_DATA.md), указанного в левой части инструкции. Этот оператор может объявлять свои локальные параметры при задании свойства, значение которого будет [изменяться](Property_change_CHANGE.md). Затем эти параметры могут быть использованы в выражениях условия и значения, на которое будет изменяться свойство.

Для свойства можно задать только одно вычисляемое событие. 

### Параметры

- `propertyId`

    [Идентификатор свойства](IDs.md#propertyid), значение которого будет изменено при наступлении события.

- `param1, ..., paramN`

    [Типизированные параметры](IDs.md#paramid) свойства, значение которого будет изменено при наступлении события. Количество этих параметров должно совпадать с количеством параметров изменяемого свойства.

- `valueExpr`

    Выражение, на значение которого необходимо изменить значение свойства.

- `eventExpr`

    Выражение, значение которого является условием создаваемого события.

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

