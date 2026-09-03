---
slug: "/Rules_view"
title: 'Rules: view logic'
---

- [Forms](#forms)
- [Form design](#form-design)
- [Navigator](#navigator)
- [Reports](#reports)
- [Internationalization](#internationalization)

## Forms

### Form rules

1. To place several objects in one table at once,
   the assistant SHOULD combine them into one object group
   using brackets.

2. In a `FORM ... ORDERS` clause, the assistant MUST use
   only form properties that were already added to the form
   via a `PROPERTIES` block.

   In `ORDERS`, the assistant MUST specify either:
   - the form property name with its parameters, if no explicit alias was given
   - the explicit form property alias, if such an alias was specified

   Raw expressions, objects, or properties not added to the form
   MUST NOT be placed into `ORDERS`.

3. The assistant MUST NOT use `INPUT` inside actions
   added directly to a form through `PROPERTIES`,
   unless that action is used in an `ON CHANGE` handler:
   a built-in-class value is entered in the editor of the
   property whose change is being handled, so outside
   a change handler there is nowhere to render it.
   The exceptions are file and color values, which are
   entered through a separate dialog and MAY be requested
   from any form action.

   To request values from a button instead, the assistant
   SHOULD either place the input properties on the form
   itself (data properties, including local ones, in a
   panel) and read them in the action, or open a dialog
   form returning the entered values (`DIALOG` with
   `INPUT`-marked objects).

4. The assistant MUST NOT display internal object identifiers
   on a form, including through object-valued properties:
   an object value is displayed as its internal identifier —
   a number that tells the user nothing.

   Meaningful primitive or derived primitive / text properties
   MUST be exposed instead.

   The most common case is a link to a static object of an
   enumeration class (`status = DATA Status (Project)`): the
   platform does NOT substitute the static object's caption
   by itself. The form exposes the caption composition —
   `captionStatus 'Status' = caption(status(p))`: a write
   through the composition goes into the link, not into the
   static object's caption (see rule 7), and with a small
   number of options, as enumerations have, the web client
   shows it as a selection element (button group / list /
   dropdown — by the number of options and the length of
   the captions).

   The rule concerns what the user sees. In a container with
   the `custom` attribute (a custom view on a React component,
   form design rule 8) the platform does not show property
   values but hands them to the component in the `props.data`
   projection, where an object value is the numeric identifier.
   Object links (`assignedTo(s)`, `customer(o)`) are needed there
   in exactly that form: by the identifier the view lays rows
   out into cells, matches a row with a row of another group —
   in a single-object group of a custom class a row's `row.key`
   numerically equals that object's identifier — and writes the
   link back through `changeProperty`. So in such a container
   the assistant MAY add an object-valued property to the form
   as is — for the component's logic, not for display; if the
   link is shown to the user, its caption MUST be added as a
   separate entry — the caption composition, as above. A property
   marked `LSF` is drawn by the platform, and the rule applies to
   it as usual.

5. A `PANEL` object of a user-defined class
   is NOT user-selectable by default.

   If such an object is meant to be chosen by the user
   (for example, a filter parameter shown on the form),
   the assistant MUST mark a displayed property of that object
   with `SELECTOR` in the `PROPERTIES` block.

   Without `SELECTOR`, the panel cell does not open
   a selection dialog and the object cannot be changed.
   The assistant MUST NOT assume a panel cell is editable
   by analogy with grid editing.

6. In a form `PROPERTIES` block, the parameter style on the
    property or action being added to the form MUST match
    the block header:
    - With a common-parameter header
      `PROPERTIES(p1, ..., pN)`, each entry MUST be specified
      by its ID only — the common parameters are bound
      implicitly. Writing `propName(p1, ..., pN)` after the
      ID is a parse error.
    - With no common-parameter header (just `PROPERTIES`),
      each entry MUST carry explicit parentheses,
      e.g. `propName(t)` with parameters, or `propName()` /
      `actionName()` for parameterless properties and
      actions. Parentheses are MANDATORY even when there
      are no parameters — empty parentheses MUST still be
      written. Writing the bare name without parentheses
      is a parse error.
    - Empty parentheses right after the keyword
      (`PROPERTIES()`) are a common-parameter header, not
      its absence: the entries of such a block are bare IDs
      (`PROPERTIES() total, newCustomer`), and only
      parameterless properties and actions can be added
      this way. An entry with parentheses in it is the same
      parse error as in any header block
      (`mismatched input '(' expecting ';'`), and the error
      does not point at the cause. The equivalent without
      a header is `PROPERTIES total(), newCustomer()`.

    The assistant MUST NOT mix the two styles in one block,
    and MUST NOT repeat the common parameters after the
    property name when a common-parameter header is in use.

    This rule applies only to the entry being added to the
    form. Argument lists inside option clauses such as
    `ON CHANGE actionName(...)`, `READONLYIF expr`,
    `BACKGROUND expr`, etc. are regular action calls /
    expressions and ALWAYS use explicit parameters,
    regardless of the block header.

    An entry whose expression carries a comma not enclosed in
    brackets of its own needs a form this block accepts: with
    no common-parameter header, write it as `alias = (expr)`,
    since a bare `(expr)` is a parse error; with
    `PROPERTIES(o)`, an entry is a property usage even after
    `alias =`, so no expression is accepted at all and the
    only remedy is a named property added by its ID.

7. The default `CHANGE` handling of a property shown on a form
    is derived not from what its expression looks like, but from
    the property's write path: changeable properties are data
    properties, the selection operator, and compositions of
    changeable properties — a write is passed through the
    composition into the underlying changeable property. The
    write path is not visible at the usage site, so the assistant
    MUST NOT assume that a computed-looking property is
    non-editable.

    Outwardly similar entries are edited differently:
    - a composition through an object link (`name(customer(o))`) —
      the user is offered a choice of the linked object,
      and the link (`customer(o)`) is written;
      this is the usual way of entering data;
    - an attribute of the row object itself (`name(c)`) — the
      entered value is written in place, that is, the object
      is renamed;
    - a property of a static object (`caption(st)`) — the write
      goes into the stored caption of the static object and
      lasts until the next synchronization of the database with
      the code (normally at server startup), which restores
      the caption from the code.

    Every property displayed for reading only MUST be explicitly
    marked `READONLY` — in all form contexts: not only in the
    grids of list forms, but also in panels, dialogs, dashboards,
    and charts. When the whole block is displayed for reading
    only, the block SHOULD be marked as a whole
    (`PROPERTIES(o) READONLY ...`) rather than every entry;
    marking an individual entry remains for blocks where
    some entries are meant for input.
    In browsing and selection contexts every entry carries an
    explicit `READONLY` except the ones deliberately meant
    for input. The explicit mark also documents intent
    for the code reader.

    In a container with the `custom` attribute (form design
    rule 8) the component draws the values and decides itself
    what is edited: an edit goes through the controller
    (`changeProperty`). A static `READONLY` mark does not reach
    the `props.data` projection — only the data-dependent
    `readOnly` from `READONLYIF` arrives there — but the server
    refuses a change to a marked property, and an edit through
    the controller is silently not performed. So there `READONLY`
    marks the properties the view does not change, and a
    property the view changes through the controller MUST NOT
    carry `READONLY`. A property marked `LSF` is drawn by the
    platform with its own editor, and the rule applies to it as
    usual.

### Flow rules (`WAIT`, `NOWAIT`)

1. With neither option given, the platform picks the mode
   itself: the operator works synchronously when the form
   opens in a modal location, when the form it is opened from
   is modal, or when the current session may still be used
   afterwards, and asynchronously otherwise.

   Two calls that look the same can therefore behave
   differently. Where the code after the call depends on the
   form having been closed, or must run without waiting for
   it, the assistant MUST say so explicitly — with `WAIT` or
   `NOWAIT` on `SHOW`, which is the only operator whose
   syntax takes them.

   `DIALOG` has no such option: it is synchronous whenever
   its result is consumed, and left to the same heuristic
   otherwise. When a dialog must block, the assistant makes
   it block by using what it returns.

2. In synchronous mode `DOCKED` is a tab that blocks the
   calling form, and from a form shown as a window such a
   tab is shown as a window. So `SHOW ... DOCKED` from a
   form shown as a window (`FLOAT`) without `NOWAIT` opens
   a window, not a tab: such a form is modal, so the default
   mode there is synchronous (rule 1). To open a tab from it,
   the assistant MUST specify `NOWAIT`.

## Form design

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

   For a form with such a container opened as a window
   (`FLOAT`; the default location for `DIALOG`), the assistant
   MUST give the container a base size — `size = (w, h)` or
   the separate `width` and `height` attributes: the
   window size is computed from the content at the moment of
   opening, when the component has not drawn anything yet, so
   without it a window with a single such container collapses
   to the caption and the system buttons, and the content
   drawn later pushes the OK / Close buttons past the window's
   edge. For a form with no tables and content of moderate
   height, the assistant MAY instead leave the container
   without a base size and set `size = (-1, -1)` on the main
   container of the form itself: the window is then not fixed
   and follows the content (details in `Form_design`).

9. `FALSE` is valid in the logical attributes of a `DESIGN`
   block — `defaultComponent`, `activated` and the like —
   because their values are literals, not expressions. The
   core rule that bans `FALSE` covers expressions only, and
   MUST NOT be applied here by rewriting it as `NULL`.

## Navigator

1. A folder whose children should appear only when the folder
   is selected MUST place those children in a different window
   than the folder itself (typically `WINDOW toolbar`). In a
   horizontal toolbar such as `System.root`, a folder that keeps
   its children in its own window cannot switch anything — they
   are shown flattened next to it and selecting the folder does
   nothing. A vertical toolbar instead renders same-window
   children as a nested group under the folder, so there the
   separate window is not required.

## Reports

1. Before designing or editing jrxml report templates, or reasoning
   about report structure or template naming, the assistant MUST
   retrieve the `Report_design` documentation; it MUST NOT rely on
   these rules as a template-format or layout reference.

2. When a form has no object groups independent of each other
   (all groups form a single dependency chain), only ONE jrxml
   template is created by default, named by the form's canonical name
   (namespace + form name, each `.` replaced by `_`) WITHOUT a
   postfix — a group's only child is merged into it.

   The merge is what produces the single template, so it is also what
   the developer can switch off: the `SUBREPORT` option on a child
   object group keeps that group out of its parent, and it then needs
   a template of its own with the `_<group>` postfix. The assistant
   MUST therefore read the object blocks of the form, not just its
   dependency shape: a linear chain with a `SUBREPORT` in it needs
   more than one template, and a missing one silently discards
   all of them (rule 3).

3. The assistant MUST name every template exactly: the top report
   by the canonical form name without a postfix, and each subreport
   by the canonical form name plus the `_<group>` postfix of its
   first non-empty object group. If even one template name is wrong
   (not found from the platform's point of view), the platform
   silently falls back to a fully automatic design for the WHOLE
   report, with no error in the logs — so a single mismatch silently
   discards all custom templates.

## Internationalization

1. The assistant MUST use `*ResourceBundle.properties` files
   for UI localization.

   The value inside `{...}` MUST be treated
   as the lookup key that lsFusion resolves
   according to the current locale.

2. The assistant MUST first determine
   whether reverse translation is used
   in the current project area.

   If it is used,
   the assistant MUST continue using it
   in that area
   and MUST follow the existing project policy.

   The assistant MUST keep id selection
   consistent with the established pattern
   already used there.

   The assistant MUST NOT introduce
   a new explicit id policy
   unless the user requests it.

3. Reverse translation means
   translating in the opposite direction
   of normal UI localization:
   not `key -> localized text`,
   but `localized text -> key`,
   and then, if needed, to another locale.

   If ids are not specified explicitly in code,
   this canonical value is the source-language text itself.
   It is what the platform LOOKS UP, not what it stores as
   the key: the entry stays `id = source text`, and the
   dictionary built for the lookup is the reversed one,
   `value -> id`. An assistant writing the bundle the other
   way round produces entries reverse translation never
   matches.

4. Reverse translation is turned on by the launch parameter
   that sets the language of lsf string literals
   (`logics.lsfStrLiteralsLanguage`).
   When it is active, ANY plain `'...'` literal
   in a localizable position —
   including a constant literal in any expression —
   that matches a ResourceBundle entry value
   is silently replaced at code parse time with its key `{id}`
   and is substituted in the current locale at runtime:
   `'position'` can become `'pozycja'`.
   Leading and trailing spaces take no part in the match and
   are kept around the substitution; a literal that is empty
   or made of spaces alone is never replaced.

   Therefore the assistant MUST write technical literals —
   JSON keys, URLs, formats, canonical names,
   external identifiers —
   as raw literals `r'...'`,
   which take part neither in localization
   nor in reverse translation.
   Plain `'...'` literals are meant
   for user-visible text.
