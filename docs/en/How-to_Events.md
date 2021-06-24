---
title: 'How-to: Events'
---

## Example 1

### Task

We have an order with a date, a number and a marker of whether it is closed.

```lsf
CLASS Order 'Order';
isClosed 'Closed' = DATA BOOLEAN (Order);
date 'Date' = DATA DATE (Order);
number 'Number' = DATA INTEGER (Order);
```

We need to make it so that orders are closed automatically at the end of the day.

### Solution

```lsf
// Option 1
WHEN SET(date(Order o) < currentDate()) DO
    isClosed(o) <- TRUE;

// Option 2
WHEN CHANGED(currentDate()) AND date(Order o) < currentDate() DO
    isClosed(o) <- TRUE;
```

In the first case, the event will only be executed in one transaction at the time the expression inside the [`SET` operator](Change_operators_SET_CHANGED_etc.md) operator changes. That is, at the moment when the order date becomes smaller than the current date. However, if the user manually changes the order date to one greater than the current date and saves, the system will automatically execute this event and close the order. Therefore, the second option is preferable, since it will only come into effect when the current date changes at midnight.

## Example 2

### Task

Similar to [**Example 1**](#example-1), but the order contains lines for the quantity, price and total.

```lsf
CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;

quantity 'Qty' = DATA NUMERIC[15,3] (OrderDetail);
price 'Price' = DATA NUMERIC[14,2] (OrderDetail);
sum 'Amount' = DATA NUMERIC[16,2] (OrderDetail);
```

We need to make it so that when the price or quantity changes, their product is automatically recorded as the total amount.

### Solution

```lsf
WHEN LOCAL (CHANGED(quantity(OrderDetail d)) OR CHANGED(price(d)))
            AND NOT CHANGED(sum(d)) DO {
    sum(d) <- NUMERIC[16,2](quantity(d) * price(d));
}
```

Events of type `LOCAL` count all property changes relative not to the state of the database but to the values at the time of the previous occurrence of this event (or rather, the end of its handling). We need to check that `sum` has not changed, to avoid erasing changes made by previous changes. For example, if order lines have been imported from a file, in which the quantity, price and amount are recorded, then this event will no longer take effect.

## Example 3

### Task

Similar to [**Example 2**](#example-2), but a book is specified for the order line. Each book also has a default price.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book);
price 'Price' = DATA NUMERIC[14,2] (Book);

book 'Book' = DATA Book (OrderDetail);
nameBook 'Book' (OrderDetail d) = name(book(d));

FORM order
    OBJECTS o = Order PANEL
    PROPERTIES(o) date, number

    OBJECTS d = OrderDetail
    PROPERTIES(d) nameBook, quantity, price, sum
    FILTERS order(d) == o
;
```

We need to make sure that when the book is changed, the price is automatically appended to the order line. This event should only work on the order edit form.

### Solution

```lsf
WHEN LOCAL FORMS order SETCHANGED(book(OrderDetail d)) DO {
    price(d) <- price(book(d));
}
```

In this case, the event will be triggered only when the book is changed or set. When a book is dropped, the price will not change.

Without the `FORMS` block, this event would be triggered by any change to the order line book. For example, when an order was imported in any other form.

## Example 4

### Task

Similar to [**Example 1**](#example-1).

We need to organize logging of the deletion of orders

### Solution

```lsf
CLASS OrderLog 'Order deletion log';
date 'Order date' = DATA DATE (OrderLog);
number 'Order number' = DATA INTEGER (OrderLog);

dateTime 'Date' = DATA DATETIME (OrderLog);
user 'User' = DATA User (OrderLog);

WHEN DROPPED(Order o IS Order) DO {
    NEW l = OrderLog {
        date(l) <- PREV(date(o));
        number(l) <- PREV(number(o));

        dateTime(l) <- currentDateTime();
        user(l) <- currentUser();
    }
}
```

When deleting an order and triggering an event with the `DROPPED` modifier, it is important to remember that the object no longer exists and all properties that take it as input will give `NULL` values. Therefore, we need to access them not directly, but through the `PREV` operator.
