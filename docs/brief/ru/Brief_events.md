---
slug: "/Brief_events"
title: 'Brief: события'
---

## События данных (WHEN)

[Событие](../paradigm/Events.md) выполняет заданное действие — свою *обработку* — при изменении данных. [Блок описания события](../language/Event_description_block.md) задает, глобальное событие или локальное — для всей базы или в пределах [сессии изменений](../paradigm/Change_sessions.md) — ключевыми словами `GLOBAL` и `LOCAL`, ограничивает событие заданными формами через `FORMS` и упорядочивает его обработку относительно других через `AFTER`.

Событие на изменение данных создают три инструкции:

- [`WHEN`](../language/WHEN_statement.md) — [простое событие](../paradigm/Simple_event.md): обработка выполняется на каждый набор объектов, на котором условие не `NULL`;
- [`<- WHEN`](../language/lt-_WHEN_statement.md) — [вычисляемое событие](../paradigm/Calculated_events.md): вместо обработки задается изменение первичного свойства;
- [`ON`](../language/ON_statement.md) — событие общего вида: обработка выполняется один раз на все изменения.

```
WHEN eventClause eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;
propertyId(param1, ..., paramN) <- valueExpr WHEN eventExpr;
ON eventClause eventAction;
```

**Аналогия**: триггер базы данных, но условием служит произвольное свойство над всей базой.

```lsf
sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));
```

## События формы (ON)

[События формы](../paradigm/Form_events.md) возникают на открытой форме: в точке её жизни (`INIT`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`), на действие пользователя либо по таймеру через `SCHEDULE`. Обработка подключается опцией `ON` — в блоках [событий](../language/Event_block.md), [свойств и действий](../language/Properties_and_actions_block.md) и [объектов](../language/Object_blocks.md#objects) инструкции `FORM` либо в [опциях свойства](../language/Property_options.md).

| Что событие обслуживает | События |
| ----------------------- | ------- |
| форму целиком | `INIT`, `QUERYCLOSE`, `QUERYOK`, `OK`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`, `SCHEDULE` |
| объект формы | `CHANGE` |
| группу объектов | `FILTER`, `ORDER`, `SELECT`, `FILTERS`, `ORDERS` |
| группу фильтров | `FILTERGROUPS` |
| свойство или действие | `CHANGE`, `CHANGEWYS`, `GROUPCHANGE`, `EDIT`, `CONTEXTMENU`, `KEYPRESS`, `FILTERS PROPERTY`, `SELECT PROPERTY` |
| контейнер | `EXPAND`, `COLLAPSE`, `TAB` |

Постфиксы `BEFORE` и `AFTER` дают моменты до и после операции. Для событий изменения свойства есть стандартные обработки `READONLY`, `READONLYIF` и `SELECTOR`.

```lsf
FORM sku 'Товар'
    OBJECTS s = Sku
    PROPERTIES(s) price ON CHANGE changePrice(s)
    EVENTS ON INIT initSku()
;
```

## Порядок выполнения

Обработки локальных событий выполняются не в момент изменения данных, а в определенные моменты жизни сессии — см. [выполнение локальных событий](../paradigm/Events.md#local). Обработки синхронных глобальных событий выполняются внутри транзакции [применения изменений](../paradigm/Apply_changes_APPLY.md), вместе с проверками [ограничений](../paradigm/Constraints.md).

Порядок между обработками, реагирующими на одно и то же изменение, определяют зависимости по данным; явно он задается ключевым словом `AFTER` (синоним `GOAFTER`) в [блоке описания события](../language/Event_description_block.md).
