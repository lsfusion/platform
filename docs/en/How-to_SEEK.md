---
title: 'How-to: SEEK'
---

## Example 1

### Task

We have a defined logic for books and categories. A form has been created with a list of books categorized.

```lsf
REQUIRE Time;

CLASS Category 'Category';
name 'Name' = DATA ISTRING[50] (Category) IN id;

CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book) IN id;

category 'Category' = DATA Category (Book) NONULL;
nameCategory 'Category' (Book b) = name(category(b));

FORM book 'Book'
    OBJECTS b = Book PANEL
    PROPERTIES(b) name, nameCategory

    EDIT Book OBJECT b
;

FORM books 'Books'
    OBJECTS c = Category
    PROPERTIES(c) READONLY name
    PROPERTIES(c) NEWSESSION NEW, EDIT, DELETE

    OBJECTS b = Book
    PROPERTIES(b) READONLY name
    FILTERS category(b) == c
;

NAVIGATOR {
    NEW books;
}
```

We need to create an action that creates a new book, automatically assigns it to the current category and then makes this book active once the user saves and closes the form.

### Solution

```lsf
createBook 'Create book' (Category c)  {
    NEWSESSION {
        NEW newBook = Book {
            category(newBook) <- c;
            DIALOG book OBJECTS b = newBook INPUT DO {
                SEEK books.b = newBook;
            }
        }
    }
}

EXTEND FORM books
    PROPERTIES(c) createBook DRAW b TOOLBAR
;
```

After closing the form, we need to call the [`SEEK` operator](SEEK_operator.md) which will make the added object active.

## Example 2

### Task

Similar to [**Example 1**](#example-1). We have also added the customer logic. The user can set a price for each customer and book in the dedicated form.

```lsf
CLASS Customer 'Customer';
name 'Name' = DATA ISTRING[50] (Customer) IN id;

price 'Price' = DATA NUMERIC[14,2] (Customer, Book);

FORM prices 'Prices'
    OBJECTS c = Customer PANEL
    PROPERTIES(c) name SELECTOR

    OBJECTS b = Book
    PROPERTIES name(b) READONLY, price(c, b)
;

NAVIGATOR {
    NEW prices;
}
```

We need to add a default customer whose data will be populated when the user opens the form.

### Solution

```lsf
defaultCustomer 'Default customer' = DATA Customer ();
nameDefaultCustomer 'Default customer' () = name(defaultCustomer());

EXTEND FORM options PROPERTIES() nameDefaultCustomer;
DESIGN options { commons { MOVE PROPERTY(nameDefaultCustomer()); } }

EXTEND FORM prices
    EVENTS ON INIT { SEEK prices.c = defaultCustomer(); }
;
```

The property with the default customer is added to the `'Settings'` form on the `'General'` tab. The current object will change once the user opens the form, since the [`ON INIT` event](Event_block.md) will be triggered.

## Example 2

### Task

Let's assume that we have a report form for which a date range is specified.

```lsf
FORM report 'Report'
    OBJECTS dFrom = DATE PANEL, dTo = DATE PANEL
    PROPERTIES VALUE(dFrom), VALUE(dTo)
;

NAVIGATOR {
    NEW report;
}
```

We need to create buttons that will modify the interval to the last week, current month or last month.

### Solution

```lsf
setReportLastWeek 'Last week' ()  {
    SEEK report.dFrom = subtract(currentDate(), 7);
    SEEK report.dTo = subtract(currentDate(), 1);
}
setReportCurrentMonth 'Current month' ()  {
    SEEK report.dFrom = firstDayOfMonth(currentDate());
    SEEK report.dTo = lastDayOfMonth(currentDate());
}
setReportLastMonth 'Last month' ()  {
    SEEK report.dFrom = firstDayOfMonth(subtract(firstDayOfMonth(currentDate()), 1));
    SEEK report.dTo = subtract(firstDayOfMonth(currentDate()), 1);
}

EXTEND FORM report
    PROPERTIES() setReportLastWeek, setReportCurrentMonth, setReportLastMonth
;
```

Date properties can be found in the `Time` [system module](Modules.md) which is loaded at the very beginning using the `REQUIRE` statement.
