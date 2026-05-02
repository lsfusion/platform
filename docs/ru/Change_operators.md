---
title: 'Операторы изменений'
---

Операторы изменений - набор операторов реализующих определение различных типов [изменений значений свойств](Change_operators_SET_CHANGED_etc.md). 

### Синтаксис

```
typeChange(propExpr)
```

### Описание

Операторы изменений создают [свойства](Properties.md), которые определяют, произошли ли для некоторого свойства в текущей сессии те или иные виды изменений. Это [контекстно-зависимые](Property_operators.md) операторы-свойства, которые могут использоваться внутри [выражений](Expression.md). Внутри обработчика [события](Events.md#change) они переключаются в событийный режим и сообщают об изменениях с момента предыдущего срабатывания этого события.

### Параметры

- `typeChange`

    Тип оператора изменений. Задается одним из ключевых слов:

    - `SET` — значение стало не `NULL` (раньше было `NULL`)
    - `DROPPED` — значение стало `NULL` (раньше было не `NULL`)
    - `CHANGED` — значение изменилось в любую сторону
    - `SETCHANGED` — значение изменилось и сейчас не `NULL`
    - `DROPCHANGED` — значение или сброшено, или изменилось между двумя значениями не `NULL`
    - `SETDROPPED` — значение или установлено, или сброшено (пересечена граница `NULL` / не `NULL`)

- `propExpr`

    [Выражение](Expression.md), для которого проверяется наличие изменения.

### Примеры

```lsf
CLASS Order;
CLASS Item;
quantity = DATA NUMERIC[14,2] (Item);
price = DATA NUMERIC[14,2] (Item);
sum = DATA NUMERIC[14,2] (Item);
posted = DATA BOOLEAN (Order);
status = DATA STRING[20] (Order);

// CHANGED — производное свойство пересчитывается при изменении любого из операндов
sum(Item i) <- quantity(i) * price(i) WHEN CHANGED(quantity(i)) OR CHANGED(price(i));

// SET — срабатывает только на NULL → значение (первичное присвоение)
WHEN SET(posted(Order o)) DO
    MESSAGE 'Заказ проведён';

// SETCHANGED — значение изменилось и сейчас не NULL
WHEN SETCHANGED(status(Order o)) DO
    MESSAGE 'Статус теперь: ' + status(o);

// DROPPED — срабатывает только на значение → NULL; PREV читает значение до сброса
WHEN DROPPED(status(Order o)) DO
    MESSAGE 'Статус был: ' + PREV(status(o));

// DROPCHANGED — значение либо сброшено, либо заменено на другое не `NULL`
CONSTRAINT DROPCHANGED(status(Order o))
    MESSAGE 'Статус нельзя сбросить или изменить после присвоения';

// SETDROPPED — значение пересекло границу `NULL` / не `NULL` в любую сторону
WHEN SETDROPPED(status(Order o)) DO
    MESSAGE 'Статус назначен или сброшен';
```
