---
title: 'CANCEL operator'
---

The `CANCEL` operator creates an [action](Actions.md) that [cancels changes](Cancel_changes_CANCEL.md) in the current session.

### Syntax

    CANCEL [nestedBlock]

where `nestedBlock` has one of two possible syntaxes:

    NESTED LOCAL
    NESTED (propertyId1, ..., propertyIdN)

### Description

The `CANCEL` operator creates an action that cancels changes in the current session. By specifying the keyword `NESTED` you can specify [local properties](Data_properties_DATA.md#local) whose changes are not dropped when cancelling the changes. 

### Parameters

- `LOCAL`

    Keyword. If specified, all local properties preserve their changes after the `CANCEL` operator is executed. 

- `propertyId1, ..., propertyIdN`

    List of local properties. Each list element is a [property ID](IDs.md#propertyid). The local properties specified in the list will preserve their changes after the operator is executed.

### Examples

```lsf
dropChanges()  {
    CANCEL NESTED (in[Sku]); // cancel all changes except the in property
}
```
