---
title: 'Apply changes (APPLY)'
---

The *apply changes* operator saves all changes made to the database, and also triggers the handling of all synchronous global [events](Events.md).

For this operator you can also define an *applied* action — it runs before event handling is triggered, but inside the same transaction. Execution within a single transaction increases performance and integrity, but a [cancel](Cancel_changes_CANCEL.md) rolls back the applied action's changes as well. Because the apply transaction may be retried automatically after an update conflict, deadlock, or timeout, the applied action may be executed more than once and should not perform irreversible side effects outside the platform (such as sending emails or making remote calls).

The apply operation itself can be canceled by the [cancel changes](Cancel_changes_CANCEL.md) operator running during the apply transaction — for example, inside the applied action or inside an event handler. The result of the apply is reflected in the `System.canceled[]` property: `TRUE` if the apply was canceled, `NULL` otherwise. After the operation completes (whether successfully or not), all messages issued during the apply — including those produced by the applied action and by event handlers — are written to the special property `System.applyMessage[]`.

The apply transaction is atomic: a database error or a [constraint](Constraints.md) violation also rolls it back, returning the session and the database to their pre-apply state. By default the transaction uses the database's default isolation level; for a particular apply the platform also allows requesting the strictest *serializable* isolation level when stronger guarantees against concurrent applies are needed.

As with other session management operators, you can explicitly specify [nested local properties](Session_management.md#nested) for the apply changes operator, which will preserve their changes after the operator is executed.

:::warning
This operator works differently if executed inside a [nested session](New_session_NEWSESSION_NESTEDSESSION.md#nested): in this case, all changes are copied back to the session in which the nested session is nested (and changes are not saved to the database).
:::

### Language

To declare an action that applies changes, use the [`APPLY` operator](APPLY_operator.md).

### Examples

```lsf
CLASS Sku;
id = DATA INTEGER (Sku);

in = DATA LOCAL BOOLEAN (Sku);
applyIn()  {
    in(Sku s) <- TRUE WHERE id(s) == 123;
    APPLY NESTED (in[Sku]) {};
    IF canceled() THEN
        MESSAGE applyMessage();
    FOR in(Sku s) DO
        MESSAGE id(s); // shows '123'
}

calculateInTransaction()  {
    APPLY {
        id(Sku s) <- (GROUP MAX id(Sku ss)) (+) 1; // putting down a new code inside the transaction
    }
}
```
