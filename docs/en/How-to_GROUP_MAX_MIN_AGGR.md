---
title: 'How-to: GROUP MAX/MIN/AGGR'
---

## Example 1

### Task

We have a set of books, where each book has a unique ID.

```lsf
CLASS Book 'Book';

id 'Number' = DATA INTEGER (Book);
```

We need to find the maximum book ID.

### Solution

```lsf
maxId 'Maximum number' () = GROUP MAX id(Book b);
```

## Example 2

### Task

Similar to [**Example 1**](#example-1).

We need to find a `Book` object by book ID.

### Solution

```lsf
// Option 1
book1 'Book' (INTEGER i) = GROUP MAX Book b BY id(b);

// Option 2
book2 'Book' (INTEGER i) = GROUP AGGR Book b BY id(b);
```

The difference between Option 2 and Option 1 is that declaration of this property puts a [constraint](Constraints.md) on the uniqueness of book IDs. Any attempt to add two or more books with the same ID will result in the error message.

## Example 3

### Task

We have a set of books, where each book is associated with a category and price.

```lsf
CLASS Category 'Category';

category 'Category' = DATA Category (Book);
price 'Price' = DATA NUMERIC[10,2] (Book);
```

We need to calculate the minimum price per category.

### Solution

```lsf
minPrice 'Maximum number' (Category c) = GROUP MIN price(Book b) BY category(b);
```

## Example 4

### Task

We have a book shipment document.

```lsf
CLASS Shipment 'Shipment';
CLASS ShipmentDetail 'Shipment line';
shipment 'Shipment' = DATA Shipment (ShipmentDetail) NONULL DELETE;

book 'Book' = DATA Book (ShipmentDetail);
```

We need to find a line with a given shipment by shipment document and book.

### Solution

```lsf
shipmentDetail 'Shipment line' (Shipment s, Book b) = GROUP MAX ShipmentDetail d BY shipment(d), book(d);
```

You can use this property to implement the Search functionality when inputting a shipment document.
