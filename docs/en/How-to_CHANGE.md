---
title: 'How-to: CHANGE'
---

## Example 1

### Task

We have a sales order for the books.

```lsf
CLASS Order 'Order';

CLASS Customer 'Customer';
name = DATA ISTRING[50] (Customer);

date 'Date' = DATA DATE (Order);

customer 'Customer' = DATA Customer (Order);
nameCustomer 'Customer' (Order o) = name(customer(o));

discount 'Discount, %' = DATA NUMERIC[5,2] (Order);

FORM order
    OBJECTS o = Order PANEL
    PROPERTIES(o) date, nameCustomer, discount
;
```

We need to create an action to place the order on 30 days from today and apply a 5% discount.

### Solution

```lsf
setDateDiscount 'Apply discount (late delivery)' (Order o)  {
    date(o) <- sum(currentDate(), 30);
    discount(o) <- 5.0;
}

EXTEND FORM order
    PROPERTIES(o) setDateDiscount
;
```

## Example 2

### Task

Similar to [**Example 1**](#example-1), except that a specification was added to the order. The current price for each book is also defined.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book);
price 'Current price' (Book b) = DATA NUMERIC[14,2] (Book);

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;
book 'Book' = DATA Book (OrderDetail);
nameBook 'Book' (OrderDetail d) = name(book(d));

price 'Price' = DATA NUMERIC[14,2] (OrderDetail);

EXTEND FORM order
    OBJECTS d = OrderDetail
    PROPERTIES(d) nameBook, price
;
```

We need to create an action to populate all the lines in the order with current prices for the corresponding books.

### Solution

```lsf
fillPrice 'Set current prices' (Order o)  {
    price(OrderDetail d) <- price(book(d)) WHERE order(d) == o;
}

EXTEND FORM order
    PROPERTIES(o) fillPrice
;
```

Make sure to use `WHERE` in the action. Otherwise, the prices will be set for all lines of all orders in the database.
