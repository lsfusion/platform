---
title: 'Ввод примитива (INPUT)'
---

Оператор *ввода примитива* создает [действие](Actions.md), которое запрашивает у клиента [ввод значения](Value_input.md) [встроенного класса](Built-in_classes.md). Пользователь при желании может [отменить ввод](Value_input.md#result), например, нажав на клавиатуре клавишу `Esc`.

Также как и в остальных операторах ввода значения, в этом операторе можно:

-   задавать [начальные значения](Value_input.md) объектов
-   задавать [основное и альтернативное](Value_input.md#result) действия. Первое вызовется, если ввод был успешно завершен, второе - в обратном случае (если ввод был отменен).
-   [осуществлять изменение](Value_input.md) заданного свойства

Этот оператор можно использовать только в обработке [событий изменения](Form_events.md#property) свойства на форме.

### Язык

Синтаксис оператора ввода значений описывается [оператором `INPUT`](INPUT_operator.md).

### Примеры

```lsf
changeCustomer (Order o)  {
    INPUT s = STRING[100] DO {
        customer(o) <- s;
        IF s THEN
            MESSAGE 'Customer changed to ' + s;
        ELSE
            MESSAGE 'Customer dropped';
    }
}

FORM order
    OBJECTS o = Order
    PROPERTIES(o) customer ON CHANGE changeCustomer(o)
;

testFile  {
    INPUT f = FILE DO { // запрашиваем диалог по выбору файла
        open(f); // открываем выбранный файл
    }
}
```
