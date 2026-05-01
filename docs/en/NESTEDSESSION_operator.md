---
title: 'NESTEDSESSION operator'
---

The `NESTEDSESSION` operator creates an [action](Actions.md) that executes the other action in a [nested session](New_session_NEWSESSION_NESTEDSESSION.md#nested).

### Syntax

```
NESTEDSESSION [SINGLE] action
```

### Description

The `NESTEDSESSION` operator creates an action which executes the other action in a nested session. With that, all changes that have already been made in the current session get into the created nested session. Also, all changes that are made in the nested session will get into the current session when [the changes are applied](Apply_changes_APPLY.md) in the nested session.

### Parameters

- `SINGLE`

    Optional keyword. If the `NESTEDSESSION` is itself called inside an [apply transaction](Apply_changes_APPLY.md), this flag is propagated to the inner action: changes to stored properties used by it are flushed incrementally during the transaction instead of being batched at the end of the apply.

- `action`

    A [context-dependent action operator](Action_operators.md#contextdependent) that defines an action to be executed in the nested session.

### Examples

```lsf
testNestedSession ()  {
    NESTEDSESSION {
        name(Sku s) <- 'aaa';
        // in fact, the changes will not be applied to the database, but to the "upper" session
        APPLY; 
    }

    MESSAGE (GROUP SUM 1 IF name(Sku s) == 'aaa'); // returns all rows
    CANCEL;
    // returns NULL if there was no Sku named aaa in the database before
    MESSAGE (GROUP SUM 1 IF name(Sku s) == 'aaa'); 

}

FORM sku
    OBJECTS s = Sku PANEL
    PROPERTIES(s) id, name
;
newNestedSession()  {
    NESTEDSESSION {
        NEW s = Sku {
            // shows the form, but any changes in it will not be applied to the database, 
            // but will be saved in the "upper session" session
            SHOW sku OBJECTS s = s;
        }
    }
}

// SINGLE — only meaningful when NESTEDSESSION is itself executed inside an apply transaction
recalcNested ()  {
    APPLY {
        NESTEDSESSION SINGLE {
            // changes here are flushed incrementally during the outer apply
            name(Sku s) <- 'recalculated';
        }
    }
}
```
