---
title: 'MULTI operator'
---

The `MULTI` operator creates an [action](Actions.md) that implements [branching](Branching_CASE_IF_MULTI.md#poly) (polymorphic form).

### Syntax

    MULTI [exclusionType] action1, ..., actionN 

### Description

The `MULTI` operator creates an action that executes one of the actions passed to it depending on whether the selection conditions are met. The property selection condition is that the parameters match this action [signature](CLASS_operator.md). 

### Parameters

- `exclusionType`

    [Type of mutual exclusion](Branching_CASE_IF_MULTI.md#exclusive). Determines whether several conditions for the action selection can be met simultaneously with a certain set of parameters. It is specified by one of the keywords:

    - `EXCLUSIVE`
    - `OVERRIDE`

  The `EXCLUSIVE` type indicates that the conditions for the action selection cannot be met simultaneously. The `OVERRIDE` type allows several conditions to be met simultaneously, in which case the first action in the list which selection condition is met will be selected. 

    The `EXCLUSIVE` type is used by default.

- `action1, ..., actionN` 

    A list of [context dependent action operators](Action_operators.md#contextdependent) which define the actions from which the selection is made.

### Example

```lsf
CLASS Shape;

CLASS Square : Shape;
CLASS Circle : Shape;

message (Square s)  { MESSAGE 'Square'; }
message (Circle c)  { MESSAGE 'Circle'; }

message (Shape s) = MULTI message[Square](s), message[Circle](s);
```
