---
title: 'How-to: FOR'
---

## Example 1

### Task

We have a list of books with titles.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book);

FORM books 'Books'
    OBJECTS b = Book
    PROPERTIES(b) name, NEW, DELETE
;

NAVIGATOR {
    NEW books;
}
```

We need to find all the books containing a given line and display a message with their names and internal codes.

### Task

```lsf
findNemo 'Find book' ()  {
    FOR isSubstring(name(Book b), 'Nemo') DO {
        MESSAGE 'Found book ' + name (b) + ' with internal code ' + b;
    }
}
EXTEND FORM books
    PROPERTIES() findNemo
;
```

Use the isSubstring property (defined in the `Utils` system [module](Modules.md)) to identify whether a given line contains another line.

## Example 2

### Task

Similar to [**Example 1**](#example-1).

We need to create an action that creates 100 new books with certain titles.

### Solution

```lsf
add100Books 'Add 100 books' ()  {
    // Option 1
    FOR iterate(INTEGER i, 1, 100) NEW b = Book DO {
        name(b) <- 'Book ' + i;
    }

    // Option 2
    FOR iterate(INTEGER i, 1, 100) DO {
        NEW b = Book {
            name(b) <- 'Book ' + i;
        }
    }
}

EXTEND FORM books
    PROPERTIES() add100Books
;
```

Both these implementations will provide the same result.

To solve this task, use the `iterate` property (defined in the `Utils` system module) which returns `TRUE` for all integers in the range.

## Example 3

### Task

Similar to [**Example 1**](#example-1), but with the order logic. Each order contains lines where books and discount prices are specified.

```lsf
CLASS Order 'Order';

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;
book 'Book' = DATA Book (OrderDetail);
nameBook 'Book' (OrderDetail d) = name(book(d));

price 'Price' = DATA NUMERIC[14,2] (OrderDetail);

discount 'Discount, %' = DATA NUMERIC[8,2] (OrderDetail);
discountPrice 'Discount price' = DATA NUMERIC[14,2] (OrderDetail);
```

We need to create an action that applies a discount to all the lines with prices above 100.

### Solution

```lsf
makeDiscount 'Make discount' (Order o)  {
    // Option 1
    FOR order(OrderDetail d) == o AND price(d) > 100 DO {
        discount(d) <- 10;
        discountPrice(d) <- price(d) * (100.0 - discount(d)) / 100.0;
    }

    // Option 2
    discount(OrderDetail d) <- 10 WHERE order(d) == o AND price(d) > 100;
    discountPrice(OrderDetail d) <- price(d) * (100.0 - discount(d)) / 100.0 WHERE order(d) == o AND price(d) > 100;
}
```

Both these implementations will provide the same result.

## Example 4

### Task

Similar to [**Example 3**](#example-3), but a default price was added for each book.

```lsf
price 'Price' = DATA NUMERIC[14,2] (Book);
```

We need to create an action that adds all the books with prices above 100 to the order.

### Solution

```lsf
addSelectedBooks 'Add marked books' (Order o)  {
    // Option 1
    FOR price(Book b) > 100 NEW d = OrderDetail DO {
        order(d) <- o;
        book(d) <- b;
        price(d) <- price(b);
    }

    // Option 2
    FOR price(Book b) == NUMERIC[14,2] p AND p > 100 NEW d = OrderDetail DO {
        order(d) <- o;
        book(d) <- b;
        price(d) <- p;
    }
}
```

Both these implementations will provide the same result.
