---
title: 'ABSTRACT operator'
---

The `ABSTRACT` operator creates an [abstract property](Property_extension.md).

### Syntax

    ABSTRACT [type [exclusionType]] [CHECKED] returnClassName(argClassName1, ..., argClassNameN)

Where `exclusionType` is of two types:

    EXCLUSIVE
    OVERRIDE [FIRST | LAST]

### Description

The `ABSTRACT` operator creates an abstract property, the implementations of which can be defined later (for example, in other [modules](Modules.md) dependent on the module containing the `ABSTRACT` property). Implementations are added to the property using the [`+=` statement](+=_statement.md). When calculating an abstract property, its *matching* implementation is selected and calculated. The selection of the matching implementation depends on the *selection conditions* that are defined when adding implementations, and on the `ABSTRACT` operator type.

-   `CASE` - a general case. The selection condition will be explicitly specified in the implementation using the [`WHEN` block](+=_statement.md).
-   `MULTI` – a [polymorphic form](Property_extension.md#poly). The selection condition is that the parameters match the implementation [signature](CLASS_operator.md). This type is the default type and need not to be explicitly specified.
-   `VALUE` - a polymorphic form. The selection condition will be definiteness (a none-`NULL` value) of the implementation value (essentially, the implementation itself).

The [type of mutual exclusion](Property_extension.md#exclusive) of an operator determines whether several conditions for the implementation of an abstract property can simultaneously be met with a certain set of parameters. The `EXCLUSIVE` type indicates that implementation conditions cannot be met simultaneously. The `OVERRIDE` type allows several simultaneously met conditions. In this case, the implementation to be selected is determined by the keywords `FIRST` and `LAST`.

The `ABSTRACT` operator cannot be used inside [expressions](Expression.md).

### Parameters

- `type`

    Type of abstract property. It is specified by one of the keywords:
    
    - `CASE`
    - `MULTI`
    - `VALUE`
    
  The default value is `MULTI`.

- `exclusionType`

    Type of mutual exclusion. One of these keywords: `EXCLUSIVE` or `OVERRIDE`. Unless explicitly specified, in a `MULTI` abstract property the default type of mutual exclusion is `EXCLUSIVE`, and in all other cases the default mutual exclusion type is `OVERRIDE`.
    
    - `FIRST` | `LAST`

    Keywords. Determine which of the matching implementations will be selected. When the word `FIRST` is specified, implementations will be added to the top of the implementations list, so that the last added implementation will be selected. When the word `LAST` is specified, implementations will be added to the end of the implementations list, so that the implementation added first will be selected. If not specified, the default is `FIRST`. 

- `FULL`

    Keyword. If specified, the platform will automatically check that at least one implementation is specified for all child objects of the argument classes (or exactly one if the conditions are mutually exclusive).

- `returnClassName`

    Class of the return value of the property. [Class ID](IDs.md#classid).

- `argClassName1, ..., argClassNameN`

    List of class names of property arguments. Each name is defined by a class ID.

### Examples


```lsf
CLASS Invoice;
CLASS InvoiceDetail;
CLASS Range;

// In this case, ABSTRACT MULTI EXCLUSIVE is created
rateChargeExchange(invoice) = ABSTRACT NUMERIC[14,6] (Invoice);
             
// In this case, ABSTRACT CASE OVERRIDE LAST is created, and if there are
// several suitable implementations, the first of them will be calculated
backgroundSku 'Color' (d) = ABSTRACT CASE FULL COLOR (InvoiceDetail);
 
// The last matching implementation will be calculated here
overVAT = ABSTRACT VALUE OVERRIDE FIRST Range (InvoiceDetail);          
```

