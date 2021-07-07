---
title: 'How-to: IF/CASE'
---

## Example 1

### Task

We have a list of books associated with certain categories. Each book is assigned a price.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book);

CLASS Category 'Category' {
    novel 'Novel',
    thriller 'Thriller',
    fiction 'Fiction'
}

category 'Category' = DATA Category (Book);
price 'Price' = DATA NUMERIC[14,2] (Book);
```

We need to create an action that sets a given price for books associated with the specific category and a fixed price for all other books. When no category is selected, the error message must appear.

### Solution

```lsf
setPriceIf 'Set price' (Book b)  {
    IF NOT category(b) THEN
        MESSAGE 'No category selected for the book';
    ELSE
        IF category(b) == Category.novel THEN
            price(b) <- 50.0;
        ELSE
            price(b) <- 100.0;
}
```

## Example 2

### Task

Similar to [**Example 1**](#example-1).

We need to create an action that sets pre-defined prices for books associated with any of the three categories and sets zero price for all other books.

### Solution

```lsf
setPriceCase 'Set price' (Book b)  {
    CASE
        WHEN category(b) == Category.novel THEN
            price(b) <- 50.0;
        WHEN category(b) == Category.thriller THEN
            price(b) <- 100.0;
        WHEN category(b) == Category.fiction THEN
            price(b) <- 150.0;
    ELSE
        price(b) <- 0.0;
}
```
