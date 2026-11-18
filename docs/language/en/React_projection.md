---
slug: "/React_projection"
title: 'Form projection'
---

The form projection is the state of a form open in the [interactive view](../paradigm/Interactive_view.md), as a [React view](React_views.md) receives it: object groups, properties and design components, as JS objects.

`props.data` is the form projection — its own for each React custom container, holding only what that container draws or places, so a second React container on the form gets a separate one, and a [controller](React_view_controller.md) of its own with it.

A view reads only its own container's parts, and changes them through its controller's members; what another container draws, or the platform, it changes through the form's controller, by names ([across containers](Form_and_navigator_controllers.md#across-containers)). What containers share is the form itself — its current objects, sortings, filters and session — so a filter or a current object changed in one container shows in another, as it does in the standard client.

A page may be built of several React containers laid out by ordinary `DESIGN` containers, and another module can still add to such a page from `DESIGN`. A change re-renders only the containers whose projections changed. A container in an inactive tab or in a collapsed container is not read until it is shown, unless a part of the form that is shown reads the same ([A hidden view](React_form_behavior.md#hidden-view)).

### Entries, parts and nodes {#parts}

Every property, group and container the projection carries is an object in `data`: read a value as `.value` and its attributes as sibling fields, and read a list property's column attributes at `data.<group>.<prop>`. The entries a container carries for properties and components are design data: each of them is there from the start, and whether the form shows it now is its [`hidden`](#display-options) field, never whether it is there. `data` contains a group when the view draws a PART of that group, and holds exactly the parts it draws. A part of a group, or of a [tree](React_trees_sortings_selection.md#trees), is projected — its data, and the controller members that change it — in the React container where that part's component is, on the node of its group or tree there:

| Component | Part | Node |
| --- | --- | --- |
| The group's grid, `GRID(<group>)`, or its tree, `GRID(TREE <tree>)` | The rows and the current object, and — of a group not in a tree — the [sortings](React_trees_sortings_selection.md#orders) | `data.<group>` |
| A panel property — one the platform draws where the view places it | The property — one the platform draws as the [entry that labels it](React_platform_components.md#placing) (the entry of an action the platform draws only says whether the form shows it: its caption and image are its button's face) | `data.<group>`, or `data` itself for the empty group |
| What `FILTERS(<group>)` holds | The [user filters](React_filters_and_toolbar.md#filters) | `data.<group>` |
| `FILTERCONTROLS(<group>)` | The [filter controls](React_filters_and_toolbar.md#filter-controls) | `data.<group>` |
| `FILTERGROUP(<filterGroup>)` | A [filter group](React_filters_and_toolbar.md#filter-groups) | `data.<group>` |
| A property's filter, `FILTER(<property>)` | The [filter](React_filters_and_toolbar.md#property-filters)'s condition | `data.<group>`, under `filter` |
| `TOOLBARSYSTEM(<group>)` | The [toolbar](React_filters_and_toolbar.md#toolbar) | `data.<group>` |
| The tree, `GRID(TREE <tree>)` | The [tree](React_trees_sortings_selection.md#trees)'s current row, the opening of its current node, and its [sortings](React_trees_sortings_selection.md#orders) | `data.<tree>` |
| What `FILTERS(TREE <tree>)` holds, `FILTERCONTROLS(TREE <tree>)`, `TOOLBARSYSTEM(TREE <tree>)` | A [tree](React_trees_sortings_selection.md#trees)'s user filters, its [filter controls and toolbar](React_filters_and_toolbar.md#tree-toolbar) | `data.<tree>` |

A group in `PANEL` view has only its panel properties and its filter groups: no rows, sortings, user filters,
filter controls or toolbar — the platform shows none of them for it either.

A component's place is found by walking up from its container: a React custom container is the answer, and a container with a view of its own — `custom = NULL`, or a custom of its own — ends the walk, since that view draws everything under it. Whether the component itself has one decides only which entry it has there — its content, or the [entry that labels it](React_platform_components.md#placing) — never where. A view that draws the rows — the grid is inside the custom container, and neither it nor a container between them has a view of its own, so React renders them from `data` rather than the platform — gets `props.data.<g>` = `{ list, byKey, keys, options, orders, properties }` for that group object SID `g` — a group of a tree has no `orders`: the [tree](React_trees_sortings_selection.md#trees)'s sortings are its own node's —, where `list` is the array of rows in display order, `byKey` maps a row's key string to the same row object, and `keys` is the array of those key strings in the same order. `byKey` is keyed by application data, so it is built with NO prototype: a row whose key is the string `__proto__` is an ordinary entry in it, and `byKey.hasOwnProperty` and the other `Object.prototype` methods are not there to call — read it as `byKey[k]`, and test with `k in byKey` or `Object.keys(byKey)`. Wherever the view draws the rows, `list`, `byKey` and `keys` are all present: they are empty while the group has no rows — before its first rows arrive, say — so the view reads `props.data.<g>.list` directly without guarding a missing field. A grid (or a tree) the form hides with `SHOWIF` says so in [its own entry](#display-options), `data['GRID(g)'].hidden` — a tree's is `data['GRID(TREE t)']`, one for all its groups —, and its rows stay as the server last sent them, as the platform's own grid keeps them — the server may stop refreshing them while the grid is hidden — so draw them only while the grid is not hidden. Where it does not, the node has none of them, and no `options` or `orders` (the group's sortings, the order those rows are drawn in) either: there are no rows here for them to be about, and a panel property of the group may take one of those names. A group in `PANEL` view draws its rows nowhere — the server sends it none, only its current object —, so its node is such a one wherever its grid is, the container its grid is in included: its panel properties, and no `change(row)`. A group **no part of which** the view draws is absent from `props.data` altogether (`props.data.<g>` is `undefined`): a container merely standing beside a platform-drawn grid gets nothing, and neither does one that only frames it (`MOVE GRID(g) { custom = NULL; }` — there the platform draws the rows, so the container gets the [entry that labels what it places](React_platform_components.md#placing) and no rows; one that holds `FILTERS(g)` as well gets the group's [user filters](React_filters_and_toolbar.md#filters)). Asking [`<List>`](React_rendering.md#rendering-rows) for rows that are not there is a mistake in the view, and it throws: the view shows the reason in its place and in the console. A group's panel properties are members of the group node in the container each property itself sits in, keyed by integration SID. The top level of `props.data` is the node of the empty group (the form level): each property of the empty group is a member of `props.data` directly, under its integration SID. A list property's column attributes are a member of the group too, at `data.<g>.<prop>` — one entry per column — while its per-row value and cell attributes live on each row. Actions React draws are projected the same way as properties — an action drawn on a group is a field of each row (a list action) or of the group node (a panel action), an object of its attributes like a property, so `controller.<group>.<action>.exec(row)` runs it (a panel action's `.exec()` takes no row). Its `value` field is there too but carries nothing. An action the platform draws has only its entry, and the platform's button runs it. `options` is the group's own [display options](#display-options); `properties` names every PROPERTY entry this node carries, shown or not, in the form's order — not everything on the node, which also holds what the projection itself defines (`list`, `byKey`, `keys`, `options`, `orders`) and is known by name rather than discovered. The number of rows is `list.length`. Each row carries:

| Field | Meaning |
| --- | --- |
| `key` | A stable public id for the row — use it as the React key |
| `current` | Whether the row is the current one in its group (`true` or `false`) |
| `<integrationSID>` | Each list property, an object `{ value, ...cell attributes }` keyed by the property's integration SID — read its value as `.value` |
| `objects` | An opaque row handle the controller uses to address the row |
| `background`, `foreground`, `selected` | The row's own [display options](#display-options): its background and text colors and whether it is selected |

So a `MOVE` of a panel property is what decides which engine draws it: inside a custom container React draws it from `data`, outside one the platform draws it where it now stands and it leaves `data`.

A property's component is in one container only, so a form-level property two containers show is added twice, under two aliases, one in each container.

A container with a `custom` of its own inside what a React component draws — another React component, an HTML template, `''` — is not drawn by that component: it is drawn by its own view, and the outer component places it like any other [child the platform draws](React_platform_components.md#placing). So is a table, a toolbar, filter controls, a filter group or a property's filter with a [React component of its own](React_component_views.md#component-views).

A property grouped in columns (`COLUMNS`) cannot be projected: its values are addressed by a row-and-column key, and everything here — a row of `list`, an entry of `byKey`, a name on the controller — means one row. So a React container may not carry one — a column of the rows it draws, or a panel property or a property of the empty group standing in it: such a form is rejected when it is built, naming the property. Elsewhere on the form it stays as it is, and a React view simply has no name for it: the [controller](React_view_controller.md) carries what the projection carries, so what is not projected has no member. The same holds whenever a name does not single out one property — two properties of a projected group may share an integration name only when their entries land in different containers, and each view then means the one its own node carries. Within one container they may not, because one entry and one member cannot stand for two properties; nor anywhere, one drawn by the platform included, when a view draws a part that names a property of the group ([Reserved names](#reserved-names)). Give one of them an explicit `EXTID`. Declare an ordinary property if the view has to read those values.

`list` contains the rows read — up to three pages, not all rows of the group. The view type of a group rendered by a React container remains the table, and the group is read page by page, but since the table itself is not displayed, the page size is not adjusted to the visible rows — the server default page size (50 objects) applies, so `list` holds up to 150 rows. For a view that shows all rows of the group — a calendar, a board, a map — specify the `PAGESIZE 0` option (read all objects) or an explicit page size in the [`OBJECTS`](Object_blocks.md) block.

The number of rows read is `list.length`, not how many rows the group's filters admit, so under paged reading the projection alone does not give that number.

### Values {#values}

A property's `value` is converted to a JS value depending on the property's class:

| Property class | JS value |
| --- | --- |
| `BOOLEAN` | `true` / `false` (`false` instead of `NULL`) |
| `TBOOLEAN` | `true` / `false` / `null` |
| numeric classes | number |
| user classes | number — the object's internal id |
| date and time classes | `Date` |
| interval classes | string `<from>.<to>` |
| `COLOR` | string `#rrggbb` |
| `JSON` | parsed JSON value |
| file classes | string with a download link |
| images | string with the image address or HTML |
| other classes | string |

A string property whose integration SID is `image` is given as the HTML of the image its value names (an icon class, an application image file, or an HTTP(S) URL), as in other custom views. A `SELECT` property is given its options instead.

Except for `BOOLEAN`, a `NULL` value is converted to `null`.

An interval's bounds are milliseconds since 1970: a bound of an `INTERVAL[ZDATETIME]` is the moment itself, while the other interval classes keep their date and time as those of UTC — `new Date(from).getUTCDate()` is the day an `INTERVAL[DATE]` starts on. A value a view hands back — to a member's `change`, or as a condition's `value` — is read the way this table writes it, so a value taken from `data` goes back as it is — but for a file's: a link is not the file —, and a `COLOR` or an interval written any other way is refused. A date or a time goes back as a string too, the one a date input of the class has for its value, with no time zone: `'2026-11-15'` for a `DATE`, `'10:30'` for a `TIME`, `'2026-11-15T10:30'` for a `DATETIME` or a `ZDATETIME`. Any other string is refused, and so is a day its month does not have.

### Reserved names {#reserved-names}

The reservation follows what a row or a node carries:

| Where | Reserved |
| --- | --- |
| A row | `key`, `current`, `objects`, `background`, `foreground`, `selected` — on a row of a [tree](React_trees_sortings_selection.md#trees) also `parent`, `hasChildren`, `expanded`, `loading` |
| A group's node, wherever it is | `properties`, `change` |
| A group's node where its rows are drawn | `list`, `byKey`, `keys`, `options`, and `orders`, `viewFilters`, `selectAll`, `copy` and `paste` of a group not in a tree — on a group of a tree `expand`, `collapse`, `toggle`, `expandAll`, `collapseAll` |
| The node of a group in `LIST` view where `FILTERS(<group>)` is | `filters`, `filterProperties` |
| The node of a group in `LIST` view where `FILTERCONTROLS(<group>)` is | The names of the [filter controls](React_filters_and_toolbar.md#filter-controls): `addFilter`, `applyFilters`, `resetFilters`, `filtersPending`, `filtersManual` |
| The node of a group in `LIST` view where `TOOLBARSYSTEM(<group>)` is, unless the design hides it (`visible = FALSE`) | The names of all the [toolbar](React_filters_and_toolbar.md#toolbar)'s buttons, whatever the design's `show…` flags say: `manual`, `updatePending`, `update`, `filterControlsVisible`, `listView`, `listViews`, `getCount`, `getSum`, `export` |
| A group's node where `FILTERGROUP(<filterGroup>)` is | The [filter group](React_filters_and_toolbar.md#filter-groups)'s own name, as a property's |
| A group's node where a property's filter, `FILTER(<property>)`, is drawn | `filter` |
| The top level, where the node of the empty group is the controller itself as well | What the [controller](React_view_controller.md) carries beside its members: `form`, `batch` |

A tree's own `current` and `expandCurrent` and its sortings, where its rows are drawn, and its user filters, filter controls and toolbar write their names on [its node](React_trees_sortings_selection.md#trees). A container carrying just a panel property of the group has none of the names of the other parts, so a property called `list` is refused there by nothing. `__proto__` is reserved everywhere: assigning it does not add a field, it replaces the object's prototype. There is no `meta` object anywhere. A form whose projected integration SID takes a reserved name, or where two projected items claim the same name at one data level, is rejected with an explicit error when it is built. So is a form in which a group whose rows or user filters a view draws, or its toolbar with the sum, carries one name on two properties anywhere, one drawn by the platform included: a [sorting](React_trees_sortings_selection.md#orders), a [condition](React_filters_and_toolbar.md#filters) or the [toolbar](React_filters_and_toolbar.md#toolbar)'s `getSum` names the group's property by that name. Give it an explicit `EXTID`, or rename it. A property such a container draws or places has to have a name at all - the view reaches it by that name alone, its entry and its `<Platform>` alike: one declared `NOEXTID` is rejected the same way - give it an `EXTID`. A name that is empty or carries a dot is rejected too — a qualified name, `o.note`, is split at its last dot —, and so is a group of several objects with no name of its own, `OBJECTS (d = X, t = Y)`, wherever the view carries a part of it: its SID joins the objects' names with dots, so name it, `OBJECTS pair = (d = X, t = Y)`. A property or action the platform draws is named as any other, and all of this holds for it.

### Reading the projection {#reading}

The component reads the projection in one of two ways, and the choice is about who re-renders when data changes.

`props.data` is the whole snapshot. The component re-renders — with a fresh `data` — whenever anything in its scope changes, and renders everything it draws from the new snapshot. For a small view this is the whole story: no hooks, a plain function of the data, and [`OrderBoard`](React_views.md#the-component) is written this way. It is also how the scope is read as a whole — its top-level entries enumerated, the containers read. `useData()` with no selector returns the same snapshot.

`useData(selector)` subscribes the component to a slice: it re-renders only when the reference of `selector(data)` changes — so the selector must return something the projection already holds (a group node, a row, an entry, a value), never a new object or array built inside it, which would differ on every read and re-render without end. Because of structural sharing — an unchanged node, row or entry keeps its previous reference — this is what lets a component pay only for what it reads: a component subscribed to a group node (`useData(s => s.o)`) ignores the rest of the scope, a row component subscribed to its own row (`useData(s => s.o.byKey[rowKey])`) ignores the other rows. The root is the exception: the platform hands it a new `props.data` on every change, so it re-renders whatever it subscribes to, and what it renders below it skips the render only when it is memoized (`React.memo`) and is not handed `data`. `useController()` returns the same `controller` the root receives as a prop — its identity never changes for the life of the root, so it is safe in dependencies and closures, and passing it down as a prop is equally fine.

`useData` and `useController` read whatever root the component is mounted under, and know nothing about forms in particular. `Platform` and `usePlatform` place a view the platform draws itself, so they serve a form container and a [navigator window](React_windows.md#navigator-window) alike. The helpers built on the group and row shape of a form projection — `List`, `BucketScope`, `useBucket`, `Buckets`, `useSeekOnScroll` — are form-only: they read `data.<group>.list` / `.byKey` / `.keys`, which another kind of root does not have — and neither does a form view that draws no rows of that group.

### Display options {#display-options}

For every property drawn on a form the platform computes semantic options that determine how it is shown — its caption, image, background and text colors, whether it is read-only or disabled, its comment, the text shown in an empty cell, its tooltip. They come from the property's design and from the data-dependent options of the [properties and actions block](Properties_and_actions_block.md) (`HEADER`, `IMAGE`, `BACKGROUND`, `FOREGROUND`, `READONLYIF`, `OPTIONS` and the rest), so an option that depends on the data is recomputed per row. Native element classes and fonts are not projected: a React component owns its CSS. `background` and `foreground` are projected — they are `BACKGROUND` / `FOREGROUND` highlighting, a data-dependent business signal, not a theme the component's own CSS should own. For a property the React container draws itself, the semantic result is projected into `data` as sibling fields of the property's value, so the view does not have to recompute it.

The projection follows what an option describes — a whole column, one cell, or the row:

| Where | What it holds |
| --- | --- |
| `data.<g>.<integrationSID>` | For a property shown in the table — its column attributes, one entry for the whole column: `caption`, `image`, `footer`, `comment`, `tooltip`, `defaultValue`, and `hidden` while the form hides it. For a panel property of the group — the property's `value` and its attributes, read for the current object, and `hidden` while the form hides it; while the group has no current object the `value` is empty and the entry stays, as in the platform's own panel. For one the platform draws — [the entry that labels it](React_platform_components.md#placing), with no `value` |
| `data.<g>.list[i].<integrationSID>` | One cell of a table property — its `value` for that row and the cell attributes computed for that row: `readOnly`, `disabled`, `background`, `foreground` and the rest |
| `data.<g>.list[i]` | The row's own options, direct on the row: `background`, `foreground`, `selected` |
| `data.<g>` | `options` — the group's custom options, where the view draws its rows; `properties` — the property entries this node carries, shown or not |
| `data.<integrationSID>` | A property of the empty group — its `value`, its attributes, and `hidden` while the form hides it; one the platform draws — the entry that labels it, with no `value` |
| `data.<componentSID>` | The entry of every other component this view draws or places — a container, declared with `NEW` or generated like `PANEL(o)` or `TOOLBAR(o)`; a grid, like `GRID(o)`, or a tree, like `GRID(TREE t)`; a toolbar, like `TOOLBARSYSTEM(o)`; a property's filter, like `FILTER(name(o))`; and any component the platform draws other than a property: its `caption` and `image` when it has them, and `hidden` while the form hides it, keyed by its design component identifier, always at the top level, whatever it is nested in. The entry is always there; a shown component with neither a caption nor an image has an empty one |

Each attribute is delivered at one point, already resolved: a property's static design value and its per-row `BACKGROUND` / `READONLYIF` / … result are folded into a single effective value before the view sees it, so the view reads an attribute straight from where it lives and never merges a column base with a row override. A list property's whole-column attributes — `caption`, `image`, `footer`, `comment`, `tooltip`, `defaultValue` — are on its column node `data.<g>.<prop>`, while its per-row value and the cell attributes that can vary by row are on the cell `data.<g>.list[i].<prop>`:

```jsx
const caption = props.data.o.sum.caption;   // a column attribute, once for the column
const cell = row.sum;                        // { value, background, readOnly, ... } for this row
```

The server computes a dynamic `IMAGE` for a property once at the column key (using the current object), while an action's dynamic `IMAGE` is computed for every row. The projection preserves that distinction: a property's image is on its column node `data.<g>.<prop>`, while an action's image is on its row cell `data.<g>.list[i].<action>`.

An option the platform computed nothing for is absent — a property with no `BACKGROUND` has no `background` in its cell entry — and a row the platform computed no row options for has no `background`, `foreground` or `selected`.

`SHOWIF` controls the property itself, and what it changes is the entry's `hidden`, never whether the entry is there. When it hides a table column, the column entry says `hidden: true`, and the column's cells stay on the rows without a `value`. When it hides a panel property or a property of the empty group, its entry says `hidden: true` and has no `value`. A property the form hides when it opens is `hidden` until it is first sent. Read `hidden` when a hidden property must be distinguished from one whose `value` is `null`. An entry the form shows has no `hidden` at all, as any option the platform computed nothing for is absent.

| Option | What it is | JS value |
| --- | --- | --- |
| `type` | The kind the value was converted by — `number`, `boolean`, `string`, `date`, or `json` (parsed, so the value is whatever the JSON held); of a [selection](#select), `string`: its value is made of the views of its options, which are text, as the platform's own element shows them. It describes the property rather than one of its values, so it sits with the column attributes: `data.<g>.<prop>.type` for a table property, and on the entry itself for a panel or empty-group one. A cell does not carry it | string |
| `askConfirm` | Whether a change of the property — for an action, running it — asks the user first: `CONFIRM` on the action, or `askConfirm = TRUE` in the design. The platform's own editor and button ask before they send; a view's call through the property's member does not, so a view that wants the question asks it itself, before the call. It is where `type` is | `true` / `false` |
| `askConfirmMessage` | The question to ask: the design's `askConfirmMessage`, or the platform's own | string; only where `askConfirm` is `true` |
| `hidden` | Whether the form hides the property now — by its `SHOWIF`, or by the `SHOWIF` of a container above it. It is on the property's entry — the column entry of a table property, the entry itself of a panel or empty-group one — and never on a cell | `true`; absent while the form shows it |
| `loading` | Whether a change of the cell still waits for the server's answer, whoever sent it — this view, through the property's member (`change` or `exec`, in a batch or outside one), or another part of the form. An action's cell says it while the action runs, and the entry of the form's Refresh button while the [form waits for the server](React_form_behavior.md#form-loading). It is on the cell of a table property, and on the entry itself of a panel or empty-group one | `true`; absent otherwise |
| `caption` | The property's caption | string |
| `image` | The property's image | string with the image HTML |
| `footer` | The column's footer value | converted like a cell value of its own type |
| `readOnly` | The cell is read-only: as delivered by `READONLYIF`, and where it delivers nothing, for a property that is `READONLY` on the form or has no change at all. Absent otherwise | `true` / absent |
| `disabled` | The cell is disabled: as delivered by `DISABLEIF`, and where it delivers nothing, for a property that is `DISABLE` on the form. Absent otherwise. One cell never carries both `readOnly` and `disabled` | `true` / absent |
| `background`, `foreground` | The background and text colors of the cell | string with a color |
| `comment` | The comment shown next to the value | string |
| `placeholder` | The text shown in an empty cell | string |
| `pattern` | The pattern the value is displayed with | string |
| `regexp`, `regexpMessage` | The regular expression the entered value is checked against, and the message shown when it does not match | string |
| `tooltip`, `valueTooltip` | The tooltips of the property and of its value | string |
| `options` | The property's custom options | parsed JSON value |
| `defaultValue` | The value editing starts with | string |
| `select` | What a [selection](#select) adds — its options and how it selects. It is on the cell of a table property, and on the entry itself of a panel or empty-group one | object |

Of these, `caption`, `image`, `footer`, `comment`, `tooltip` and `defaultValue` are the column's own attributes, on the column node `data.<g>.<prop>` — one per column, beside `type`. The rest (`background`, `foreground`, `readOnly`, `disabled`, `placeholder`, `pattern`, `regexp`, `regexpMessage`, `valueTooltip`, `options`, `select`, `loading`) are cell attributes, on each row's cell `data.<g>.list[i].<prop>` next to its `value`. The row itself carries `background` and `foreground` — the colors of the row as a whole — and `selected`, which is `true` while the row belongs to the selected range and absent otherwise — directly, not inside any property entry.

```jsx
function Row(props) {
    const r = props.row;
    const sum = r.sum;              // { value, readOnly, disabled, background, foreground, ... }
    return (
        <div className="order" style={{ background: r.background }}>
            <span>{r.number.value}</span>
            <input value={sum.value} readOnly={!!sum.readOnly} disabled={!!sum.disabled}
                   style={{ background: sum.background, color: sum.foreground }} />
        </div>
    );
}
```

### A selection {#select}

A property given `SELECT` in the `FORM` is shown as a choice among all its possible values: the objects a link can point at, by their view (the statuses an order can have, by their names), or, for a [selection of several values](../paradigm/Interactive_view.md#multiselect), the objects that can be checked (the tags of an order). Inside a container a React component draws, the component draws such a property itself, and the property's entry carries all its options at once:

```lsf
FORM orders 'Orders'
    OBJECTS o = Order PANEL
    PROPERTIES(o) number READONLY, nameStatus SELECT, tags SELECT
;

DESIGN orders {
    NEW card FIRST { custom = 'OrderCard'; MOVE BOX(o); }
}
```

```js
data.o.nameStatus = {
    value: 'New',              // the value of the option picked
    type: 'string',
    select: {                  // what SELECT adds
        list: [                // all the options, in their order
            { key: 581, value: 'New', selected: true },
            { key: 583, value: 'Done' },
            { key: 584, value: 'Lost' }
        ]
    }
}
data.o.tags = {
    value: 'red',              // the property's own: the values of the options checked, joined
    type: 'string',
    select: {
        multi: true, separator: ', ',
        list: [{ key: 577, value: 'blue' }, { key: 576, value: 'green' },
               { key: 575, value: 'red', selected: true }]
    }
}
```

`select` is what `SELECT` adds to the entry, in one field. Each option of its `list` has `key` — the id of the object it is (of an option of several objects, as a selection over a group by several objects has, a string of their ids), the same while the option is there, which keys it in React —, `value` — the view it is shown by, a text — and `selected` while it is picked. Of one value, the entry's `value` is the value of the option picked, which is the property's own value; with nothing picked no option is `selected`, and `value` is `null`. Of several values, `select.multi: true`, `value` is the views of the options checked, in the list's order, joined by `select.separator`, and `null` with none checked. `select.separator` is the separator of its `GROUP CONCAT` where it is a literal, not to be localized, of an `OVERRIDE` of several the one they all join by, and a comma, `', '`, otherwise — where it is computed or localized, or the `OVERRIDE` joins by different ones. For the usual `tags(o) = GROUP CONCAT name(t) IF in(o, t), ', ' ORDER name(t), t` this is the property's own value; where the property is something else over the options — an expression over the `GROUP CONCAT` (`upper(tags(o))`), an `OVERRIDE` —, it is still the options checked, joined: `'green, red'` while the property has `'GREEN, RED'`. `select.notNull: true` says the value has to be there, so a view offers no way to clear it. How the selection is drawn is the view's own: an element type the form gives `SELECT` (`'list'`, `'dropdown'`, ...) is the platform's own element's, so the form is refused with one here — with `NOCUSTOM` the platform draws that element, and the view places it; a type the property declares is only its default, which the view overrides. For a column, `select` is on each row's cell, `row.nameStatus.select`: what is picked is each row's own. The view of an option is drawn with [`<Caption>`](React_rendering.md#caption): it may be HTML.

The property is changed by what is picked, with its member's `change`:

- Of one value, `change` takes an option — an element of `select.list`, or its value: `change('Done')` —, and `null` clears the value; a value that has to be there is not cleared. The option picked already changes nothing, and nothing is sent.
- Of several values, it takes their value, as `value` has it — the views joined by `select.separator`: `change('green, red')` —, or the array of the options checked from now on, elements of `select.list` or their values: `change(['red', 'green'])`; `null` and `[]` uncheck all of them, and so does `''`. A value that splits back into views more ways than one — a view with the separator in it, or one that joins with it at its edge, or an empty separator — is refused: pass the array then. An option alone — an element of `select.list` — is checked or unchecked: the opposite of what the cell shows now, a pick not answered yet included.
- A value no option has is refused, and so is a value several options have — options may share a view —: pass the option itself then.

A pick is sent as the platform's own element sends it, one option at a time, picked or no longer: the link is written — `status(o)` gets the status picked —, and for a selection of several values the condition of each option checked or unchecked, `in(o, t)`. A call that checks and unchecks several options sends them in one request, the checks first. The view shows the pick at once — in `select.list` and in `value` —, the cell says `loading` until the answer, and the answer brings the options read again. In a [batch](Form_and_navigator_controllers.md#batch) a pick goes with the rest.

The options are read and sent as a value is: when the form is opened, and again whenever what they depend on changes — their views, which objects they are, what is picked — or the current object or the rows do; each time the cell's options come whole. Where the platform's own element is given only the options picked and looks the others up by the text typed — over many options —, a view is still given all of them.

The platform also builds a [selection element](../paradigm/Interactive_view.md#select) by itself, for a link with a unique view or a selection of several values; a property a React component draws gets none of that: its entry has options only where a selection is given — `SELECT` in the form or in the property's declaration. The design's `select` always names an element of the platform's, so it goes with `custom = NULL`. With `NOCUSTOM` — `PROPERTIES(o) nameStatus SELECT 'buttonGroup' NOCUSTOM` — the platform draws its own element of the selection, and the component places it with `<Platform name/>` like any [child the platform draws](React_platform_components.md#placing), per row in the rows it draws.
