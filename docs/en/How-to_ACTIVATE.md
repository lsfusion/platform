---
title: 'How-to: ACTIVATE'
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
                ACTIVATE books.b = newBook;
            }
        }
    }
}

EXTEND FORM books
    PROPERTIES(c) createBook DRAW b TOOLBAR
;
```

After closing the form, we need to call the [`ACTIVATE` operator](ACTIVATE_operator.md) which will make the added object active.

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
    EVENTS ON INIT { ACTIVATE prices.c = defaultCustomer(); }
;
```

The property with the default customer is added to the `'Settings'` form on the `'General'` tab. The current object will change once the user opens the form, since the [`ON INIT` event](Event_block.md) will be triggered.

## Example 3

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
    ACTIVATE report.dFrom = subtract(currentDate(), 7);
    ACTIVATE report.dTo = subtract(currentDate(), 1);
}
setReportCurrentMonth 'Current month' ()  {
    ACTIVATE report.dFrom = firstDayOfMonth(currentDate());
    ACTIVATE report.dTo = lastDayOfMonth(currentDate());
}
setReportLastMonth 'Last month' ()  {
    ACTIVATE report.dFrom = firstDayOfMonth(subtract(firstDayOfMonth(currentDate()), 1));
    ACTIVATE report.dTo = subtract(firstDayOfMonth(currentDate()), 1);
}

EXTEND FORM report
    PROPERTIES() setReportLastWeek, setReportCurrentMonth, setReportLastMonth
;
```

Date properties can be found in the `Time` [system module](Modules.md) which is loaded at the very beginning using the `REQUIRE` statement.

## Example 4

### Task

When the user opens a report form, the focus must land directly on the start-date input so that a date can be typed without clicking first.

```lsf
FORM report 'Report'
    OBJECTS dFrom = DATE PANEL, dTo = DATE PANEL
    PROPERTIES VALUE(dFrom), VALUE(dTo)
;
```

### Solution

```lsf
EXTEND FORM report
    EVENTS ON INIT { ACTIVATE PROPERTY report.dFrom; }
;
```

On form open, the [`ON INIT`](Event_block.md) event fires and the [`ACTIVATE` operator](ACTIVATE_operator.md) moves focus to the `dFrom` field. `ACTIVATE PROPERTY` works for any property displayed on the form — panel, grid, and tree alike.

## Example 5

### Task

A CRM form has two tabs — `'Customers'` and `'Orders'`. After a new order is created, the form must automatically switch to the `'Orders'` tab and select the new order.

```lsf
CLASS Customer 'Customer';
name 'Name' = DATA ISTRING[50] (Customer);

CLASS Order 'Order';
number 'Number' = DATA INTEGER (Order);
customer 'Customer' = DATA Customer (Order);

FORM crm 'CRM'
    OBJECTS c = Customer
    PROPERTIES(c) name

    OBJECTS o = Order
    PROPERTIES(o) number, nameCustomer = name(customer(o))
;

DESIGN crm {
    NEW tabs FIRST {
        tabbed = TRUE;
        NEW customersTab { caption = 'Customers'; MOVE BOX(c); }
        NEW ordersTab { caption = 'Orders'; MOVE BOX(o); }
    }
}
```

### Solution

```lsf
createOrder 'Create order' (Customer c)  {
    NEW o = Order {
        customer(o) <- c;
        ACTIVATE TAB crm.ordersTab;
        ACTIVATE crm.o = o;
    }
}

EXTEND FORM crm
    PROPERTIES(c) createOrder
;
```

`ACTIVATE TAB` switches the tab panel to `'Orders'`, after which `ACTIVATE crm.o = o` sets the newly created order as the current object.
