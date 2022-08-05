---
title: 'Операторы-действия'
---

*Оператор-действие* - это синтаксическая конструкция, описывающая [оператор](Action_operators_paradigm.md), создающий [действие](Actions.md).

Операторы-действия можно разделить на *контекстно-зависимые* и *контекстно-независимые*.

### Контекстно-зависимые операторы {#contextdependent}

Контекстно-зависимые операторы-действия могут использовать параметры из внешнего контекста. Они могут быть использованы везде, где необходимо описать действие, например, при создании другого действия либо [события](Events.md). Такими операторами являются:

-   [Оператор `{...}`](Braces_operator.md)
-   [Оператор `ACTIVATE`](ACTIVATE_operator.md)
-   [Оператор `ACTIVE FORM`](ACTIVE_FORM_operator.md)
-   [Оператор `APPLY`](APPLY_operator.md)
-   [Оператор `ASK`](ASK_operator.md)
-   [Оператор `BREAK`](BREAK_operator.md)
-   [Оператор `CANCEL`](CANCEL_operator.md)
-   [Оператор `CASE`](CASE_action_operator.md)
-   [Оператор `CHANGE`](CHANGE_operator.md)
-   [Оператор `CHANGECLASS`](CHANGECLASS_operator.md)
-   [Оператор `DELETE`](DELETE_operator.md)
-   [Оператор `DIALOG`](DIALOG_operator.md)
-   [Оператор `EMAIL`](EMAIL_operator.md)
-   [Оператор `EVAL`](EVAL_operator.md)
-   [Оператор `EXEC`](EXEC_operator.md)
-   [Оператор `EXPORT`](EXPORT_operator.md)
-   [Оператор `FOR`](FOR_operator.md)
-   [Оператор `IF ... THEN`](IF_..._THEN_action_operator.md)
-   [Оператор `IMPORT`](IMPORT_operator.md)
-   [Оператор `INPUT`](INPUT_operator.md)
-   [Оператор `MESSAGE`](MESSAGE_operator.md)
-   [Оператор `MULTI`](MULTI_action_operator.md)
-   [Оператор `NEW`](NEW_operator.md)
-   [Оператор `NESTEDSESSION`](NESTEDSESSION_operator.md)
-   [Оператор `NEWEXECUTOR`](NEWEXECUTOR_operator.md)
-   [Оператор `NEWSESSION`](NEWSESSION_operator.md)
-   [Оператор `NEWTHREAD`](NEWTHREAD_operator.md)
-   [Оператор `PRINT`](PRINT_operator.md)
-   [Оператор `READ`](READ_operator.md)
-   [Оператор `REQUEST`](REQUEST_operator.md)
-   [Оператор `RETURN`](RETURN_operator.md)
-   [Оператор `SEEK`](SEEK_operator.md)
-   [Оператор `SHOW`](SHOW_operator.md)
-   [Оператор `TRY`](TRY_operator.md)
-   [Оператор `WHILE`](WHILE_operator.md)
-   [Оператор `WRITE`](WRITE_operator.md)

### Контекстно-независимые операторы {#contextindependent}

Контекстно-независимые операторы-действия не могут использовать параметры из внешнего контекста. Из-за этого они могут могут быть использованы только в [инструкции `ACTION`](ACTION_statement.md):

-   [Оператор `ABSTRACT`](ABSTRACT_action_operator.md)
-   [Оператор `INTERNAL`](INTERNAL_operator.md)

 
