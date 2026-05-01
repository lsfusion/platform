---
title: 'CANCEL operator'
---

The `CANCEL` operator creates an [action](Actions.md) that [cancels changes](Cancel_changes_CANCEL.md) in the current session.

### Syntax

```
CANCEL [NESTED [nestedPropertySelector] [CLASSES]]
```

where `nestedPropertySelector` has one of the following forms:

```
LOCAL
(propertyId1, ..., propertyIdN)
```

### Description

The `CANCEL` operator creates an action that cancels changes in the current session. By specifying the keyword `NESTED` you can specify [local properties](Data_properties_DATA.md#local) whose changes are not dropped when cancelling the changes.

### Parameters

- `NESTED`

    Optional keyword after which you can specify which local properties preserve their changes after the `CANCEL` operator is executed. By itself, with neither `LOCAL` nor a property list, it has no effect on the operator.

- `LOCAL`

    Keyword. If specified after `NESTED`, all local properties preserve their changes after the `CANCEL` operator is executed. 

- `propertyId1, ..., propertyIdN`

    Non-empty list of local properties, specified after `NESTED` in parentheses. Each list element is a [property ID](IDs.md#propertyid). The local properties specified in the list will preserve their changes after the operator is executed.

- `CLASSES`

    Keyword. Can be written, but has no effect on the `CANCEL` operator.

### Examples

```lsf
CLASS Sku;
in = DATA LOCAL BOOLEAN (Sku);

// bare cancel — drops every change accumulated in the current session
dropChanges()  { CANCEL; }

// cancel that preserves a specific local property
dropChangesKeepIn()  {
    CANCEL NESTED (in[Sku]); // cancel all changes except the `in[Sku]` property
}

// cancel that preserves all local properties
dropChangesKeepAll()  { CANCEL NESTED LOCAL; }
```
