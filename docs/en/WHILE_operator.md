---
title: 'WHILE operator'
---

The `WHILE` operator creates an [action](Actions.md) that implements a [recursive loop](Recursive_loop_WHILE.md).

### Syntax

    WHILE expression [ORDER [DESC] orderExpr1, ..., orderExprN]
    [NEW [alias =] className]
    DO action

### Description

The `WHILE` operator creates an action implementing a recursive loop. This operator  can add its local [parameters](Actions.md) while defining a condition. These parameters correspond to the objects being iterated and are not parameters of the created action. You can also use the `NEW` block to specify the name of the [class](Classes.md) whose object will be created for each object collection meeting the condition. The name of this object needs to be specified. This name will be used as the name of the local parameter that the created object will be written to.

The object iteration order in the `WHILE` operator can be specified with the `ORDER` block.

### Parameters

- `expression`

    [Expression](Expression.md) defining a condition. In this expression, you can both access already declared parameters and declare new local parameters. 

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprK`

    A list of expressions that define the order in which object collections will be iterated over. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. If the list is undefined, iteration is performed in an arbitrary order.

- `alias`

    The name of the local parameter that will correspond to the object being created. [Simple ID](IDs.md#id).

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
