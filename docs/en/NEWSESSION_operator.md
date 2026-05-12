---
title: 'NEWSESSION operator'
---

The `NEWSESSION` operator creates an [action](Actions.md) that executes the other action in a [new session](New_session_NEWSESSION_NESTEDSESSION.md).

### Syntax

```
NEWSESSION [NEWSQL] [FORMS formId1, ..., formIdM] [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] action
```

where `nestedPropertySelector` has one of the following forms:

```
LOCAL
(propertyId1, ..., propertyIdN)
```

### Description

The `NEWSESSION` operator creates an action which executes the other action in a new session.

If `NESTED LOCAL` or `NESTED (propertyId1, ..., propertyIdN)` is specified, changes of the corresponding [local properties](Data_properties_DATA.md#local) of the current session are made visible in the new session. Also, changes to these local properties in the new session will get to the current session when applying changes in this new session.

### Parameters

- `NEWSQL`

    Keyword. When the operator runs outside an already-running apply transaction, the new session gets a [separate SQL connection](New_session_NEWSESSION_NESTEDSESSION.md#newsql), independent of the current session's connection, and the entire `NESTED ... [CLASSES]` block is ignored — neither local property values nor class changes are migrated into the new session. When the operator is invoked inside the current session's `APPLY` transaction, behavior follows the recursive-apply rules and a separate SQL connection is not opened in that case.

- `formId1, ..., formIdM`

    List of [form IDs](IDs.md#cid), specified after `FORMS`, that the new session is fixed to. The session will appear as the change session of those forms; this is used when the action being executed needs to behave as if invoked from those forms.

- `NESTED`

    Optional keyword after which you can specify which local properties of the current session are migrated into the new session. By itself, with neither `LOCAL` nor a property list, it has no effect.

- `LOCAL`

    Keyword. If specified after `NESTED`, changes to all the local properties will be visible in the new session.

- `propertyId1, ..., propertyIdN`

    Non-empty list of local properties, specified after `NESTED` in parentheses, whose changes will be visible in the new session. Each list element must be a [property ID](IDs.md#propertyid).

- `CLASSES`

    Optional keyword. If specified after `NESTED` and the optional nested-property selector, [class changes](Class_change_CHANGECLASS_DELETE.md) of existing objects (and the objects [created](New_object_NEW.md) in the current session) are also migrated into the new session, in addition to whatever local properties the selector covers.

- `SINGLE`

    Optional keyword. If the `NEWSESSION` is itself called inside an [apply transaction](Apply_changes_APPLY.md), this flag is propagated to the inner action: changes to stored properties used by it are flushed incrementally during the transaction instead of being batched at the end of the apply.

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
    NEWSESSION NESTED (local[Currency]) {
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

// migrate a new object together with the selected local property into the new session
selected = DATA LOCAL BOOLEAN (Sku);
markSelected ()  {
    NEW s = Sku;
    selected(s) <- TRUE;
    NEWSESSION NESTED (selected[Sku]) CLASSES {
        // both the newly created Sku and selected[Sku] are visible here
        MESSAGE (GROUP SUM 1 IF selected(Sku s));
    }
}

// fix the new session to a specific form
showOnOrders ()  {
    NEWSESSION FORMS orders {
        SHOW orders;
    }
}

// run an action in a fresh SQL connection
backgroundJob ()  {
    NEWSESSION NEWSQL {
        APPLY;
    }
}

// SINGLE — only meaningful when NEWSESSION is itself executed inside an apply transaction
recalc ()  {
    APPLY {
        NEWSESSION SINGLE {
            // changes here are flushed incrementally during the outer apply
            id(Sku s) <- (GROUP MAX id(Sku ss)) (+) 1;
        }
    }
}
```
