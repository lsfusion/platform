---
slug: "/Event_block"
title: 'Event block'
---

The event block of the  [`FORM` statement](FORM_statement.md) - a set of constructions controlling [events](../paradigm/Form_events.md) in an interactive form view.

### Syntax

```
[EVENTS] formEventDecl1, ..., formEventDeclN
```

Where each `formEventDecli` has the following syntax:

```
ON eventType [replaceMode] eventAction
```

Where `eventType` is one of the following forms:

```
INIT
OK [eventPhase]
APPLY [eventPhase]
CANCEL
CLOSE
DROP
CHANGE objName
[CHANGE] OBJECT objName
[CHANGE] groupObjectEvent groupObjectName
[CHANGE] FILTERGROUPS filterGroupName
[CHANGE] FILTERS PROPERTY formPropertyName
[CHANGE] PROPERTY [eventPhase] formPropertyName
QUERYOK
QUERYCLOSE
containerEvent componentSelector
SCHEDULE PERIOD intPeriod [FIXED]
```

Where `eventAction` is one of the following forms:

```
eventActionId(param1, ..., paramK)
{ eventActionOperator }
```

### Description

The event block allows to define handlers for form events that occur as the result of certain user actions. Each block can have an arbitrary number of comma-separated event handlers. If several handlers are defined for an event, they are guaranteed to be executed in the order they are defined. 

The `FILTERGROUPS` and `FILTERS PROPERTY` events occur during interactive filtering: `FILTERGROUPS` — when the user changes the active filter in the specified filter group; `FILTERS PROPERTY` — when the user sets or changes a user filter on the specified form property.

### Parameters 

- `objName`

    Name of an object on the form. [Simple ID](IDs.md#id).

- `groupObjectName`

    Name of an object group on the form. Simple ID.

- `filterGroupName`

    Name of a filter group on the form. Simple ID.

- `formPropertyName`

    [Name of the property on a form](Properties_and_actions_block.md#name).

- `componentSelector`

    Design component [selector](DESIGN_statement.md#selector).

- `groupObjectEvent`

    A group object event type. It is specified by one of the keywords:

    - `FILTER` - occurs when the filter applied to the group object changes, for any reason (a change to data affecting the filter condition, a programmatic change, or a user action).
    - `ORDER` - occurs when the order applied to the group object changes, for any reason.
    - `FILTERS` - occurs when the user interactively changes the group object's filters.
    - `ORDERS` - occurs when the user interactively changes the group object's orders.

- `containerEvent`

    Container event type. It is specified by one of the keywords:

    - `COLLAPSE` - occurs when the user collapses the container.
    - `EXPAND` - occurs when the user expands the container.
    - `TAB` - occurs when the user selects the container as a tab in a tabbed container.

- `eventPhase`

    One of `BEFORE`, `AFTER`. When omitted from `OK` / `APPLY`, the handler runs on the event itself; when omitted from `PROPERTY`, it sets the single `CHANGE` handler for the property (see `replaceMode`).

- `intPeriod`

    The scheduler period in seconds (an integer literal).

- `FIXED`

    Keyword. When specified, the period to the next run is counted from the start of the current action instead of from its end.

- `replaceMode`

    Controls whether the handler replaces previously defined handlers for this event or is added to them. `REPLACE` replaces all handlers previously defined for the event; `NOREPLACE` adds the handler to them. When omitted, the default is `REPLACE` for `QUERYOK` and `QUERYCLOSE` and `NOREPLACE` for all other events. `replaceMode` does not apply to the plain `PROPERTY` event form (without `BEFORE` / `AFTER`), which always replaces its single handler.

- `eventActionId`

    The [ID of the action](IDs.md#propertyid), that will be the event handler.

- `param1, ..., paramK`

    List of action parameters. Each parameter is specified with the object name on the form. The object name, in turn, is specified with a simple ID.

- `eventActionOperator`

    [Context-dependent action operator](Action_operators.md#contextdependent). You can use the names of already declared objects on the form as parameters.


### Examples

```lsf
showImpossibleMessage()  { MESSAGE 'It\'s impossible'; };

posted = DATA BOOLEAN (Invoice);

FORM invoice 'Invoice' // creating a form for editing an invoice
    OBJECTS i = Invoice PANEL // creating an object of the invoice class

//    ...  setting the rest of the form behavior

    EVENTS
        // specifying that when the user clicks OK, an action should be executed 
        // that will execute actions to "conduction" this invoice
        ON OK { posted(i) <- TRUE; },
 
        // by clicking the formDrop button, showing a message that this cannot be, 
        // since this button by default will be shown only in the form for choosing an invoice, 
        // and this form is basically an invoice edit form
        ON DROP showImpossibleMessage() 
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

    //  ... declaring the behavior of the form
;

createReceipt ()  {
    NEW r = Receipt {
        shift(r) <- currentShift();
        cashier(r) <- currentCashier();

        ACTIVATE POS.r = r;
    }
}

// extending the form with an ON INIT handler so that opening it runs createReceipt,
// which creates a new receipt and makes it the current object on the form
EXTEND FORM POS 
    EVENTS
        // when opening the form, executing the action to create a new receipt, 
        // which fills in the shift, cashier and other information
        ON INIT createReceipt()
        //apply every 60 seconds
        ON SCHEDULE PERIOD 60 FIXED apply(); 
;
```
