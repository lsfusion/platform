---
title: 'ABSTRACT operator'
---

The `ABSTRACT` operator creates an [abstract action](Action_extension.md).

### Syntax

```
ABSTRACT [type [exclusionType] [order]] [FULL] [(argClassName1, ..., argClassNameN)] [returnClassName [(returnArgClassName1, ..., returnArgClassNameM)]]
```

### Description

The `ABSTRACT` operator creates an abstract action. Its implementations are added later by [`ACTION+` statements](ACTION+_statement.md). Depending on the selected type, the platform builds from them the behavior of a [branch operator](Branching_CASE_IF_MULTI.md) or a [sequence operator](Sequence.md).

The `ABSTRACT` operator is a [context-independent action operator](Action_operators.md#contextindependent), so it can only be used in the [`ACTION` statement](ACTION_statement.md).

### Parameters

- `type`

    Option. Possible values:

    - `CASE` - the explicit conditional form of the abstract action. The selection condition of each implementation is defined in the corresponding [`ACTION+` statement](ACTION+_statement.md) using the `WHEN` block.
    - `MULTI` - [a polymorphic form](Branching_CASE_IF_MULTI.md#poly) of the abstract action. An implementation is selected when the current arguments are compatible with its [signature](ISCLASS_operator.md).
    - `LIST` - the sequential form of the abstract action. In this form all implementations are executed one after another.

    If this option is omitted, `MULTI` is used by default.

- `exclusionType`

    Option. It specifies the [type of mutual exclusion](Branching_CASE_IF_MULTI.md#exclusive). Possible values:

    - `EXCLUSIVE` - the mutually exclusive mode for the `CASE` and `MULTI` forms. In this mode, for each set of arguments there must be at most one matching implementation.
    - `OVERRIDE` - the mode for the `CASE` and `MULTI` forms in which several implementations may match simultaneously.

    Used only with `CASE` or `MULTI`. For `CASE`, `OVERRIDE` is used by default; for `MULTI`, `EXCLUSIVE` is used by default.

- `order`

    Option. Possible values:

    - `FIRST` - for the `CASE` and `MULTI` forms with `OVERRIDE`, new implementations are added to the beginning of the list, so the last added matching implementation is executed. For the `LIST` form, it sets the execution order reverse to the addition order. If this value is not specified after `OVERRIDE`, it is used by default.
    - `LAST` - for the `CASE` and `MULTI` forms with `OVERRIDE`, new implementations are added to the end of the list, so the first added matching implementation is executed. For the `LIST` form, it sets the execution order equal to the addition order and is used by default.

    Used either after `OVERRIDE` for the `CASE` and `MULTI` forms or after `LIST`.

- `FULL`

    Keyword. If specified, the platform automatically checks the [completeness of implementations](Action_extension.md#full): for all descendants of the argument classes there must be at least one applicable implementation, or exactly one if the conditions are mutually exclusive.

- `argClassName1, ..., argClassNameN`

    List of class names of action arguments. Each name is defined by a [class ID](IDs.md#classid). The list may be empty. If the list is omitted, the parameter classes are taken from the [`ACTION` statement](ACTION_statement.md) in which the `ABSTRACT` operator is used.

- `returnClassName`

    Name of the class of the value returned by the action. It is defined by a [class ID](IDs.md#classid). If this parameter is specified, the abstract action is declared as an action with a result.

- `returnArgClassName1, ..., returnArgClassNameM`

    List of class names of the additional parameters on which the returned value depends. It is used when the action returns not a single value, but a set of values over those parameters. Each name is defined by a [class ID](IDs.md#classid). The list may be empty.

### Examples


```lsf
exportXls 'Export to Excel' ABSTRACT CASE OVERRIDE LAST (Order);
exportXls (Order o) + WHEN name(currency(o)) == 'USD' THEN {
    MESSAGE 'Export USD not implemented';
}

CLASS ABSTRACT Task;
run 'Execute' ABSTRACT MULTI EXCLUSIVE FULL (Task);

CLASS Task1 : Task;
name = DATA STRING[100] (Task);
run (Task1 t) + {
    MESSAGE 'Run Task1 ' + name(t);
}

onStarted ABSTRACT LIST ();
onStarted () + {
    MESSAGE 'Preparing data';
}
onStarted () + {
    MESSAGE 'Starting handlers';
}

CLASS Issue;
CLASS Language;
localizedTitle = DATA STRING[100] (Issue, Language);

getLocalizedTitle(Issue issue) ABSTRACT STRING[100] (Language);
getLocalizedTitle (Issue issue) + {
    FOR Language l IS Language DO
        RETURN localizedTitle(issue, l);
}
```
