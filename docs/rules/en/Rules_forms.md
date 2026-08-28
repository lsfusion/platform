---
slug: "/Rules_forms"
title: 'Rules: forms'
---

## Form rules

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

## Flow rules (`WAIT`, `NOWAIT`)

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
