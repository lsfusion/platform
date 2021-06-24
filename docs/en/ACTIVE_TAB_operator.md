---
title: 'ACTIVE TAB operator'
---

The `ACTIVE TAB` operator creates a [property](Properties.md) that checks if specified tab is [active](Activity_ACTIVE.md).

### Syntax 

    ACTIVE TAB formName.componentSelector

### Description

The `ACTIVE TAB` operator creates a property that returns `TRUE` if the specified tab is active on a [form](Forms.md). 

### Parameters

- `formName`

    Form name. [Composite ID](IDs.md#cid).

- `componentSelector`  

    Design component [selector](DESIGN_statement.md#selector). The component must be a tab in the tab panel.

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
