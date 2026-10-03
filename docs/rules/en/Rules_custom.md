---
slug: "/Rules_custom"
title: 'Rules: custom views'
---

- [Choosing a custom view](#choosing-a-custom-view)
- [Data path](#data-path)
- [Projection and editing](#projection-and-editing)
- [Container sizing](#container-sizing)
- [Live data](#live-data)
- [Server calls](#server-calls)
- [Client code](#client-code)

## Choosing a custom view

1. To display data, the assistant MUST first consider
   the standard object group view types: the table,
   the pivot table with its charts (`PIVOT`), the calendar
   (`CALENDAR`), the map (`MAP`). A custom view is used when
   something beyond a simple table, a simple calendar, a
   simple chart, or a pivot table is needed: a kanban board,
   a timetable, a card feed, a seating chart, drag-and-drop
   the standard views do not provide, a nonstandard layout
   or interactivity.

2. A custom view is one of three things, and the assistant
   MUST pick by what it draws:
   - the `CUSTOM` option of a form property — a render
     function over ONE value of ONE object (a custom editor,
     a widget for a field);
   - the `CUSTOM` view of an object group — a JS
     `render` / `update` pair over the rows of ONE group,
     with the controller's `diff`, view filters and page size;
   - a `DESIGN` container with the `custom` attribute — a
     React component (a name matching `[A-Z][A-Za-z0-9_$]*`)
     that draws the container's whole subtree from the
     `props.data` projection, or an HTML template that only
     places platform-drawn children.

   All three are web client only: the desktop client renders
   the standard views and the container's regular subtree.
   Before creating a React view the assistant MUST retrieve
   the `How-to_Custom_React_views` documentation; before a
   classic view, `How-to_Custom_components_objects` or
   `How-to_Custom_components_properties`. These rules do not
   describe the component API and MUST NOT be used instead
   of those articles.

## Data path

1. A custom view of a property — the `CUSTOM` option of a
   form property, or a `.value` the component reads from
   `props.data` — draws ONE value of ONE object. A set of
   objects MUST reach the client as an object group — the
   table (`GRID`), a `CUSTOM` object-group view, or the group's
   box nested inside a React container (`props.data.<g>.list`)
   — and MUST NOT be packed into a single `JSON` / `JSONTEXT`
   property built by `JSON FROM` over a class. Such a property
   is one value: it is re-read and resent whole whenever any
   of the data it is built from changes, its size is unbounded,
   and the platform cannot page it (`PAGESIZE`,
   `useSeekOnScroll`), filter it on the server (form `FILTERS`
   and `FILTERGROUP`, the `setBooleanViewFilter` /
   `setDateIntervalViewFilter` filters of a `CUSTOM` view) or
   apply the per-property security policy to its fields — so
   every column of every row reaches every user allowed to
   view that one property, whatever the policy on the
   properties it is built from.

   A screen made of several independent lists is split into
   several object groups and, where the standard views do not
   fit, into several `custom` containers, each over its own
   group with its own component — not one component over one
   payload of the whole database. A group whose rows a board
   or a calendar shows all at once gets `PAGESIZE 0`; a list
   whose number of rows is unbounded keeps the page and follows
   the scroll with `useSeekOnScroll`.

   A `JSON FROM` over objects MAY be handed to a view only as
   a small, bounded payload — a chart series, a summary, the
   last N items, the axis of a board — and then it MUST be
   bounded explicitly: an explicit `WHERE`,
   `ORDER ..., <object>` with `TOP n`, only the fields the
   view renders, long text cast to `STRING[n]`, no email,
   phone, free text or other personal data the view does not
   show. It MUST NOT be the primary data path of a list the
   user browses.

2. Values MUST reach a view as separate form properties, and
   changes MUST go back through the controller property by
   property: a row of a group carries each property under its
   integration name, a property view works with its one value
   (`update(..., value)` / `controller.change(value)`), and an
   action drawn on the form is run through its member,
   `controller.<group>.<action>.exec(row)` (a React
   component), or with `controller.form.exec('<action>')` and
   the action listed in the form's `CUSTOMS` (a property view,
   whose form controller has no members). The assistant MUST
   NOT use JSON as a
   transport in either direction — neither `JSON FROM` to
   collect several values into one column, nor a JSON
   assembled in the component and taken apart on the server
   with `INPUT f = JSON DO IMPORT JSON FROM f` to dispatch
   events. When a property view has to show several values,
   the view is the wrong kind: the assistant switches to a
   `CUSTOM` object-group view or a React container.

## Projection and editing

1. In a container with the `custom` attribute the platform
   does not show property values but hands them to the
   component in the `props.data` projection, where an object
   value is the numeric identifier. Object links
   (`assignedTo(s)`, `customer(o)`) are needed there in exactly
   that form: by the identifier the view lays rows out into
   cells, matches a row with a row of another group — in a
   single-object group of a custom class a row's `row.key`
   numerically equals that object's identifier — and writes
   the link back through the property's member
   (`.change(id, row)`). So in such a
   container the assistant MAY add an object-valued property
   to the form as is — for the component's logic, not for
   display. If the link is shown to the user, its caption MUST
   be added as a separate entry — the caption composition of
   the `view` rules (`captionStatus 'Status' = caption(status(p))`).
   A property marked `LSF` is drawn by the platform, and the
   ban on showing identifiers applies to it as usual.

2. In a container with the `custom` attribute the component
   draws the values and decides itself what is edited: an edit
   goes through the controller (the property's member,
   `.change(value[, row])`). A static
   `READONLY` mark does not reach the `props.data` projection
   — only the data-dependent `readOnly` from `READONLYIF`
   arrives there — but the server refuses a change to a marked
   property, and an edit through the controller is silently
   not performed. So there `READONLY` marks the properties the
   view does not change, and a property the view changes
   through the controller MUST NOT carry `READONLY`. A
   property marked `LSF` is drawn by the platform with its own
   editor, and the `READONLY` rule of the `view` rules applies
   to it as usual.

3. A change made through the controller is made in the form's
   change session, like an edit in the standard table: the
   view sees the new value in the next projection at once,
   and it reaches the database when the form applies. A result
   the user must keep seeing MUST be put into a form property,
   not kept only in DOM nodes or component state: a classic
   `update` that rebuilds its elements from the new `list`
   instead of applying the change incrementally through
   `controller.diff` discards it on the next change of the
   list.

## Container sizing

1. For a form with a `custom` container opened as a window
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

2. A tab (`WINDOW`) is sized by the forms window, but the
   base size bounds the height of the container itself there
   too: the assistant SHOULD give a base height (`height`)
   that fits on the form to a container whose component draws
   more than fits on the form — a card feed, a view with
   `useSeekOnScroll`: without it the container stretches the
   form, with it the container expands into the free space by
   its extension coefficient (`fill`) and scrolls its content
   inside.

## Live data

1. Data a form has to keep current — quotes, a queue, a
   monitor — is refreshed by the form's `SCHEDULE` event
   (`EVENTS ON SCHEDULE PERIOD n formRefresh()`, the `view`
   rules), and the platform delivers what changed through
   `props.data` like any other change. The assistant MUST NOT
   poll the server from a custom React component with a timer
   of its own through an action's member
   (`controller.<action>.exec()`):
   an action drawn without `NOWAIT` goes as a synchronous
   request that blocks input to the whole web client on every
   tick, and the component's timer keeps polling while the
   form is hidden — a background tab, or a form the
   forms-window component places nowhere. The scheduled event
   goes as an asynchronous request and runs only while the
   form is shown on screen. `formRefresh[]` re-reads the whole
   form on every run, so such a form is kept small: there is
   no refresh of one object group — `forceUpdate[STRING]`
   only applies the pending update of a group in manual
   update mode.

## Server calls

1. An action a view needs SHOULD be drawn on the form and run
   through the form-edit channel, which is not gated: in a
   React component through the action's member,
   `controller.<group>.<action>.exec(row)`, which passes
   nothing beyond the row; in a classic object-group view
   through `changeProperty` with the target row, which passes
   beyond it only the value an action with a value request
   asks for (`changeProperty(action, row, value)`). As soon as
   more is needed — a value from a React component, several
   values, an array — the call is the form controller's
   `exec(...)` / `change(...)`
   (`props.controller` in a React component, `controller.form`
   in a classic view), and the target MUST be listed in the
   form's `CUSTOMS` clause.

2. The assistant MUST prefer `CUSTOMS` over `@@api` for a call
   a custom view needs: `CUSTOMS` scopes access to this form
   ("the user can open this form" plus the explicit listing),
   while `@@api` also exposes the action or property over the
   external HTTP API. Something is marked `@@api` only when it
   is genuinely part of that API. `CUSTOMS` does not restrict
   the argument values the component passes, so every listed
   entry MUST stay safe for an arbitrary id and arbitrary
   values. `eval` / `evalAction` run arbitrary script and stay
   under the gate.

## Client code

1. In a project with the build, browser code is placed under
   `src/main/web` of the logic module (`.js`, `.jsx`, `.ts`,
   `.tsx`), each file outside the `lib` subfolder is bundled
   into an auto-loaded bundle of its own — `lib` holds shared
   helpers reached only through imports — and a component is
   exposed as a **named export** — the name the
   `DESIGN` `custom = '...'` / `CUSTOM '...'` refers to. A
   name on `window` is only a fallback for existing scripts.
   The assistant MUST NOT bundle a copy of React or ReactDOM:
   the platform provides them, and `react` / `react-dom`
   imports resolve to its build.

2. Without the build, a file goes under
   `src/main/resources/web`: into `web/init` to be auto-loaded
   — then it MUST be load-order-independent, registering at
   load and reaching other libraries only at render or on an
   event — or outside it, listed in `onWebClientInit` with an
   integer order when order matters (a library that must load
   before the component using it) or the load is conditional.
   Such a file cannot `import` local modules. `.jsx` is
   transformed when served, and a `.js` file writes
   `React.createElement` against `window.React`.

3. Styles: CSS modules (`Component.module.css`) for a
   component's own styles, inline `style={{ ... }}` for values
   computed from data, and a plain global stylesheet — with a
   namespace prefix on its class names — only for vendor or
   deliberately global styles. The compiled `.css` of a bundle
   is auto-loaded and MUST NOT be registered in
   `onWebClientInit` a second time.
