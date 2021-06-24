---
title: 'How-to: WHILE'
---

## Example 1

### Task

We have an order for which a date is defined.

```lsf
CLASS Order 'Order';

date 'Date' = DATA DATE (Order);
```

We need to display a message containing the number of orders for each date from a given interval.

### Solution

```lsf
countOrders (DATE date) = GROUP SUM 1 BY date(Order o);

messageCountOrders 'Count the number of orders' (DATE dFrom, DATE dTo)  {
    // Option 1
    LOCAL date = DATE ();
    date() <- dFrom;

    WHILE date() <= dTo DO {
        MESSAGE 'Number of orders for ' + date () + ' : ' + OVERRIDE countOrders(date()), 0.0;
        date() <- sum(date(), 1);
    }

    // Option 2
    FOR iterate(DATE date, dFrom, dTo) DO
        MESSAGE 'Number of orders for ' + date + ' : ' + OVERRIDE countOrders(date()), 0.0;
}
```

Both these options will provide the same result.

The `sum` property defined in the `Time` system [module](Modules.md) is used to add one day to the given date.

## Example 2

### Task

Similar to [**Example 1**](#example-1). We have also defined order lines, so that each line contains the (full) amount and the discount.

```lsf
CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;

sum 'Amount' = DATA NUMERIC[14,2] (OrderDetail);
discountSum 'Discount amount' = DATA NUMERIC[14,2] (OrderDetail);
```

We need to create an action that "distributes" given discount by line, starting from the line with the largest amount.

### Solution

```lsf
distributeDiscount 'Distribute discount' (Order o, NUMERIC[14,2] discount)  {
    LOCAL discount = NUMERIC[14,2] ();
    discount() <- discount;

    LOCAL leftSum = NUMERIC[14,2] (OrderDetail);
    leftSum(OrderDetail d) <- sum(d) WHERE order(d) == o;

    WHILE discount() > 0 DO {
        FOR OrderDetail d == [ GROUP LAST OrderDetail od ORDER leftSum(od), od BY order(od)](o) DO { // finding the row with the largest "remaining" amount
            discountSum(d) <- MIN leftSum(d), discount();
            discount() <- discount() (-) discountSum(d);
        }
        IF (GROUP SUM 1 IF leftSum(OrderDetail d) > 0) THEN
            BREAK; // nothing left to break down
    }
}
```

## Example 3

### Task

The logic for changing the balance for the book is defined as follows:

```lsf
CLASS Book 'Book';

CLASS Ledger 'Change balance';
date 'Date' = DATA DATE (Ledger);
book 'Book' = DATA Book (Ledger);
quantity 'Qty' = DATA NUMERIC[14,2] (Ledger);
```

We need to create an action that will calculate the accumulated (integral) balance for a given time period.

### Solution

```lsf
calculateIntegral (DATE dFrom, DATE dTo)  {
    LOCAL date = DATE();
    date() <- dFrom;

    LOCAL balance = NUMERIC[14,2] (Book);
    balance(Book b) <- [ GROUP SUM quantity(Ledger l) IF date(l) < dFrom BY book(l)](b);

    LOCAL cumBalance = NUMERIC[14,2] (Book);

    WHILE date() <= dTo DO {
        cumBalance(Book b) <- cumBalance(b) (+) balance(b); //
        balance(Book b) <- balance(b) (+) [ GROUP SUM quantity(Ledger l) BY book(l), date(l)](b, date());
        date() <- sum(date(), 1);
    }

    FOR cumBalance(Book b) DO {
        MESSAGE 'Accumulated balance for book ' + b + ' : ' + cumBalance(b);
    }
}
```
