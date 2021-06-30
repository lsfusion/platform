---
title: 'How-to: GROUP SUM'
---

## Example 1

### Task

We have a set of books associated with certain category.

```lsf
CLASS Book 'Book';
CLASS Category 'Category';

category 'Category' = DATA Category (Book);
```

We need to calculate the number of books in the category.

### Solution

```lsf
countBooks 'Number of books' (Category c) = GROUP SUM 1 BY category(Book book);
```

## Example 2

### Task

We have a set of books associated with certain tags. Each book can be associated with several tags at the same time.

```lsf
CLASS Tag 'Tag';

in 'On' = DATA BOOLEAN (Tag, Book);
```

We need to calculate the number of books in the tag.

### Solution

```lsf
countBooks 'Number of books' (Tag t) = GROUP SUM 1 IF in(t, Book b);
```

## Example 3

### Task

We have the information about the movement of books: each record is linked to the book itself and the warehouse where the movement occured, and also contains quantity and types of operations (receipt/shipment).

```lsf
CLASS Stock 'Warehouse';


CLASS Ledger 'Movement';
book 'Book' = DATA Book (Ledger);
stock 'Warehouse' = DATA Stock (Ledger);

quantity 'Qty' = DATA INTEGER (Ledger);
out 'Expenses' = DATA BOOLEAN (Ledger);
```

We need to calculate the current balance for a book at the warehouse.

### Solution

```lsf
TABLE bookStock (Book, Stock);
currentBalance 'Current balance' (Book b, Stock s) = GROUP SUM IF out(Ledger l) THEN -quantity(l) ELSE quantity(l) BY book(l), stock(l) MATERIALIZED;
```

It is recommended to mark the `currentBalance` property as [`MATERIALIZED`](Materializations.md), so that when reading the current balances, the system could take the calculated value from the `bookStock` table instead of recalculating this value based on all movements. Though this will slow down the writing process (since writing each movement will require updating the current balance), the reading process will become much faster.

Note that you do not need to define explicitly in which table to keep the `currentBalance` property, since the system will use the signature to find out the most suitable table for it (in this case, this will be `bookStock`).

## Example 4

### Task

Similar to [**Example 3**](#example-3), except that each movement is associated with the date of movement.

```lsf
date 'Date' = DATA DATE (Ledger) INDEXED; // it is better to add an index to filter by date quickly
```

We need to calculate the current balance for a given book at the warehouse for the specific date (as of the morning, without the movements occured on that day).

### Solution

```lsf
// Option 1
balance1 'Balance as of date' (Book b, Stock s, DATE d) = GROUP SUM (IF out(Ledger l) THEN -quantity(l) ELSE quantity(l)) IF date(l) < d BY book(l), stock(l);

// Option 2
balance2 'Balance as of date' (Book b, Stock s, DATE d) = currentBalance(b, s) (-) [ GROUP SUM (IF out(Ledger l) THEN -quantity(l) ELSE quantity(l)) IF date(l) >= d BY book(l), stock(l)](b, s);
```

  

The second option is preferable. Since requests usually refer to recent dates, the server will be calculating the result "retrospectively" from the current balance, thereby analyzing fewer records.

## Example 5

### Task

Similar to [**Example 3**](#example-3), except that we need to calculate the current balance for a given book across all the warehouses.

### Solution

```lsf
currentBalance 'Current balance' (Book b) = GROUP SUM currentBalance(b, Stock s);
```

  

Unlike the current balance for all the warehouses, it is not reasonable to mark this property as `MATERIALIZED` if you have only few warehouses — otherwise, UPDATE CONFLICT may occur when several users try to write the movement of the same book at different warehouses simultaneously.
