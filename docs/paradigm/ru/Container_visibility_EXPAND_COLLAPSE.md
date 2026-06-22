---
slug: "/Container_visibility_EXPAND_COLLAPSE"
title: 'Видимость контейнеров (EXPAND, COLLAPSE)'
---

Операторы *разворачивания* и *сворачивания* контейнеров управляют тем, показывается ли содержимое *сворачиваемого* [контейнера](Form_design.md#containers) на [форме](Forms.md). Сворачиваемый контейнер можно свернуть, скрыв его содержимое, и развернуть, снова показав его; это состояние является частью [интерактивного](Interactive_view.md) представления формы для пользователя.

В качестве входных данных этим операторам передаётся один контейнер формы. Контейнер должен быть сворачиваемым. Полученное [действие](Actions.md) разворачивает или сворачивает этот контейнер для пользователя, работающего с формой, и ожидает наличия формы в контексте.

### Язык

Для объявления действий, разворачивающих или сворачивающих контейнер, используются операторы [`EXPAND`](../language/EXPAND_operator.md) и [`COLLAPSE`](../language/COLLAPSE_operator.md).

### Примеры

```lsf
CLASS Store;
name = DATA ISTRING[100] (Store);

FORM dashboard
    OBJECTS s = Store
    PROPERTIES(s) name
;

DESIGN dashboard {
    NEW detailsBox {
        collapsible = TRUE;
        caption = 'Детали';
        MOVE BOX(s);
    }
}

expandDetails {
    EXPAND CONTAINER dashboard.detailsBox;
}

collapseDetails {
    COLLAPSE CONTAINER dashboard.detailsBox;
}

EXTEND FORM dashboard
    PROPERTIES() expandDetails, collapseDetails
;
```
