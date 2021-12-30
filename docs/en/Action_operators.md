---
title: 'Action operators'
---

*Action operator* is a syntax construct that describes an [operator](Action_operators_paradigm.md) creating an [action](Actions.md).

Action operators can be divided into *context dependent* and *context independent*.

### Context dependent operators {#contextdependent}

Context dependent action operators can use external context parameters. They can be used wherever you need to define an action, for example, when creating another action or [event](Events.md). These operators are:

-   [`{...}` operator](Braces_operator.md)
-   [`ACTIVATE` operator](ACTIVATE_operator.md)
-   [`ACTIVE FORM` operator](ACTIVE_FORM_operator.md)
-   [`APPLY` operator](APPLY_operator.md)
-   [`ASK` operator](ASK_operator.md)
-   [`BREAK` operator](BREAK_operator.md)
-   [`CANCEL` operator](CANCEL_operator.md)
-   [`CASE` operator](CASE_action_operator.md)
-   [`CHANGE` operator](CHANGE_operator.md)
-   [`CHANGECLASS` operator](CHANGECLASS_operator.md)
-   [`DELETE` operator](DELETE_operator.md)
-   [`DIALOG` operator](DIALOG_operator.md)
-   [`EMAIL` operator](EMAIL_operator.md)
-   [`EVAL` operator](EVAL_operator.md)
-   [`EXEC` operator](EXEC_operator.md)
-   [`EXPORT` operator](EXPORT_operator.md)
-   [`FOR` operator](FOR_operator.md)
-   [`IF ... THEN` operator](IF_..._THEN_action_operator.md)
-   [`IMPORT` operator](IMPORT_operator.md)
-   [`INPUT` operator](INPUT_operator.md)
-   [`MESSAGE` operator](MESSAGE_operator.md)
-   [`MULTI` operator](MULTI_action_operator.md)
-   [`NEW` operator](NEW_operator.md)
-   [`NESTEDSESSION` operator](NESTEDSESSION_operator.md)
-   [`NEWEXECUTOR` operator](NEWEXECUTOR_operator.md)
-   [`NEWSESSION` operator](NEWSESSION_operator.md)
-   [`NEWTHREAD` operator](NEWTHREAD_operator.md)
-   [`PRINT` operator](PRINT_operator.md)
-   [`READ` operator](READ_operator.md)
-   [`REQUEST` operator](REQUEST_operator.md)
-   [`RETURN` operator](RETURN_operator.md)
-   [`SEEK` operator](SEEK_operator.md)
-   [`SHOW` operator](SHOW_operator.md)
-   [`TRY` operator](TRY_operator.md)
-   [`WHILE` operator](WHILE_operator.md)
-   [`WRITE` operator](WRITE_operator.md)

### Context independent operators {#contextindependent}

Context independent action operators cannot use external context parameters. Thus they can only be used in the [`ACTION` statement](ACTION_statement.md):

-   [`ABSTRACT` operator](ABSTRACT_action_operator.md)
-   [`INTERNAL` operator](INTERNAL_operator.md)
