---
title: 'How-to: Numbering'
---

Let's suppose we have a set of books. For each of these books, we define a number as an integer.

```lsf
CLASS Book 'Book';
number 'Number' = DATA INTEGER (Book) IN id;
name 'Name' = DATA ISTRING[50] (Book) IN id;
```

We implement a property that will find a book by its number. It can be useful, for example, for importing data where each book is identified by a number. It can be used to get a link to a book object by getting its number as a parameter.

```lsf
book (INTEGER number) = GROUP AGGR Book b BY number(b);

bookExists (INTEGER number)  {
    IF book(number) THEN
        MESSAGE 'The book with the number ' + number + ' exists. Its name : ' + name(book(number));
    ELSE
        MESSAGE 'The book with the number ' + number + ' does not exist';
}
```

The [`GROUP AGGR` operator](Grouping_GROUP.md) automatically adds a constraint on the uniqueness of the number. If you try to save the same number to the database, you will get an error message.

Let's add an [event](Events.md) that will automatically number books by increasing the maximum number existing in the database.

```lsf
WHEN SET(Book b IS Book) AND NOT number(b) DO {
    number(b) <- (GROUP MAX number(Book bb)) (+) 1;
}
```

The event will be triggered at the moment of saving a book to the database in the same transaction.

In some situations, you may need to apply different numbering for the same object. For this purpose, you can add a special `Numerator` class.

```lsf
CLASS Numerator 'Numerator';
name 'Name' = DATA ISTRING[50] (Numerator) IN id;

value = DATA INTEGER (Numerator);
```

The `value` property will store the current value of the numerator that will be written to the number of the necessary object. To achieve this, a reference to a particular numerator is set for an object (for example, an order). If such a reference is specified at the time of object creation, you need to automatically assign the numerator's current value increased by one to the order number.

```lsf
CLASS Order 'Order';
number 'Number' = DATA INTEGER (Order) IN id;

numerator 'Numerator' = DATA Numerator (Order);
WHEN CHANGED(numerator(Order o)) AND NOT CHANGED(number(o)) DO {
    number(o) <- value(numerator(o));
    value (Numerator n) <- value(n) (+) 1 WHERE n == numerator(o);
}
```

The event conditions check if the number has been changed in order to avoid changing it if the user specified it manually (or if it was assigned during import).

An important difference between the numerator and "assigning the maximum value plus one" is the support of the simultaneous object creation. In this case, if two users simultaneously create objects, the last started saving user will get a message about number duplication and will need to manually re-save it. Changes made in all events in this way will be "rolled back" and re-saving will generate a new number. If you use a numerator, the last user's transaction will get a CONFLICT UPDATE on the `value` field for the numerator (since both transactions change the field of the same row in the database). The system will then automatically roll back the transaction and all the changes made in the event and will start processing it again without the user's involvement. This way, the user will only experience slower data saving (up two times slower), but no additional actions will be required.

You can define a default numerator with property without arguments so that the user does not have to select a numerator every time. After that, you can create an event that will automatically set the numerator if the user doesn't choose it manually.

```lsf
defaultNumerator 'Default numerator' = DATA Numerator();

WHEN SET(Order o IS Order) AND NOT CHANGED(numerator(o)) DO
    numerator(o) <- defaultNumerator();
```
