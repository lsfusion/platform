---
slug: "/ASYNCUPDATE_operator"
title: 'ASYNCUPDATE operator'
---

The `ASYNCUPDATE` operator creates an [action](../paradigm/Actions.md) that implements [asynchronous update](../paradigm/State_change.md#asyncupdate).

### Syntax

```
ASYNCUPDATE expr
```

### Description

The `ASYNCUPDATE` operator creates an action that evaluates `expr` and sends the resulting value to the open editor on the client.

### Parameters

- `expr`

    [Expression](Expression.md) whose value is sent to the open editor.

### Examples

```lsf
// pushing the new value of the displayed code back to the open editor
onChangeSizeCode(Store store)  {
    DIALOG SelectStoreSize OBJECTS ss INPUT DO {
        storeSize(store) <- ss;
    }
    ASYNCUPDATE storeSizeCode(store);
}
```
