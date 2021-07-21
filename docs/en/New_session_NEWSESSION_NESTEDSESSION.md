---
title: 'New session (NEWSESSION, NESTEDSESSION)'
---

The new [session](Change_sessions.md) operator allows you to execute an action in a session different from the current one. 

As with other session management operators, you can explicitly specify [nested local properties](Session_management.md#nested) for the new session operator.

### Nested sessions {#nested}

It is also possible to create a new *nested* session. In this case, all changes that occurred in the current session are copied to the nested session (the same happens when [changes are discarded](Cancel_changes_CANCEL.md) in a nested session). At the same time, when you [apply changes](Apply_changes_APPLY.md) in the nested session, all changes are copied back to the current session (without being saved to the database). 

### Language

To create an action that executes another action in a new session, use the [`NEWSESSION` operator](NEWSESSION_operator.md) (for nested sessions, use the [`NESTEDSESSION` operator](NESTEDSESSION_operator.md)).

### Examples

```lsf
testNewSession ()  {
    NEWSESSION {
        NEW c = Currency {
            name(c) <- 'USD';
            code(c) <- 866;
        }
        APPLY;
    }
    // here a new object of class Currency is already in the database

    LOCAL local = BPSTRING[10] (Currency);
    local(Currency c) <- 'Local';
    NEWSESSION {
        MESSAGE (GROUP SUM 1 IF local(Currency c) == 'Local'); // will return NULL
    }
    NEWSESSION NESTED (local) {
        // will return the number of objects of class Currency
        MESSAGE (GROUP SUM 1 IF local(Currency c) == 'Local'); 
    }

    NEWSESSION {
        NEW s = Sku {
            id(s) <- 1234;
            name(s) <- 'New Sku';
            SHOW sku OBJECTS s = s;
        }
    }

}
```


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
```
