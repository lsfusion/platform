---
title: 'CASE operator'
---

The `CASE` operator creates an [action](Actions.md) that implements [branching](Branching_CASE_IF_MULTI.md).

### Syntax 

    CASE [exclusionType]
        WHEN condition1 THEN action1
        ...
        WHEN conditionN THEN actionN
        [ELSE elseAction]

### Description

The `CASE` operator creates an action that executes one of the actions passed to it depending on whether the selection conditions are met. Selection conditions are defined using the properties specified in the `WHEN` block. If a selection condition is met, the action specified in the corresponding `THEN` block is executed. If none of the conditions is met, the action specified in the `ELSE` block will be executed if this block is specified.

### Parameters

- `exclusionType`

    [Type of mutual exclusion](Branching_CASE_IF_MULTI.md#exclusive). Determines whether several conditions can be met simultaneously for a certain set of parameters. It is specified by one of the keywords:

    - `EXCLUSIVE`
    - `OVERRIDE`

  The `EXCLUSIVE` type indicates that none of the conditions listed can be met simultaneously. The `OVERRIDE` type allows several conditions to be met simultaneously, in this case, the action corresponding to the first met condition is executed

    The `OVERRIDE` type is used by default.

- `condition1 ... conditionN`

    [Expressions](Expression.md) whose values determine the selection conditions. 

- `action1 ... actionN`

    [Context-dependent operators](Action_operators.md#contextdependent) that describe actions that may be called when the corresponding condition is met.

- `elseAction`

    A context-dependent operator that describes an action to be executed if none of the conditions is met. 

### Examples

```lsf
test = DATA INTEGER (INTEGER);
caseActionTest(a)  {
    CASE
        WHEN test(a) > 7 THEN MESSAGE '>7';
        WHEN test(a) > 6 THEN MESSAGE '>6';
        WHEN test(a) > 5 THEN MESSAGE '>5';
}
```
