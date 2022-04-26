---
title: 'Scheduler Block' ---
---

Scheduler blocks [instructions `FORM`](FORM_statement.md) - a set of constructions controlling the scheduler in an interactive form view.

### Syntax

    SCHEDULE PERIOD intPeriod FIXED? actionOperator

### Description.

The scheduler block allows you to specify a periodic action for a form. There can be an unlimited number of such blocks for a form.

### Parameters

- `intPeriod`.

  The periodicity of the action in seconds.

- `FIXED`.

  An option that means that the period to the next action is counted from the start of the current action. By default, the period is counted from the end of the current action.

- `actionOperator`.

  [Context-sensitive action operator](Action_operators.md).


### Examples

```lsf
someProperty = DATA STRING();
FORM autoApplyForm
PROPERTIES() someProperty
SCHEDULE PERIOD 60 FIXED apply();
;
```