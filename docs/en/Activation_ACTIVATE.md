---
title: 'Activation (ACTIVATE)'
---

The *activation* operator creates an [action](Actions.md) that activates one of three form elements:

-   Property - sets the focus on the specified [property](Properties.md) on the form.
-   Tab — selects one of the tabs in the specified [tab panel](Form_design.md#containers).
-   Form - activates the specified [form](Forms.md), if open. If one form was opened several times, the one opened first is activated.

### Language

To create an action that activates a form element, use the [`ACTIVATE` operator](ACTIVATE_operator.md).

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

  
