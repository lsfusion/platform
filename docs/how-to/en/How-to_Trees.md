---
slug: "/How-to_Trees"
title: 'How-to: Trees'
---

## Example 1

### Task

We have a set of books associated with certain categories.

```lsf
CLASS Category 'Category';
name 'Name' = DATA ISTRING[50] (Category);

CLASS Book 'Book';
name 'Name' = DATA ISTRING[50] (Book);
category 'Category' = DATA Category (Book);
```

We need to build a form with a tree, where the category is shown above and the product is shown below.

### Solution

```lsf
FORM books 'Books'
    TREE cb c = Category, b = Book
    PROPERTIES name(c), name(b)
    FILTERS category(b) == c
;
```

## Example 2

### Task

Similar to [**Example 1**](#example-1), except that each category has a parent in the hierarchy.

```lsf
parent 'Parent' = DATA Category (Category);
```

We need to build a form with a tree, where the categories will be displayed as hierarchy.

### Solution

```lsf
FORM categories 'Categories'
    TREE categories c = Category PARENT parent(c)
    PROPERTIES(c) name
;
```

## Example 3

### Task

Similar to [**Example 2**](#example-2).

We need to create a form with a category tree, so that the books that belong to the current category and all its subcategories are shown to the right.

### Solution

```lsf
isParent 'Is parent' (Category child, Category parent) = RECURSION 1 IF child IS Category AND parent == child
                                                                   STEP 1 IF parent == parent($parent) MATERIALIZED;

FORM categoryBooks 'Books by category'
    TREE categories c = Category PARENT parent(c)
    PROPERTIES(c) name

    OBJECTS b = Book
    PROPERTIES(b) name
    FILTERS isParent(category(b), c)
;

DESIGN categoryBooks {
    NEW pane FIRST {
        fill = 1;
        horizontal = TRUE;
        MOVE BOX(TREE categories);
        MOVE BOX(b);
    }
}
```

The `isParent[Category, Category]` property is not `NULL` when its second argument is an ancestor of the first or the same category (its numeric value is the number of paths between them, always `1` in a tree), so the filter selects the books of the current category and of all its descendants. The same set of properties for a hierarchy by `parent[Category]` — `isParent[Category, Category]`, `level[Category]`, `canonicalName[Category]` and others — is provided by the `@defineHierarchy` metacode of the [`Hierarchy`](../paradigm/Utils_Hierarchy.md) system module; here, where the `parent[Category]` property is already declared, it is attached by `@defineHierarchyCustom(category, Category)`, and `isParent[Category, Category]` then need not be declared by hand — the metacode creates it with the same meaning and the value `TRUE`.
