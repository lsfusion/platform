---
title: 'Activity (ACTIVE)'
---

The *activity* operator creates a property that determines whether one of the following form elements is active:

-   Property - whether the focus is on the specified [property](Properties.md) on the form.
-   Tab - whether one of the tabs in the specified [tab panel](Form_design.md#containers) is active.
-   Form - determines whether the specified [form](Forms.md) is active for the user.

### Language

To create a property that determines whether a tab is active, use the [`ACTIVE TAB` operator](ACTIVE_TAB_operator.md). Whether a form is active is determined by creating an action using the [`ACTIVE FORM` operator](ACTIVE_FORM_operator.md).

### Examples

```lsf
//Form with two tabs
FORM tabbedForm 'Tabbed form'
    OBJECTS u = CustomUser
    PROPERTIES(u) name

    OBJECTS c = Chat
    PROPERTIES(c) name
;

DESIGN tabbedForm {
    NEW tabPane FIRST {
        type = TABBED;
        NEW contacts {
            caption = 'Contacts';
            MOVE BOX(u);
        }
        NEW recent {
            caption = 'Recent';
            MOVE BOX(c);
        }
    }
}

//If the 'Recent' tab is active
recentActive() = ACTIVE TAB tabbedForm.recent;
```


```lsf
FORM exampleForm;
testActive  {
    ACTIVE FORM exampleForm;
    IF isActiveForm() THEN MESSAGE 'Example form is active';
}
```

  
