---
slug: "/Utils_Hierarchy"
title: 'Hierarchy'
---

The `Hierarchy` module gives a class a hierarchy by parent: an object refers by a `parent[class]` property to an object of the same class, and the module builds on that reference the ancestor flag, the level, the number of descendants, the leaf flag, and the full name from the root. The `level[class, class]` property is computed by [recursion](Recursion_RECURSION.md); the remaining properties are derived from it or directly from `parent[class]`, where needed by [grouping](Grouping_GROUP.md) or [partitioning](Partitioning_sorting_PARTITION_..._ORDER.md); the [materialized](Materializations.md) properties are marked in the table below. It is pulled in via `REQUIRE Hierarchy`.

### Attaching to a class

The hierarchy is added by one of three metacodes; they differ in where the `parent[class]` property comes from. The class must have a `name[class]` property: `nameParent[class]` and `canonicalName[class]` are computed from it.

| Metacode | The `parent[class]` property |
|---|---|
| `@defineHierarchy(object)`, `@defineHierarchy(object, class)` | Declared as a data property of the class; when an object is created with automatic setting enabled, it receives the current object of the same class, if there is one |
| `@defineHierarchyAbstract(object)`, `@defineHierarchyAbstract(object, class)` | Declared as an abstract materialized property of the class; the implementation is added by the application module |
| `@defineHierarchyCustom(object, class)` | Not declared: the `parent[class]` already present in the class is used |

In the one-argument form the class is obtained from `object` by capitalizing its first letter: `@defineHierarchy(itemGroup)` is the same as `@defineHierarchy(itemGroup, ItemGroup)`. The `object` argument also becomes the name of the "ancestor by level" property and the base of the names derived from it (see the table below).

### Hierarchy properties

All three metacodes add the same set of properties to the class (`class` is the object class, `object` is the first argument of the metacode; for two-parameter properties the first parameter is the descendant, the second is the presumed ancestor).

| Property | Value |
|---|---|
| `nameParent[class]` | The parent's `name[class]`; placed in the `base` group |
| `level[class, class]` | Not `NULL` if the second object is an ancestor of the first or the object itself; the value (of the `LONG` class) is `2` to the power of the distance between them: `1` for the object itself, `2` for the parent, `4` for the parent's parent, and so on. Under the default `CYCLES NO` policy the distance cannot exceed `30`: a longer chain of ancestors violates the recursion constraint, and the save is canceled. Materialized |
| `isParent[class, class]` | `TRUE` wherever `level[class, class]` is not `NULL`: the "ancestor or the object itself" flag |
| `object[class, LONG]` | The ancestor of the first argument for which `level[class, class]` equals the second argument (`1` — the object itself, `2` — the parent, `4` — the parent's parent); the property name is the `object` argument, for example `itemGroup[ItemGroup, LONG]` |
| `level[class]` | The level of the object — the number of its ancestors including itself (`1` for a root). Materialized |
| `levelRoot[class, class]` | The number of the ancestor in the chain from the root to the object: `1` for the root, the object's `level[class]` for the object itself. Materialized |
| `objectRoot[class, INTEGER]` | The ancestor of the first argument standing at the position given by the second argument counted from the root (`1` — the root); the name is the `object` argument with the `Root` suffix, for example `itemGroupRoot[ItemGroup, INTEGER]` |
| `childNumber[class]` | The number of direct descendants; `NULL` when there are none. Materialized |
| `descendantNumber[class]` | The number of objects for which the given one is an ancestor, including itself. Materialized |
| `isLeaf[class]` | `TRUE` if there are no direct descendants. Materialized |
| `isParentLeaf[class, class]` | `isParent[class, class]` provided that the first object is a leaf |
| `canonicalName[class]` | The full name: the `name[class]` of the ancestors from the root down to the object itself, joined with ` / ` (`ISTRING[255]`). Materialized |

### Additional metacodes

The additional metacodes rely on the hierarchy properties already added: `@defineHierarchyPlain` uses `objectRoot[class, INTEGER]`, `@defineHierarchyFilter` uses `isParent[class, class]` and `name[class]`; they are applied after one of the three metacodes above.

| Metacode | What it adds |
|---|---|
| `@defineHierarchyPlain(object)` | Materialized properties `object1[class]` … `object6[class]` — the ancestors of the object at positions `1` to `6` from the root, that is `objectRoot[class, INTEGER]` with a fixed position; convenient for flat reports and columns like "category / direction / group". The class is obtained from `object` by capitalizing its first letter |
| `@defineHierarchyFilter(object, class, property, caption)` | A local property `filter###property###object[]` of the `STRING[255]` class with the caption `caption` (for example, `filterNameItemGroup[]` for `@defineHierarchyFilter(ItemGroup, ItemGroup, name, ...)`; the `property` argument affects only this name) and two counters over the object's subtree — `inFilterName[class]` and `inIFilterName[class]`: the number of objects of the subtree, including the object itself, whose `name[class]` contains the entered string, case-sensitively and case-insensitively (`NULL` when there are none); they are used to filter a tree by a substring of the name |

### Language

- [`META` statement](../language/META_statement.md) — declaring a metacode and applying it with the `@` statement, by which the hierarchy is attached to a class.
- [Module header](../language/Module_header.md) — `REQUIRE Hierarchy`, which pulls the module in.

### Examples

```lsf
CLASS Category 'Category';
name 'Name' = DATA ISTRING[50] (Category);

@defineHierarchy(category); // parent, level, isParent, canonicalName, childNumber, isLeaf, ...

// books of a category and of all its subcategories
CLASS Book 'Book';
category = DATA Category (Book);
inCategory (Book b, Category c) = isParent(category(b), c);

// the root of the category's branch and its parent
root (Category c) = categoryRoot(c, 1);
parentByLevel (Category c) = category(c, 2l);

FORM categories 'Categories'
    OBJECTS c = Category
    PROPERTIES(c) name, canonicalName, level, childNumber, isLeaf
;
```
