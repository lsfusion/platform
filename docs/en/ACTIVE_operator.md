---
title: 'ACTIVE operator'
---

The `ACTIVE` operator creates a [property](Properties.md) or an [action](Actions.md) that returns information about an [active](Activity_ACTIVE.md) form element: a tab, a property (or action), a form, or the current object of an object group.

### Syntax

```
ACTIVE TAB formName.componentSelector
ACTIVE PROPERTY formPropertyId
ACTIVE FORM formName

ACTIVE formObjectId
```

### Description

The syntax of `ACTIVE` depends on the kind of activity being checked.

#### Tab and property activity

The `ACTIVE TAB` and `ACTIVE PROPERTY` forms create a parameterless `BOOLEAN` property that performs the activity check for a tab or for a property (or action) on a form. The platform recomputes the value automatically:

- `TAB` — the value is recomputed whenever the active tab on the form changes. Typically used to gate other properties' computation (for example, in `SHOWIF`, export conditions, etc.) to avoid doing work for tabs that are not currently visible.
- `PROPERTY` — the value is recomputed whenever the focus changes on the form and pushed to the client.

To run an action at the moment a tab is switched to (as opposed to reading the current state), use the [`EVENTS ON TAB`](Event_block.md) handler.

#### Form activity

The `ACTIVE FORM` form creates an action that performs the activity check for the specified form for the current user and writes the result (`TRUE` or `FALSE`) into the built-in [local](Data_properties_DATA.md#local) `System.isActiveForm[]` property. The result is available for reading via the `isActiveForm()` property within the same action block / session.

#### Current object value

The `ACTIVE formObjectId` form (without a `TAB`/`PROPERTY`/`FORM` qualifier) creates a parameterless property that returns the current value of the specified form object.

### Parameters

- `formName`

    Form name. [Composite ID](IDs.md#cid).

- `componentSelector`

    Design component [selector](DESIGN_statement.md#selector). The component must be a tab in a tab panel (that is, placed inside a container with `tabbed = TRUE`).

- `formPropertyId`

    The global [ID of a property or action on a form](IDs.md#formpropertyid) whose activeness is checked.

- `formObjectId`

    Global [form object ID](IDs.md#groupobjectid) whose value is returned.

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

//The property is computed only when the 'Recent' tab is active
//(any heavy computation inside is skipped while the tab is hidden)
chatNameIfActive (Chat c) = name(c) IF ACTIVE TAB tabbedForm.recent;
```

```lsf
FORM users
    OBJECTS c = CustomUser
    PROPERTIES(c) name, login
;

activeLogin = ACTIVE PROPERTY users.login(c);
EXTEND FORM users
    PROPERTIES() activeLogin
;

//Show the hint only while the login field is focused
loginHint 'Enter login in Latin script' () = 'Enter login in Latin script' IF activeLogin();
EXTEND FORM users
    PROPERTIES() loginHint
;
```

```lsf
FORM exampleForm;
testActive  {
    ACTIVE FORM exampleForm;
    IF isActiveForm() THEN MESSAGE 'Example form is active';
}
```

```lsf
FORM report
    OBJECTS dFrom = DATE PANEL
    PROPERTIES VALUE(dFrom)
;

//Save the current dFrom value from the report form into an external property
savedFromDate = DATA DATE ();
saveFromDate { savedFromDate() <- ACTIVE report.dFrom; }
```
