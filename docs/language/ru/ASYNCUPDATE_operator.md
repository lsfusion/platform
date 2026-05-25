---
slug: "/ASYNCUPDATE_operator"
title: 'Оператор ASYNCUPDATE'
---

Оператор `ASYNCUPDATE` создает [действие](../paradigm/Actions.md), реализующее [асинхронное обновление](../paradigm/State_change.md#asyncupdate).

### Синтаксис

```
ASYNCUPDATE expr
```

### Описание

Оператор `ASYNCUPDATE` создает действие, которое вычисляет `expr` и отправляет полученное значение в открытый редактор на клиенте.

### Параметры

- `expr`

    [Выражение](Expression.md), значение которого отправляется в открытый редактор.

### Примеры

```lsf
// возвращаем новое значение отображаемого кода в открытый редактор
onChangeSizeCode(Store store)  {
    DIALOG SelectStoreSize OBJECTS ss INPUT DO {
        storeSize(store) <- ss;
    }
    ASYNCUPDATE storeSizeCode(store);
}
```
