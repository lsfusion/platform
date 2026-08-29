---
slug: "/Brief_actions"
title: 'Brief: actions'
---

## State changes (`<-`, NEW, DELETE)

The [`CHANGE` operator](../language/CHANGE_operator.md) writes a value into a changeable property, the [`NEW` operator](../language/NEW_operator.md) adds an [object](../paradigm/New_object_NEW.md) of a concrete class, and the [`DELETE`](../language/DELETE_operator.md) and [`CHANGECLASS`](../language/CHANGECLASS_operator.md) operators delete an object or move it to another class:

```
[CHANGE] propertyId(expr1, ..., exprN) <- valueExpr [WHERE whereExpr]
NEW className WHERE whereExpr [TO propertyId(prm1, ..., prmN)]
NEW [alias =] className [AUTOSET] action
DELETE expr [WHERE whereExpr]
CHANGECLASS expr TO className [WHERE whereExpr]
```

A [change](../paradigm/Property_change_CHANGE.md) is written for all argument sets satisfying the condition at once, as a single set operation. **Analogy**: `UPDATE ... SET ... WHERE`, not an assignment to a variable. On a [class change or a deletion](../paradigm/Class_change_CHANGECLASS_DELETE.md) the platform clears the stored values of the data properties the object is no longer valid in.

```lsf
setDiscount () {
    discount(Customer c) <- 15 WHERE totalOrders(c) > 100;
    NEW o = Order { date(o) <- currentDate(); }
}
```

## Calls and sequencing

An [action](../paradigm/Actions.md) is dual to a property: a property says what the value is, an action says how it changes. It is declared by the [`ACTION` statement](../language/ACTION_statement.md):

```
name [caption] [(param1, ..., paramN)] { actionBody } [options]
```

The body in braces is a [sequence](../paradigm/Sequence.md): nested actions run in the order written; inside the block `LOCAL` properties can be declared that live only while the block runs. An [action call](../paradigm/Call_EXEC.md) is written as the name with arguments, `[EXEC] actionId(expression1, ..., expressionN) [TO toProperty]`, or substituted directly as a value. **Analogy**: a procedure call.

## Loops (FOR, WHILE)

The [`FOR` operator](../language/FOR_operator.md) runs its body once per object set for which the condition is not `NULL`; the [`WHILE` operator](../language/WHILE_operator.md) recomputes the condition at every step, so the changes made by the body are taken into account:

```
FOR expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[TOP topExpr] [OFFSET offsetExpr]
[NEW [alias =] className]
DO action
[ELSE alternativeAction]

WHILE expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[NEW [alias =] className]
DO action
```

A loop is used when the body is genuinely row-by-row — a dialog, a message, an external call. The mechanisms are described in [loop](../paradigm/Loop_FOR.md) and [recursive loop](../paradigm/Recursive_loop_WHILE.md).

```lsf
createDetails (Order o) {
    FOR in(Sku s) NEW d = OrderDetail DO {
        order(d) <- o;
        sku(d) <- s;
    }
}
```

## Branching (CASE, IF)

[Branching](../paradigm/Branching_CASE_IF_MULTI.md) runs the action matching the condition that holds; a condition holds if its value is not `NULL`. In the [`IF ... THEN`](../language/IF_..._THEN_action_operator.md) and [`CASE`](../language/CASE_action_operator.md) operators the condition is written out; in the [`MULTI` operator](../language/MULTI_action_operator.md) it is that the call arguments match an action's signature, that is, dispatch by the argument class:

```
IF condition
THEN action
[ELSE alternativeAction]

CASE [exclusionType]
    WHEN condition1 THEN action1
    ...
    WHEN conditionN THEN actionN
    [ELSE elseAction]

MULTI [exclusionType] action1, ..., actionN
```

```lsf
message (Shape s) { MULTI { message[Square](s); }, { message[Circle](s); } }
```

The deferred variant is an abstract action, [`ABSTRACT`](../language/ABSTRACT_action_operator.md): a base module declares the extension point and other modules add implementations to it ([action extension](../paradigm/Action_extension.md)).

## Flow control

- [`BREAK`](../paradigm/Interruption_BREAK.md) exits the innermost loop, [`CONTINUE`](../paradigm/Next_iteration_CONTINUE.md) moves to its next iteration, [`RETURN [expression]`](../paradigm/Exit_RETURN.md) exits the innermost action call with the given value as its result.
- [`TRY action [CATCH catchAction] [FINALLY finallyAction]`](../language/TRY_operator.md) — `CATCH` swallows the [error](../paradigm/Exception_handling_TRY.md), giving access to it through `messageCaughtException[]` and `lsfStackTraceCaughtException[]`; `FINALLY` runs in any case. **Analogy**: `try` / `catch` / `finally`.
- [`NEWTHREAD action [dispatchClause]`](../language/NEWTHREAD_operator.md) — execution in a [separate thread](../paradigm/New_threads_NEWTHREAD_NEWEXECUTOR.md), at once or on a schedule (`SCHEDULE`: a delay and a period). [`NEWEXECUTOR`](../language/NEWEXECUTOR_operator.md) picks where the thread goes: a server pool, where the body runs in the caller's [change session](../paradigm/Change_sessions.md), or the client connection (`CLIENT`), where it gets a new session of its own in that connection's navigator.

## Form actions

- [`SHOW`](../language/SHOW_operator.md) — [opening a form](../paradigm/In_an_interactive_view_SHOW_DIALOG.md) in the interactive view; the passed objects become the current ones.
- [`DIALOG`](../language/DIALOG_operator.md) — the same opening as a value-input dialog: every object marked `INPUT` or `CHANGE` returns its last current value to the `DO` block.
- [`ACTIVATE`](../language/ACTIVATE_operator.md) — [activation](../paradigm/Activation_ACTIVATE.md) of a form, a tab, a property, or a set of objects in an object group.
- [`EXPAND`](../language/EXPAND_operator.md) / `COLLAPSE` — expanding and collapsing a [form container](../paradigm/Container_visibility_EXPAND_COLLAPSE.md) and the nodes of an [object tree](../paradigm/Object_tree_visibility_EXPAND_COLLAPSE.md).
- [`MESSAGE`](../paradigm/Show_message_MESSAGE_ASK.md) and [`INPUT`](../paradigm/Value_input.md) — a message and a value input without a separate form.

```
ACTIVATE FORM formName
ACTIVATE TAB formName.componentSelector
ACTIVATE PROPERTY formPropertyId

ACTIVATE [seekDirection] formObjectId = expr
ACTIVATE [seekDirection] formGroupObjectId [OBJECTS formObject1 = expr1, ..., formObjectK = exprK]
```
