---
slug: "/Brief_events"
title: 'Brief: events'
---

## Data events (WHEN)

An [event](../paradigm/Events.md) runs a given action — its *handler* — on a data change. The [event description block](../language/Event_description_block.md) says whether the event is global or local — for the whole database, or within a [change session](../paradigm/Change_sessions.md) — with `GLOBAL` and `LOCAL`, restricts it to given forms with `FORMS`, and orders its handler against others with `AFTER`.

Three statements create an event on a data change:

- [`WHEN`](../language/WHEN_statement.md) — a [simple event](../paradigm/Simple_event.md): the handler runs per object set on which the condition is not `NULL`;
- [`<- WHEN`](../language/lt-_WHEN_statement.md) — a [calculated event](../paradigm/Calculated_events.md): instead of a handler it gives a change of a data property;
- [`ON`](../language/ON_statement.md) — the general event: the handler runs once over all the changes.

```
WHEN eventClause eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;
propertyId(param1, ..., paramN) <- valueExpr WHEN eventExpr;
ON eventClause eventAction;
```

**Analogy**: a database trigger, except that the condition is an arbitrary property over the whole database.

```lsf
sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));
```

## Form events (ON)

[Form events](../paradigm/Form_events.md) occur on an open form: at a point in its life (`INIT`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`), on what the user does, or on a timer with `SCHEDULE`. A handler is attached with the `ON` option — in the [event](../language/Event_block.md), [properties and actions](../language/Properties_and_actions_block.md) and [object](../language/Object_blocks.md#objects) blocks of the `FORM` statement, or in the [property options](../language/Property_options.md).

| What the event serves | Events |
| --------------------- | ------ |
| the form as a whole | `INIT`, `QUERYCLOSE`, `QUERYOK`, `OK`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`, `SCHEDULE` |
| a form object | `CHANGE` |
| an object group | `FILTER`, `ORDER`, `SELECT`, `FILTERS`, `ORDERS` |
| a filter group | `FILTERGROUPS` |
| a property or an action | `CHANGE`, `CHANGEWYS`, `GROUPCHANGE`, `EDIT`, `CONTEXTMENU`, `KEYPRESS`, `FILTERS PROPERTY`, `SELECT PROPERTY` |
| a container | `EXPAND`, `COLLAPSE`, `TAB` |

The `BEFORE` and `AFTER` postfixes give the moments before and after the operation. For the property change events there are the predefined handlers `READONLY`, `READONLYIF` and `SELECTOR`.

```lsf
FORM sku 'Item'
    OBJECTS s = Sku
    PROPERTIES(s) price ON CHANGE changePrice(s)
    EVENTS ON INIT initSku()
;
```

## Order of execution

The handlers of local events run not at the moment the data changes but at certain moments in the life of the session — see [executing local events](../paradigm/Events.md#local). The handlers of synchronous global events run inside the transaction of [applying changes](../paradigm/Apply_changes_APPLY.md), together with the checks of [constraints](../paradigm/Constraints.md).

The order between the handlers reacting to the same change is determined by the data dependencies; it is set explicitly with the `AFTER` keyword (the synonym `GOAFTER`) in the [event description block](../language/Event_description_block.md).
