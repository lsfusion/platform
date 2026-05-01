---
title: 'APPLY operator'
---

The `APPLY` operator creates an [action](Actions.md) that [applies changes](Apply_changes_APPLY.md) to the database.

### Syntax

```
APPLY [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] [SERIALIZABLE] [action]
```

where `nestedPropertySelector` has one of the following forms:

```
LOCAL
(propertyId1, ..., propertyIdN)
```

### Description

The `APPLY` operator creates an action that applies changes to the database. By specifying the keyword `NESTED` you can specify [local properties](Data_properties_DATA.md#local) whose changes are not dropped when applying the changes. This operator also includes an action to be executed before applying the changes to the database.

### Parameters

- `NESTED`

    Optional keyword after which you can specify which local properties preserve their changes after the `APPLY` operator is executed. By itself, with neither `LOCAL` nor a property list, it has no effect on the operator.

- `LOCAL`

    Keyword. If specified after `NESTED`, all local properties preserve their changes after the `APPLY` operator is executed. 

- `propertyId1, ..., propertyIdN`

    Non-empty list of local properties, specified after `NESTED` in parentheses. Each list element is a [property ID](IDs.md#propertyid). The local properties specified in the list will preserve their changes after the operator is executed.

- `CLASSES`

    Keyword. Can be written, but has no effect on the `APPLY` operator.

- `SINGLE`

    Optional keyword. Enables an apply-transaction optimization for cases where the applied action reads stored properties it also modifies: changes to such stored properties are flushed incrementally during the transaction instead of being batched at the end of the apply.

- `SERIALIZABLE`

    A keyword that sets the transaction isolation level to "Serializable".

- `action`

    A [context-dependent operator](Action_operators.md#contextdependent) that describes an action to be executed before applying changes. It is executed in the same transaction as the application of changes, so the platform may run it more than once when retrying the transaction after an update conflict, deadlock, or timeout — see [`APPLY` paradigm](Apply_changes_APPLY.md) for the implications.

### Examples

```lsf
CLASS Sku;
id = DATA INTEGER (Sku);
in = DATA LOCAL BOOLEAN (Sku);
locked = DATA BOOLEAN (Sku);

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

// bare apply — saves accumulated session changes
saveChanges()  { APPLY; }

// SINGLE optimization combined with an applied action
recalcCost()  {
    APPLY SINGLE {
        id(Sku s) <- (GROUP MAX id(Sku ss)) (+) 1;
    }
}

// keep all local properties on apply
sendBatch()  {
    in(Sku s) <- TRUE;
    APPLY NESTED LOCAL;
}

// SERIALIZABLE on a write to a stored property; applied action is a single context-dependent operator (no braces)
unlock (Sku s)  {
    APPLY SERIALIZABLE locked(s) <- NULL;
}
```
