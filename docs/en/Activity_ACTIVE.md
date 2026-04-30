---
title: 'Activity (ACTIVE)'
---

The *activity* operator creates a property or an action that returns information about an active form element. The element can be one of:

-   Property — returns `TRUE` if the focus is on the specified [property](Properties.md) (or [action](Actions.md)) on the form; `NULL` otherwise, including when the form is not open.
-   Tab — returns `TRUE` if the specified tab is active in its [tab panel](Form_design.md#containers); `NULL` otherwise.
-   Form — writes `TRUE` into a local property if the specified [form](Forms.md), or a form that [extends](Form_extension.md) it, is currently active for the user; `FALSE` otherwise.
-   Objects — returns the current value of the specified object in a form's object group (`ACTIVE formObjectId`).

For tabs, properties, and objects, the operator creates a regular property; the platform automatically updates its value whenever the focus changes, a different tab is selected, or the current object changes, so it can be used in expressions (in particular, to gate computations on inactive tabs via `IF` or `SHOWIF`). Activity of a form, on the other hand, depends on the current state of the user session and is therefore exposed via an [action](Actions.md) that writes the result of the check into the [local](Data_properties_DATA.md#local) property `isActiveForm[]`.

### Language

All activity forms are implemented by a single [`ACTIVE` operator](ACTIVE_operator.md): `ACTIVE TAB` (tab activity), `ACTIVE PROPERTY` (property activity), `ACTIVE FORM` (form activity — creates an action), `ACTIVE formObjectId` (active object value in a group).

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
        tabbed = TRUE;
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
FORM users
OBJECTS c = CustomUser
PROPERTIES(c) name, login;

activeLogin = ACTIVE PROPERTY users.login(c);
EXTEND FORM users
PROPERTIES() activeLogin;
```

```lsf
FORM exampleForm;
testActive  {
    ACTIVE FORM exampleForm;
    IF isActiveForm() THEN MESSAGE 'Example form is active';
}
```
