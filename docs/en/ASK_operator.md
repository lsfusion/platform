---
title: 'ASK operator'
---

The `ASK` operator creates an [action](Actions.md) that asks the user a question or requests confirmation in a [dialog form](Show_message_MESSAGE_ASK.md#dialog).

### Syntax

```
ASK expression
[HEADER headerExpression]
[[alias =] YESNO]
[DO actionOperator [ELSE elseOperator]]
```

### Description

The `ASK` operator creates an action that requests confirmation or an answer to a question in a dialog form.

### Parameters

- `expression`

    An [expression](Expression.md) whose value is the question or message text. If the value is `NULL`, the dialog is not shown and the input is considered completed successfully. If `YESNO` is used, this is equivalent to a positive answer.

- `headerExpression`

    An expression whose value is used as the dialog header. If omitted, or if its value is `NULL`, the `lsFusion` header is used.

- `YESNO`

    A keyword whose presence changes the dialog behavior. By default, the dialog contains only buttons for positive and negative answers, and the negative answer is treated as [input cancellation](Value_input.md#result). If `YESNO` is specified, the client also shows a separate cancellation button, while the positive and negative answers become successful input results: the positive answer gives `TRUE`, and the negative answer gives `NULL`.

- `alias`

    The name of the local logical parameter that stores the dialog result and is available only in `actionOperator`. It can only be used together with `YESNO`. The positive answer is written as `TRUE`, the negative answer as `NULL`. [Simple ID](IDs.md#id). If the dialog is canceled, `actionOperator` is not executed.

- `actionOperator`

    A [context-dependent action operator](Action_operators.md#contextdependent) that is executed if the input is completed successfully. Both the parameters of the created action and `alias` (if specified) can be used as parameters.

- `elseOperator`

    A context-dependent action operator that is executed if the input is [cancelled](Value_input.md#result). Only the parameters of the created action can be used as parameters.

### Example

```lsf
testAsk() {
    ASK 'Are you sure you want to continue?' HEADER 'Confirmation' DO {
        MESSAGE 'You continued';
    }

    ASK 'Use old values?' useOld = YESNO DO {
        IF useOld THEN
            MESSAGE 'Using old values';
        ELSE
            MESSAGE 'Using new values';
    }
}
```
