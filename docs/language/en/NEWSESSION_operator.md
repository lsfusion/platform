---
slug: "/NEWSESSION_operator"
title: 'NEWSESSION operator'
---

The `NEWSESSION` operator creates an [action](../paradigm/Actions.md) that executes the other action in a [new session](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md).

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

The `NEWSESSION` operator creates an action that executes the other action in a new session. `NEWSQL` and `NESTED` are mutually exclusive: when `NEWSQL` is given, the entire `NESTED ... [CLASSES]` clause is ignored and the new session inherits neither local-property values nor class changes from the current one.

### Parameters

- `NEWSQL`

    Optional keyword. Opens the new session on a [separate SQL connection](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md#newsql), independent of the current session's connection. Has no effect when `NEWSESSION` is itself called inside an [apply transaction](../paradigm/Apply_changes_APPLY.md) of the current session — the platform falls back to recursive apply and does not open a separate SQL connection in that case.

- `formId1, ..., formIdM`

    List of [form IDs](IDs.md#cid), specified after `FORMS`, that the new session is fixed to. The session will appear as the change session of those forms; this is used when the action being executed needs to behave as if invoked from those forms.

- `NESTED`

    Optional keyword after which you can specify which [local properties](../paradigm/Data_properties_DATA.md#local) of the current session are considered [nested](../paradigm/Session_management.md#nested) for this operator: when the new session is created, their current values are copied into it, and when the executed action finishes, their values from the new session are copied back into the current session. Of the covered properties, only those that the executed action uses or changes are migrated; if the action opens interactive forms, all the covered properties are. An [`APPLY`](APPLY_operator.md) inside the new session clears the changes of these properties just like those of all other local properties (it is the cleared values that will then be copied back) — to keep the values, mark the properties as nested in the `APPLY` operator as well, or declare them `NESTED` in the [`DATA` operator](DATA_operator.md), which covers both cases. Properties declared `NESTED` in the `DATA` operator are migrated even without being listed here. By itself, with neither `LOCAL` nor a property list, the keyword has no effect.

- `LOCAL`

    Keyword. If specified after `NESTED`, all the local properties of the current session are migrated.

- `propertyId1, ..., propertyIdN`

    Non-empty list of local properties, specified after `NESTED` in parentheses, that are migrated. Each list element must be a [property ID](IDs.md#propertyid).

- `CLASSES`

    Optional keyword. If specified after `NESTED` and the optional nested-property selector, [class changes](../paradigm/Class_change_CHANGECLASS_DELETE.md) of existing objects (and the objects [created](../paradigm/New_object_NEW.md) in the current session) are also migrated into the new session, in addition to whatever local properties the selector covers.

- `SINGLE`

    Optional keyword. If the `NEWSESSION` is itself called inside an [apply transaction](../paradigm/Apply_changes_APPLY.md), this flag is propagated to the inner action: changes to stored properties used by it are flushed incrementally during the transaction instead of being batched at the end of the apply.

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

// compute a value in the new session and read it after the block finishes
currencyCount = DATA LOCAL INTEGER ();
countCurrencies ()  {
    NEWSESSION NESTED (currencyCount) {
        currencyCount() <- GROUP SUM 1 IF Currency c IS Currency;
    }
    MESSAGE currencyCount(); // the value computed in the new session
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
