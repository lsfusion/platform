---
title: 'ACTIVATE operator'
---

The `ACTIVATE` operator creates an [action](Actions.md) that [activates](Activation_ACTIVATE.md) one of the form elements: a specified [form](Forms.md), tab, property (or action) on a form, or a set of [objects](Activation_ACTIVATE.md#search) within a form object group.

### Syntax 

```
ACTIVATE FORM formName
ACTIVATE TAB formName.componentSelector
ACTIVATE PROPERTY formPropertyId

ACTIVATE [FIRST | LAST | NULL] formObjectId = expr
ACTIVATE [FIRST | LAST | NULL] formGroupObjectId [OBJECTS formObject1 = expr1, ..., formObjectK = exprK]
```

### Description

The syntax of `ACTIVATE` depends on the kind of form element being activated.

#### Activating a form, tab, or property

The `ACTIVATE FORM`, `ACTIVATE TAB` and `ACTIVATE PROPERTY` forms create an action that activates a form, a tab, or a property (action) on a form. The action has no parameters and uses no [context](Action_operators.md#contextdependent). The behavior depends on the keyword:

- `FORM` — activates the specified form for the user, if it is already opened (sent to the client as a delayed user-interaction request). If the form was opened multiple times, the one opened first is activated. If the form is not open, the action has no effect.
- `TAB` — selects the specified tab in the containing tab panel. The activation happens only if the form that owns the tab is the currently active form at the moment of execution; otherwise, the action has no effect. Empty containers (with no children) cannot be activated as tabs.
- `PROPERTY` — moves the focus to the specified property or action displayed on the currently active form. The specified property must be placed on the form that is executing the action.

#### Activating objects in a group

The `ACTIVATE ... formObjectId = expr` and `ACTIVATE ... formGroupObjectId [OBJECTS ...]` forms create an action that activates objects in a group (see [object search](Activation_ACTIVATE.md#search)). In the first form, the required value of a single object on a form is specified (this object may be a part of an object group); in the second form, a specific object group and the required values for some of its objects are specified (these objects shall be called *seek objects*).

The [seek direction](Activation_ACTIVATE.md#direction) is specified with one of the keywords `FIRST`, `LAST`, or `NULL` (`PREV` cannot be written directly in the operator). If none of these keywords is specified, the [default objects type](Object_blocks.md) set on the object group is used.

For the single-object form (`formObjectId = expr`) and for the group form with an `OBJECTS` block, the `NULL` keyword resets to `NULL` the objects of the group that are not listed explicitly (including *additional* ones); the explicitly listed objects take the specified values.

### Parameters

- `formName`

    Form name. [Composite ID](IDs.md#cid).

- `componentSelector`

    Design component [selector](DESIGN_statement.md#selector). The component must be a tab in a tab panel (that is, placed inside a container with `tabbed = TRUE`).

- `formPropertyId`

    The global [ID of a property or action on a form](IDs.md#formpropertyid) which should receive focus.

- `FIRST`

    Keyword. If specified, the current set of objects for:

    - additional objects will be the **first** matching collection, selected in accordance with the specified order. 
    - main objects, if the required object collection is not found, will be the **next** closest collection, selected in accordance with the specified order. 

- `LAST`

    Keyword. If specified, the current set of objects for:

    - additional objects will be the **last** matching collection, selected in accordance with the specified order. 
    - main objects, if the required object collection is not found, will be the **previous** closest collection, selected in accordance with the specified order. 

- `NULL`

    Keyword. If specified, the current values of the objects of the specified object group are reset to `NULL` (objects explicitly listed in the operator take the specified expressions instead).

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
