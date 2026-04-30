---
title: 'Activation (ACTIVATE)'
---

The *activation* operator creates an [action](Actions.md) that makes one of the form elements active: a property (or action), a tab, a form, or a set of objects in a form's object group.

### Activation of a form, tab, or property

Activation of a form, tab, or property changes the focus / selection in the user interface:

-   Property — sets the focus on the specified [property](Properties.md) (or [action](Actions.md)) on the form. Applied to the currently active form, which must contain the specified property.
-   Tab — selects one of the tabs in the specified [tab panel](Form_design.md#containers). Applied only if the form that owns the tab is the active form at the moment of execution.
-   Form — activates the specified [form](Forms.md), if it is opened for the user. If the same form was opened several times, the one opened first is activated. If the form is not open, the action has no effect.

If the form is not open, or the tab does not belong to the active form, the action simply performs no changes. Property activation, in contrast, expects to be called from the context of the form that contains the property: if there is no form in the action's context or the property does not belong to that form, the behavior is undefined.

Activation of these kinds only switches focus / selection in the user interface and does not modify data or the set of opened forms: if the target form is not yet opened for the user, it has to be opened first ([`SHOW`](SHOW_operator.md)).

### Activation of objects in a group {#search}

Object activation (object search) makes a specified collection of objects [current](Form_structure.md#currentObject) in a form's object group. The objects involved in the operation shall be called *seek objects*.

#### Seek direction {#direction}

The seek direction determines the collection of objects that will be selected as current:

-   for the group's *additional* objects (those not listed among the seek objects), a matching collection is picked;
-   for seek objects, if the required collection is not found on the form, the closest collection is picked instead.

The direction can take one of four values:

-   `FIRST` - for additional objects the **first** matching collection according to the specified order is selected; for seek objects, if the required collection is not found, the **next** closest one is selected.
-   `LAST` - for additional objects the **last** matching collection is selected; for seek objects, if the required collection is not found, the **previous** closest one is selected.
-   `PREV` - the collection closest to the previous current one (that is, the state before the seek operation) is kept.
-   `NULL` - the objects of the group are reset to `NULL`.

In the operator, the direction is specified with the `FIRST`, `LAST`, or `NULL` keywords; `PREV` cannot be written directly. If none of these keywords is specified, the direction is taken from the [default objects type](Interactive_view.md#defaultobject) set on the object group (including `PREV`). If the group does not set a type explicitly, it is [chosen heuristically](Interactive_view.md#defaultobject) based on the group's filters — `FIRST` for a narrow filter, and `PREV` otherwise.

#### Setting `NULL` values

The operator also allows resetting the objects of the specified group to `NULL`. The `NULL` option applies both to the group-object form (with or without an `OBJECTS` block) and to the single-object form (`formObjectId = expr`): in the group, all objects are reset except those explicitly listed in the operator — for those objects the specified expressions take effect. The seek direction is not combined with `NULL`.

### Language

To create the action, use the [`ACTIVATE` operator](ACTIVATE_operator.md).

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
