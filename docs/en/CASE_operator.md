---
title: 'CASE operator'
---

The `CASE` operator  creates a [property](Properties.md) that [selects](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) a result by condition.

### Syntax 

    CASE [exclusionType]
        WHEN condition1 THEN result1
        ...
        WHEN conditionN THEN resultN
        [ELSE elseResult]

### Description

The `CASE` operator creates a property that implements a conditional selection. Selection conditions are defined using the properties specified in the `WHEN` block. If the selection condition is met, the property value will be the value of the property specified in the corresponding `THEN` block. If none of the conditions are met, the property value will be the property value in the `ELSE` block, if that block is specified (if not, `NULL` is returned).

### Parameters

- `exclusionType`

    [Type of mutual exclusion](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#exclusive). Determines whether several conditions can be met simultaneously for a certain set of parameters. It is specified by one of the keywords:

    - `EXCLUSIVE`
    - `OVERRIDE`

  The `EXCLUSIVE` type indicates that none of the conditions listed can be met simultaneously. The `OVERRIDE` type allows several conditions to be met simultaneously, in this case, the property value will be the value of the value property for the first met condition. 

    The `OVERRIDE` type is used by default.

- `condition1 ... conditionN`

    [Expressions](Expression.md) whose values define the selection condition. 

- `result1 ... resultN`

    Expressions whose values define the selection result.

- `elseResult`

    An expression whose value defines the property value if none of the conditions are met.


### Examples

```lsf
CLASS Color;
id = DATA STRING[100] (Color);

background 'Color' (Color c) = CASE
    WHEN id(c) == 'Black' THEN RGB(0,0,0)
    WHEN id(c) == 'Red' THEN RGB(255,0,0)
    WHEN id(c) == 'Green' THEN RGB(0,255,0)
;

id (TypeExecEnv type) = CASE EXCLUSIVE
    WHEN type == TypeExecEnv.materialize THEN 3
    WHEN type == TypeExecEnv.disablenestloop THEN 2
    WHEN type == TypeExecEnv.none THEN 1
    ELSE 0
;
```
