---
slug: "/WHILE_operator"
title: 'WHILE operator'
---

The `WHILE` operator creates an [action](../paradigm/Actions.md) that implements a [recursive loop](../paradigm/Recursive_loop_WHILE.md).

### Syntax

```
WHILE expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[NEW [alias =] className]
DO action
```

### Description

The `WHILE` operator creates an action that implements a recursive loop. This operator can add its local parameters while defining a condition. These parameters correspond to the objects being iterated and are not parameters of the created action. You can also use a `NEW` block to specify the [class](../paradigm/Classes.md) of the object that will be created for each object collection that meets the condition. The created object is bound to the local parameter named by `alias` (or `added` by default), which can be used in the main action.

The object iteration order in the `WHILE` operator can be specified with the `ORDER` block. If a new parameter is declared in the expressions that define the order (a parameter not previously encountered in the `WHILE` clause or in the upper context), the condition that all these expressions are non-`NULL` is automatically added.

### Parameters

- `expression`

    [Expression](Expression.md) defining a condition. In this expression, you can both access already declared parameters and declare new local parameters. 

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprN`

    A list of expressions that define the order in which object collections will be iterated over. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. If the list is undefined, iteration is performed in an arbitrary order.

- `alias`

    The name of the local parameter that will correspond to the object being created. [Simple ID](IDs.md#id). If omitted, the parameter is named `added`.

- `className`

    The name of the class of the object to create. Defined by a [class ID](IDs.md#classid).

- `action`

    [Context-dependent action operator](Action_operators.md#contextdependent) describing the action being executed.

### Examples

```lsf
iterateDates (DATE dateFrom, DATE dateTo)  {
    LOCAL dateCur = DATE();

    dateCur() <- dateFrom;
    WHILE dateCur() <= dateTo DO {
        MESSAGE 'I have a date ' + dateCur();
        dateCur() <- sum(dateCur(), 1);
    }
}
```
