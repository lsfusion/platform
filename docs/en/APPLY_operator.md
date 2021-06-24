---
title: 'APPLY operator'
---

The `APPLY` operator creates an [action](Actions.md) that [applies changes](Apply_changes_APPLY.md) to the database.

### Syntax

    APPLY [nestedBlock] [SERIALIZABLE] [action]

where `nestedBlock` has one of two possible syntaxes:

    NESTED LOCAL
    NESTED (propertyId1, ..., propertyIdN)

### Description

The `APPLY` operator creates an action that applies changes to the database. By specifying the keyword `NESTED` you can specify [local properties](Data_properties_DATA.md#local) whose changes are not dropped when applying the changes. This operator also includes an action to be executed before applying the changes to the database.

### Parameters

- `LOCAL`

    Keyword. If specified, all local properties preserve their changes after the `APPLY` operator is executed. 

- `propertyId1, ..., propertyIdN`

    List of local properties. Each list element is a [property ID](IDs.md#propertyid). The local properties specified in the list will preserve their changes after the operator is executed.

- `SERIALIZABLE`

    A keyword that sets the transaction isolation level to "Serializable".

- `action`

    A [context-dependent operator](Action_operators.md#contextdependent) that describes an action to be executed before applying changes. It is executed in the same transaction as the application of changes.

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
