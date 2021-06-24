---
title: 'How-to: Using objects as templates'
---

## Example 1

### Task

We have a book for which a name and price are defined. List and edit forms are defined for the book.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book) IN id;
price 'Price' = DATA NUMERIC[14,2] (Book);

FORM book 'Book'
    OBJECTS b = Book PANEL
    PROPERTIES(b) name, price

    EDIT Book OBJECT b
;

FORM books 'Books'
    OBJECTS b = Book
    PROPERTIES(b) READONLY name, price
    PROPERTIES(b) NEWSESSION NEW, EDIT, DELETE
;
```

We need to make a button that creates a new book based on the current one, and opens a form to edit it.

### Solution

```lsf
copy 'Copy' (Book book)  {
    NEWSESSION {
        NEW newBook = Book {
            name(newBook) <- name(book);
            price(newBook) <- price(book);
            SHOW book OBJECTS b = newBook DOCKED;
        }
    }
}

EXTEND FORM books
    PROPERTIES(b) copy TOOLBAR
;
```

When the button is pressed, a new [change session](Change_sessions.md) will be created within which the book will be created and the new form will work with. If the user closes the form without saving, the new book will not be saved to the database and will be lost. The word `TOOLBAR` specifies that this button should be displayed in the toolbar of the table with the list of orders.

## Example 2

### Task

We have the concepts of order documents and invoices, as well as forms for listing and editing them. Lines are defined for each document.

```lsf
// Orders
CLASS Order 'Order';
date 'Date' = DATA DATE (Order);
number 'Number' = DATA INTEGER (Order);

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;

book 'Book' = DATA Book (OrderDetail) NONULL;
nameBook 'Book' (OrderDetail d) = name(book(d));

quantity 'Quantity' = DATA INTEGER (OrderDetail);
price 'Price' = DATA NUMERIC[14,2] (OrderDetail);

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
// Accounts
CLASS Invoice 'Invoice';
date 'Date' = DATA DATE (Invoice);
number 'Number' = DATA INTEGER (Invoice);

CLASS InvoiceDetail 'Invoice line';
invoice 'Invoice' = DATA Invoice (InvoiceDetail) NONULL DELETE;

book 'Book' = DATA Book (InvoiceDetail) NONULL;
nameBook 'Book' (InvoiceDetail d) = name(book(d));

quantity 'Quantity' = DATA INTEGER (InvoiceDetail);
price 'Price' = DATA NUMERIC[14,2] (InvoiceDetail);

FORM invoice 'Invoice'
    OBJECTS i = Invoice PANEL
    PROPERTIES(i) date, number

    OBJECTS d = InvoiceDetail
    PROPERTIES(d) nameBook, quantity, price, NEW, DELETE
    FILTERS invoice(d) == i
;
```

We need to implement an [action](Actions.md) that will create an invoice based on an order and open a form for editing it.

### Solution

```lsf
createInvoice 'Create invoice' (Order o)  {
    NEWSESSION {
        NEW i = Invoice {
            date(i) <- date(o);
            number(i) <- number(o);

            FOR order(OrderDetail od) == o NEW id = InvoiceDetail DO {
                invoice(id) <- i;

                book(id) <- book(od);

                quantity(id) <- quantity(od);
                price(id) <- price(od);
            }
            SHOW invoice OBJECTS i = i DOCKED;
        }
    }
}

EXTEND FORM orders
    PROPERTIES(o) createInvoice TOOLBAR
;
```

## Example 3

### Task

Similar to [**Example 2**](#example-2).

We need to implement an [action](Actions.md) that will open a dialog for the invoice with a list of orders and add lines from the selected one.

### Solution

```lsf
fillOrder 'Fill in by order' (Invoice i)  {
    DIALOG orders OBJECTS o INPUT DO {
        date(i) <- date(o);
        number(i) <- number(o);

        FOR order(OrderDetail od) == o NEW id = InvoiceDetail DO {
            invoice(id) <- i;

            book(id) <- book(od);

            quantity(id) <- quantity(od);
            price(id) <- price(od);
        }
    }
}
EXTEND FORM invoice
    PROPERTIES(i) fillOrder
;
```

No manipulation with change sessions is required because the button will be called from the account edit form and changes will occur within that session.
