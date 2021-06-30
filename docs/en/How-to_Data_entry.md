---
title: 'How-to: Data entry'
---

## Example 1

### Task

We have a form that displays a list of books. We need to implement an option for entering a name only in upper case. Group change, copy&paste, and similar features must also be supported.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[100] (Book);

FORM books
     OBJECTS b = Book
;

NAVIGATOR {
    NEW books;
}
```

### Solution

```lsf
changeName(Book b)  {
     INPUT s = ISTRING[100] // inputting ISTRING[100] "to parameter" s (automatically wrapped in REQUEST, that is, group adjustments, PASTE, etc. are available)
                     DO // checking for requestCanceled
                          name(b) <- s;
}

EXTEND FORM books
    PROPERTIES (b) name ON CHANGE changeName(b)
;
```

  

## Example 2

### Task

We have a form that displays a list of books. In this form, the user can specify a genre, so that only books of this genre will be displayed. We also have a form containing the list of orders where you can also apply a filter by genre. Each book has a restricted/allowed flag, and the order may contain only allowed books.

```lsf
CLASS Genre 'Genre';
name 'Name' = DATA ISTRING[100] (Genre);

genre 'Genre' = DATA Genre (Book);
isForbidden 'Prohibition' = DATA BOOLEAN (Book);

CLASS Order 'Order';
book 'Book' = DATA Book (Order);
nameBook 'Book name' (Order o) = name(book(o));

date 'Date' = DATA DATE (Order);
number 'Number' = DATA STRING[100] (Order);

CONSTRAINT isForbidden(book(Order o))
    CHECKED BY book MESSAGE 'It is forbidden to choose this book';

FORM booksByGenre
     OBJECTS g = Genre PANEL
     PROPERTIES (g) name
     OBJECTS b = Book
     PROPERTIES (b) name
;


FORM orders 'Orders'
    OBJECTS g = Genre PANEL
    PROPERTIES (g) name

    OBJECTS o = Order
    PROPERTIES(o) READONLY date, number
    FILTERS g == genre(book(o))
;

NAVIGATOR {
    NEW orders;
}
```

We need to replace the book selection mechanism on the order form so that the genre selection form is called. Additional requirements:

-   The genre specified in the order form must be set as default value for the filter by genre
-   The current book from the order must be set as default value (if it has been selected)
-   The book in the order must be resettable (i. e. it must be possible to set the "Undefined" value)
-   Only allowed books can be selected
-   Group change, copy&paste, and similar features must also be supported for the field.

### Solution

```lsf
changeNameBook(Genre g, Order o)  {
          DIALOG booksByGenre OBJECTS // (automatically wrapped in REQUEST, that is, group adjustments, PASTE, etc. are available)
                g = g NULL, // NULL input allowed
                b = book(o) NULL INPUT bk NULL CONSTRAINTFILTER
                    // book(o) NULL - substituting book(o) to the input (if necessary, the input can be omitted, that is, the user can simply write b INPUT ..., which in turn is equivalent to b=NULL NULL INPUT ...)
                    // INPUT b NULL - returning the value of this object "to parameter" bk (return NULL is allowed, that is, there will be a reset button). Since the TO option is not specified, the result will be written to the requestedObject
                    // CONSTRAINTFILTER - taking into account the constraints for object b on the assumption that the result will be written to the property passed to the input (in this case book(o),
                    // if necessary, another expression can be specified in the form CONSTRAINTFILTER = dataBook(o)
    DO // checking for requestCanceled
        book(o) <- bk;
}

EXTEND FORM orders
    PROPERTIES (o) nameBook ON CHANGE changeNameBook(g, o)
;
```

  
