---
slug: "/System_UserEvents"
title: 'UserEvents'
---

`UserEvents` is a [system module](System_modules.md) that gives an action running on an open form programmatic read and write access to the form's filters and orders by group-object name and property name. It is pulled in via `REQUIRE UserEvents`. The two public actions reach the live filter structure of a group object, change a single property's filter value, and write it back, so an action can set or read a filter without the user touching the filter dialog.

### Local staging

The module declares local buffers that hold a copy of a group object's orders and filters while an action edits them, together with the two forms that move that copy between the buffers and the running form.

| Property                                  | What it holds                                                                 |
|-------------------------------------------|--------------------------------------------------------------------------------|
| `orders[]`                                | the orders of a group object as `JSON`                                          |
| `filters[]`                               | the filters of a group object as `JSON`                                         |
| `filterGroups[]`                          | the filter-group count as `INTEGER`                                             |
| `filtersProperty[]`                       | the filter value read back for a property, as `STRING`                          |
| `property[INTEGER]` / `desc[INTEGER]`     | per-row order: the property name and the descending flag, keyed by row number   |
| `property[INTEGER]` / `compare[INTEGER]` / `negation[INTEGER]` / `or[INTEGER]` / `value[INTEGER]` | per-row filter: the property name, the comparison, the negation flag, the OR flag, and the filtered value, keyed by row number |

The `orders` form exposes the order rows (one `INTEGER` object, with `property` and `desc`), and the `filters` form exposes the filter rows (one `INTEGER` object, with `property`, `negation`, `compare`, `value`, `or`). Each form keeps only the rows whose `property` is filled. These forms are the staging structures that the import and export steps read from and write to.

### Setting a filter

`filterProperty[STRING groupObject, STRING property, STRING value]` sets the filter on the named group object so that the named property is filtered to the given value, adding the filter when the property is not yet filtered and replacing its value when it is. It runs as a round-trip over the staging structure:

1. read the current filters of the group object — `EVAL ACTION 'FILTERS ' + groupObject + ';'` fills the `filters` form's buffers from the form's live filters for that group object;
2. import them into the staging structure with `IMPORT filters FROM filters()`, giving one row per current filter;
3. modify the structure — for the row whose `property` equals the given property, set `value[INTEGER]` to the new value; when no such row exists, append a new row (at the index one past the largest filled row, or `0` when none are filled) carrying the property name and the value;
4. write the structure back with `EXPORT filters TO filters`, refilling the `filters` form from the edited rows;
5. re-apply — `EVAL ACTION 'FILTER ' + groupObject + ';'` applies the staged filters back to the group object on the running form.

### Reading a filter

`filtersProperty[STRING groupObject, STRING property]` reads back the filter value currently applied to the named property of the group object. It performs the read and import steps of the round-trip — `EVAL ACTION 'FILTERS ' + groupObject + ';'` followed by `IMPORT filters FROM filters()` — then, for the row whose `property` equals the given property, writes that row's `value[INTEGER]` into `filtersProperty[]`, where the caller reads the result.

### Language

- [`EVAL` operator](../language/EVAL_operator.md) — runs the `FILTERS` / `FILTER` form action that reads and re-applies a group object's filters.
- [`IMPORT` operator](../language/IMPORT_operator.md) — reads the `filters` form into the staging structure.
- [`EXPORT` operator](../language/EXPORT_operator.md) — writes the staging structure back into the `filters` form.

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Eval (EVAL)`](Eval_EVAL.md) — running generated lsFusion code, used here to read and re-apply form filters.
- [`Forms`](Forms.md) — what a form is, and its group objects, filters, and orders.
- [`Reflection`](System_Reflection.md) — metadata about forms, group objects, and properties addressed here by name.
