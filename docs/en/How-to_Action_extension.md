---
title: 'How-to: Action extension'
---

We can use the following scheme to implement polymorphism:

### Example 1

Create an abstract class `Shape` with an abstract action `whoAmI`:

```lsf
CLASS ABSTRACT Shape;
whoAmI  ABSTRACT ( Shape);
```

Then, create `Square` and `Circle` classes inherited from `Shape`:

```lsf
CLASS Rectangle : Shape;
CLASS Circle : Shape;
```

Define the implementation of `whoAmI` for the created classes:

```lsf
whoAmI (Rectangle r) + {
    IF r IS Rectangle THEN {
        MESSAGE 'I am a rectangle';
    }
}
whoAmI (Circle c) + {
    IF c IS Circle THEN {
        MESSAGE 'I am a circle';
    }
}
```

When executing the `whoAmI` action, all the actions added as an implementation will be called. In this case, a corresponding message will appear depending on the argument.

### Example 2

Suppose that we need to implement an action that copies an object (e. g. the `Book` class) with its semantics defined in multiple modules. This can be implemented as follows:

Declare the `Book` class and the actions to copy it:

```lsf
MODULE Book;

CLASS Book; // declaring class "book"
overCopy  ABSTRACT ( Book, Book); // abstract action that takes an input two books and is "entry point", to which other modules can add realization
copy (Book book)  { // creating action on book copy
    NEW b = Book { // adding new book
        overCopy(b, book);
    }
}
```

In the dependent module `MyBook`, we want to extend the `Book` class with new properties and also define the action to copy them:

```lsf
MODULE MyBook;

REQUIRE Book;

name = DATA STRING[100] (Book); // adding some name property to the product
overCopy (Book s, Book d) + {
    name(d) <- name(s); // connecting the copying of the created property to the product copy action
}
```
