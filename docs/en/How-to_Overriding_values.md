---
title: 'How-to: Overriding values'
---

Often there are tasks when it is necessary to give the user opportunity to enter the value of some attribute for some general object and then override it for some specific object.

Let's take a look when you need to define a trade mark-up for a book. At the same time, we have the logic of categories defined. These categories form a tree by specifying a parent for each category. The user should be able to set a mark-up for any product and category at any level.

Let's define the logic of categories and the [data property](Data_properties_DATA.md) of this category's markup.

```lsf
CLASS Category 'Category';
name 'Name' = DATA ISTRING[50] (Category) IN id;

parent 'Parent' = DATA Category (Category) AUTOSET;
nameParent 'Parent' (Category c) = name(parent(c)) IN id;

markup 'Markup' = DATA NUMERIC[8,2] (Category);
```

The [`RECURSION` operator](RECURSION_operator.md) is used to calculate the `level` property for given two categories. This property will be equal to two to the power of N, where N is the distance between these categories.

```lsf
level 'Level' (Category child, Category parent) = RECURSION 1l IF child IS Category AND parent == child
                                                                 STEP 2l IF parent == parent($parent) MATERIALIZED;
```

Let's create a property that will determine the corresponding parent by category and level.

```lsf
parent (Category child, LONG level) = GROUP MAX Category parent
                                                       BY level(child, parent);
```

Let's find the minimal level of a category for which the a mark-up is defined. It will also be the level of "closest upper" category with a set mark-up.

```lsf
nearestGroupLevel 'The closest level for which the markup is set' (Category child) =
    GROUP MIN level(child, Category parent) IF markup(parent);
```

We use this level to determine the category and its mark-up.

```lsf
nearestGroup 'The closest group for which the markup is set' (Category category) = parent(category, nearestGroupLevel(category));

overMarkup 'Overidden markup' (Category category) = markup(nearestGroup(category));
```

Thus, the `overMarkup` property will contain the required markup value for this category with its hierarchy taken into account.

Let's now define the logic for books. Each of them is associated with a certain category that may be located at any level of the category hierarchy.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[100] (Book) IN id;

category 'Category' = DATA Category (Book) AUTOSET;
nameCategory 'Category' (Book b) = name(category(b)) IN id;
```

Let's define the data property of a product markup. After that, let's construct an overridden property that will return a product markup if it's not `NULL` and a previously created property with a category markup.

```lsf
markup 'Product markup' = DATA NUMERIC[8,2] (Book);

overMarkup 'Overidden markup' (Book b) = OVERRIDE markup(b), overMarkup(category(b));
```

Finally, let's design a form that will allow the user to enter the markup for categories and products at the same time. Let's output both the data and the overridden markup for the category and the product. Note that changes in overridden properties on the form will be displayed immediately, but saved only when the corresponding button is clicked.

```lsf
markup 'Product markup' = DATA NUMERIC[8,2] (Book);

overMarkup 'Overidden markup' (Book b) = OVERRIDE markup(b), overMarkup(category(b));
```

As a result, the form with the filled data will look like this:

![](images/How-to_Overriding_values.png)
