---
title: 'How-to: DELETE'
---

## Example 1

### Task

We have an order with a given date and buyer and also with lines that refer to the books.

```lsf
CLASS Order 'Order';

CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book);

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;
book 'Book' = DATA Book (OrderDetail);
nameBook 'Book' (OrderDetail d) = name(book(d));
```

We need to create an action that deletes the book for which no orders have been placed.

### Solution

```lsf
delete (Book b)  {
    IF NOT [ GROUP SUM 1 BY book(OrderDetail d)](b) THEN
        DELETE b;
    ELSE
        MESSAGE 'It is forbidden to delete a book, as there is an order for it';
}
```

## Example 2

### Task

Similar to [**Example 1**](#example-1).

We need to create an action that clears the order by deleting all its lines.

### Solution

```lsf
clear (Order o)  {

    // Option 1
    DELETE OrderDetail d WHERE order(d) == o;

    // Option 2
    FOR order(OrderDetail d) == o DO
        DELETE d;
}
```

Both these implementations are acting similarly.
