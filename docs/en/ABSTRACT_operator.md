---
title: 'ABSTRACT operator'
---

The `ABSTRACT` operator creates an [abstract property](Property_extension.md).

### Syntax

```
ABSTRACT [type [exclusionType] [order]] [FULL] returnClassName [(argClassName1, ..., argClassNameN)]
```

### Description

The `ABSTRACT` operator creates an abstract property. Its implementations are added later by [`+=` statements](+=_statement.md). Depending on the selected type, the platform builds from them the behavior of a [selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md).

The `ABSTRACT` operator cannot be used inside [expressions](Expression.md).

### Parameters

- `type`

    Option. Possible values:

    - `CASE` - the explicit conditional form of the abstract property. The selection condition of each implementation is defined in the corresponding [`+=` statement](+=_statement.md) using the `WHEN` block.
    - `MULTI` - [a polymorphic form](Property_extension.md#poly) of the abstract property. An implementation is selected when the current arguments are compatible with its [signature](ISCLASS_operator.md).
    - `VALUE` - the polymorphic value-based form. An implementation is considered matching if it returns a defined value, that is, a non-`NULL` value.

    If this option is omitted, `MULTI` is used by default.

- `exclusionType`

    Option. It specifies the [type of mutual exclusion](Property_extension.md#exclusive). Possible values:

    - `EXCLUSIVE` - the mutually exclusive mode for the `CASE`, `MULTI`, and `VALUE` forms. In this mode, for each set of arguments there must be at most one matching implementation.
    - `OVERRIDE` - the mode for the `CASE`, `MULTI`, and `VALUE` forms in which several implementations may match simultaneously.

    Used only with `CASE`, `MULTI`, or `VALUE`. For `MULTI`, `EXCLUSIVE` is used by default; for `CASE` and `VALUE`, `OVERRIDE` is used by default.

- `order`

    Option for `OVERRIDE`. Possible values:

    - `FIRST` - new implementations are added to the beginning of the list, so the last added implementation will be selected. If this value is not specified after `OVERRIDE`, it is used by default.
    - `LAST` - new implementations are added to the end of the list, so the implementation added first will be selected.

    Used only with `OVERRIDE`.

- `FULL`

    Keyword. If specified, the platform automatically checks the [completeness of implementations](Property_extension.md#full): for all descendants of the argument classes there must be at least one applicable implementation, or exactly one if the conditions are mutually exclusive.

- `returnClassName`

    Class of the return value of the property. [Class ID](IDs.md#classid).

- `argClassName1, ..., argClassNameN`

    List of class names of property arguments. Each name is defined by a [class ID](IDs.md#classid). The list may be empty. If the list is omitted, the parameter classes are taken from the property declaration in which the `ABSTRACT` operator is used.

### Examples


```lsf
CLASS Invoice;
CLASS InvoiceDetail;
CLASS Range;
CLASS Connection;

rateChargeExchange(invoice) = ABSTRACT NUMERIC[14,6] (Invoice);

defaultIsMobileMode(Connection c) = ABSTRACT BOOLEAN;
             
backgroundSku 'Color' (d) = ABSTRACT CASE FULL COLOR (InvoiceDetail);
 
overVAT = ABSTRACT VALUE OVERRIDE FIRST Range (InvoiceDetail);          
```
