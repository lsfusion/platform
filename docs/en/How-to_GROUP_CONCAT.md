---
title: 'How-to: GROUP CONCAT'
---

## Example 1

### Task

We have a set of books associated with certain priority tags.

```lsf
CLASS Book 'Book';
CLASS Tag 'Tag';
name 'Name' = DATA ISTRING[10] (Tag);

in 'On' = DATA BOOLEAN (Tag, Book);
```

We need to retrieve a list of book tags separated by commas in alphabetical order.

### Solution

```lsf
tags 'Tags' (Book b) = GROUP CONCAT name(Tag t) IF in(t, b), ',' ORDER name(t), t CHARWIDTH 10;
```

It is recommended that you specify the sizes of all the properties created using `GROUP CONCAT` that will be visible on a form. By default, the system implements the "pessimistic" scenario and reserves a lot of space for such properties.

## Example 2

### Task

We have a set of books associated with certain categories and authors.

```lsf
CLASS Category 'Category';

CLASS Author 'Author';
name 'Author' = DATA ISTRING[20] (Author);

category 'Category' = DATA Category (Book);
author 'Author' = DATA Author (Book);
```

We need to retrieve a list of all authors by category separated by commas in descending order of the number of books.

### Solution

```lsf
countBooks 'Number of books' (Author a, Category c) = GROUP SUM 1 BY author(Book b), category(b);

authors 'Authors' (Category c) = GROUP CONCAT name(Author a) IF countBooks(a, c) ORDER DESC countBooks(a, c), a;
```
