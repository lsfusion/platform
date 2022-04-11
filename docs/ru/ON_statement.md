---
title: 'Инструкция ON'
---

Инструкция `ON` - добавление обработчика [события](Events.md).

### Синтаксис 

    ON eventClause eventAction;

### Описание

Инструкция `ON` добавляет обработчик для заданного события. 

### Параметры

- `eventClause`

    [Блок описания события](Event_description_block.md). Описывает событие, для которого необходимо добавить обработчик.

- `eventAction`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий обработчик события.

### Примеры

```lsf
CLASS Sku;
name = DATA STRING[100] (Sku);

ON {
    LOCAL changedName = BOOLEAN (Sku);
    changedName(Sku s) <- CHANGED(name(s));
    IF (GROUP SUM 1 IF changedName(Sku s)) THEN {
        MESSAGE 'Changed ' + (GROUP SUM 1 IF changedName(Sku s)) + ' skus!!!';
    }
}

CLASS Order;

CLASS Customer;
name = DATA STRING[50] (Customer);

customer = DATA Customer (Order);
discount = DATA NUMERIC[6,2] (Order);

ON LOCAL {
    FOR CHANGED(customer(Order o)) AND name(customer(o)) == 'Best customer' DO
        discount(o) <- 50;
}
```
