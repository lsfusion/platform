---
title: 'How-to: Documents with lines'
---

## Example 1

### Task

We have the orders and their specification lines.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[100] (Book) IN id;

CLASS Order 'Order';
date 'Date' = DATA DATE (Order);
number 'Number' = DATA STRING[10] (Order);

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;

book 'Book' = DATA Book (OrderDetail) NONULL;
nameBook 'Book' (OrderDetail d) = name(book(d));

quantity 'Quantity' = DATA INTEGER (OrderDetail);
price 'Price' = DATA NUMERIC[14,2] (OrderDetail);
```

We need to create a form with a list of orders where the user can add, edit or delete them.

### Solution

```lsf
FORM order 'Order'
    OBJECTS o = Order PANEL
    PROPERTIES(o) date, number

    OBJECTS d = OrderDetail
    PROPERTIES(d) nameBook, quantity, price, NEW, DELETE
    FILTERS order(d) == o

    EDIT Order OBJECT o
;


FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) READONLY date, number
    PROPERTIES(o) NEWSESSION NEW, EDIT, DELETE
;

NAVIGATOR {
    NEW orders;
}
```

We do not add a link to the order for the line object on the `order` form, since when adding the object using `NEW`, the system will automatically set up this link based on the `FILTERS` block.

## Example 2

### Task

Similar to [**Example 1**](#example-1).

We need to add order specification to the order list form.

### Solution

```lsf
EXTEND FORM orders
    OBJECTS d = OrderDetail
    PROPERTIES(d) READONLY nameBook, quantity, price
    FILTERS order(d) == o
;
```

It may be convenient that the user can view the order contents without editing it.

  
