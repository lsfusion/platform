---
slug: "/IF_..._THEN_action_operator"
title: 'IF ... THEN operator'
---

The `IF ... THEN` operator creates an [action](../paradigm/Actions.md) that implements [branching](../paradigm/Branching_CASE_IF_MULTI.md#single) with one condition (single form).

### Syntax

```
IF condition 
THEN action
[ELSE alternativeAction]
```

### Description

The `IF ... THEN` operator creates an action that implements branching with one condition. When this action is executed, the condition is checked: if it is met, the action specified after the keyword `THEN` is called. If it is not met, the action specified after the keyword `ELSE` is called (if this block is specified).

An `ELSE` branch attaches to the nearest preceding operator that admits one and does not have one yet. If the action after `THEN` is written without [braces](Braces_operator.md) and is itself an operator whose syntax ends with an optional `ELSE` branch — [`ASK`](ASK_operator.md), [`INPUT`](INPUT_operator.md), [`DIALOG`](DIALOG_operator.md) and [`IMPORT`](IMPORT_operator.md) when written with `DO`, [`FOR`](FOR_operator.md), [`REQUEST`](REQUEST_operator.md), [`CASE`](CASE_action_operator.md) or another `IF ... THEN` — the `ELSE` that follows it becomes the branch of that operator, not of this one. No error or warning is reported, and when the condition is not met nothing is executed. To attach `ELSE` to the `IF ... THEN` operator, enclose the action after `THEN` in braces.

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

// ELSE belongs to ASK (the cancellation branch), not to IF: when confirm() is NULL, nothing is executed
sendOrAsk (Order o)  {
    IF confirm() THEN
        ASK 'Send the order?' DO send(o);
    ELSE
        send(o);
}

// the braces attach ELSE to IF
sendOrAskFixed (Order o)  {
    IF confirm() THEN {
        ASK 'Send the order?' DO send(o);
    } ELSE
        send(o);
}
```
