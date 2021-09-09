---
title: 'How-to: NEWSESSION'
---

## Example 1

### Task

We have an order with a number and a posted/not posted flag.

```lsf
CLASS Order 'Order';

isPosted 'Completed' = DATA BOOLEAN (Order);
number 'Number' = DATA INTEGER (Order);

FORM orders
    OBJECTS o = Order
    PROPERTIES(o) READONLY isPosted, number
;
```

We need to create an action that will post this order in a separate change session and then will add it to the form containing the list of orders.

### Solution

```lsf
post 'Complete' (Order o)  {
    NEWSESSION {
        isPosted(o) <- TRUE;
        APPLY;
    }
}

EXTEND FORM orders
    PROPERTIES(o) post TOOLBAR
;
```

If you do not "wrap" the action that sets the `isPosted` property in the [`NEWSESSION` operator](NEWSESSION_operator.md), then the [`APPLY` operator](APPLY_operator.md) will also write other changes (including those made on the `orders` form) to the database.

## Example 2

### Task

Similar to [**Example 1**](#example-1), except that the dedicated edit form is available for the order.

```lsf
FORM order
    OBJECTS o = Order PANEL
    PROPERTIES(o) isPosted, number

    EDIT Order OBJECT o
;
```

We need to create an action that creates a new order and open the edit form for it. This action must be added to the form containing the list of orders.

### Solution

```lsf
newOrder ()  {
    NEWSESSION {
        NEW o = Order {
            number(o) <- (GROUP MAX number(Order oo)) (+) 1;
            SHOW order OBJECTS o = o;
        }
    }
}

EXTEND FORM orders
    // Option 1
    PROPERTIES() newOrder DRAW o TOOLBAR

    // Option 2
    PROPERTIES(o) NEWSESSION NEW
;
```

If you do not use the `NEWSESSION` operator, then the object for the new order will be created in the [change session](Change_sessions.md) of the `orders` form. And if the user closes the form without saving, then all the changes "will remain" in the change session for the form, and the created order will be displayed in the form containing the list of orders.

## Example 3

### Task

Similar to [**Example 2**](#example-2), except that the order can be marked.

```lsf
selected 'Mark' = DATA LOCAL BOOLEAN (Order);
EXTEND FORM orders
    PROPERTIES(o) selected
;
```

We need to create an action that will delete the marked orders and immediately save the changes to the database (so that the user does not need to click "Save").

### Solution

```lsf
deleteSelectedOrders 'Delete marked orders' ()  {
    NEWSESSION NESTED(selected) {
        DELETE Order o WHERE selected(o);
        ASK 'You are about to delete ' + (GROUP SUM 1 IF DROPPED (Order o)) + ' orders. Continue?' DO {
            APPLY;
        }
    }
}

EXTEND FORM orders
    PROPERTIES() deleteSelectedOrders DRAW o TOOLBAR
;
```

By default, a new session ignores changes made in the "upper" session. To make the selected property available in the new session, use the `NESTED` block of operators. Otherwise, the `selected` property will always be `NULL`. Alternatively, you can use the `NESTED LOCAL` block instead of specifying particular properties. In this case, changes made to all local properties in the upper session will be visible.

## Example 4

### Task

Similar to [**Example 2**](#example-2), except that the payment logic for the order has been added.

```lsf
CLASS Payment 'Payment';

date 'Date' = DATA DATE (Payment);
sum 'Amount' = DATA NUMERIC[14,2] (Payment);

order 'Order' = DATA Order (Payment);
```

We need to create a button on the form for opening a separate edit form for payments in this order.

### Solution

```lsf
FORM orderPayments 'Order payments'
    // Not adding properties so that this object is not visible on the form at all
    OBJECTS o = Order PANEL 

    OBJECTS p = Payment
    PROPERTIES(p) date, sum, NEW, DELETE
    FILTERS order(p) == o
;

editPayments 'Edit payments' (Order o)  {
    NESTEDSESSION {
        SHOW orderPayments OBJECTS o = o;
    }
}

EXTEND FORM order
    PROPERTIES(o) editPayments
;
```

If you use the [`NESTEDSESSION` operator](NESTEDSESSION_operator.md), then all the changes made in the "upper" change session will be available in the nested session. If the user closes the form without clicking OK, then all changes made directly in the form will be lost. If the user clicks OK, then the changes will be written to the "upper" change session rather than to the database. They will be written to the database along with the changes made in the main `orders` form.

It is not allowed to use `NEWSESSION` here simply because the `orderPayments` form will not be able to see the newly created order which has not yet been added to the database (since changes made in the "upper" session are not visible in the nested one), and thus the behavior will be unpredictable.

If we do not use any session management operator at all, then if the user make any changes in the `orderPayments` form and clicks the Close button, the changes will still be "saved", though the user might not expect that.
