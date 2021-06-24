---
title: 'ABSTRACT operator'
---

The `ABSTRACT` operator - creating an [abstract action](Action_extension.md). 

### Syntax

    ABSTRACT [type [exclusionType]] [FIRST | LAST] [CHECKED] (argClassName1, ..., argClassNameN) 

### Description

The `ABSTRACT` operator creates an abstract action, the implementation of which can be defined later (for example, in other [modules](Modules.md) dependent on the module containing the `ABSTRACT` action). Implementations are added to the action using the [`ACTION+` statement](ACTION+_statement.md). When executing `MULTI` or `CASE` type abstract actions, their matching implementation is selected and executed. The selection of the matching implementation depends on the selection conditions that are defined when adding implementations, and on the `ABSTRACT` operator type.

- `CASE` - a general case. The selection condition will be explicitly specified in the implementation using the [`WHEN` block](ACTION+_statement.md).
- `MULTI` - [a polymorphic form](Branching_CASE_IF_MULTI.md#poly). The selection condition is that the parameters match the implementation [signature](CLASS_operator.md). This type is the default type and need not be explicitly specified.

The [type of mutual exclusion](Branching_CASE_IF_MULTI.md#exclusive) of an operator determines whether several conditions for the implementation of an abstract action can simultaneously be met with a certain set of parameters. The `EXCLUSIVE` type indicates that implementation conditions cannot be met simultaneously. The `OVERRIDE` type allows several simultaneously fulfilled conditions, while which implementation is ultimately selected is determined by the keywords `FIRST` and `LAST`.

When performing a `LIST` abstract action, all implementations are executed sequentially. The implementation order is determined by the keywords `FIRST` and `LAST`.

The `ABSTRACT` operator cannot be used inside the [`{...}` operator](Braces_operator.md).

### Parameters

- `type`

    Type of abstract action. It is specified by one of these keywords:

    - `CASE`
    - `MULTI`
    - `LIST`

  The default value is `MULTI`.

- `exclusionType`

    Type of mutual exclusion. One of these keywords: `EXCLUSIVE` or `OVERRIDE`. Unless explicitly specified, in a `MULTI` abstract action the default type of mutual exclusion is `EXCLUSIVE`, and in a `CASE` action the default type is `OVERRIDE`. For a `LIST` abstract action the type of mutual exclusion is not specified.

- `FIRST` | `LAST`

    Keywords. When the word `FIRST` is specified, implementations will be added to the top of the implementations list; when `LAST` is specified, implementations will be added to the end of the implementations list. Unless specified, the default is `FIRST` (except `LIST`, where the default is `LAST`)

    For abstract actions such as `CASE` and `MULTI` with the type of mutual exclusion `OVERRIDE`, specifying `FIRST` will mean that of the matching implementations, the last one added will be executed. For actions such as `LIST`, specifying `FIRST` will mean that implementations will be executed in the reverse order of their addition. 

- `CHECKED`

    Keyword. If specified, the platform will automatically check that at least one implementation is defined for all descendants of the argument classes (or exactly one, if the conditions are mutually exclusive).

- `argClassName1, ..., argClassNameN`

    List of class names of property arguments. Each name is defined by a [class ID](IDs.md#classid).

### Examples


```lsf
exportXls 'Export to Excel'  ABSTRACT CASE ( Order);         // In this case, ABSTRACT CASE OVERRIDE LAST is created
exportXls (Order o) + WHEN name(currency(o)) == 'USD' THEN {
    MESSAGE 'Export USD not implemented';
}

CLASS Task;
run 'Execute'  ABSTRACT ( Task);                           // ABSTRACT MULTI EXCLUSIVE

CLASS Task1 : Task;
name = DATA STRING[100] (Task);
run (Task1 t) + {
    MESSAGE 'Run Task1 ' + name(t);
}


CLASS OrderDetail;
price = DATA NUMERIC[14,2] (OrderDetail);

CLASS InvoiceDetail;
price = DATA NUMERIC[14,2] (InvoiceDetail);
fill  ABSTRACT LIST ( OrderDetail, InvoiceDetail);   // ABSTRACT LIST LAST

fill (OrderDetail od, InvoiceDetail id) + {
    price(id) <- price(od);
}
```

