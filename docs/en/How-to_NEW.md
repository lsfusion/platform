---
title: 'How-to: NEW'
---

## Example 1

### Task

We have an order with a given date and customer.

```lsf
CLASS Order 'Order';

CLASS Customer 'Customer';
name = DATA ISTRING[50] (Customer);

date 'Date' = DATA DATE (Order);

customer 'Customer' = DATA Customer (Order);
nameCustomer 'Customer' (Order o) = name(customer(o));
```

We need to create an action that will create a new order based on the specified one.

### Solution

```lsf
copyOrder 'Copy' (Order o)  {
    NEW n = Order {
        date(n) <- date(o);
        customer(n) <- customer(o);
    }
}
```

## Example 2

### Task

Similar to [**Example 1**](#example-1), except that the order contains lines with other orders.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book);

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;
book 'Book' = DATA Book (OrderDetail);
nameBook 'Book' (OrderDetail d) = name(book(d));

price 'Price' = DATA NUMERIC[14,2] (OrderDetail);
```

We need to create an action that will create a new order with identical lines based on the selected order.

### Solution

```lsf
copyDetail (Order o)  {
    NEW n = Order {
        date(n) <- date(o);
        customer(n) <- customer(o);
        FOR order(OrderDetail od) == o NEW nd = OrderDetail DO {
            order(nd) <- n;
            book(nd) <- book(od);
            price(nd) <- price(od);
        }
    }
}
```
