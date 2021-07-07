---
title: 'Оператор REQUEST'
---

Оператор `REQUEST` - создание [действия](Actions.md), осуществляющего [запрос значения](Value_request_REQUEST.md).

### Синтаксис

    REQUEST requestAction 
    DO doAction [ELSE elseAction]

### Описание

Оператор `REQUEST` создает действие, которое позволяет отделить запрос значения от его обработки.

### Параметры

- `requestAction`

    [Контекстно-зависимый оператор-действие](Action_operators.md), выполняет запрос значения.

- `doAction`

    [Контекстно-зависимый оператор-действие](Action_operators.md), выполняется, если ввод был успешно завершен.

- `elseAction`

    [Контекстно-зависимый оператор-действие](Action_operators.md), выполняется, если ввод был [отменен](Value_input.md#result).

### Примеры

```lsf
requestCustomer (Order o)  {
    LOCAL resultValue = STRING[100] ();
    REQUEST {
        ASK 'Choose from list?' DO
            DIALOG customers OBJECTS c = resultValue() CHANGE;
        ELSE
            INPUT = resultValue() CHANGE;
    } DO
        customer(o) <- resultValue();
}

FORM request
    OBJECTS o = Order
    PROPERTIES(o) customer ON CHANGE requestCustomer(o) // будет работать, к примеру, групповая корректировка
;
```
