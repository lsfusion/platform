---
title: 'How-to: GROUP LAST'
---

## Example 1

### Task

We have a set of books associated with a certain category, and the dates of their receipt.

```lsf
CLASS Book 'Book';
CLASS Category 'Category';

category 'Category' = DATA Category (Book);
date 'Arrival date' = DATA DATE (Book);
```

We need to find the latest received book in the selected category.

### Solution

```lsf
book 'Last book' (Category c) = GROUP LAST Book b ORDER date(b), b BY category(b);
```

It is important to remember that order in the `ORDER` clause should be uniquely determined. To do this, the book (specifically, its internal ID) should be used as the second parameter since several books may have the same date of receipt.

## Example 2 

### Task

Similar to [**Example 1**](#example-1), but the author and genre list are specified for each book.

```lsf
CLASS Author 'Author';
CLASS Genre 'Genre';

author 'Author' = DATA Author (Book);
genre 'Genre' = DATA Genre (Book);
in 'On' = DATA BOOLEAN (Book, Genre);
```

We need to find the most popular category by author and genre.

### Solution

```lsf
countBooks 'Number of books' (Category c, Author a, Genre g) = GROUP SUM 1 IF in(Book b, g) BY category(b), author(b);

category (Author a, Genre g) = GROUP LAST Category c ORDER countBooks(c, a, g), c WHERE countBooks(c, a, g);
```

## Example 3

### Task

We have a certain set of books and the information about price changes per book and warehouse. Each object of the `Ledger` class reflects a single change in price since a specific date.

```lsf
CLASS Stock 'Warehouse';

CLASS Ledger 'Price change';
date = DATA DATE (Ledger);
stock = DATA Stock (Ledger);
book = DATA Book (Ledger);

price = DATA NUMERIC[10,2] (Ledger);
```

We need to identify the current price for a given book at the warehouse.

### Solution

```lsf
currentPrice (Book b, Stock s) = GROUP LAST price(Ledger l) ORDER date(l), l BY book(l), stock(l);//#solution3
```

## Example 4

### Task

Similar to [**Example 3**](#example-3).

We need to find the price valid for a specific date for a book at the warehouse.

### Solution

```lsf
price (Book b, Stock s, DATE d) = GROUP LAST price(Ledger l) ORDER date(l), l WHERE date(l) <= d BY book(l), stock(l);
```

## Example 5

### Task

Similar to [**Example 4**](#example-4), except that a change in price has an expiration date. If this date is not specified, then the price is not limited in time.

```lsf
dateTo 'Valid until (inclusive)' = DATA DATE (Ledger);
```

### Solution

```lsf
currentPriceDate (Book b, Stock s) = GROUP LAST price(Ledger l) ORDER date(l), l WHERE NOT dateTo(l) < currentDate() BY book(l), stock(l);
priceDate(Book b, Stock s, DATE d) = GROUP LAST price(Ledger l) ORDER date(l), l WHERE date(l) <= d AND NOT dateTo(l) < d BY book(l), stock(l);
```

Note that the expression `NOT dateTo(l) < date` does not always mean the same as `dateTo(l) >= date`. The difference occurs when the value `dateTo(l)` equals `NULL`. In the first case, `dateTo(l) < date` equals `NULL` (i. e. false), while `NOT NULL` equals `TRUE`. In the second case, the expression obviously equals `NULL` (i. e. false).
