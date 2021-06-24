---
title: 'How-to: Filtering and ordering'
---

## Example 1

### Task

There are remaining books in stock.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[100] (Book);

CLASS Stock 'Warehouse';
name 'Name' = DATA ISTRING[100] (Stock);

balance 'Balance' = DATA INTEGER (Book, Stock); // for example it is made a data property, although usually it is calculated
```

We need to create a form to display the balances of books in a given stock in alphabetical order.

### Solution

```lsf
FORM onStockObject 'Balances'
    OBJECTS s = Stock PANEL
    PROPERTIES(s) name SELECTOR

    OBJECTS b = Book
    PROPERTIES READONLY name(b), balance(b, s)
    ORDERS name(b)

    // Option 1
    FILTERS balance(b, s)

    // Option 2
    FILTERGROUP bal
        FILTER 'With positive balance' balance(b, s) > 0 'F10'
        FILTER 'With negative balance' balance(b, s) < 0 'F9'
        FILTER 'With balance' balance(b, s) 'F8' DEFAULT
        FILTER 'No remainder' NOT balance (b, s) 'F7'
;
```

Option 1 sets up a fixed filter that the user cannot remove. Option 2 allows the user to choose between predefined criteria (by default the one for which the `DEFAULT` option is set).

## Example 2

### Task

Similar to [**Example 1**](#example-1).

We need to create a form to display remaining books in several warehouses, with the possibility of filtering by a specific warehouse. Ordering should be first by warehouse, and within that by book title.

### Solution

```lsf
filterStock = DATA LOCAL Stock ();
nameFilterStock 'Warehouse' = name(filterStock());

FORM onStockLocal 'Balances'
    PROPERTIES() nameFilterStock

    OBJECTS sb = (s = Stock, b = Book)
    PROPERTIES READONLY name(s), name(b), balance(b, s)
    ORDERS name(s), name(b)

    FILTERS s == filterStock() OR NOT filterStock()
;
```

In this case a warehouse cannot be declared via the `OBJECTS` block, because then not specifying a warehouse for filtering will not be an option.

## Example 3

### Task

There is a list of orders for certain customers

```lsf
CLASS Customer 'Customer';
name 'Name' = DATA ISTRING[100] (Customer);

CLASS Order 'Order';
date 'Date' = DATA DATE (Order);

customer 'Customer' = DATA Customer (Order);
nameCustomer 'Customer' (Order o) = name(customer(o));
```

We need to create a form to display the list of orders allowing to filter by date and/or customer. By default, orders should be in descending date order.

### Solution

```lsf
filterCustomer = DATA LOCAL Customer ();
nameFilterCustomer 'Customer' = name(filterCustomer());

FORM orders 'Orders'
    PROPERTIES() nameFilterCustomer

    OBJECTS dates = (dateFrom = DATE, dateTo = DATE) PANEL
    PROPERTIES df = VALUE(dateFrom), dt = VALUE(dateTo)

    OBJECTS o = Order
    PROPERTIES(o) READONLY do = date, nameCustomer
    ORDERS do DESC
    FILTERS date(o) >= dateFrom, date(o) <= dateTo,
            customer(o) == filterCustomer() OR NOT filterCustomer()
;
```

It should be noted that the dates in this case should always be selected (by default, the current date will be set when the form is opened). But it is possible not to select a customer.

Also, note that what is set in `ORDER BY` is not an expression but a specific property added to the form. Thus, we cannot order by a property that has not been added to the form.
