---
slug: "/Rules_design"
title: 'Rules: form design'
---

## Form design rules

1. These design rules do NOT cover the `DESIGN` layout
   model — the default container tree, the flexbox `fill` /
   alignment model, or the container idioms. They give only
   placement meta-advice.
   Before writing or modifying any `DESIGN`, the assistant MUST
   retrieve the `Form_design` documentation; it MUST NOT rely on
   these rules as if they described the layout model.

   The complete tables of the properties of components of every
   kind (containers, components of properties and actions on the
   form, toolbars, grids) live in the `DESIGN` statement
   documentation (`DESIGN_statement`). When setting a component
   property, the assistant MUST check its name and allowed values
   against those tables, and MUST NOT guess them by analogy.

2. The assistant SHOULD specify a `DESIGN`
   for all interactive forms containing more than four properties.

3. Exception:
   for a trivial form with only one or two objects in `GRID` mode
   and no other properties displayed in `PANEL` mode,
   omitting `DESIGN` is acceptable.

4. In `DESIGN`, the assistant SHOULD prefer moving `BOX(...)`
   containers for tables first.

   `GRID(...)` SHOULD be used only when absolutely necessary.

5. If possible, the assistant SHOULD avoid form designs
   with more than two tables stacked vertically
   and more than two tables placed horizontally.

6. Custom actions added to a grid form (status changes,
   document generation, bulk operations) MUST be given an
   explicit `TOOLBAR` view, e.g. `PROPERTIES(o) confirmDoc TOOLBAR`.
   Actions default to the `PANEL` view, so without `TOOLBAR` the
   custom button is drawn as a separate group below the table
   instead of in the grid toolbar next to the predefined
   `NEW` / `EDIT` / `DELETE`, to which the platform gives that
   same `TOOLBAR` view — so they share the `TOOLBAR` container
   with it, not the `TOOLBARSYSTEM` one, which is where a
   `MOVE` or `REMOVE` of the wrong container goes astray. The property / action views are
   `GRID`, `TOOLBAR`, `PANEL`, and `POPUP`.

7. A `TEXT`-typed property displayed as a grid column is
   rendered as a multi-line row four lines tall by default,
   degrading list density. Such columns commonly result
   from the standard string properties
   (`lpad[TEXT, INTEGER, TEXT]`,
   `substr[TEXT, INTEGER, INTEGER]`, `trim[TEXT]` and the
   rest): they, as a rule, return `TEXT` regardless of the
   argument classes, and concatenating a `TEXT` operand
   with a bounded string keeps `TEXT` as well.
   On list forms, the assistant
   SHOULD instead expose the value cast to `STRING[n]`.
   The cast entry follows the expression-entry rules:
   in a `PROPERTIES` block without a common-parameter
   header, with an explicit alias
   (`shortNote = STRING[100](note(o))`). A bare cast
   without an alias, like any expression inside a
   common-parameter header block `PROPERTIES(o)`, is a
   parse error — there, declare a named property with the
   cast and add it by its ID.

8. To display data, the assistant MUST first consider
   the standard object group view types: the table,
   the pivot table with its charts (`PIVOT`), the calendar
   (`CALENDAR`), the map (`MAP`). A custom view on a React
   component — a `DESIGN` container with the `custom`
   attribute; web client only, the desktop client renders
   the container's regular subtree — is used when something
   beyond a simple table, a simple calendar, a simple
   chart, or a pivot table is needed: a kanban board,
   a timetable, a card feed, a seating chart,
   drag-and-drop the standard views do not provide,
   a nonstandard layout or interactivity. Before creating
   such a view, the assistant MUST retrieve the
   `How-to_Custom_React_views` documentation.

9. `FALSE` is valid in the logical attributes of a `DESIGN`
   block — `defaultComponent`, `activated` and the like —
   because their values are literals, not expressions. The
   core rule that bans `FALSE` covers expressions only, and
   MUST NOT be applied here by rewriting it as `NULL`.
