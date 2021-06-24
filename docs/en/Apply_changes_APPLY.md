---
title: 'Apply changes (APPLY)'
---

The *apply changes* operator saves all changes made to the database, and also triggers the handling of all synchronous global [events](Events.md).

For this operator you can also define an *applied* action to be executed before events handling is triggered, but it will be done in the same transaction. Execution within a single transaction increases its performance and integrity. It should be kept in mind, however, that when canceling changes, for example, all changes made in this applied action will also be canceled. During event handling the apply operation may be canceled if the [cancel changes](Cancel_changes_CANCEL.md) operator is executed. If this occurs, in the `System.canceled` property a value of `TRUE` is written (if not, then `NULL`).

After completion of operation (whether successful or unsuccessful), all messages shown to the user during event handling are written to the special property `System.applyMessage`.

As with other session management operators, you can explicitly specify [nested local properties](Session_management.md#nested) for the apply changes operator, which will preserve their changes after the operator is executed.

:::caution
This operator works differently if executed inside a [nested session](New_session_NEWSESSION_NESTEDSESSION.md#nested): here all changes are copied back to the session in which this session is nested (and changes are not saved to the database)
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
