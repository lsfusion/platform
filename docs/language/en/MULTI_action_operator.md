---
slug: "/MULTI_action_operator"
title: 'MULTI operator'
---

The `MULTI` operator creates an [action](../paradigm/Actions.md) that implements [branching](../paradigm/Branching_CASE_IF_MULTI.md#poly) (polymorphic form).

### Syntax

```
MULTI [exclusionType] action1, ..., actionN 
```

### Description

The `MULTI` operator creates an action that executes one of the actions passed to it depending on whether the selection conditions are met. The selection condition for each action is that the call parameters match that action's [signature](Property_signature_ISCLASS.md); the action whose condition is met is executed.

### Parameters

- `exclusionType`

    [Type of mutual exclusion](../paradigm/Branching_CASE_IF_MULTI.md#exclusive). Determines whether several action-selection conditions can be met simultaneously for a certain set of parameters:

    - `EXCLUSIVE` - the action-selection conditions cannot be met simultaneously. Used by default.
    - `OVERRIDE` - several conditions can be met simultaneously; in this case the first action in the list whose condition is met is selected.

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
