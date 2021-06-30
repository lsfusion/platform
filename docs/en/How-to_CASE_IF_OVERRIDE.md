---
title: 'How-to: CASE/IF/OVERRIDE'
---

## Example 1

### Task

We have a set of white books and black books.

```lsf
CLASS Color 'Color' {
    white 'White',
    black 'Black'
}

CLASS Book 'Book';

color 'Color' = DATA Color (Book);
```

We need to define a property that returns the color of a given book.

### Solution

```lsf
// Option 1
nameColor1 'Color' (Book b) = staticCaption(color(b));

// Option 2
nameColor2 'Color' (Book b) = IF color(b) == Color.white THEN 'White' ELSE 'Black';
```

These two expressions provide identical results.

## Example 2

### Task

We have multiple purchase orders to suppliers for books. For each purchase order defined it's status if it was placed, agreed and delivered. In this example these statuses are declared as [data](Data_properties_DATA.md) properties, but in more sophisticated cases they may be calculated.

```lsf
CLASS Order 'Order';

sent 'Sent' = DATA BOOLEAN (Order);
agreed 'Agreed' = DATA BOOLEAN (Order);
accepted 'Accepted' = DATA BOOLEAN (Order);
```

We need to obtain the current status of an order.

### Solution

```lsf
// Option 1
nameStatus1 'Status' (Order o) = CASE WHEN accepted(o) THEN 'Accepted'
                                     WHEN agreed(o) THEN 'Agreed'
                                     WHEN sent(o) THEN 'Sent'
                                ELSE 'New';

// Option 2
CLASS Status 'Status' {
    new 'New',
    sent 'Sent',
    agreed 'Agreed',
    accepted 'Accepted'
}

status 'Status' (Order o) = CASE WHEN accepted(o) THEN Status.accepted
                                 WHEN agreed(o) THEN Status.agreed
                                 WHEN sent(o) THEN Status.sent
                            ELSE Status.new;
nameStatus2 'Status' (Order o) = staticCaption(status(o));
```

## Example 3

### Task

We have a set of books described in [**Example 1**](#example-1).

We need to set a markup for the book and also provide an option for setting a default value.

### Solution

```lsf
dataMarkup 'Markup, %' = DATA NUMERIC[6,2] (Book);

defaultMarkup 'Default markup' = DATA NUMERIC[6,2] ();

markup1 'Markup, %' (Book b) = OVERRIDE dataMarkup(b), defaultMarkup();

// Equivalent to:
markup2 'Markup, %' (Book b) = IF dataMarkup(b) THEN dataMarkup(b) ELSE defaultMarkup();
```

## Example 4

### Task

The same set of books from [**Example 1**](#example-1), but categorized.

```lsf
CLASS Category 'Category';

category 'Category' = DATA Category (Book);
```

We need to set a markup for a book and also provide an option for setting a default value for the corresponding category.

### Solution

```lsf
markup 'Markup, %' = DATA NUMERIC[6,2] (Category);

markup 'Markup, %' (Book b) = OVERRIDE dataMarkup(b), markup(category(b));
```

## Example 5

### Task

We have a set of enumerated books.

```lsf
number 'Number' = DATA INTEGER (Book);
```

We need to find the number following the maximum book number.

### Solution

```lsf
freeNumber1 () = (GROUP MAX number(Book b)) (+) 1;

// Equivalent to:
freeNumber2() = (OVERRIDE 0, (GROUP MAX number(Book b))) + 1;
```

We use the operator `(+)` instead of the regular operator `+`, because otherwise if no books are found, then the standard increment by `1` will return `NULL`.
