---
slug: "/State_change"
title: 'State change'
---

The *state-change* operators create [actions](Actions.md) that modify the state of the system in which the action runs. The platform provides the following kinds.

-   [Property change (`CHANGE`)](Property_change_CHANGE.md) — writes the value of an expression to a [mutable property](Property_change_CHANGE.md#changeable) for the sets of arguments where a condition holds.
-   [New object (`NEW`)](New_object_NEW.md) — creates objects of a [custom class](User_classes.md), optionally writing each created object to a [data property](Data_properties_DATA.md).
-   [Class change (`DELETE` / `CHANGECLASS`)](Class_change_CHANGECLASS_DELETE.md) — assigns a target class to selected objects, or deletes them from the system.

State change here refers to the state of the system in which the action runs. Interaction with external systems is included in the [user interaction](User_IS_interaction.md) section.

### Asynchronous update {#asyncupdate}

The *asynchronous update* operator creates an action that sends the value of an expression to the editor that the user has currently open on a form, replacing its in-progress edit value. It is used in [change-event](Form_events.md#property) handlers of properties displayed on a form, where the handler computes a new value that the open editor should reflect immediately, before the next form refresh. Outside an active edit the action has no effect.

The operator takes one argument — the expression whose value should be displayed — and uses its value at the moment of execution.

#### Language

To declare an asynchronous-update action, use the [`ASYNCUPDATE` operator](../language/ASYNCUPDATE_operator.md).

#### Examples

```lsf
// pushing the new value of the displayed code back to the open editor
onChangeSizeCode(Store store)  {
    DIALOG SelectStoreSize OBJECTS ss INPUT DO {
        storeSize(store) <- ss;
    }
    ASYNCUPDATE storeSizeCode(store);
}
```
