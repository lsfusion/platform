---
title: 'Class change (CHANGECLASS, DELETE)'
---

The *class change* operator creates an [action](Actions.md) that assigns the given [class](Classes.md) to all objects where value of a particular [property](Properties.md) (*condition*) is not equal to `NULL`. The condition can be omitted, in which case it is considered to be equal to `TRUE`.  


:::info
The platform also has a builtin `changeClass` action with two parameters: the first defines the object for which you want to change the class, and the second defines an object of the new class. Since it is much more difficult to determine the possible values of a new class when using the builtin action than in the case of an operator (for which the new class is specified explicitly), it is recommended that you use the operator (and not the builtin action)
:::

If there is a non-`NULL` value of some [data property](Data_properties_DATA.md) for which the "changed" object is either its parameter or the value itserf, then this value is automatically changed to `NULL`.


:::info
This behavior is implemented by analogy with [computed](Calculated_events.md) and [simple](Simple_event.md) events.
:::

### Language

To declare an action that implements a change of object classes, use the [`CHANGECLASS` operator](CHANGECLASS_operator.md) or the [`DELETE` operator](DELETE_operator.md).

### Examples

```lsf
// deleting an object
deleteObject(obj)  { DELETE obj; }

// deleting all inactive products
CLASS Article;
active = DATA BOOLEAN (Article);
deleteInactiveArticles()  {
    // a local parameter a is added corresponding to the objects to be iterated over
    DELETE Article a WHERE a IS Article AND NOT active(a); 
}
```


```lsf
CLASS Document;
date = DATA DATE (Document);

CLASS ClosedDocument : Document;
// sets status to closed for all documents with a date older than 2 weeks
changeStatus()  {
    CHANGECLASS Document d TO ClosedDocument WHERE sum(date(d), 14) <= currentDate();
}
```
