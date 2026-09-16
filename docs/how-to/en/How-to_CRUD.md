---
slug: "/How-to_CRUD"
title: 'How-to: CRUD'
---

## Example 1

### Task

We have a set of predefined book types.

```lsf
CLASS Type 'Type' {
    novel 'Novel',
    thriller 'Thriller',
    fiction 'Fiction'
}
name 'Name' (Type g) = caption(g) IF g IS Type;
```

We need to create a form to select a type from the list.

### Solution

```lsf
FORM types 'List of types'
    OBJECTS g = Type
    PROPERTIES(g) READONLY name

    LIST Type OBJECT g
;
```

`LIST` indicates that this form will be used for selecting a type from the list (e. g. when the user wants to change the book type).

## Example 2

### Task

We have a set of books with given titles.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[30] (Book) IN id;
```


:::info
It is recommended that you add all the `name` properties to the `id` group. Values of this property will help identify the object in case of the constraint violations. It will also be added to automatic forms when no edit (`EDIT`) or list (`LIST`) forms are defined for the class.
:::

  

We need to create a form with a list of books where the user can add, edit or delete them.

### Solution

```lsf
FORM book 'Book' // form for displaying "card' // form for displaying the book card
    OBJECTS b = Book PANEL
    PROPERTIES(b) name

    EDIT Book OBJECT b
;

FORM books 'Books'
    OBJECTS b = Book
    PROPERTIES(b) READONLY name
    PROPERTIES(b) NEWSESSION NEW, EDIT, DELETE

    LIST Book OBJECT b
;

NAVIGATOR {
    NEW books;
}
```

## Example 3

### Task

We have a set of book genres with given titles.

```lsf
CLASS Genre 'Genre';
name 'Name' = DATA ISTRING[30] (Genre);
```

We need to create a form with a list of genres where the user can add, edit or delete them, and one more form with a list of genres but without these options.

### Solution

```lsf
FORM genre 'Genre'
    OBJECTS g = Genre PANEL
    PROPERTIES(g) name

    EDIT Genre OBJECT g
;

FORM genres 'Genres'
    OBJECTS g = Genre
    PROPERTIES(g) READONLY name
    PROPERTIES(g) NEWSESSION NEW, EDIT, DELETE
;

FORM dialogGenre 'Genres'
    OBJECTS g = Genre
    PROPERTIES(g) READONLY name

    LIST Genre OBJECT g
;

NAVIGATOR {
    NEW genres;
}
```

Use this scheme (with three forms instead of two) when you want to allow users to select genres and prevent any accidental changes to the genre information. In this case, the user will be able to edit genres only on a dedicated form.

## Example 4

### Task

Same as [**Example 2**](#example-2). We need a single book card on which the user either picks an existing book or creates a new one, and then edits its details.

```lsf
pages 'Pages' = DATA INTEGER (Book);
```

### Solution

```lsf
FORM bookCard 'Book'
    OBJECTS b = Book PANEL NULL
    PROPERTIES(b) book 'Book' = name SELECTOR, NEW
    PROPERTIES(b) name, pages
;

DESIGN bookCard {
    // selection block: the book selection field and the create button in one row
    NEW selection FIRST {
        caption = 'Book selection';
        horizontal = TRUE;
        MOVE PROPERTY(book);
        MOVE PROPERTY(NEW(b));
    }
    // details of the picked or created book
    NEW details AFTER selection {
        caption = 'Details';
        MOVE PROPERTY(name(b));
        MOVE PROPERTY(pages(b));
    }
}

NAVIGATOR {
    NEW bookCard;
}
```

The `NULL` default objects type opens the card without a current book. On a change, the book selection field (`SELECTOR`) does not edit the name but opens the book selection dialog, so the name is added to the form once more — as an ordinary editable field. So that the two names are not confused, the design keeps them in separate blocks: in the "Book selection" block the selection field stands in one row with the create button, in the "Details" block — the editable name and number of pages. Until a book is picked or created (`NEW`), of the properties of the object `b` only the selection field is visible on the form: the other properties and actions that take `b` are not shown, since there is no object they would belong to, and the empty details block is hidden. As soon as the user picks or creates a book, the name and the number of pages appear and can be edited. If all the fields must be available from the very opening, the object has to be created beforehand and the card opened for it — as the `NEWSESSION NEW` action does in [**Example 2**](#example-2).
