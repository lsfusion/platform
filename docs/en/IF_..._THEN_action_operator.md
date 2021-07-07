---
title: 'IF ... THEN operator'
---

The `IF ... THEN` operator creates an [action](Actions.md) that implements [branching](Branching_CASE_IF_MULTI.md#single) with one condition (single form).

### Syntax

    IF condition 
    THEN action
    [ELSE alternativeAction]

### Description

The `IF ... THEN` operator creates an action that implements branching with one condition. When this action is executed, the condition is checked and, if the condition is met, the action specified after the keyword `THEN` is called. There is also the option of specifying an alternative action with the keyword `ELSE`, which is called if the condition is not met.

### Parameters

- `condition`

    [Expression](Expression.md) defining a condition. If the value of the expression does not equal `NULL`, then the condition is met and the action specified after the keyword `THEN` is called.

- `action`

    [Context-dependent operator](Action_operators.md#contextdependent) that describes the action that will be executed when the corresponding condition is met.

- `alternativeAction`

    Context-dependent operator that describes an action that will be executed if the condition is not met.

### Examples

```lsf
// Action that compares the value of the count property to 3 and displays a message to the user
moreThan3(obj)  {
    IF count(obj) > 3 THEN
        MESSAGE '>3';
    ELSE
        MESSAGE '<=3';
}

checkNullName (Store st) {
    IF NOT name(st) THEN
        MESSAGE 'Name is null';
}
```
