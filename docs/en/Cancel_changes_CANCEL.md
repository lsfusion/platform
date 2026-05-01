---
title: 'Cancel changes (CANCEL)'
---

The *cancel changes* operator completely clears the current [change session](Change_sessions.md).

Clearing the session reverts every change accumulated in it: [data property](Data_properties_DATA.md) values (including [local](Data_properties_DATA.md#local) ones) go back to what they were at the start of the session, [newly created](New_object_NEW.md) objects disappear, and [class changes](Class_change_CHANGECLASS_DELETE.md) of existing objects are undone.

As with other session management operators, you can explicitly specify [nested local properties](Session_management.md#nested) for the cancel operator.

:::warning
This operator works differently if executed during an [apply transaction](Apply_changes_APPLY.md) — for example, inside the applied action or inside a [global synchronous event handler](Events.md#change). In that case, instead of clearing the session, it cancels the apply that is currently running.
:::

### Language

To declare an action that implements cancellation, use the [`CANCEL` operator](CANCEL_operator.md).

### Examples

```lsf
CLASS Sku;
in = DATA LOCAL BOOLEAN (Sku);

dropChanges()  {
    CANCEL NESTED (in[Sku]); // cancel all changes except the `in[Sku]` property
}
```
