---
title: 'Инструкция AFTER'
---

Инструкция `AFTER` - вызов [действия](Actions.md) после вызова другого действия. 

### Синтаксис

    AFTER action(param1, ..., paramN) DO aspectAction;

### Описание

Инструкция `AFTER` задает действие (будем называть его *аспектом*), которое будет вызываться после вызова указанного действия.

### Параметры

- `action`

    [Идентификатор действия](IDs.md#propertyid), после которого будет вызываться аспект.

- `param1, ..., paramN`

    Список имен параметров действия. Каждое имя задается [простым идентификатором](IDs.md#id). К этим параметрам можно обращаться при задании аспекта.

- `aspectAction`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий аспект.

### Примеры

```lsf
changePrice(Sku s, DATE d, NUMERIC[10,2] price)  { price(s, d) <- price; }
// Сообщение будет показано после каждого вызова changePrice
AFTER changePrice(Sku s, DATE d, NUMERIC[10,2] price) DO MESSAGE 'Price was changed'; 
```
