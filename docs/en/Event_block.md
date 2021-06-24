---
title: 'Event block'
---

The event block of the  [`FORM` statement](FORM_statement.md) - a set of constructions controlling [events](Form_events.md) in an interactive form view.

### Syntax

    EVENTS formEventDecl1, ..., formEventDeclN

Where each `formEventDecli` has the following syntax:

    ON eventType eventActionId(param1, ..., paramK) | { eventActionOperator }

### Description

The event block allows to define handlers for [form events](Form_events.md) that occur as the result of certain user actions. Each block can have an arbitrary number of comma-separated event handlers. If several handlers are defined for an event, they are guaranteed to be executed in the order they are defined. 

### Parameters 

- `eventType`

    Type of form event. It is specified with one of the following keywords:

    - `INIT` 
    - `OK`
    - `OK BEFORE`
    - `OK AFTER`
    - `APPLY`
    - `APPLY BEFORE` 
    - `APPLY AFTER` 
    - `CANCEL`
    - `CLOSE`
    - `DROP`
    - `CHANGE objName` – specifies that the action will be executed when the object `objName` is changed.
    - `QUERYOK`
    - `QUERYCANCEL`

- `eventActionId`

    The [ID of the action](IDs.md#propertyid), that will be the event handler.

- `param1, ..., paramK`

    List of action parameters. Each parameter is specified with the object name on the form. The object name, in turn, is specified with a [simple ID](IDs.md#id).

- `actionOperator`

    [Context-dependent action operator](Action_operators.md#contextdependent). You can use the names of already declared objects on the form as parameters.


### Examples

```lsf
showImpossibleMessage()  { MESSAGE 'It\'s impossible'; };

posted = DATA BOOLEAN (Invoice);

FORM invoice 'Invoice' // creating a form for editing an invoice
    OBJECTS i = Invoice PANEL // creating an object of the invoice class

//    ...  setting the rest of the form behavior

    EVENTS
        ON OK { posted(i) <- TRUE; }, // specifying that when the user clicks OK, an action should be executed that will execute actions to "conduction" this invoice
        ON DROP showImpossibleMessage() // by clicking the formDrop button, showing a message that this cannot be, since this button by default will be shown only in the form for choosing an invoice, and this form is basically an invoice edit form
;

CLASS Shift;
currentShift = DATA Shift();

CLASS Cashier;
currentCashier = DATA Cashier();

CLASS Receipt;
shift = DATA Shift (Receipt);
cashier = DATA Cashier (Receipt);

FORM POS 'POS' // declaring the form for product sale to the customer in the salesroom

    OBJECTS r = Receipt PANEL // adding an object that will store the current receipt
//    ... declaring the behavior of the form

;

createReceipt ()  {
    NEW r = Receipt {
        shift(r) <- currentShift();
        cashier(r) <- currentCashier();

        SEEK POS.r = r;
    }
}

EXTEND FORM POS // adding a property through the form extension so that SEEK could be applied to the already created object on the form
    EVENTS
        ON INIT createReceipt() // when opening the form, executing the action to create a new receipt, which fills in the shift, cashier and other information
;
```
