---
slug: "/ACTIVATE_operator"
title: 'ACTIVATE operator'
---

The `ACTIVATE` operator creates an [action](../paradigm/Actions.md) that [activates](../paradigm/Activation_ACTIVATE.md) one of the form elements: a specified [form](../paradigm/Forms.md), tab, property (or action) on a form, or a set of [objects](../paradigm/Activation_ACTIVATE.md#search) within a form object group.

### Syntax 

```
ACTIVATE FORM formAddress
ACTIVATE TAB formName.componentSelector
ACTIVATE PROPERTY formPropertyId

ACTIVATE [seekDirection] formObjectId = expr
ACTIVATE [seekDirection] formGroupObjectId [OBJECTS formObject1 = expr1, ..., formObjectK = exprK]
```

Where `formAddress` is one of:

```
[formLabel =] formName [WINDOW windowName]
formLabel [WINDOW windowName]
WINDOW windowName
```

### Description

The syntax of `ACTIVATE` depends on the kind of form element being activated.

#### Activating a form, tab, or property

The `ACTIVATE FORM`, `ACTIVATE TAB` and `ACTIVATE PROPERTY` forms create an action that activates a form, a tab, or a property (action) on a form. The action has no parameters and uses no [context](Action_operators.md#contextdependent). The behavior depends on the keyword:

- `FORM` — activates for the user the first form the address names among the forms open in the [`FORMS` windows](WINDOW_statement.md) (sent to the client as a delayed user-interaction request). The address names a form by any combination of the label it was opened with, its name, and the window it was opened into; at least one of the three has to be specified, and a part that is not specified does not narrow the address. If several forms match, the windows are searched in turn, `System.forms` first, and within a window the forms in the order they were opened. If the address names no open form, the action has no effect.
- `TAB` — selects the specified tab in the containing tab panel. The activation happens only if the form that owns the tab is the currently active form at the moment of execution; otherwise, the action has no effect. Empty containers (with no children) cannot be activated as tabs.
- `PROPERTY` — moves the focus to the specified property or action displayed on the currently active form. The specified property must be placed on the form that is executing the action.

#### Activating objects in a group

The `ACTIVATE ... formObjectId = expr` and `ACTIVATE ... formGroupObjectId [OBJECTS ...]` forms create an action that activates objects in a group (see [object search](../paradigm/Activation_ACTIVATE.md#search)). In the first form, the required value of a single object on a form is specified (this object may be a part of an object group); in the second form, a specific object group and the required values for some of its objects are specified (these objects shall be called *seek objects*).

### Parameters

- `formLabel`

    A [string literal](Literals.md#strliteral) — the label the form was given when it was opened by the [`SHOW` operator](SHOW_operator.md). If it is not specified, a form is activated whatever label it carries.

- `formName`

    Form name. [Composite ID](IDs.md#cid). Inside `formAddress`, if it is not specified, a form of any name is activated.

- `windowName`

    Name of the [`FORMS` window](WINDOW_statement.md) the form was opened into. [Composite ID](IDs.md#cid). If it is not specified, the form is looked for in every window. On the mobile web client and in the desktop client, which draw `System.forms` alone, this part is ignored.

- `componentSelector`

    Design component [selector](DESIGN_statement.md#selector). The component must be a tab in a tab panel (that is, placed inside a container with `tabbed = TRUE`).

- `formPropertyId`

    The global [ID of a property or action on a form](IDs.md#formpropertyid) which should receive focus.

- `seekDirection`

    Option. It specifies the [seek direction](../paradigm/Activation_ACTIVATE.md#direction). Possible values:

    - `FIRST` - for additional objects, the **first** matching collection is selected; for seek objects, if the required collection is not found, the **next** closest one is selected.
    - `LAST` - for additional objects, the **last** matching collection is selected; for seek objects, if the required collection is not found, the **previous** closest one is selected.
    - `NULL` - the current values of the objects of the specified object group are reset to `NULL`. For the single-object form and for the group form with an `OBJECTS` block, all objects of the group not listed explicitly in the operator (including *additional* ones) are reset; the explicitly listed objects take the specified values.

    If this option is omitted, the [default objects type](Object_blocks.md) set on the object group is used (`PREV` cannot be written directly in the operator).

- `formObjectId`

    Global [form object ID](IDs.md#groupobjectid) for which the required value is specified.

- `expr`

    An [expression](Expression.md) whose value is the required value of the form object.

- `formGroupObjectId`

    A global [ID for an object group](IDs.md#groupobjectid) for whose objects required values are specified.

- `formObject1 ... formObjectK`

    List of form object names. May contain only a part of the objects of the specified object group. An object name is defined by a [simple ID](IDs.md#id).

- `expr1 ... exprK`

    A list of expressions whose values are the required values of the corresponding objects in the specified group of objects.

### Examples

```lsf
//Form with two tabs
FORM myForm 'My form'
    OBJECTS u = CustomUser
    PROPERTIES(u) name

    OBJECTS c = Chat
    PROPERTIES(c) name
;

DESIGN myForm {
    NEW tabbedPane FIRST {
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

testAction()  {
    ACTIVATE FORM myForm;
    ACTIVATE TAB myForm.recent;
}

WINDOW workspace FORMS;

showOrders()  { SHOW 'main' = orders WINDOW workspace NOWAIT; }
// the same form can be open more than once, so the label says which of them
backToMain()  { ACTIVATE FORM 'main' = orders WINDOW workspace; }

CLASS ReceiptDetail;
barcode = DATA STRING[30] (ReceiptDetail);
quantity = DATA STRING[30] (ReceiptDetail);

FORM POS
    OBJECTS d = ReceiptDetail
    PROPERTIES(d) barcode, quantityGrid = quantity
;

createReceiptDetail 'Add sales line'(STRING[30] barcode)  {
    NEW d = ReceiptDetail {
        barcode(d) <- barcode;
        ACTIVATE PROPERTY POS.quantityGrid;
    }
}
```

```lsf
number = DATA INTEGER (Order);
FORM orders
    OBJECTS o = Order
    PROPERTIES(o) READONLY number, currency, customer
;
newOrder  {
    NEW new = Order {
        number(new) <- (GROUP MAX number(Order o)) (+) 1;
        ACTIVATE orders.o = new;
    }
}
activateFirst  { ACTIVATE FIRST orders.o; }
activateLast  { ACTIVATE LAST orders.o; }

EXTEND FORM orders
    PROPERTIES(o) newOrder, activateFirst, activateLast
;
```
