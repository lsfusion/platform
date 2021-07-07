---
title: 'NEWSESSION operator'
---

The `NEWSESSION` operator creates an [action](Actions.md) that executes the other action in a [new session](New_session_NEWSESSION_NESTEDSESSION.md).

### Syntax

    NEWSESSION [NEWSQL] [nestedBlock] action 

where `nestedBlock` has one of two possible syntaxes:

    NESTED LOCAL
    NESTED (propertyId1, ..., propertyIdN)

### Description

The `NEWSESSION` operator creates an action which executes the other action in a new session.

If the `NESTED` keyword is specified, the changes of the [local properties](Data_properties_DATA.md#local) will be visible in the new session. If the `LOCAL` keyword is specified, changes of all the local properties will be visible, otherwise, a list of the local properties whose changes need to be visible in the new session should be specified. Also, changes to these local properties in the new session will get to the current session when applying changes in this new session.

### Parameters

- `NEWSQL`

    If this keyword is specified, a new SQL connection will be created. In this case, the block containing the `NESTED` keyword will be ignored.

- `LOCAL`

    If this keyword is specified, changes to all the local properties will be visible in the new session.

- `propertyId1, ..., propertyIdN`

    A list of local properties whose changes will be visible in the new session. Each list element must be a [property ID](IDs.md#propertyid).

- `action`

    A [context-dependent action operator](Action_operators.md#contextdependent) that defines an action to be executed in the new session.

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
