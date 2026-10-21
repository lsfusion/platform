---
slug: "/How-to_Custom_React_views"
title: 'How-to: Custom React form views'
---

A [`DESIGN`](../language/DESIGN_statement.md) container can be rendered by a React component instead of the standard layout. The component receives a projection of the form state and draws the container's whole subtree itself. A table, a toolbar, filter controls, a filter group and a property's filter can be drawn by a React component of their own the same way, instead of the platform's view of them ([A component's own view](#component-views)).

This is a web-client feature only. The desktop client deserializes the container and renders its regular (non-React) subtree, and draws a component with a React component of its own with the platform's view, so the design stays usable in both clients.

### Selecting the component {#selecting-the-component}

In `DESIGN`, set the container's `custom` attribute to the component name as a string literal matching `[A-Z][A-Za-z0-9_$]*` (a bare identifier starting with an uppercase letter):

```lsf
FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) READONLY number, date, sum
;

DESIGN orders {
    BOX(o) {
        custom = 'OrderBoard';
    }
}
```

The value form selects the renderer: a string literal matching `[A-Z][A-Za-z0-9_$]*` names a React component, while an empty string `''`, an HTML template string, or a property gives the classic (non-React) custom container described in [How-to: Custom Components (objects)](How-to_Custom_components_objects.md). Here the object `o` is rendered by the `OrderBoard` React component instead of the standard table. A component that is not a container takes a React component name alone — see [A component's own view](#component-views).

A form opened as a window (`SHOW ... FLOAT`; for `DIALOG` the window is the default location) gets its size from its content at the moment of opening and keeps it: the component's first drawing, of the form's first data, is measured, and what it draws later - more rows, data that arrives afterwards - is not. So a container whose content grows after the opening is given a base size: `size = (900, 600)` or the separate `width` and `height` attributes. Without it, the window keeps the size of the first drawing, and the content drawn later pushes the OK / Close buttons past the window's edge. A tab (`WINDOW`) is sized by the forms window, so no base size is needed there.

Another way is to leave the React container without a base size and make the window itself dynamic: in the form's own `DESIGN`, set its main container to `size = (-1, -1)`, or only `height = -1` — then the width is measured and fixed at opening while the height stays dynamic. In each dynamic direction the window follows the component's content, including content drawn later. This suits a form with no tables (their height stops fitting the rows) and content of moderate height: the window is centered only at opening, and its height is not capped at the screen height — a window taller than the screen scrolls as a whole.

The base size also bounds the height of the container itself, wherever the form opens. Without it the container is as tall as what the component drew, and the containers above it grow with it, so a component that draws more than fits on the form stretches the whole form, and the form scrolls as a whole — in a tab, its main container. A base height (`height` or `size`) separates the container's size from the content's: the container gets that height, expands from it into the form's free space by its extension coefficient (`fill`), and content that does not fit scrolls inside it (`overflowVert` is `auto` by default). It stretches the form only when it does not fit there itself — like the base size of any component.

### The component {#the-component}

`OrderBoard` is a named export from a `.jsx` module under `src/main/web`. How the module is compiled and registered is covered in [How-to: Custom client JS modules](How-to_Custom_client_JS_modules.md). The examples here use JSX. For a project [without the build](How-to_Custom_client_JS_modules.md#without-the-build), ship the same component as a `.jsx` file — it is transformed on the server when served. `import` and `export` are not available there, so the `export` keyword is dropped and the component works against the platform-provided `window.React` — or write it with `React.createElement` in a plain `.js`. Either file is placed under `src/main/resources/web/init` (auto-loaded) or under `src/main/resources/web` and registered with `onWebClientInit`.

The component is a plain function that receives `props.data` and `props.controller`:

```jsx
export function OrderBoard(props) {
    const orders = props.data.o.list;
    return <div className="order-board">{orders.length} orders</div>;
}
```

The component is first drawn with the first projection of its container, once the form's first data has arrived, and never before it: `props.data` has the shape described below from the very first render, so nothing in it needs a guard against a projection that has not arrived yet.

`props.data` is the form projection — its own for each `CUSTOM REACT` container, holding only what that container draws or places, so a second React container on the form gets a separate one, and a [controller](How-to_Custom_view_controller.md) of its own with it.

A view reads and changes only its own container's parts. What containers share is the form itself — its current objects and its session — so a filter or a current object changed in one container shows in another, as it does in the standard client. A page built of such containers is in [A page of several containers](#several-containers).

Every property, group and container the projection carries is an object in `data`: read a value as `.value` and its attributes as sibling fields, and read a list property's column attributes at `data.<group>.<prop>`. The entries a container carries for properties and components are design data: each of them is there from the start, and whether the form shows it now is its [`hidden`](#display-options) field, never whether it is there. `data` contains a group when the view draws a PART of that group, and holds exactly the parts it draws. A part of a group, or of a [tree](#trees), is projected — its data, and the controller members that change it — in the React container where that part's component is, on the node of its group or tree there:

| Component | Part | Node |
| --- | --- | --- |
| The group's grid, `GRID(<group>)`, or its tree, `GRID(TREE <tree>)` | The rows and the current object, and — of a group not in a tree — the [sortings](#orders) | `data.<group>` |
| A panel property — one the platform draws where the view places it | The property — one the platform draws as the [entry that labels it](#placing) (the entry of an action the platform draws only says whether the form shows it: its caption and image are its button's face) | `data.<group>`, or `data` itself for the empty group |
| What `FILTERS(<group>)` holds | The [user filters](#filters) | `data.<group>` |
| `FILTERCONTROLS(<group>)` | The [filter controls](#filter-controls) | `data.<group>` |
| `FILTERGROUP(<filterGroup>)` | A [filter group](#filter-groups) | `data.<group>` |
| A property's filter, `FILTER(<property>)` | The [filter](#property-filters)'s condition | `data.<group>`, under `filter` |
| `TOOLBARSYSTEM(<group>)` | The [toolbar](#toolbar) | `data.<group>` |
| The tree, `GRID(TREE <tree>)` | The [tree](#trees)'s current row, the opening of its current node, and its [sortings](#orders) | `data.<tree>` |
| What `FILTERS(TREE <tree>)` holds, `FILTERCONTROLS(TREE <tree>)`, `TOOLBARSYSTEM(TREE <tree>)` | A [tree](#trees)'s user filters, its [filter controls and toolbar](#tree-toolbar) | `data.<tree>` |

A group in `PANEL` view has only its panel properties and its filter groups: no rows, sortings, user filters,
filter controls or toolbar — the platform shows none of them for it either.

A component's place is found by walking up from its container: a `CUSTOM REACT` container is the answer, and a container with a view of its own — `custom = NULL`, or a custom of its own — ends the walk, since that view draws everything under it. Whether the component itself has one decides only which entry it has there — its content, or the [entry that labels it](#placing) — never where. A view that draws the rows — the grid is inside the custom container, and neither it nor a container between them has a view of its own, so React renders them from `data` rather than the platform — gets `props.data.<g>` = `{ list, byKey, keys, options, orders, properties }` for that group object SID `g` — a group of a tree has no `orders`: the [tree](#trees)'s sortings are its own node's —, where `list` is the array of rows in display order, `byKey` maps a row's key string to the same row object, and `keys` is the array of those key strings in the same order. `byKey` is keyed by application data, so it is built with NO prototype: a row whose key is the string `__proto__` is an ordinary entry in it, and `byKey.hasOwnProperty` and the other `Object.prototype` methods are not there to call — read it as `byKey[k]`, and test with `k in byKey` or `Object.keys(byKey)`. Wherever the view draws the rows, `list`, `byKey` and `keys` are all present: they are empty while the group has no rows — before its first rows arrive, say — so the view reads `props.data.<g>.list` directly without guarding a missing field. A grid (or a tree) the form hides with `SHOWIF` says so in [its own entry](#display-options), `data['GRID(g)'].hidden` — a tree's is `data['GRID(TREE t)']`, one for all its groups —, and its rows stay as the server last sent them, as the platform's own grid keeps them — the server may stop refreshing them while the grid is hidden — so draw them only while the grid is not hidden. Where it does not, the node has none of them, and no `options` or `orders` (the group's sortings, the order those rows are drawn in) either: there are no rows here for them to be about, and a panel property of the group may take one of those names. A group in `PANEL` view draws its rows nowhere — the server sends it none, only its current object —, so its node is such a one wherever its grid is, the container its grid is in included: its panel properties, and no `change(row)`. A group **no part of which** the view draws is absent from `props.data` altogether (`props.data.<g>` is `undefined`): a container merely standing beside a platform-drawn grid gets nothing, and neither does one that only frames it (`MOVE GRID(g) { custom = NULL; }` — there the platform draws the rows, so the container gets the [entry that labels what it places](#placing) and no rows; one that holds `FILTERS(g)` as well gets the group's [user filters](#filters)). Asking [`<List>`](#rendering-rows) for rows that are not there is a mistake in the view, and it throws: the view shows the reason in its place and in the console. A group's panel properties are members of the group node in the container each property itself sits in, keyed by integration SID. The top level of `props.data` is the node of the empty group (the form level): each property of the empty group is a member of `props.data` directly, under its integration SID. A list property's column attributes are a member of the group too, at `data.<g>.<prop>` — one entry per column — while its per-row value and cell attributes live on each row. Actions React draws are projected the same way as properties — an action drawn on a group is a field of each row (a list action) or of the group node (a panel action), an object of its attributes like a property, so `controller.<group>.<action>.exec(row)` runs it (a panel action's `.exec()` takes no row). Its `value` field is there too but carries nothing. An action the platform draws has only its entry, and the platform's button runs it. `options` is the group's own [display options](#display-options); `properties` names every PROPERTY entry this node carries, shown or not, in the form's order — not everything on the node, which also holds what the projection itself defines (`list`, `byKey`, `keys`, `options`, `orders`) and is known by name rather than discovered. The number of rows is `list.length`. Each row carries:

| Field | Meaning |
| --- | --- |
| `key` | A stable public id for the row — use it as the React key |
| `isCurrent` | Whether the row is the current (selected) one |
| `<integrationSID>` | Each list property, an object `{ value, ...cell attributes }` keyed by the property's integration SID — read its value as `.value` |
| `objects` | An opaque row handle the controller uses to address the row |
| `background`, `foreground`, `selected` | The row's own [display options](#display-options): its background and text colors and whether it is selected |

So a `MOVE` of a panel property is what decides which engine draws it: inside a custom container React draws it from `data`, outside one the platform draws it where it now stands and it leaves `data`.

A container with a `custom` of its own inside what a React component draws — another React component, an HTML template, `''` — is not drawn by that component: it is drawn by its own view, and the outer component places it like any other [child the platform draws](#placing). So is a table, a toolbar, filter controls, a filter group or a property's filter with a [React component of its own](#component-views).

A property grouped in columns (`COLUMNS`) cannot be projected: its values are addressed by a row-and-column key, and everything here — a row of `list`, an entry of `byKey`, a name on the controller — means one row. So a React container may not carry one — a column of the rows it draws, or a panel property or a property of the empty group standing in it: such a form is rejected when it is built, naming the property. Elsewhere on the form it stays as it is, and a React view simply has no name for it: the [controller](How-to_Custom_view_controller.md) carries what the projection carries, so what is not projected has no member. The same holds whenever a name does not single out one property — two properties of a projected group may share an integration name only when their entries land in different containers, and each view then means the one its own node carries. Within one container they may not, because one entry and one member cannot stand for two properties; nor anywhere, one drawn by the platform included, when a view draws a part that names a property of the group ([Reserved names](#reserved-names)). Give one of them an explicit `EXTID`. Declare an ordinary property if the view has to read those values.

A member takes a row only where the group's rows are drawn — `<group>.change(row)`, and the member of a list property, which lives there — and there it takes the row object, its `objects` handle, or the **key** string the projection gave it, looked up in this view's `byKey`. A panel property's member and a member of the empty group take no row: they act on the current objects. [Row identity](How-to_Custom_view_controller.md#row-identity-contract) says what each of the three means.

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

Except for `BOOLEAN`, a `NULL` value is converted to `null`.

An interval's bounds are milliseconds since 1970: a bound of an `INTERVAL[ZDATETIME]` is the moment itself, while the other interval classes keep their date and time as those of UTC — `new Date(from).getUTCDate()` is the day an `INTERVAL[DATE]` starts on. A value a view hands back — to a member's `change`, or as a condition's `value` — is read the way this table writes it, so a value taken from `data` goes back as it is — but for a file's: a link is not the file —, and a `COLOR` or an interval written any other way is refused.

`list` contains only the read page, not all rows of the group. The view type of a group rendered by a React container remains the table, and the group is read page by page, but since the table itself is not displayed, the page size is not adjusted to the visible rows — the server default page size (50 objects) applies. For a view that shows all rows of the group — a calendar, a board, a map — specify the `PAGESIZE 0` option (read all objects) or an explicit page size in the [`OBJECTS`](../language/Object_blocks.md) block.

A view that lays rows out in their own order — a card flow, a feed — can instead keep the page and let it follow the scroll, the way the standard table does. `useSeekOnScroll(controller.<group>)` returns a function that marks each row's element. The hook watches which of them are on screen as the view's own box scrolls — the nearest element that scrolls the rows, looked for from a row up to the element the view is drawn in and no further — and follows the table's own rules: while the current record is on screen, scrolling changes nothing; when it leaves, it is reseated on the visible edge it left through, which is also what requests the next page; on a page change the reseated row keeps its on-screen position, so nothing jumps under the eye; a current record moved from outside — a click, a programmatic seek — is scrolled into view instead. The pact rests on the same contract the table itself lives by, and it is the developer's to keep: the page must hold more rows than the viewport can show — set `PAGESIZE` on the [`OBJECTS`](../language/Object_blocks.md) block accordingly. The window then always extends a page beyond the current record, so it leaves the viewport — and reseats, pulling the next page — before the scroller runs out of room at a loaded edge:

```jsx
const seekRef = useSeekOnScroll(controller.o, { enabled: follow });
...
{rows.map(row => <div key={row.key} ref={seekRef(row)}>...</div>)}
```

Options: `enabled` (default `true`) suspends the tracking, `threshold` (`0.6`) is how much of an element must be visible to count as on screen, `settle` (`250` ms) is how long the rows on screen must stay the same before the view seeks — the wait starts over whenever a row comes on screen or leaves it, not on every scroll event —, and `onSeek(row)` is called for each issued seek. Use one `useSeekOnScroll` per scrolling element. The rows delivered for the new position replace `list`, and everything built from it — values and [placed lsFusion views](#placing) alike — follows.

The hook moves the scroll position of that box and switches off the browser's own scroll anchoring there, so it touches nothing outside the element the view is drawn in: everything there — a platform container, a box another view shares, the page, a portal's box — belongs to something else. Rows that scroll only in such an element leave the hook doing nothing at all — no seek, and no scroll into view of a current moved out of sight — and it says so once in the console. Give the rows a scrolling box of the view's own — a bounded height and `overflow: auto`.

The number of rows read is `list.length`, not how many rows the group's filters admit, so under paged reading the projection alone cannot show "6 of 23". The total is a separate property that counts the rows under the form's current filter with the [`FILTER` operator](../paradigm/Filter_FILTER.md), as in [How-to: Table status](How-to_Table_status.md), placed inside the container so that it reaches `data`:

```lsf
filteredCount 'Orders' = GROUP SUM 1 IF [ FILTER orders.o](Order o);

EXTEND FORM orders PROPERTIES() filteredCount;
DESIGN orders { BOX(o) { MOVE PROPERTY(filteredCount()); } }
```

```jsx
const { o, filteredCount } = props.data;
<div>{o.list.length} of {filteredCount.value ?? 0}</div>
```

Drawn for the group instead (`PROPERTIES() filteredCount DRAW o PANEL`), the property needs no `MOVE`: its default place is in `PANEL(o)`, inside `BOX(o)`. It is a member of the group node, read as `data.o.filteredCount.value`.

```jsx
function Row(props) {
    const r = props.row;
    return (
        <div className={r.isCurrent ? "order order-current" : "order"}>
            <span>{r.number.value}</span>
            <span>{r.sum.value}</span>
        </div>
    );
}
```

#### Reserved names {#reserved-names}

The reservation follows what a row or a node carries:

| Where | Reserved |
| --- | --- |
| A row | `key`, `isCurrent`, `objects`, `background`, `foreground`, `selected` — on a row of a [tree](#trees) also `parent`, `hasChildren`, `expanded`, `loading` |
| A group's node, wherever it is | `properties`, `change` |
| A group's node where its rows are drawn | `list`, `byKey`, `keys`, `options`, and `orders` of a group not in a tree — on a group of a tree `expand`, `collapse`, `toggle`, `expandAll`, `collapseAll` |
| The node of a group in `LIST` view where `FILTERS(<group>)` is | `filters`, `filterProperties` |
| The node of a group in `LIST` view where `FILTERCONTROLS(<group>)` is | The names of the [filter controls](#filter-controls): `addFilter`, `applyFilters`, `resetFilters`, `filtersPending`, `filtersManual` |
| The node of a group in `LIST` view where `TOOLBARSYSTEM(<group>)` is, unless the design hides it (`visible = FALSE`) | The names of all the [toolbar](#toolbar)'s buttons, whatever the design's `show…` flags say: `manual`, `updatePending`, `update`, `filterControlsVisible`, `listView`, `listViews`, `getCount`, `getSum`, `export` |
| A group's node where `FILTERGROUP(<filterGroup>)` is | The [filter group](#filter-groups)'s own name, as a property's |
| A group's node where a property's filter, `FILTER(<property>)`, is drawn | `filter` |
| The top level, where the node of the empty group is the controller itself as well | The names of the [controller](How-to_Custom_view_controller.md)'s own members: `exec`, `eval`, `evalAction`, `change`, `startEditing`, `stopEditing`, `isEditing`, `batch` |

A tree's own `current` and `expandCurrent` and its sortings, where its rows are drawn, and its user filters, filter controls and toolbar write their names on [its node](#trees). A container carrying just a panel property of the group has none of the names of the other parts, so a property called `list` is refused there by nothing. `__proto__` is reserved everywhere: assigning it does not add a field, it replaces the object's prototype. There is no `meta` object anywhere. A form whose projected integration SID takes a reserved name, or where two projected items claim the same name at one data level, is rejected with an explicit error when it is built. So is a form in which a group whose rows or user filters a view draws, or its toolbar with the sum, carries one name on two properties anywhere, one drawn by the platform included: a [sorting](#orders), a [condition](#filters) or the [toolbar](#toolbar)'s `getSum` names the group's property by that name. Give it an explicit `EXTID`, or rename it. A property such a container draws or places has to have a name at all - the view reaches it by that name alone, its entry and its `<Platform>` alike: one declared `NOEXTID` is rejected the same way - give it an `EXTID`. A name that is empty or carries a dot is rejected too — a qualified name, `o.note`, is split at its last dot —, and so is a group of several objects with no name of its own, `OBJECTS (d = X, t = Y)`, wherever the view carries a part of it: its SID joins the objects' names with dots, so name it, `OBJECTS pair = (d = X, t = Y)`. A property or action the platform draws is named as any other, and all of this holds for it.

### Reading the projection {#reading}

The component reads the projection in one of two ways, and the choice is about who re-renders when data changes.

`props.data` is the whole snapshot. The component re-renders — with a fresh `data` — whenever anything in its scope changes, and renders everything it draws from the new snapshot. For a small view this is the whole story: no hooks, a plain function of the data, and the examples above are written this way. It is also how the scope is read as a whole — its top-level entries enumerated, the containers read. `useData()` with no selector returns the same snapshot.

`useData(selector)` subscribes the component to a slice: it re-renders only when the reference of `selector(data)` changes — so the selector must return something the projection already holds (a group node, a row, an entry, a value), never a new object or array built inside it, which would differ on every read and re-render without end. Because of structural sharing — an unchanged node, row or entry keeps its previous reference — this is what lets a component pay only for what it reads: a component subscribed to a group node (`useData(s => s.o)`) ignores the rest of the scope, a row component subscribed to its own row (`useData(s => s.o.byKey[rowKey])`) ignores the other rows. The root is the exception: the platform hands it a new `props.data` on every change, so it re-renders whatever it subscribes to, and what it renders below it skips the render only when it is memoized (`React.memo`) and is not handed `data`. `useController()` returns the same `controller` the root receives as a prop — its identity never changes for the life of the root, so it is safe in dependencies and closures, and passing it down as a prop is equally fine.

`useData` and `useController` read whatever root the component is mounted under, and know nothing about forms in particular. `Platform` and `usePlatform` place a view the platform draws itself, so they serve a form container and a [navigator window](#navigator-window) alike. The helpers built on the group and row shape of a form projection — `List`, `BucketScope`, `useBucket`, `Buckets`, `useSeekOnScroll` — are form-only: they read `data.<group>.list` / `.byKey` / `.keys`, which another kind of root does not have — and neither does a form view that draws no rows of that group.

The two compose into tiers, and each following one is only needed when the previous one's re-render becomes the cost:

1. a small view — `props.data`, render everything;
2. a longer list — `props.data` plus row components memoized at module level (`React.memo` bails out on the unchanged rows' stable references), or [`List`](#rendering-rows) which does exactly that;
3. a big board — subscriptions down the tree: the root renders memoized columns and hands them no `data`, a column subscribes to its [bucket](#bucketing), a row to itself, so one edited value re-renders the root and one card.

### Display options {#display-options}

For every property drawn on a form the platform computes semantic options that determine how it is shown — its caption, image, background and text colors, whether it is read-only or disabled, its comment, the text shown in an empty cell, its tooltip. They come from the property's design and from the data-dependent options of the [properties and actions block](../language/Properties_and_actions_block.md) (`HEADER`, `IMAGE`, `BACKGROUND`, `FOREGROUND`, `READONLYIF`, `OPTIONS` and the rest), so an option that depends on the data is recomputed per row. Native element classes and fonts are not projected: a React component owns its CSS. `background` and `foreground` are projected — they are `BACKGROUND` / `FOREGROUND` highlighting, a data-dependent business signal, not a theme the component's own CSS should own. For a property the React container draws itself, the semantic result is projected into `data` as sibling fields of the property's value, so the view does not have to recompute it.

The projection follows what an option describes — a whole column, one cell, or the row:

| Where | What it holds |
| --- | --- |
| `data.<g>.<integrationSID>` | For a property shown in the table — its column attributes, one entry for the whole column: `caption`, `image`, `footer`, `comment`, `tooltip`, `defaultValue`, and `hidden` while the form hides it. For a panel property of the group — the property's `value` and its attributes, read for the current object, and `hidden` while the form hides it; while the group has no current object the `value` is empty and the entry stays, as in the platform's own panel. For one the platform draws — [the entry that labels it](#placing), with no `value` |
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
| `type` | The kind the value was converted by — `number`, `boolean`, `string`, `date`, or `json` (parsed, so the value is whatever the JSON held). It describes the property rather than one of its values, so it sits with the column attributes: `data.<g>.<prop>.type` for a table property, and on the entry itself for a panel or empty-group one. A cell does not carry it | string |
| `askConfirm` | Whether a change of the property — for an action, running it — asks the user first: `CONFIRM` on the action, or `askConfirm = TRUE` in the design. The platform's own editor and button ask before they send; a view's call through the property's member does not, so a view that wants the question asks it itself, before the call. It is where `type` is | `true` / `false` |
| `askConfirmMessage` | The question to ask: the design's `askConfirmMessage`, or the platform's own | string; only where `askConfirm` is `true` |
| `hidden` | Whether the form hides the property now — by its `SHOWIF`, or by the `SHOWIF` of a container above it. It is on the property's entry — the column entry of a table property, the entry itself of a panel or empty-group one — and never on a cell | `true`; absent while the form shows it |
| `loading` | Whether a change of the cell still waits for the server's answer, whoever sent it — this view, through the property's member (`change` or `exec`, in a batch or outside one), or another part of the form. An action's cell says it while the action runs. It is on the cell of a table property, and on the entry itself of a panel or empty-group one | `true`; absent otherwise |
| `caption` | The property's caption | string |
| `image` | The property's image | string with the image HTML |
| `footer` | The column's footer value | converted like a cell value |
| `readOnly` | The cell is read-only, as delivered by `READONLYIF`. Absent otherwise — a statically `READONLY` property projects nothing here, since the view is not editing it anyway | `true` / absent |
| `disabled` | The cell is disabled, as delivered by `DISABLEIF`. Absent otherwise. One cell never carries both `readOnly` and `disabled` | `true` / absent |
| `background`, `foreground` | The background and text colors of the cell | string with a color |
| `comment` | The comment shown next to the value | string |
| `placeholder` | The text shown in an empty cell | string |
| `pattern` | The pattern the value is displayed with | string |
| `regexp`, `regexpMessage` | The regular expression the entered value is checked against, and the message shown when it does not match | string |
| `tooltip`, `valueTooltip` | The tooltips of the property and of its value | string |
| `options` | The property's custom options | parsed JSON value |
| `defaultValue` | The value editing starts with | string |

Of these, `caption`, `image`, `footer`, `comment`, `tooltip` and `defaultValue` are the column's own attributes, on the column node `data.<g>.<prop>` — one per column. The rest (`background`, `foreground`, `readOnly`, `disabled`, `placeholder`, `pattern`, `regexp`, `regexpMessage`, `valueTooltip`, `options`, `loading`) are cell attributes, on each row's cell `data.<g>.list[i].<prop>` next to its `value`. The row itself carries `background` and `foreground` — the colors of the row as a whole — and `selected`, which is `true` when the row is selected — directly, not inside any property entry.

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

### Rendering rows {#rendering-rows}

Use `window.lsfusion.List` to render the rows of a group with per-row render economy. It is a runtime global, so to write it as a JSX tag bind it to a local capitalized name first. Without an alias, call it through `React.createElement`. The alias is read where the module runs, which for a compiled bundle and for a `.jsx` resource is before anything renders, so it can sit at the top of the module. In a resource written as `.js`, which is served as it is, read the platform's components and hooks inside the component instead — the module runs before the first view mounts, and they are installed at that mount:

```jsx
const List = window.lsfusion.List;
// ...
<List group="o" component={Row} />
// or, without an alias:
React.createElement(window.lsfusion.List, { group: 'o', component: Row })
```

`group` is the group object SID. `List` reads the group's rows out of the view's own projection by that name, the way [`BucketScope`](#bucketing) does, so it is given the name rather than the node. `keys` draws only some of the rows, in the order given — any array of the group's row keys, such as a filtered or re-sorted copy of `props.data.o.keys`:

```jsx
<List group="o" keys={props.data.o.keys.filter(k => props.data.o.byKey[k].sum.value > 0)} component={Row} />
```

A key the group does not hold draws nothing. A `List` given a group whose rows this view does not draw — one the view holds only a panel property of, or nothing of at all — throws: whether a view draws a group's rows is the form's design, not its data, so this is a mistake in the view, and the view shows the reason in its place and in the console. `List` draws the rows the projection holds, and a grid the form hides with `SHOWIF` keeps its rows, as the platform's own grid does: where the grid has a `SHOWIF`, render the `List` only while `props.data['GRID(o)'].hidden` is not set.

Render `List` as a component — through JSX or `React.createElement` — never by calling it as a plain function: it renders each row through a component that uses hooks, so it only works when React mounts it.

`List` keys each row by `row.key`, passes the row to the component as `props.row` — with `rowKey`, `index` and any other props given to `List` besides its own `group`, `keys` and `component` — or `children`, which names the row component when `component` is not given — and renders each row through a memoized wrapper bound to that row, so on a change only the rows that actually changed re-render. The plain alternative maps the list directly:

```jsx
props.data.o.list.map(r => <Row key={r.key} row={r} />)
```

Why per-row economy matters. When any single row changes, `props.data.<g>.list` is rebuilt as a new array reference, but the projection keeps the same object reference for every row that did not change (structural sharing) — only the rows whose contents changed get a new row object. A plain `list.map(r => <Row row={r}/>)` re-creates the `Row` element for every entry on any single-row change, so React re-renders all of them. The React `key` does not change this: it lets React preserve each row's element identity, DOM, and component state across renders, but it is not a render bail-out. The React Compiler does not help either — it memoizes the `.map` as one reactive scope keyed by the array reference, which has just changed, and it does not wrap the row children in `React.memo`, so every row still re-renders.

`window.lsfusion.List` adds the missing per-row bail-out: it renders each row through a stable memoized wrapper that tracks that one row, so a value change re-renders only the changed row. The list itself is not re-walked when a row's value changes — only when rows are added, removed, or reordered —, so an update renders only what changed. What still grows with the number of rows is each mounted row's check of its own subscription, a lookup by its key. To get a per-row bail-out by hand without `window.lsfusion.List`, declare the memoized row component once at module level and key by `row.key`:

```jsx
const MRow = React.memo(Row);
// ...
props.data.o.list.map(r => <MRow key={r.key} row={r} />)
```

A `React.memo(Row)` created inside the component on each render is a new component type every time, which defeats the memoization and re-renders every row.

### Bucketing rows into cells {#bucketing}

When the view lays a group's rows out as a matrix rather than a list — a calendar, a kanban board, a timetable, a seating chart — each row belongs to a derived cell (day × employee, status column, and so on). `window.lsfusion.BucketScope` maintains a cell → rows index over one group, and each cell subscribes to only its own membership:

```jsx
const { BucketScope, useBucket, useData } = window.lsfusion;

const Shift = React.memo(({ rowKey }) => {
    const s = useData(d => d.ss.byKey[rowKey]);          // subscribes to its own row
    return s ? <button>{s.intervalS.value}</button> : null;
});

const Cell = React.memo(({ ck }) => {
    const rowKeys = useBucket(ck);                       // subscribes to its own cell
    return <div className="cell">{rowKeys.map(k => <Shift key={k} rowKey={k} />)}</div>;
});

export function Board(props) {
    // the view owns both axes: the days of the shown week and one board row per employee
    const days = weekOf(props.data.dates.scheduleFrom.value);
    const rows = props.data.boardEmployees.value;        // e.g. a pre-parsed JSON property: [{ id, ... }]
    return (
        <BucketScope group="ss" bucketDeps={[]}
                     bucketOf={s => dateKey(s.date.value) + '|' + (s.assignedTo.value ?? '0')}>
            <div className="grid">
                {rows.map(row => days.map(d =>
                    <Cell key={row.id + '/' + dateKey(d)} ck={dateKey(d) + '|' + row.id} />))}
            </div>
        </BucketScope>
    );
}
```

`<BucketScope group bucketOf bucketDeps>` wraps the grid markup. `group` is the group object SID. A group whose rows this view does not draw throws, as it does in [`List`](#rendering-rows). `bucketOf(row, rowKey)` computes the row's cell key from the row's property values — a string (any value is coerced to a string), an array of keys to place the row into several cells, or `null` for none. `bucketDeps` lists the outside values `bucketOf` closes over — like a hook dependency array, the index is rebuilt when they change. Keep the array's length constant.

`useBucket(cellKey)` returns the array of row keys currently in that cell, in the group's display order, and subscribes the component to only that cell. Call it once per cell component, with that cell's fixed key (the usual hook rules). An empty cell always returns the same frozen empty array. The cell component resolves each row key to a row component that subscribes to its own row via `useData(d => d.<g>.byKey[rowKey])`, as above.

The view keeps the layout: it supplies the cell keys — so empty cells exist and render too, e.g. as drop targets — and the cell markup. The platform keeps the index and the render economy: moving a row between cells re-renders only the old and the new cell; editing a value that does not change the row's cell re-renders only that row's own component; every other cell keeps its previous array reference and its `React.memo` skips. The plain alternative — grouping `data.<g>.list` into cells by hand on each render — rebuilds every cell's array every time, so any change re-renders the whole board.

When the cells form a flat list and each cell's markup lives in one component, the `<Buckets group cells bucketOf component/>` form does the mapping itself, the way `List` does for rows: one memoized wrapper per key in `cells`, and the cell component receives `cellKey`, `rowKeys`, `index`, and the pass-through props. Keep the explicit `<BucketScope>` + `useBucket` markup when the view itself lays out the grid — a two-axis matrix, axis headers, pinned columns:

```jsx
const { Buckets } = window.lsfusion;
const STATUSES = ['new', 'inProgress', 'done'];

// Card subscribes to its own row, like Shift above
const Column = ({ cellKey, rowKeys }) => (
    <div className="column">{rowKeys.map(k => <Card key={k} rowKey={k} />)}</div>
);

<Buckets group="t" cells={STATUSES} bucketOf={t => t.status.value} component={Column} />
```

Use bucketing for placing one group's rows into derived cells where only the membership matters — pivots, calendars, kanban boards, timetables, drag-and-drop grids. It does not compute per-cell aggregates: `useBucket` returns row keys, not sums or counts, and the cell component re-renders only when that cell's row-key array changes — live aggregates are what the [pivot table view type](../paradigm/Interactive_view.md#property) provides. For a plain one-to-one list of rows use `List`, and grouping only works over the group's own projected values.

### A page of several containers {#several-containers}

A page of several regions — a list of categories, the items of the chosen category, the details of the current item — is not one component over the whole form: each region is a `custom` container of its own, over its own part of the form, and ordinary `DESIGN` containers lay them out. A change then re-renders only the regions whose projections changed, a region in an inactive tab or a collapsed container is not read until it is shown — unless a visible region depends on it — and another module can still add to the page from `DESIGN`. What the regions share is the form's state, not a component's:

```lsf
CLASS Category;
name = DATA ISTRING[100] (Category);

CLASS Item;
name = DATA ISTRING[100] (Item);
category = DATA Category (Item);
archived = DATA BOOLEAN (Item);

CLASS Detail;
item = DATA Item (Detail);
name = DATA ISTRING[100] (Detail);
quantity = DATA INTEGER (Detail);

showArchived = DATA LOCAL BOOLEAN ();

FORM catalog 'Catalog'
    OBJECTS c = Category
    PROPERTIES(c) READONLY name

    OBJECTS i = Item
    PROPERTIES(i) READONLY name, archived, nameItem = name PANEL
    PROPERTIES() showArchived
    FILTERS category(i) = c, NOT archived(i) OR showArchived()
    ORDERS name(i)

    OBJECTS d = Detail
    PROPERTIES(d) READONLY name, quantity
    FILTERS item(d) = i
;

DESIGN catalog {
    NEW page {
        horizontal = TRUE;
        NEW categories { fill = 1; custom = 'CategoryList'; MOVE BOX(c); }
        NEW items { fill = 2; custom = 'ItemList'; MOVE BOX(i); MOVE PROPERTY(showArchived()); }
        NEW details { fill = 1; custom = 'DetailPanel'; MOVE PROPERTY(nameItem); MOVE BOX(d); }
    }
}
```

`CategoryList`, `ItemList` and `DetailPanel` are components like [`OrderBoard`](#the-component), and each container's `props.data` holds only the parts it draws: `CategoryList` sees `data.c`, `ItemList` sees `data.i` — its rows — and `data.showArchived`, `DetailPanel` sees `data.d` and, as `data.i`, only the panel property `nameItem` moved there. A part of a group is projected where its component stands — the rows where the group's grid is, a panel property where the property itself is — so the current item's name reaches the detail panel as a panel property of `i`, added under an alias and `MOVE`d into the container that draws it, without the group's rows. A click in a list sets the current object (`controller.c.change(row)`), and the filters of the groups below follow it — the platform's own dependency between groups, so the containers need no shared JavaScript state. Only the container that draws a group's rows can change its current object. A form-level property two containers show is added twice, under two aliases, one in each container.

The switch that decides which rows are shown is a form property, changed through the controller (`controller.showArchived.change(true)`) and applied by the form's filter — not a component state that filters `data.i.list`: the server then reads only the rows that pass the filter, and a count or a total the region shows over all the rows that pass it is a property over the same filter ([the count under the form's filter](#the-component)) — `list.length` is only the rows already read. The same holds for a search text or a chosen section. Only the state that does not decide what is read — an expanded card, an open sheet — stays in the component.

The regions are laid out by the standard containers: a horizontal parent, an extension coefficient (`fill`) on each column, and a base height on a column whose content scrolls inside it ([the base size](#selecting-the-component)).

### Crossing back to lsFusion

`custom` crosses from the platform to React; `custom = NULL` is the crossing back. By default the component draws the container's whole subtree from `props.data`. A child given `custom = NULL` — the platform's standard view rather than a custom one — instead keeps its lsFusion view, and the component places that view with `<Platform name/>` rather than drawing it. So does a child with a custom view of its own: a container with its own `custom`, or a property the `FORM` gives a `CUSTOM` render function. `NOCUSTOM` is `custom = NULL` said in the `FORM`.

```lsf
FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) READONLY number, date, sum
    PROPERTIES() comment = orderComment      // given `custom = NULL` below; its entry is data.comment
;

DESIGN orders {
    NEW board {
        custom = 'Board';
        MOVE BOX(o) { custom = NULL; }        // the standard grid, placed by the component
        MOVE PROPERTY(comment) { custom = NULL; }
    }
}
```

A child the platform draws is not projected into `props.data` — the platform builds its view, feeds it the property values, and renders it, exactly as in a standard container. The component only decides where it goes. Its caption and image - and a property's comment - are the exception: they go to the component in `data` instead of the child's own view. Such a property projects only `{ caption, image, comment, hidden }` — it has no `.value`, since the platform draws the value with the rest of the child's presentation. It projects that entry even with neither a caption, an image nor a comment — an empty one while the form shows the child. Such an action projects only `{ hidden }`: its caption and image are its button's face, which the platform draws. Such a container is projected the same way as any container the view draws: the component reads its `caption` and `image` from `data` and decides where the platform's view goes.

The entry holds `caption`, `image` - a property's `comment` too - and, while the form hides the child, `hidden` (an action's holds only `hidden`) — a table property's is its whole column entry — and it sits at the same place in `data` the [display options](#display-options) use, under the same name the child's value is keyed by:

| Child the platform draws | Where its entry is | Key |
| --- | --- | --- |
| A **table** property of an object group, [drawn per row](#live-editor) | `data.<g>.<integrationSID>`, an ordinary column entry alongside the group's other columns | The property's integration SID, `qty` |
| A **panel** property, or a property of the empty group | `data.<g>.<integrationSID>`, on its group's node (`data.<integrationSID>` for the empty group), where its value would be if React drew it | The property's integration SID, `note` |
| A container, or any other component | `data.<componentSID>`, always at the top level | Its design component identifier, `BOX(o)`, `GRID(o)` |

A property is named by its integration SID, table or panel, its entry and its place alike. Its entry sits on its group's node, where the entry of a property React draws would be, and a `<Platform>` names a panel one the way that entry is reached: `<Platform name="o.note"/>` for `data.o.note`, `<Platform name="note"/>` for the empty group's `data.note`; it hides the host while that entry says `hidden`. A table one's per-row renderer is named the same way, `<Platform name="o.quantity" row={row}/>` for its column entry `data.o.quantity`, and the row says which renderer: it has to be a row of that group — a tree's row of a group below passes, narrowed to the path down to the group, as a group's `change(row)` takes it —, and the column entry does not hide the host. A container or any other component is named by its design identifier, `<Platform name="BOX(o)"/>` and `data['BOX(o)']`. So the name a component places a child by is the path it reads the child's entry by.

Every component the view draws, other than a property it carries — a container, one declared with `NEW` and a generated one (`PANEL(o)`, `TOOLBAR(o)`, …) alike, a grid (`GRID(o)`) or a tree (`GRID(TREE t)`), a toolbar (`TOOLBARSYSTEM(o)`) — and every component the platform draws that it places, other than a property, gets such an entry, `data['<component SID>'] = { caption, image, hidden }`, at the top level of this view's `data` — except what is inside a container the platform draws, which it draws whole and the component never looks into. The `CUSTOM REACT` container itself has no entry in its own `data`: nothing there draws its caption and image — the platform draws them around the container, or, for one another React view places, that view does, from its entry there. An entry's `hidden` follows `SHOWIF` — the component's own, or a container's above it — and not whether the view shows the component: a tab the view does not show keeps its caption, and has no `hidden`. While a placed component's entry has it, `<Platform>` and `usePlatform` hide the host that places it — the host gets `hidden`, and stays: the component is still placed, so it goes on being read and comes back with its `SHOWIF`. Being part of the projected `data`, a dynamic caption or image re-renders the component like any other data change.

A `CUSTOM REACT` container obeys its own `SHOWIF` as any other component does: the platform hides its box. Inside what React draws, `tabbed`, `collapsed`, `popup` and `activated` do nothing: React lays that content out, and there is no tab strip or collapse header to apply them to — the view has each container's entry instead, and shows what it wants. A scripted `ACTIVATE TAB`, `COLLAPSE` or `EXPAND` on a component there is an error, as on a child the platform draws: its visibility is the view's.

`Platform`, `usePlatform`, `useShown`, `Caption` and `Image` are runtime globals, like `List`, so bind them to local names before the examples below work: `const { Platform, usePlatform, useShown, Caption, Image } = window.lsfusion;`.

The component names each child it places, and draws the caption itself where it wants it:

```jsx
export function Board(props) {
    const data = props.data;
    return <div className="board">
        <h3><Caption value={data['BOX(o)'].caption}/></h3>
        <Platform name="BOX(o)"/>
        <h3><Caption value={data.comment.caption}/></h3>
        <Platform name="comment"/>
    </div>;
}
```

### Placing a child the platform draws {#placing}

A child the platform draws need not be a direct child of the container: it may sit inside the containers the view draws itself, however deep — the [record](../paradigm/Form_design.md#record) of a table the view draws, what a design puts into `GRID(<group>)`, included —, and the `CUSTOM REACT` container above it places it by its name all the same — so a design groups such pieces the way the view draws them, a page or a section each. Inside a container the platform draws nothing is placed separately: the platform draws that container whole, with whatever is inside it — unless a `CUSTOM REACT` container inside it places a component, as it places its own.

Such a child's view is moved into a *host*: a DOM node React owns and never renders children into. React places the view relative to a node it owns, and it must keep owning it to go on rendering the surrounding tree. Which node that is, is the only difference between the two ways to place a child:

```jsx
// the platform creates the host — a <div> inside the section
<section className="board-panel"><Platform name="BOX(o)"/></section>

// the component's own element is the host — one node less
<section {...usePlatform('BOX(o)', { className: 'board-panel' })}/>
```

`<Platform>` is the shorter one. `usePlatform(name, { className, row })` is for an element the component renders anyway — a panel, a card, a grid cell — so the view goes straight into it: it returns the props that make that element a host, the ref that moves the view in, the host's marks and, while the child's entry says so, `hidden`. Spread them onto the element, and give its class to `usePlatform` rather than to the element: a `className` written after the spread replaces the one the marks are merged into.

Everything else is the same for both. The host carries the class `lsf-platform` and the attribute `data-lsf-sid` with the child's name, whoever created it: `<Platform>` and `usePlatform` render them as props of their own, merged with the class the component gives the host, so a class that changes between renders keeps them. Every host is styled so that the view fills it, whatever the child is, so the component sizes the host and the view follows:

```css
.board > .lsf-platform[data-lsf-sid="BOX(o)"] { height: 260px; }
```

Sizing is the component's job, because a placed child's `width`, `height`, `fill` and alignment attributes are **not** applied: those describe a position inside a standard container, and here the surrounding element is the component's own markup. Its `caption` and `image` are not drawn by the child either — except an action's, which are its button's face: they are handed to the component in `data`, so a component that places children itself draws them where it wants them — nothing draws them otherwise:

```jsx
<section className="board-panel">
    <h3><Image value={props.data['BOX(o)'].image}/><Caption value={props.data['BOX(o)'].caption}/></h3>
    <Platform name="BOX(o)"/>
</section>
```

Drawn with [`<Caption>`](#caption) and [`<Image>`](#image), for the reason those exist: a caption is not always plain text, and an image is either an address or a ready element.

This is what makes a **tab strip** possible, and it is what the entry is for. A component that shows one placed child at a time still has to name the ones it is not showing, and their captions are the only thing it has to name them with. Two things make that work: the entry does not follow the placement — every child the container places has one from the start, and its `hidden` follows `SHOWIF`, not whether the view shows the child, so a closed tab has a label — and a caption goes on being read while its body is hidden, so a computed caption on a closed tab keeps up to date.

```jsx
const TABS = ['BOX(o)', 'BOX(i)'];

export function PlacedTabs(props) {
    const [active, setActive] = React.useState(TABS[0]);
    return <div className="placed-tabs">
        <div className="placed-tabs-strip" role="tablist">
            {TABS.map(sid => <button key={sid} role="tab" aria-selected={sid === active}
                                     onClick={() => setActive(sid)}>
                <Image value={props.data[sid].image}/><Caption value={props.data[sid].caption}/>
            </button>)}
        </div>
        <Platform key={active} name={active} className="placed-tabs-body"/>
    </div>;
}
```

Rendering only the active child, rather than hiding the others, is what makes the closed tabs stop being read — see [below](#placing). Put the tabs over a group's `BOX`, not its `GRID`: only a container and a property carry a caption and an image, so a placed `GRID(o)` has an entry with no caption — it says only whether the form hides the grid.

A tab strip over children of both kinds keeps one name per tab, the name it places the child by, and reads the child's entry by it — a design identifier at the top level, a property's name as its path:

```jsx
const TABS = ['BOX(o)', 'o.note'];   // a container, and a panel property of o the platform draws
const entryOf = (data, name) => {
    const dot = name.lastIndexOf('.');
    return name in data || dot < 0 ? data[name] : data[name.slice(0, dot)][name.slice(dot + 1)];
};
// the strip: <Caption value={entryOf(props.data, sid).caption}/>; the body: <Platform key={active} name={active}/>
```

These rules bound the placement:

- Each child the platform draws is placed by at most one host. A child no host places is not shown. A duplicate host reports itself in the page and in the console, and the first one keeps the child.
- A component inside the container that the platform builds a view of without its being given `custom = NULL` — a [filter group](#filter-groups) that filters by no object, moved in — is shown by nobody, and the console says so once, with the fix. A component whose [part](#the-component) a view draws — a group's or a tree's filter box, filter controls or toolbar, or a group's filter group — is not such a component: the view gets that part.
- The host has to reach the page. A host that is still outside the document once the render that created it is over gives the child up: the view goes back to waiting, a `<Platform>` with the same name that IS in the page gets it, and the console says which name was given up. A host a portal appends in an effect is in the page by then and is placed as usual.
- The node `<Platform>` renders holds the lsFusion view, so it must stay empty: give it a class or a style, never children.
- `custom = NULL` is the platform's standard view wherever it is given: outside a `CUSTOM REACT` container, or inside a container the platform draws whole, the component is drawn where it stands, with that view.

A placement that cannot work says so in the host itself, not only in the console: a name that names nothing inside the container (nor in a container it draws itself), a component the view draws itself, a second host for the same child, a name given a `row` that names no grid property the platform draws per row of a group this view draws, a `row` of another group, and a `row` that is not a row each render their message into the host, in an element of the class `lsf-view-error`. The host's own class stays as the component gave it.

The view tells the server which of the children the platform draws it places — the set of them, not the mounts and unmounts that changed it: it is worked out after every change of placement and once after the component first draws, and only what changed is sent. A child with a host — holding its view, or waiting for it — is placed. A child no `<Platform>` places is not read, the same gating an inactive tab or a collapsed container gets — except what labels it: its caption, image and comment go on being read, as an inactive tab's title is, so the component can still name it. Its group stops being read only when the child was the group's last visible place on the form. So a component that shows one child at a time renders only that child, rather than hiding the others with CSS: a CSS-hidden child is still placed as far as the server knows, and goes on being read. For the same reason the visibility of a placed child belongs to the component alone — its `collapsible`, `collapsed` and `activated` attributes are ignored, and a scripted `COLLAPSE` / `EXPAND` or `ACTIVATE TAB` on it is an error.

A container the view draws itself — a page of a tab strip the view lays out, a section it folds — the server reads as a shown one for as long as the view says nothing of it: it cannot see what the view shows. `useShown(name, shown)` says it. A container the view does not show is not read, and neither is anything inside it — a child the platform draws placed there included, host or no host —, the same gating an inactive tab gets, its caption and image going on being read as an inactive tab's title is: a property there that changes meanwhile reads as `null` until the container is shown again, as on an inactive tab. Call it where it stays mounted while the container comes and goes — in the strip, not in the page —, one hook per container, under the container's design name: unmounted, the hook leaves the container shown, which is what the server assumes of a container no hook names. `shown` left out is `true`. A name that is no container the view draws itself is reported in the console. The container's `SHOWIF` stays the form's, and its entry's `hidden` says only that.

```jsx
// a tab of the strip: mounted for as long as the strip is, whichever page is shown
function Tab(props) {
    useShown(props.name, props.active);
    return <button role="tab" aria-selected={props.active} onClick={props.onClick}>
        <Caption value={props.entry.caption}/>
    </button>;
}
```

### A live editor in every row {#live-editor}

A placed child is drawn once. A grid property given `NOCUSTOM` in the `FORM` is drawn once per **row**, so a component can put a real lsFusion editor into every row it renders, instead of showing the value and having to build editing itself:

```lsf
FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) number READONLY, date
    PROPERTIES(o) quantity NOCUSTOM, note NOCUSTOM
;

DESIGN orders {
    NEW board {
        custom = 'OrderBoard';
        MOVE BOX(o);                  // React draws the rows, from data.o
    }
}
```

`NOCUSTOM` says the platform draws the property, in its standard view: it is a component rather than a value. A `CUSTOM` render function says so too, the platform drawing the value by that function, and so does a `SELECT` given with `NOCUSTOM`, the platform drawing the value as a selection from a list: a `SELECT` alone says what is drawn, not who draws it. The design says the same with the property's `custom`, as with its `caption`: `custom = NULL` is `NOCUSTOM`, and `PROPERTY(quantity(o)) { custom = 'starRating'; }` is `PROPERTIES(o) quantity CUSTOM 'starRating'`. Where it is placed is not said — and cannot be: a grid property has no place of its own in the design, and its renderers go into the rows, so the component drawing the rows is the one that places them. That is the component this `MOVE BOX(o)` gave the group to. A `MOVE` of a grid property is refused when the form is built, `NOCUSTOM` or not. The component names the property as its column entry is keyed, with its group, and gives the row:

```jsx
{data.o.list.map(row => (
  <tr key={row.key}>
    <td>{row.number.value}</td>
    <td><Platform name="o.quantity" row={row}/></td>
    <td><Platform name="o.note" row={row}/></td>
  </tr>
))}
```

Pass the row object out of the projected data — a row key alone cannot be resolved back to a row. The same property may be placed once per row, and only once per row.

What the author gets and what stays theirs:

- The editor is the platform's, with everything that implies: editing, `READONLYIF`, `BACKGROUND` and the other per-cell options, all read for **that row**.
- The property leaves the rows: it is a component now, so `row.quantity` is not there, and the controller has no member for it — the platform draws and edits the values. Its column entry stays, an ordinary one at `data.o.quantity` — `caption`, `image`, `footer`, `comment`, `type`, `hidden` — and `properties` lists it. Its editors are placed by the same name, `o.quantity`, so it has to have one: a property declared `NOEXTID` is refused when the form is built. Declare a second, ordinary property if the value is also wanted as data.
- The caption is not drawn in the row, and neither is the comment: the per-row editors draw none of their own. Like a placed child's caption, both arrive in the group's column node — `data.o.quantity.caption`, `data.o.quantity.comment` — so the component puts them where they belong, usually once, in a header. An action's column entry has no caption and no image: they stay on its buttons, as their face.
- A renderer exists for every row the group is currently showing, whether the component renders that row or not. A row scrolled out of view keeps its editor. Only a row leaving that set loses it.
- Its key (`CHANGEKEY`) is one for all the rows, as a grid column's is: pressed anywhere in the form, it opens the editor in the current row, while that row's editor is placed — the editor of a row the component does not render takes no key.

Where no `CUSTOM REACT` container draws the group's rows, `NOCUSTOM` is simply the standard view: the platform's grid draws the property, without a custom render or editing function. A grid property grouped in columns is refused, with the property named, where a React view draws its rows: a per-row editor cannot address a row-and-column cell.

### Extending a container with placed children

Another module adds a child to the container from `DESIGN`:

```lsf
EXTEND FORM orders PROPERTIES() rating;
DESIGN orders {
    board { MOVE PROPERTY(rating) { custom = NULL; } }
}
```

The component does not pick it up on its own. Every child the platform draws is placed by a `<Platform>` that names it, so a child no `<Platform>` names is not shown, and adding one this way means editing the JSX too — the `DESIGN` extension declares the child, the component decides where it goes. The layout is not extensible from `DESIGN` either: a React composition cannot be modified there. To change the layout, replace the whole component with `custom = 'OtherBoard'`. The children stay as declared.

### Choosing between a React component and an HTML template

A component that places children the platform draws and reads nothing from `props.data` does what a classic custom container already does: an HTML template positions the same children through its `<Platform:name>` places, without a React runtime and without a host node per child. When the platform draws every child of the container, `props.data` carries no group or property values, because such a child is not projected — only the children's `{ caption, image, hidden }` entries in `props.data`.

A template is given no data at all. It places what the platform draws, by name, and everything else in it is written out: it cannot read the caption or the image of a child, or of the container itself. The captions it shows are drawn by the platform: a property child is drawn in parts (unless it is declared `inline = FALSE`) — its caption and its comment are places of their own, `<Platform:PROPERTY(qty).caption>` and `<Platform:PROPERTY(qty).comment>`, which the template can put inside markup of its own, a heading say —, any other child carries its caption inside its placed view, and the container's own caption and image are drawn by the platform beside the container rather than inside the template. So markup of your own that has to use a caption the application computes — as text of its own, or a container's caption — is the point at which a template stops being enough.

Both name their children, so neither is extended from `DESIGN` alone: adding a child means editing the JSX or the template string. A React component earns its place when the layout is computed — the children are placed conditionally, the grid template is derived from the data read through `useData`, or the markup comes from a component library — or when the markup has to show something the projection carries, which for a container or a child the platform draws is its `caption` and its `image`. A template string belongs to the module that declared it and can only be replaced whole.

### Interactivity

To read and change form state from the component — selecting a row, changing a property, calling actions — use `props.controller`. Its methods are described in [How-to: Custom view controller](How-to_Custom_view_controller.md).

A component that draws a field of its own also tells the form when the user is editing in it, so that `ENTER` does not move the focus to the next component and `ESCAPE` does not close the window while they are typing — see [Declaring that the user is editing](How-to_Custom_view_controller.md#editing). A component that draws no field of its own has nothing to declare.

### Keys and mouse bindings {#keys}

The keys and mouse bindings the design gives what a view draws work as in the standard form: the key and the mouse binding of a property or an action ([`CHANGEKEY`, `CHANGEMOUSE`](../language/Property_options.md)), the keys of the form's own buttons — actions of the `System` module: `ESCAPE` closes the form, `ctrl ENTER` is OK — and the key of a filter of a [filter group](#filter-groups). The platform binds them itself, and the view does nothing for them. A key works while the form shows its component — its entry is not `hidden` — and the view that draws it is shown, whatever element of the form the user presses it in, and it does what the platform's key does:

- an action runs, as a click on its button. An action in the rows a view draws runs on the current row; with no current row the key is left to the other bindings. An action with `CONFIRM` asks first, in the platform's dialog: the key is the platform's, while a call of the action's member asks nothing — the view asks itself where the entry says [`askConfirm`](#display-options);
- a property whose change asks no value — its `ON CHANGE` handler runs an action — runs that handler;
- a filter's key selects the filter, as the platform's widget does.

The key of an action or a property the form shows read only (`READONLY`, `READONLYIF`) is not taken while it is: it is left to the other bindings.

A mouse binding is taken as in the standard form, where its modes let it: by default on a click in a cell of a table the platform draws, not of a panel (the `panel` and `cell` modes). What a view draws is no cell of the platform's: a click there takes a mouse binding only where its `cell` mode allows one outside a cell.

The binding modes of a key work as anywhere: with `showing=all` the key works while the form shows its component, whether the view is on the page or not, and a key of a property of an object group with `group=only` asks for the focus in a view of that group the platform draws — what a view draws belongs to no group.

The platform does not bind:

- the key of a property or an action that asks a value in a cell right away — the default change of a data property, an action that starts by asking a value (`INPUT`), a field a barcode scanner types into: the platform opens the editor for it in a cell, and that field is the view's. A view that needs such a key handles it itself;
- the keys of the standard grid — `F3` and the other keys of its filters, the typing that starts a quick filter, `ENTER` down a column: over the rows a view draws there is no such grid, and the view uses the members of its [filter controls](#filter-controls) and `filters.change`.

A view of the platform's that a view places with [`<Platform name/>`](#placing) keeps its keys, as it does anywhere: they work while its host is shown, and a host the view hides — `<Platform>` hides it while the entry of what it holds says `hidden` — takes them with it.

A key pressed in a field the view draws reaches the form like any other key. A field where the user types declares it ([`startEditing`](How-to_Custom_view_controller.md#editing)), so that `ENTER`, `ESCAPE` and the keys bound with the default `editing` mode step aside; a key the view needs for itself it stops in its own handler (`stopPropagation`).

### Live data {#live-data}

Data that keeps changing while the form is open — quotes, a queue, a monitor — is refreshed by the form itself, not by a timer in the component. The [`SCHEDULE` event](../paradigm/Form_events.md) of the form runs an action every given number of seconds, and the platform delivers what changed through `props.data` like any other change:

```lsf
FORM quotes 'Quotes'
    OBJECTS q = Quote
    PROPERTIES(q) READONLY symbol, price, changedAt
    EVENTS ON SCHEDULE PERIOD 2 formRefresh()
;

DESIGN quotes {
    BOX(q) {
        custom = 'QuoteBoard';
    }
}
```

`System.formRefresh[]` re-reads everything the form shows, so the response carries the whole form on every run. There is no refresh of a single object group: `System.forceUpdate[STRING]` only applies the pending update of a group in manual update mode (the `enableManualUpdate` design attribute) and does nothing for a group updated automatically. So a live board is kept as a form of its own, with only the properties it draws, and the period is no shorter than the data needs.

The scheduled run differs from a request the component would make itself. A timer of the component's own that calls `controller.refresh.exec()` polls the server the way a click on the form would — as a synchronous request that blocks input until it completes, unless the action is drawn with `NOWAIT` — and, as the third point shows, goes on polling while the form is hidden.

- **It does not block the user.** The web client sends it as an asynchronous request, without blocking input and without the [busy indicator](../paradigm/Interactive_view.md#busy) a synchronous request gets, so the user keeps working while the form is re-read.
- **It runs only while the form is on screen.** For a form in a background tab, a form the [forms window](#forms-window) component places nowhere, or a form under a modal dialog, the run is skipped. The timer keeps counting, and the next run comes once the form is shown again.
- **A hidden form stays mounted.** A background tab and an unplaced form are hidden, not closed, so the component is still mounted and a `setInterval` of its own keeps firing — and keeps calling the server — while the form's schedule is paused. That is why the refresh belongs to the form, not to the component. A timer the component does need (an animation, a countdown) is cleared in the effect's cleanup as usual, but that cleanup runs when the form closes, not when it is hidden: what stops it while the view is hidden is [`useVisible()`](#hidden-view).

### A hidden view {#hidden-view}

The platform hides a view without taking it down: in a tab of a `tabbed` container that is not selected, in a collapsed container, in a `popup` container that is not open, under a `SHOWIF` — of the view's container or of a container above it —, and in a form that is not on screen: in a background tab, or one the [forms window](#forms-window) component does not place. The component stays mounted and keeps its state, and what it draws is not seen.

While the form hides the view's container, the server does not read what the view draws, as it does not read a hidden part of any form: the rows of a group stay as they were last sent, and a property whose value changes meanwhile reads as `null` — unless a part of the form that is shown reads the same. The first answer after the container is shown brings the current values. A form that is not on screen is read as usual.

What the component does on its own goes on all the while: a timer, a request, an animation. `useVisible()` says whether the view is on screen now — whether the element its root is mounted in is displayed: in the page, with no element above it under `display: none`, which is how the platform hides one — and draws the component again when that changes, so work of the component's own is done while it is `true`:

```jsx
const { useVisible } = window.lsfusion;

export function Clock() {
    const visible = useVisible();
    const [now, setNow] = React.useState(() => new Date());
    React.useEffect(() => {
        if (!visible)
            return undefined;
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, [visible]);
    return <span>{now.toLocaleTimeString()}</span>;
}
```

It does not say whether the view is scrolled into sight. What the component draws from its data needs no such check: a hidden view is drawn again with the current data once it is shown, as the platform's own views are. What it measures does: an element under a hidden container has no size, so a view that sizes what it draws by its own box — a chart, a map — measures it again once it is visible.

The tabs and the collapsible containers around a view are a part of the form, and the view reads and changes them as it does the rest of the form — through the properties and actions the design gives it: [`ACTIVE TAB`](../language/ACTIVE_operator.md) says whether a tab is selected, [`ACTIVATE TAB`](../language/ACTIVATE_operator.md) selects it, [`COLLAPSE CONTAINER`](../language/COLLAPSE_operator.md) and [`EXPAND CONTAINER`](../language/EXPAND_operator.md) collapse and expand a container. Here `lines` is a page of a `tabbed` container of the form `order`, and `details` a collapsible container of the same form:

```lsf
linesSelected = ACTIVE TAB order.lines;
selectLines() { ACTIVATE TAB order.lines; }
collapseDetails() { COLLAPSE CONTAINER order.details; }

EXTEND FORM order PROPERTIES() linesSelected, selectLines, collapseDetails;

DESIGN order {
    NEW steps FIRST {
        custom = 'Steps';
        MOVE PROPERTY(linesSelected());
        MOVE PROPERTY(selectLines());
        MOVE PROPERTY(collapseDetails());
    }
}
```

```jsx
<button aria-pressed={data.linesSelected.value === true} onClick={() => controller.selectLines.exec()}>Lines</button>
```

The actions run on the server, so the tab is selected when the answer comes, not on the click. Whether a container is collapsed is no property: the form's [`ON COLLAPSE` and `ON EXPAND`](../language/Event_block.md) events run when the user collapses or expands it. A container the view draws itself has no tab and no collapse of the platform's — the view shows it or not —, so `ACTIVATE TAB`, `COLLAPSE CONTAINER` and `EXPAND CONTAINER` on it are an error ([above](#placing)).

### A navigator window {#navigator-window}

A React component can also draw a navigator window — the menu itself — instead of the standard toolbar. The [`WINDOW`](../language/WINDOW_statement.md) is given the component name, and the elements placed in that window become its data:

```lsf
WINDOW appMenu VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CUSTOM 'AppMenu';

NAVIGATOR {
    NEW FOLDER sales 'Sales' WINDOW appMenu PARENT {
        NEW orders;
        NEW invoices;
    }
}
```

```jsx
function AppMenu({ data, controller }) {
    return <nav>{data.root.map(name => {
        const e = data.byName[name];
        return <button key={name} className={e.selected ? 'on' : ''}
                       onClick={ev => controller.activate(name, ev.nativeEvent)}>{e.caption}</button>;
    })}</nav>;
}
```

A component can also be given to a window that already exists, the standard toolbar included — then the navigator is not restructured at all, and only its drawing changes:

```lsf
EXTEND WINDOW System.toolbar CUSTOM 'AppMenu';
```

`props.data` is the window's own elements, keyed by [canonical name](../paradigm/Naming.md#canonicalname) — canonical names contain dots, so they are map keys rather than fields:

| Field | Meaning |
| --- | --- |
| `root` | The canonical names of the elements drawn at the top level, in display order |
| `byName` | Each element by canonical name |

and each element carries:

| Field | Meaning |
| --- | --- |
| `name` | Its own canonical name, so a component that was handed the entry alone can still address the element |
| `caption`, `elementClass` | Its caption and CSS class, the current ones — a `HEADER` or `CLASS` expression is applied as it changes |
| `image` | Its icon, drawn with `<Image value={e.image}/>` — see [below](#image) — or `null` |
| `folder` | Whether it is a folder rather than an action |
| `hidden` | Whether `SHOWIF` currently hides it. Unlike the standard toolbar, which simply omits such an element, the projection keeps it, so the component decides how to treat it |
| `selected` | Whether it is the selected element **of this window** |
| `children` | The canonical names of its children that this window draws |

An element declared [`NOCUSTOM`](../language/NAVIGATOR_statement.md) is drawn by the platform, so what it draws is not projected: its entry carries `name`, `caption`, `image`, `children` and `platformDrawn: true`, and none of the rest.

The window is given the elements it is responsible for: an element that crossed into another window, and a subtree gated out because its parent is not selected, are absent — the same rule that decides what the standard toolbar draws. What that rule keeps, the projection keeps, hidden or not. The component is drawn once the navigator has worked out the window's elements, and a window with none is drawn too, with `root` empty. `props.controller` is the navigator controller, whose [`activate`](How-to_Custom_view_controller.md#navigator-controller) does what clicking the element does: selects a folder, or runs an action.

So a window can receive elements the module never declared. A folder's `WINDOW` names the window of its children, and a child that lands in another window is drawn there while the folder is selected. In the stock application this is how Administration works: the folder itself sits in `System.system`, and its sections are declared with `WINDOW toolbar PARENT`, so while it is selected in the top bar they, and their children, are drawn by `System.toolbar` — after `EXTEND WINDOW System.toolbar CUSTOM`, by your component. A component that draws only the elements it knows about loses such sections. Everything the window was given is reached through `root` and `children`.

Only the desktop web client draws the component. The mobile web client and the desktop client render their standard menu for that window, so the navigator stays usable in all of them.

#### The standard button {#standard-button}

An element the component does not want to draw itself is placed with `<Platform name/>` — the tag that places a design child on a form, where `name` is the element's canonical name. The platform puts its own button inside, with its icon, caption, tooltip and click behavior. The node itself is left as the component rendered it, without the marks a form's host is given.

```jsx
const { Platform } = window.lsfusion;

function AppMenu({ data, controller }) {
    return <nav>
        <Platform name="Sale.sales" className="menu-main"/>
        {data.root.filter(name => name !== 'Sale.sales').map(name =>
            <button key={name} onClick={ev => controller.activate(name, ev)}>{data.byName[name].caption}</button>)}
    </nav>;
}
```

The element must be declared `NOCUSTOM` in the [`NAVIGATOR`](../language/NAVIGATOR_statement.md) — the crossing back is declared on the navigator side, the way `custom = NULL` declares it on a form:

```lsf
NAVIGATOR {
    Sale.sales NOCUSTOM;
}
```

A `NOCUSTOM` element no `<Platform>` names is not shown, as on a form. Four mistakes are reported in the page: a name no navigator element has, a name that is not `NOCUSTOM`, which the component is expected to draw from the projection instead, a name of an element another window draws, and a name a second `<Platform>` places again. A name the window merely does not draw right now — an element `SHOWIF` hides, one gated out because its parent is not selected — is not a mistake and is not reported: the place stays empty and waits, and is filled as soon as the window draws that element. Which window draws an element is not such a case: waiting for a button another window owns would wait for good.

A menu that is mostly standard is written this way as one `<Platform>` for each element the platform draws, and markup only for the elements the component draws itself.

#### An HTML template instead of a component {#navigator-template}

A window whose menu only needs its own markup is given an HTML template instead of a component name — the [same template](../language/WINDOW_statement.md) a `custom` container takes, where a `<Platform:name>` place takes the element's standard button. The name is resolved the way every other name in the module is, so nothing has to be spelled out in full, and the place is written open — closing it is an error:

```lsf
EXTEND WINDOW System.toolbar CUSTOM
    '<div class="menu"><div class="menu-head">Master data</div><Platform:items><Platform:partners></div>';
```

The element has to be declared before the template that gives it a place — a `NAVIGATOR` block that creates it goes above. A name that resolves to nothing is an error when the module is read, so a misspelled name stops the application instead of leaving the menu an item short.

An element the template gives no place is not drawn, so it names everything it shows, and `NOCUSTOM` is not needed on any of them — a template places nothing but the platform's own drawings.

The template may also be computed: given a property instead of a literal, it is recomputed as the property's value changes and the window is drawn again from the new markup, so a menu can rearrange itself without a component. Nothing resolves a computed template, so its places name their elements in full, by canonical name. A component is still what a menu needs when the markup depends on what the component itself reads.

### The forms window {#forms-window}

A React component can also draw the window the forms open in, instead of the standard tabs. The [`WINDOW`](../language/WINDOW_statement.md) `System.forms` is given the component name, and the open forms become its data - and so is any other `FORMS` window the application declares, which gets the forms opened into it with `SHOW ... WINDOW windowName`:

```lsf
EXTEND WINDOW System.forms CUSTOM 'FormsBoard';
```

```jsx
const { Platform, Caption } = window.lsfusion;

function FormsBoard({ data, controller }) {
    const current = data.open.find(name => data.byName[name].selected);
    return <div className="board">
        <div className="board-bar">{data.open.map(name => {
            const e = data.byName[name];
            return <span key={name} className={e.selected ? 'on' : ''}>
                <a onClick={() => controller.select(name)}><Caption value={e.caption || '...'}/></a>
                {!e.blocked && <a onClick={() => controller.close(name)}>x</a>}
            </span>;
        })}</div>
        {current && <Platform key={current} name={current} className="board-current"/>}
    </div>;
}
```

The component never draws a form: it renders a place for the form it wants shown, and the platform moves that form's own view into it. `<Platform name/>` is that place, and `name` is the name the projection gives the form. It places the form's view the same way it places a [design child](#placing) and a [navigator element](#standard-button), and the same mistakes are reported in the page. Like a design child's host, the place carries the marks — the class `lsf-platform` and `data-lsf-sid` with the form's name — and the form's view fills it.

`props.data` is the open forms and the state of the window itself. The component is drawn as soon as the window is, before any form opens — `open` is then empty — so what it shows while nothing is open is its own to draw:

| Field | Meaning |
| --- | --- |
| `open` | The names of the open forms, in the order the standard strip shows them in |
| `byName` | Each form by name |
| `editMode` | The name of the chosen edit mode — the one the platform's mode button chooses, and the one kept between sessions. A mode switched on by holding Ctrl, Shift or Alt lasts while the key is held and does not change `editMode` |
| `editModes` | Every edit mode, in the order the mode button offers them |
| `fullScreen` | Whether the forms window is expanded to the full screen |

and each form carries:

| Field | Meaning |
| --- | --- |
| `name` | Its own name, so a component that was handed the entry alone can still address the form |
| `canonicalName` | The [canonical name](../paradigm/Naming.md#canonicalname) of the form, for a component that draws a particular form its own way |
| `caption`, `image` | Its caption and icon, the current ones — the caption a tab shows. Drawn with [`<Caption value={e.caption}/>`](#caption) and [`<Image value={e.image}/>`](#image) |
| `selected` | Whether it is the current form — the one the keyboard works in |
| `loading` | Whether the form has not arrived from the server yet. Until it does, `caption` is the one the request that opened it carried and `image` is empty; the rest of the entry is there as always |
| `blocked` | Whether a form opened from this one is on top of it. Such a form cannot be closed, which is why the standard tab disables its close button |

and each edit mode in `editModes` carries what the mode button shows it with:

| Field | Meaning |
| --- | --- |
| `name` | Its own name — the one `setEditMode` takes, and the one `editMode` is compared with |
| `caption`, `image` | The mode's caption and icon. Drawn with [`<Caption value={m.caption}/>`](#caption) and [`<Image value={m.image}/>`](#image) |

`props.controller` does what only the platform can do to an open form: `select(name)` makes it the current one, the way clicking its tab does, and `close(name)` asks it to close, the way its close button does. Closing is a request — a form with unsaved changes asks the user first and may stay open — so a form leaves `open` when it is actually closed, not when `close` is called.

A form the component places nowhere stays **open and hidden**, which is what a background tab already is: the standard strip only hides the forms it does not show. So a component that places only the current form gives an application without tabs, where every form opened so far is still there, and placing one again shows it as it was left. A form is closed only through `close(name)` or `closeAll()`, or the way it is closed without a component view.

The platform's own toolbar — edit mode, full screen, the mobile menu — sits in the standard tab strip, so a window drawn by a component does not show it. What it does to the window itself the component does through the controller: `setEditMode(name)` chooses an edit mode, the way the mode button does, where `name` is the name of a mode in `editModes`, and `toggleFullScreen()` expands the forms window to the full screen and brings it back, the way the full screen button and ALT+F11 do. What the standard tab's context menu offers is here too: `closeAll()` asks every open form to close, starting from the last one in that order. Closing each of them is a request as well, so a form that did not close stays open and the rest are closed. The chosen mode and the full screen are state the platform holds itself, which is why it projects them: a mode chosen by the component and a full screen entered with ALT+F11 are both seen the same way, in `props.data`. The mobile menu is not something a component needs: on the mobile web client the platform draws the forms window.

Only the desktop web client draws the component. The mobile web client and the desktop client keep their standard forms window.

### The application header {#application-header}

A header with a search field that finds records is a form: it wants a live projection, a session to hold the query, ordering and paging, and a way to open what it finds. So it is built as a form and opened into a window of its own, a [`WINDOW ... FORMS`](../language/WINDOW_statement.md) above the forms area, with [`SHOW ... WINDOW windowName`](../language/SHOW_operator.md):

```lsf
// the window the header lives in: one form, drawn alone above the forms area, no taller than that form
WINDOW header 'Header' FORMS AUTOSIZE POSITION(20, 6, 80, 10) HIDETITLE;

// what the header searches with, and what it finds
query 'Search' = DATA LOCAL ISTRING[100] ();
found (Customer c) = isISubstring(name(c), query()) OR isISubstring(city(c), query());

// what a found record does: opens as an ordinary tab in System.forms
openCustomer 'Open' (Customer c) { SHOW customer OBJECTS c = c NOWAIT; }

// the header IS a form: the query is its property, the matches are its object group, the session is its own
FORM appHeader 'Header'
    PROPERTIES() query
    OBJECTS c = Customer
    PROPERTIES(c) READONLY name, city
    PROPERTIES(c) openCustomer
    FILTERS found(c)
    ORDERS name(c)
;

// the component draws it, from the ordinary form projection
DESIGN appHeader {
    // a header, not a document: no Save / Cancel / Ok / Close
    REMOVE TOOLBARBOX;
    NEW search {
        custom = 'Search';
        fill = 1;               // the container takes the window rather than hugging its content
        MOVE PROPERTY(query());
        MOVE BOX(c);
    }
}

// opened once, when the client starts; a reload is a client start again
onWebClientStarted() + {
    SHOW appHeader WINDOW header NOWAIT;
}
```

```jsx
// nothing here fetches, caches or invalidates: the query and the matches are the form's
const { useData, useController } = window.lsfusion;

function Search() {
    const data = useData();
    const controller = useController();
    const query = data.query.value || '';
    const rows = data.c.list;
    const input = React.useRef(null);

    // what the field sent last, until the projection shows it: until then the projection is behind the field
    const [sent, setSent] = React.useState(null);
    const send = value => {
        setSent(value);
        controller.query.change(value);
    };
    // the field follows the query only when something else changed it, and never while the user is in it -
    // so it catches up when the user leaves it
    const follow = () => {
        if (input.current && sent === null && input.current.value !== query && document.activeElement !== input.current)
            input.current.value = query;
    };
    React.useEffect(() => {
        if (query === sent)
            setSent(null);
        follow();
    }, [query, sent]);

    // where the results go: under the field, and following it
    const [box, setBox] = React.useState(null);
    const place = React.useCallback(() => {
        if (!input.current) return;
        const r = input.current.getBoundingClientRect();
        setBox({ left: r.left, top: r.bottom + 2, width: r.width });
    }, []);
    React.useEffect(place, [query, rows.length, place]);
    React.useEffect(() => {
        const onKey = e => {
            if (e.key !== 'Escape') return;
            if (input.current) input.current.value = ''; // the user is in the field, so follow() leaves it alone
            send('');
        };
        window.addEventListener('resize', place);
        window.addEventListener('scroll', place, true);
        document.addEventListener('keydown', onKey);
        return () => {
            window.removeEventListener('resize', place);
            window.removeEventListener('scroll', place, true);
            document.removeEventListener('keydown', onKey);
        };
    }, [place, controller]);

    return <div className="search">
        <input className="search-input" maxLength={100} placeholder="Search customers…" ref={input}
               onChange={e => send(e.target.value)} onBlur={follow}/>
        {query && box && ReactDOM.createPortal(
            <div className="search-results" style={{ position: 'fixed', left: box.left, top: box.top, width: box.width, zIndex: 1000 }}>
                {rows.length === 0 && <div className="search-empty">Nothing found</div>}
                {rows.map(row =>
                    <div key={row.key} className="search-row" onClick={() => controller.c.openCustomer.exec(row)}>
                        <b>{row.name.value}</b> <span className="search-city">{row.city.value}</span>
                    </div>)}
            </div>, document.body)}
    </div>;
}

window.lsfusion.custom.register('Search', Search, 'reactView');
```

Everything the component shows comes from the projection it reads with `useData()`: the query is `data.query.value`, the matches are `data.c.list`, kept current by the platform as the query changes - `controller.query.change(…)` is the ordinary edit, and the form's `FILTERS` re-apply.

The input is **uncontrolled** on purpose - it is not given `value={query}` - and that is the one thing a view like this must get right. The projection shows a value the field sends at once, as the platform shows an edit before the server answers, and keeps the latest one while the answers to the earlier ones arrive: the server's answer replaces it only when the server stored something else or refused the change. But the projection can change while the user types - somebody else writes the query, or the server corrects it - and a React-controlled field would be redrawn from it under the user's hands. So the field owns what the user types, and follows the projection only when something else changed it and the user is not in the field, catching up when the user leaves it. `sent` keeps the value the field sent last until the projection shows it, so the field does not take its own change for somebody else's. This relies on the query being stored as typed: it is a `DATA` property, and `maxLength` keeps the input within its `ISTRING[100]`. A click runs the action drawn on the group for the clicked row, `controller.c.openCustomer.exec(row)`, and the record opens where a form opens by default, as a tab in `System.forms`. The header stays: a `FORMS` window draws one form at a time, so a form the application later opens `WINDOW header` replaces it, while `WINDOW` without a window leaves it alone.

Two things make it read as a header rather than as a document. `REMOVE TOOLBARBOX` takes the form's own toolbar away - there is nothing here for the user to save, cancel or close, and the buttons are drawn by that container. (This is what `POPUP` and `EMBEDDED` get for free: every system button's `SHOWIF` is built from the form's environment properties, and an in-place editor sets `isEditing`, which those conditions negate. A form drawn as a header is not an in-place editor, so it says what it wants in its design instead.) The results are drawn through a **portal into the page body**, not inside the container, and that is the second thing this kind of view must get right. A window is short by design, and it clips what its form draws - a list left inside a header band is cut off after the first row. Nor is a high `z-index` enough: the window's own element is a stacking context (`position: relative; z-index: 0`), so a layer inside it can never rise above the neighbouring windows. A portal leaves both behind. The layer is then positioned from the field's `getBoundingClientRect()` and has to follow it on resize and scroll, and go away on Escape.

`AUTOSIZE` on the window is what keeps the band from being a tall empty strip: the header is as tall as its form - here one input - while still stretching across, and the `fill = 1` container inside it fills that height rather than hugging its own content. A window is sized by the layout rather than by the form in it, so without it the band keeps the share of the height its `POSITION` asks for, empty or not. Note also that the client remembers a size the user dragged, and restores it over the declared one - `Service.resetWindowsLayout` is what puts the declared sizes back. Two things about `POSITION` are worth knowing before the header looks wrong. The numbers are not a rectangle the window is drawn in but the way the layout splits the space, so a header of `POSITION(20, 6, 80, 10)` becomes a band to the right of the toolbar and above `System.forms`. And unless the navigator is fully pinned its panel is an overlay that hangs over the windows beside it: a click aimed at the header then lands on the menu underneath and runs whatever that menu item runs.

What the form route does not give is freshness across connections: a form does not see what another connection committed until it re-reads, so a header that must show that adds [`EVENTS ON SCHEDULE PERIOD n formRefresh()`](../paradigm/Interactive_view.md) to the form. And the header is drawn by the desktop web client only: the mobile web client and the desktop client draw `System.forms` alone and open the header there, as a tab, so an application that serves them keeps the header's design usable as a plain form.

### The log window {#log-window}

A React component can also draw the window the messages to the user appear in, instead of the standard panel. The [`WINDOW`](../language/WINDOW_statement.md) `System.log` is given the component name, and the logged messages become its data:

```lsf
EXTEND WINDOW System.log CUSTOM 'MessageLog';
```

```jsx
const { Platform } = window.lsfusion;

function MessageLog({ data, controller }) {
    return <div className="log">
        <a className="log-pin" onClick={() => controller.togglePin()}>pin</a>
        {data.messages.map(name => {
            const m = data.byName[name];
            return <div key={name} className={m.failed ? 'log-failed' : 'log-ok'}>
                <div className="log-line">{new Date(m.time).toLocaleTimeString()} {m.caption}</div>
                <Platform name={name}/>
            </div>;
        })}
    </div>;
}
```

The component never draws a message: a message is markup the platform built — the text with its icon, or the table an action reported beside it — so the component renders a place for each message it wants shown, and the platform moves that markup in. `<Platform name/>` is that place, and `name` is the name the projection gives the message. It places the message's markup the same way it places a [design child](#placing), a [navigator element](#standard-button) and an [open form](#forms-window).

`props.data` is the logged messages, and the component is drawn before the first one arrives, with `messages` empty:

| Field | Meaning |
| --- | --- |
| `messages` | The names of the logged messages, newest first — the order the standard panel shows them in |
| `byName` | Each message by name |

and each message carries:

| Field | Meaning |
| --- | --- |
| `name` | Its own name, so a component that was handed the entry alone can still place the message |
| `caption` | The caption of the action that logged the message, drawn with [`<Caption value={m.caption}/>`](#caption) |
| `time` | When the message arrived, in milliseconds — what the standard panel prints on its date line |
| `failed` | Whether the message is an error — the panel's only distinction between two messages, and the one a component styles from |

A message is markup, so what the projection carries is only what the standard panel writes itself beside that markup. The name is the platform's own: two messages can carry the same text, caption and second, so nothing about a message identifies it.

`props.controller` does the one thing only the platform can do to the log: `togglePin()` toggles the window's pin mode, the way the platform's pin button does. Pinning is a setting of the display environment, shared with the desktop client, so the component cannot do it itself. It is not told the current mode either, since the client is not — an unpinned window is unpinned by a class the server computes into the window.

A message the component places nowhere is still logged and still in the projection: nothing drops one, exactly as the standard panel keeps every message it printed. So a component that shows only the last message, or only the failed ones, loses nothing — an earlier message placed again is shown as it was. There is no clearing and no dismissing, in the projection or in the controller, because the platform has none.

The window itself is still the platform's: the component draws inside it, so the window is hidden, popped out over the form and revealed when a message arrives exactly as before.

Only the desktop web client draws the component. The desktop client keeps its standard message panel, and the mobile web client, whose layout has no message window at all, is left as it was.

### Drawing an image {#image}

An image the platform projects — a property of an image class, the icon of a navigator element or of an action — is drawn by `Image`:

```jsx
const { Image } = window.lsfusion;

<Image value={e.image} className="menu-icon"/>
<Image value={row.photo.value} className="avatar"/>
```

The projected value is either an address or a ready element, since a font icon has no address at all, and `Image` draws whichever it was given. A view that wants the address itself — for a CSS background, say — still has it in the value. `Image` is there so that no view has to insert the platform's markup by hand. A missing image draws nothing.

### Drawing a caption {#caption}

A caption the platform projects — a form's, a navigator element's, the one a message carries — is drawn by `Caption`:

```jsx
const { Caption } = window.lsfusion;

<Caption value={e.caption} className="tab-caption"/>
```

A caption is either plain text or markup the platform built, and `Caption` draws whichever it was given, by the same rule the platform draws a caption of its own by: the value is markup when a tag appears anywhere in it. Printed as text, such a caption would show its own markup, so a view that prints `{e.caption}` itself is right only while the caption is plain text. A missing caption draws nothing.

### Trees {#trees}

A group of a tree is projected like any other group: what a container has of it is what its own components put there,
and where the TREE's rows are drawn that is `list`, `byKey`, `keys` and the rows themselves. Each of those rows says
where it sits in the hierarchy:

```js
{
    key, isCurrent, objects, ...cells,
    parent,       // the key of the row it hangs under
    hasChildren,  // whether it has children, loaded or not
    expanded,     // whether it is open
    loading,      // true while its children are being loaded
}
```

Each field a row of a tree adds is reserved where the tree's rows are drawn, as `key` is: a property drawn on such a
row may not take its name.

`parent` is the key of the row this one hangs under, `null` at the top of the tree; it points into whichever group that
parent belongs to (a tree's rows are keyed by the whole path down to them, so the keys of its groups do not collide —
short of an object of a STRING class whose value contains `|`). It is written the way a `key` is, so
`row.parent === parentRow.key` holds rather than merely looking alike. Rows arrive under a loaded parent, so the parent
a row names is usually there — until something takes it away: a filter on an upper group drops the parent row while its
children stay (the server keeps sending them), and from then on `parent` can name a row this client does not have. So a
view that indexes rows by key must decide what to do with a row whose parent it cannot find, rather than assume the
lookup succeeded. A view finds a row's ancestors the same way, following `parent` through the `byKey` of the group
the parent belongs to, and stopping at a parent it does not have; the depth a row is indented by needs no such walk, as
it comes from drawing the tree from its roots down, as below.

`hasChildren` is what the server says the node has, loaded or not — not a design attribute, but the tree's live
state.

`expanded` says the node is open: its children are loaded — or a view asked to open or close it and the rows of the
answer have not arrived yet, and then it says what was asked, as a changed value is shown before the answer and as the
platform's own tree turns its expander glyph at the click. A node is closed at once, while its children stay in `list`
until the answer takes them away, so a view that draws a node's children only under an `expanded` node, as below, hides
them at once. Only a node with children opens: one whose `hasChildren` is false stays closed, as it does in the
platform's tree. It is live state as well.

`loading` is `true` while the node's children are being loaded: a view asked to open it, and the rows of the answer
have not arrived — as a cell's `loading` says a change of it waits for its answer. Closing a node loads nothing, and
neither does opening one whose `hasChildren` is false; the field is absent but while it is `true`.

Rows arrive as the tree is opened — past the roots, a group of a tree returns only the rows under nodes that are
currently expanded, which is why, before anything is opened, its lower groups project an empty list.

A row of a group below carries the path above it, so a row made current — through `change(row)`, or by the server —
makes the rows on that path current in their groups as well: `isCurrent` follows the path up, as the platform's tree
selects one node. The groups below it are left on no row — the server drops their current objects when a row above is
chosen, and reports nothing for them —, so no row of theirs says `isCurrent` until a row is chosen there again.

`controller.<g>.expand(row)` / `.collapse(row)` / `.toggle(row)` open and close one node: a node is a row of a group, so
they are the group's. Each takes a row (the row, its `objects` handle, or its key) and returns nothing: the row's
`expanded` changes at once, and its children arrive as rows. `toggle` asks for the opposite of what the row's `expanded`
says now, so two toggles before the answer open the node and close it again. `controller.<g>.expandAll()` opens every
node of the group and of the groups below it: on the tree's top group, the whole tree; on a group below, its nodes under
the nodes open above it. `controller.<g>.collapseAll()` closes the same nodes and every node below them, so a node
opened again afterwards shows its children closed. Their rows' `expanded` changes at once too. A group outside a tree has no such members at all: the five are
installed on a group of a tree and nowhere else, and they are reserved on that group's node, as `list` is.

```lsf
FORM catalogue
    TREE t c = Category PARENT parent(c), i = Item
    PROPERTIES(c) name
    PROPERTIES(i) name
    FILTERS category(i) == c
;
```

```jsx
// a row of the tree, the member of the group it belongs to, and what hangs under it - the tree's current row in bold
function Node({row, member, rows, current}) {
    return (
        <li>
            {row.hasChildren && <button onClick={() => member.toggle(row)}>{row.expanded ? '−' : '+'}</button>}
            <span style={{fontWeight: row.key === current ? 'bold' : 'normal'}} onClick={() => member.change(row)}>
                {row.name.value}
            </span>
            {row.expanded && <ul>{rows.filter(r => r.row.parent === row.key).map(r =>
                <Node key={r.row.key} {...r} rows={rows} current={current}/>)}</ul>}
        </li>
    );
}

export function Tree({data, controller}) {
    const rows = [...data.c.list.map(row => ({row, member: controller.c})),
                  ...data.i.list.map(row => ({row, member: controller.i}))];
    return <div>
        <button onClick={() => controller.t.expandCurrent()}>Expand the current node</button>
        <ul>{rows.filter(r => r.row.parent === null).map(r =>
            <Node key={r.row.key} {...r} rows={rows} current={data.t.current}/>)}</ul>
    </div>;
}
```

These verbs are members of the group's node, so a container has them exactly where it has that group's rows: the tree
React draws. A tree the platform draws is opened by the platform's own expander, and no react container has node verbs
for it; a view that draws its [toolbar](#tree-toolbar) opens it through the toolbar's two buttons.

What is a tree's, one for all its groups, is on a node of the tree's own, `data.<tree>` and `controller.<tree>`, where
`<tree>` is the tree's name in the form, the `t` of `TREE t` (a tree with no name gets a generated one, so a tree a view
draws a part of is named): at the top level, beside its groups' nodes. A container has it wherever it draws a part of
the tree: the tree's rows — as a group's node is where its rows are — with its [sortings](#orders), its [user
filters](#filters), its [toolbar and filter controls](#tree-toolbar). It carries no property, so a property of a group
of the tree may take any name its parts take there. Where a view draws any part of the tree, the tree's name is taken at
the top level there, so it may not be like a group the view carries, a container or a component it projects, a property
of the empty group or a member of the controller.

Where a view draws the tree's rows, the tree's node says which of them is current, and has the one verb that needs to
know it — no group's rows know it alone:

| on the node | member | what it is |
| --- | --- | --- |
| `current` | | the key of the tree's current row — the current row of the group whose current object was set last, by the view or by the server, as the platform's tree has its selected row —, `null` while there is none |
| | `expandCurrent()` | the current node opened, and every node under it |

`current` is written the way `key` is, so `row.key === data.t.current` holds, as `row.parent === parentRow.key` does.
It is `null` while the tree has no current row loaded: before a row is chosen, while the current node's group is on no
row, and while its row has not arrived or has gone — never the row above it in its stead. A row is made current through
its group's `change(row)`; `current` only says which, and has no member of its own. `controller.<tree>.expandCurrent()`
reads the current node when it is called, and does nothing while there is none or while it has no children; the nodes
it opens are `expanded` at once, and `loading` until their rows arrive. Inside a
[batch](How-to_Custom_view_controller.md#batch) it goes with the batch, so the row a `change(row)` made current earlier
in the same batch is the node it opens.

### Sortings {#orders}

A view that draws the rows of a group not in a tree carries `orders` — the group's sortings, in priority order — beside
`list` and `keys`: a sorting is the order the rows are drawn in, so it is projected where they are, and a container
holding only a panel property of the group has neither. Where it is there it is always there, and empty when the group
is sorted by nothing. Each entry is an object:

| Field | Meaning |
| --- | --- |
| `property` | The sorted property's integration SID — on a [tree](#trees)'s node, with its group's name, as below |
| `desc` | Whether it sorts descending |

The same shape the [`ORDER ... FROM`](../language/FILTER_ORDER_operators.md) operator applies and
`ORDERS ... TO` reads, with one difference: `orders` names a property the way the rest of the node does — by its
integration SID — while those operators name it by its SID on the form (`sum(o)`).

A sorting names a property of the group, as a [condition](#filters) does, not an entry of the node: the property need
not be one this view draws — a panel property of the group drawn in another container, or by the platform, is listed
by its name as any other, and the view can name it back. So a name the group draws twice would name no sorting: a form
whose rows a React view draws is refused when it is built until such properties have explicit `EXTID`s, even when one
of the two is drawn by the platform.

`controller.<group>.orders.change(...)` is a member in the same place, and only there. It changes **one** sorting,
leaving the group's other sortings and their priority alone: a property already sorted keeps its place when its
direction changes, and a new one is appended. An order with no `desc` is not a sorting, so it stops sorting by that
property. An array — `controller.<group>.orders.change(orders)` — states the list as a whole, so what it leaves out
stops sorting the group.

A change through the controller is sent at once, as the group's whole list — the same request a click on a standard
header sends —, and `orders` shows it straight away, before the rows sorted by it come back; an answer to a request made
before it does not take it back.

A [tree](#trees)'s sortings are the tree's, as its [user filters](#filters) are: where a view draws the tree's rows,
`data.<tree>.orders` lists the sortings of all its groups, group by group, top down — each group's in its own priority
order, as no group's sortings outrank another's —, each property named by its group's name and its own, `c.name`; its
groups' nodes have no `orders`. `controller.<tree>.orders.change(...)` changes them the same way: one order merges into
its group's sortings, and an array states the tree's whole list — each group gets the entries on its own properties, and
a group none of them names stops being sorted. Each group's list is sent as a group's is.

```jsx
export function OrderBoard(props) {
    const group = props.data.o;
    const sort = property => {                     // one sorting: the others keep their place and priority
        const current = group.orders.find(o => o.property === property);
        props.controller.o.orders.change({ property, desc: current ? !current.desc : false });
    };
    const arrow = property => {                    // what the group is sorted by is read back out of the projection
        const order = group.orders.find(o => o.property === property);
        return order ? (order.desc ? ' ↓' : ' ↑') : '';
    };
    return (
        <table>
            <thead><tr>
                <th onClick={() => sort('number')}>Number {arrow('number')}</th>
                <th onClick={() => sort('sum')}>Sum {arrow('sum')}</th>
            </tr></thead>
            <tbody>{group.list.map(row => <tr key={row.key}><td>{row.number.value}</td><td>{row.sum.value}</td></tr>)}</tbody>
        </table>
    );
}
```

`orders` describes the sorting the user can change, whoever stated it. A **fixed** sorting is not part of it and cannot
be: `ORDERS ... FIXED`, and an `ORDERS` entry that is an expression rather than a property drawn on the form, are
applied on the server and never sent to the client at all. Neither is the implicit ordering by the group's own objects
that makes a read stable. So the rows can be ordered by more than `orders` says, and a view cannot show an indicator for
the part it is not told about.

`orders` is what the form has: its own `ORDERS` when it opens, then every list the server reports — an
[`ORDER`](../language/FILTER_ORDER_operators.md) action's too. It is rebuilt only when that list changes: it keeps
its identity across renders in which the sortings stay the same — a change to a property value, to the rows or to the
current object leaves it as it was — so a view can compare it across renders and memoize on it.

Two more kinds of sorting are not listed, and a console message says so when one appears: a sorting on a property with
no integration SID (`NOEXTID`), which the projection has no name for, and a sorting on a property grouped in columns,
as a sorting here names a property, not one of its columns.

Handing `data.<group>.orders` back through the array form would therefore take those two sortings off — the one on a
`NOEXTID` property and the one on a property grouped in columns — while changing ONE sorting never touches what it does
not name.

Each property says whether a view should offer sorting by it: `data.<group>.<property>.noSort` is `true` for a
property whose design says so:

```lsf
DESIGN orders {
    PROPERTY(number(o)) { noSort = TRUE; }
}
```

It is stated on the property itself, so a panel property answers it as a column does. A view that draws its own
headers reads it to leave that header alone.

`noSort` refuses a sorting the USER states, not one the form does. What the form states may name such a property — a
default `ORDERS` entry on it, and an [`ORDER`](../language/FILTER_ORDER_operators.md) action naming it — the server
sorts the rows by it, and `orders` says so, as the standard grid's header shows it too: what is not offered is only the
click that would sort by it.

Asking to sort BY a property declared `noSort` is refused, and so is a list naming one property twice, a name the
group does not carry, and a property grouped in columns. Taking a `noSort` sorting off is stating an order with no
`desc`, which is allowed: what is refused is sorting by such a property, not stopping.

### Filters {#filters}

`filters` is the user filter conditions the server is filtering the group by, in the order they are applied. They are a
part of the group of their own, like its rows and its panel properties, and they are on the group's node where their
component is — where `FILTERS(<group>)` is: inside the group's box, which is where a `custom` on `BOX(<group>)` draws
the rows too, or `FILTERS(<group>)` itself when it has a `custom` of its own. There `filters` is always present, and
empty when the group is unfiltered; the group's node in any other container does not have it. A group in `PANEL` view
has none anywhere: the platform shows it no user filters either. Each condition is an object:

| Field | Meaning |
| --- | --- |
| `property` | The filtered property's integration SID — the group's one property of that name, wherever it is drawn; on a [tree](#trees)'s node, with its group's name, as below |
| `compare` | The comparison operator: `=`, `>`, `<`, `>=`, `<=`, `!=`, `_` (contains), `@` (match) |
| `value` | The value compared against, converted like any other property value |
| `negation` | Whether the condition is negated |
| `or` | Whether this condition is joined to the **next** one by OR rather than AND — so `or` on the first of two makes the pair a disjunction |

This is the shape the [`FILTER`](../language/FILTER_ORDER_operators.md) operator reads, so one vocabulary describes a
condition in `.lsf` and in the projection; `FILTERS` writes it too, with the value as a string and `or` only on a
condition joined by OR. Only the name differs: `FILTER` names a property by its name on the form, the projection by its
integration SID, and the two are different names when the property has an `EXTID`.

A condition names a property of the group, not an entry of a node: the filter area need not be where any column of
the group is drawn. So a name the group draws twice — a column `price` and a panel property `price` in another
container — would name no condition, and a form whose user filters a React view draws is refused when it is built
until they have explicit `EXTID`s.

`data.<g>.properties` lists the entries of the node it is on, so on a node that holds only the user filters it is
empty: the properties a condition can name are the group's, not the node's — `filterProperties` lists them.

The controller takes a condition in the same shape, so a projected condition that has a value can be handed back to
it as it is. A condition with no `compare` is read with the property's default comparison (its `defaultCompare` in
`filterProperties`). One on a file property is the exception: its `value` is a link to the file, not the file a
condition compares with, so the controller refuses one that has a value; one with none takes the conditions on that
property off, as for any other.

`controller.<group>.filters.change(condition)` — a member of the group where `filters` is — changes **one**
condition and leaves the group's other conditions, and their order, alone: it takes the place of the FIRST condition
on the same property — a range is two conditions on one property, and its other bound stays — or is appended when
there is none. A condition with no `value` — absent, `null` or `''` — removes EVERY condition on that property. A
condition set elsewhere that compares with no value (the platform's panel can state one) is listed with a `null`
`value`, so it does not survive being sent back — except on a `LOGICAL` property, which lists it as `false`.

Prefer it to replacing the whole list. A view that rebuilds the list merges into the conditions its handler saw, so
a handler that holds the list of an earlier render silently drops a filter set since.

```jsx
export function ItemBoard(props) {
    const items = props.data.i;
    const search = items.filters.find(f => f.property === 'name');
    return (
        <div>
            <input value={search ? search.value : ''} placeholder="name contains"
                   onChange={ev => props.controller.i.filters.change({ property: 'name', compare: '_', value: ev.target.value })}/>
            <ul>{items.list.map(row => <li key={row.key}>{row.name.value}</li>)}</ul>
        </div>
    );
}
```

An empty input clears every condition on `name`, keeping the other properties' conditions: its `value` is `''`, which
is no value.

`filters` follows the group's list as the client sends it and as the server reports it — a list a `FILTER` action
sets, say — and is rebuilt only then: it keeps its identity across renders in which nothing states its conditions anew
— a change to a property value, to the rows or to the current object leaves it as it was — so a view can compare it
across renders and memoize on it.

A change through the controller is sent at once, and `filters` shows it straight away, before the rows filtered by it
come back; until it is answered, a list the server reports for the group in the answer to an earlier request does not
replace it. The setting that makes the platform's filter panel apply changes by hand (`userFiltersManualApplyMode`)
does not hold it back: the apply button it waits for is the panel's. A view that wants one — or sends a search only
after a pause in the typing — keeps what is being typed in its own state and calls `change` when the user applies.

`controller.<group>.filters.change(conditions)` — the same thing the projection calls `data.<group>.filters`, changed
where it is read — replaces the whole list at once: an array states them all, a single condition states one. For a
"clear everything" button (`controller.i.filters.change([])`), or when the view genuinely owns the entire condition set.

Either form reports an author's mistake instead of dropping a condition: an unknown property, an unknown
comparison or `IN ARRAY`, which no condition compares by, an unknown field in a condition, a `negation` or `or` that
is not a boolean, a condition on an action or on a property grouped in columns.

A condition set elsewhere is not listed when it is on a property with no integration SID (`NOEXTID`), which the
projection has no name for, on an action, which is not something a form is filtered by, or on a property grouped in
columns, as a condition here names a property, not one of its columns. The console says which and why.

Handing `data.<group>.filters` back through the array form would therefore take those conditions off, and the ones
listed with a `null` `value` with them, while changing ONE condition never touches the conditions on other properties.

A tree has one filter box for all its groups, `FILTERS(TREE <tree>)`, and its user filters are on [the tree's
node](#trees). `filters` there lists the conditions of all its groups, those of the upper groups first, and names each
property by its group's name and its own, `c.name`, as a [`<Platform>`](#placing) names a property of a group.
`controller.<tree>.filters.change` takes a condition named the same way: the name is read up to its first dot, the
group's — a group's name has none, and an `EXTID` with a dot, which a property the platform draws may carry, is taken
back as `filters` gives it. A list it states is the tree's:
`controller.t.filters.change([])` clears the conditions of all its groups in one request, as the platform's own panel
does, and a single condition is merged into the conditions of all of them.

Beside `filters`, the user filters say what a condition can be on — `filterProperties`, design data, there from the
start: the group's properties a condition can name, wherever they are drawn, in the form's order — on a tree's node,
the properties of all its groups, those of the upper groups first. Each is an object:

| Field | Meaning |
| --- | --- |
| `property` | The property, named as a condition names it |
| `caption` | Its caption in the design, `null` where the design gives none |
| `type` | The type of a condition's value: `string`, `number`, `date`, `boolean` or `json` — the FILTER type, by which `value` is read and written |
| `compares` | The comparisons the platform's own filter editor offers for it, spelled as `filters` spells them |
| `defaultCompare` | The one that editor starts with — the one the design named, which need not be one of `compares` |

An action, a property grouped in columns and one with no integration SID are not listed: no condition can be on them.
Nor is a file property: a condition on one is listed in `filters` all the same, and can only be taken off, as above.
So a view that draws `FILTERS(<group>)` alone builds a condition editor from it, with no property of the group drawn
there:

```jsx
function FilterPanel({data, controller}) {
    const typed = data.i.filterProperties.filter(p => p.type === 'string' || p.type === 'number');
    const [choice, setChoice] = React.useState(typed[0]);
    const [text, setText] = React.useState('');
    const value = choice.type === 'number' ? (text === '' ? null : Number(text)) : text;
    return <div>
        <select onChange={e => setChoice(typed[e.target.selectedIndex])}>
            {typed.map(p => <option key={p.property}>{p.caption || p.property}</option>)}
        </select>
        <input type={choice.type === 'number' ? 'number' : 'text'} onChange={e => setText(e.target.value)}/>
        <button onClick={() => controller.i.filters.change({property: choice.property, compare: choice.defaultCompare, value})}>
            Add
        </button>
    </div>;
}
```

`value` is of the type `type` names, converted as any other property value: this editor reads a string or a number,
so it offers the `string` and `number` properties alone.

### Filter controls {#filter-controls}

The filter controls a view draws are the same whoever draws the conditions — the view, which gets `filters`, or the
platform's panel, `FILTERS(o)` placed back with `custom = NULL` —, and each does what the platform's button does there:

| on the node | member | over [conditions the view draws](#custom-filter-panel) | over the platform's panel |
| --- | --- | --- | --- |
| | `addFilter()` | the condition F3 puts in, at once | a new condition on the same cell, its editor open, as the panel's add does |
| `filtersPending`, `filtersManual` | `applyFilters()` | refused: nothing waits to be applied, each change is sent at once | the conditions typed in applied — only while `filtersPending` |
| | `resetFilters()` | the list cleared | the panel's conditions reset |

`filtersManual` is whether the platform shows the apply button: the setting that makes its panel apply changes by
hand (`userFiltersManualApplyMode`) is on, and the controls are shown — the toolbar's filter button shows and hides
them. `filtersPending` is whether that button is enabled: conditions typed in wait for it. The setting is off by
default: a change is applied at once, both stay `false`, and a view may as well not draw `FILTERCONTROLS(o)` at all —
the group is filtered the same. Where the view draws the conditions too, `resetFilters()` clears them as
`filters.change([])` does: members of two components may do the same thing, each component's part repeating its
component.

```jsx
function ItemFilterButtons({data, controller}) {
    const items = data.i, buttons = controller.i;
    return <div>
        <button onClick={() => buttons.addFilter()}>+</button>
        {items.filtersManual &&
            <button disabled={!items.filtersPending} onClick={() => buttons.applyFilters()}>Apply</button>}
        <button onClick={() => buttons.resetFilters()}>Reset</button>
    </div>;
}
```

The cell a condition is added on is the current cell of the standard grid where the platform draws the rows, and the
first column a view carries that the form shows and a condition can be on — a view has no current column —, in its
current row, where a view draws them. On a tree it is the current cell of the platform's tree, or the first column of
the [current node](#trees)'s group that the form shows and a condition can be on, in that group's current row.
With no `FILTERS(o)` in the design `addFilter` is refused, as the platform has no such button, and F3 does nothing.

Inside a [batch](How-to_Custom_view_controller.md#batch) the commands go with it, as `filters.change` does.

### Filter groups {#filter-groups}

A filter group a view draws is on its group's node under its own name, shaped like a property's entry:

```lsf
FORM items
    OBJECTS i = Item
    FILTERGROUP stock
        FILTER 'In stock' balance(i) > 0 'F10' DEFAULT
        FILTER 'Out of stock' balance(i) <= 0
;
```

```js
// data.i.stock
{value: 0, filters: ['In stock', 'Out of stock'], canClear: true}

controller.i.stock.change(1);      // select 'Out of stock'
controller.i.stock.change(null);   // none, as the platform's "(All)"
```

| Field | Meaning |
| --- | --- |
| `value` | The index of the filter selected in `filters`, `null` for none |
| `filters` | The filters' captions, in the design's order |
| `canClear` | Whether none may be selected, as the platform's widget offers it: a group of one filter is its check box, always clearable; a group of several is its list, with `(All)` unless `NONULL`. Where it is `false`, `change(null)` is refused |

The server applies the filter selected and reports it, so `value` is what the server holds — the default filter when the
form opens, a `FILTERGROUP` action's choice — and a filter a view selects shows at once. `change` takes the index of one
of `filters`, or `null`, and anything else is refused. It is a part of a [batch](How-to_Custom_view_controller.md#batch)
as `filters.change` is, and asks the server even for the filter shown — after a batch that failed, it is set again. A
filter's key keeps working while the form shows the filter group — its descriptor's `hidden` — and the view that draws
it is shown, the host a container places that view in included, as the platform's widget takes it: it selects the
filter, and in a group of one filter — the platform's check box —, pressed again, takes it off.

A filter group is its group's — the last group for the objects its filters take, the one a
[filter](../paradigm/Form_structure.md#filters) of the form is applied to —, a group of a [tree](#trees) included: it is
on that group's node, `data.<group>.<filterGroup>`, never on the tree's, which carries what the platform has one of for
the whole tree — its user filters, filter controls, toolbar and sortings —, though the platform's design puts a tree's
filter groups in the tree's `FILTERGROUPS(TREE <tree>)`.

### Property filters (`FILTER`) {#property-filters}

A property declared a [user filter](../paradigm/Interactive_view.md#userfilters) — `PROPERTIES(i) name FILTER`, or
`USER` in the [filters block](../language/Filters_and_sortings_block.md) — has a filter of its own among the user filters
of its group: a component the design names `FILTER(name(i))`, by the property's name on the form, which the platform's
filter panel shows as a condition row there from the start. A property filter a view draws is a part of the group of
its property there, as a filter group is: on the group's node, under `filter`, by the property's integration name — one
object for the filters the view draws, with no prototype, as `byKey` is:

```lsf
FORM items
    OBJECTS i = Item
    PROPERTIES(i) name FILTER, price
;

DESIGN items {
    NEW search FIRST {
        custom = 'ItemSearch';
        MOVE FILTER(name(i));
    }
}
```

```js
// data.i.filter.name
{caption: 'Name', type: 'string', compare: '_', negation: false, value: null}

controller.i.filter.name.change('ab');                     // filtered by it
controller.i.filter.name.change('ab', {negation: true});   // ... by its negation
controller.i.filter.name.change(null);                     // ... and not any more
```

| Field | Meaning |
| --- | --- |
| `caption` | The property's caption in the design, `null` where the design gives none |
| `type` | The type of a condition's value: `string`, `number`, `date`, `boolean` or `json` — the FILTER type, by which `value` is read and written |
| `compare`, `negation` | The comparison of the filter's condition, spelled as `filters` spells it, and whether the condition is negated; while there is no condition, the property's default comparison and `false` |
| `value` | The value of the filter's condition, `null` while there is none |

The filter's condition is the first condition on its property among the group's user filters — as the platform's row of
the filter takes the first condition on its column —, whoever states it: the filter, `filters.change`, the platform's
filter panel, a `FILTER` action. `change(value, options)` states its value: in that condition's place, or, where there
is none, appended. The options, `{compare, negation}`, either or both, state the condition's comparison — spelled as
`filters` spells it — and its negation, as the platform's row does with the filter controls shown; what they leave out
stays as the condition has it, and is the property's default comparison and no negation for a new condition; its
junction is kept. An option `change` does not know, a comparison that is no comparison of a condition — an unknown one,
`IN ARRAY` — and a negation that is not `true` or `false` are refused, with no value too, which then takes nothing off.
No value — absent, `null` or `''` — takes that condition off, whatever the options say, and that one alone: a range's
other bound stays, as when the platform's row is emptied. It goes with a [batch](How-to_Custom_view_controller.md#batch)
as `filters.change` does. The condition of a filter on a file property can only be taken off: its `value` is a link to
the file, not the file a condition compares with.

```jsx
export function ItemSearch({data, controller}) {
    if (data['FILTER(name(i))'].hidden) return null;
    const filter = data.i.filter.name;
    return <label>{filter.caption}
        <input value={filter.value ?? ''} onChange={e => controller.i.filter.name.change(e.target.value)}/>
    </label>;
}
```

A property filter does not depend on who draws the group's conditions: this view, which then gets `filters` too, or
another view — a change is then sent at once —, or the platform's filter panel. Over the platform's panel the filter is
the panel's own row of it, which the view draws: the panel shows no row of its own for it, and states its condition as
it states its rows' — at once, or, in its manual apply mode with its filter controls shown, when the user applies the
conditions, the filter showing the change meanwhile. A filter of a property of a [tree](#trees)'s group is on its
group's node, as a filter group of a tree's group is. Its [entry](#display-options), `data['FILTER(name(i))']`, says
whether the form shows the filter — its `SHOWIF`, or a container's above it.

A property filter with `custom = NULL` is the platform's: a row of its filter panel, as a classic form shows it, which
the view places with `<Platform name="FILTER(name(i))"/>` as any child the platform draws ([placing](#placing)). A
filter with a React component of its own — `FILTER(name(i)) { custom = 'NameSearch'; }` — draws its part in its own
view ([A component's own view](#component-views)).

Some combinations are refused when the form is built:

- a filter a view draws of a group in `PANEL` view, which has no user filters;
- a filter a view draws on a property its condition cannot name or give a value to: one with no integration name
  (`NOEXTID`), one grouped in columns, an action;
- a filter the platform has to draw where it builds no filter panel: one given `custom = NULL` in a view where a view
  draws the group's conditions or the toolbar turns the filters off (`showFilters = FALSE`), and one standing outside
  React where a view draws the conditions.

### Replacing only the filters {#custom-filter-panel}

A group's generated boxes — `FILTERS(o)`, `FILTERBOX(o)`, `FILTERGROUPS(o)`, `TOOLBARBOX(o)`, `TOOLBARLEFT(o)`,
`TOOLBARRIGHT(o)`, `TOOLBAR(o)`, `POPUP(o)`, `PANEL(o)` — are ordinary containers, so each of them takes `custom`.
Putting a component on one of them replaces just that part: the rows keep their standard grid, and the rest of the
box stays the platform's.

```lsf
DESIGN items {
    FILTERS(i) { custom = 'FilterPanel'; }
}
```

Such a component does not draw the group's rows — the platform still does — but the user filters are its part:
`props.data.<g>` is the group's node there, carrying `filters`, what a condition can be on (`filterProperties`) and
nothing of the rows.

The platform draws no conditions of its own for such a group, but its commands over them stay, and each changes the
group's list — the one the component is shown in `filters`:

- the toolbar's filter button shows and hides the controls the platform draws in `FILTERCONTROLS(o)`. Their reset clears
  the list, and their add does what F3 does. Where the component's box holds `FILTERCONTROLS(o)` too, the component
  draws them: they are its [filter controls](#filter-controls), whose `resetFilters()` and `addFilter()` do the same,
  and the toolbar has no filter button;
- F3 in the grid puts in the condition of its [current cell](#filter-controls) — on that cell's property, compared the
  property's default way with the cell's value — in the place of the first condition on that column, or at the end. It
  applies at once, as F3 and Enter do on the platform's panel: there is no editor here to type a value in, the component
  edits it. Alt+F3 makes it the only condition. None is started where no row is current — an empty grid, or a pivot, a
  map, a calendar or a custom view, which have no cell —, or on a cell whose property `filterProperties` does not list
  — a file property's cell excepted: F3 starts a condition on it, which a view can only take off —, or that shows
  another value than it is filtered by;
- Shift+F3 and Escape in the grid clear the list;
- typing in a column starts no condition (a grid with a quick search searches, as it does anyway), and there is no
  apply button: a change is sent at once, and the manual-apply setting holds nothing back.

The property filters the design declares in the box are the component's: it draws them as [property filters](#property-filters).

These commands are there wherever a view draws the FILTERS box of a group in `LIST` view, whatever its
`TOOLBARSYSTEM(o)` says: `showFilters`, like the toolbar's other `show…` flags, is the platform's alone, and turns off
only what the platform draws of the filters — its panel, its controls in FILTERCONTROLS and the toolbar's filter button.
With `showFilters = FALSE` and the conditions drawn by a view, the [filter controls](#filter-controls) a view draws and
the keys of the grid work all the same; with the conditions the platform's, there are no commands, and the members of
the filter controls a view draws refuse. A tree's commands are the same, over the conditions of all its groups, its
[node's](#trees) `filters`, and so is its toolbar's `showFilters`.

`props.controller.<g>.filters` changes them, so the panel both shows the current conditions and sets them: the
search box of [`ItemBoard`](#filters), without its list, is such a panel.

The conditions the component sets are the group's own: a `FILTER` action, a pivot's drill-down and this component all
change one list, so they cannot disagree.

`TOOLBARSYSTEM(o)`, `FILTERCONTROLS(o)`, `FILTERGROUP(f)` and a [property's filter](#property-filters), `FILTER(name(o))`, are not containers, but each takes a `custom` of its own, and a React component then draws it in its place ([A component's own view](#component-views)).

Receiving a group's state does not make a component the only one drawing it. Which components are drawn is decided
by the design and by what is placed with `<Platform>`, not by what `data` contains — the state is the group's, whoever draws it. So
there are two recipes, and they read differently in the design rather than in the projection:

- **replace the filters** — `custom` on `FILTERS(o)`: the component *is* the filter area;
- **replace the whole group** — `custom` on `BOX(o)`: nothing else is drawn at all, and the component owns the
  filters along with the rows.

A component on a box beside `FILTERS(o)` — `TOOLBARLEFT(o)`, say — gets no part of the filters: the group's node
there, if it has one, has no `filters`.

### Toolbar {#toolbar}

Where a view draws a group's toolbar, what its buttons show is on the group's node, `props.data.<g>`, and its buttons
are members of the group, on `props.controller.<g>`. The platform draws no toolbar of its own for the group then, over
its own grid as well. A view's header over the standard grid:

```lsf
DESIGN items {
    NEW header FIRST {
        custom = 'ItemHeader';
        MOVE TOOLBARSYSTEM(i); // the grid stays the platform's; its toolbar is the view's
    }
}
```

```jsx
function ItemHeader({data, controller}) {
    const items = data.i, buttons = controller.i;
    const [count, setCount] = React.useState(null);
    return <div>
        <button onClick={async () => setCount(await buttons.getCount())}>Count</button>
        {count != null && <span>{count} records</span>}
        <button onClick={() => buttons.export()}>Excel</button>
        <label>
            <input type="checkbox" checked={items.manual}
                   onChange={e => buttons.manual.change(e.target.checked)}/> manual update
        </label>
        {items.manual && <button disabled={!items.updatePending} onClick={() => buttons.update()}>Update</button>}
        <select value={items.listView} onChange={e => buttons.listView.change(e.target.value)}>
            {items.listViews.map(view => <option key={view}>{view}</option>)}
        </select>
    </div>;
}
```

Every member, and what its button shows, is there whatever the design's flags say — `showFilters`, `showViews`,
`showCalculateSum`, … are the platform's alone, which draw its own toolbar; a view draws what it wants —, and so is the
name each takes on the node: where a view draws the toolbar, no property of the group takes `getSum`, whatever
`showCalculateSum` says. `visible` is not one of these flags: a toolbar the design hides (`visible = FALSE`) is no part
of any view — nothing of it on the node, no members, no names taken.

| on the node | member | what it does | the platform's button |
| --- | --- | --- | --- |
| `manual` | `manual.change(on)` | the manual update turned on or off | the manual update |
| `updatePending` | `update()` | the changes the server holds back while the update is manual, brought | the update |
| `filterControlsVisible` | `filterControlsVisible.change(shown)` | whether the platform's filter controls are shown — the buttons of its FILTERCONTROLS and the condition editors of its FILTERS panel; shown on a panel with no conditions, with one to fill in | the filter button |
| `listView`, `listViews` | `listView.change(name)` | the rows of the platform's table shown in another view type, one of `listViews`: `grid`, `pivot`, and `custom`, `map` or `calendar` where the group has one | the view type switch |
| | `getCount()` | the number of the group's rows — of the marked ones only, where the user has marked any, as the platform counts them —, as a `Promise` of the server's answer, a request as `getValues` is | the count |
| | `getSum(property)` | the sum of a column — over the marked rows only, where the user has marked any, as the platform sums it —, as a `Promise`: a number property of the group, named as a [sorting](#orders) names one, not one grouped in columns | the sum, of the current column — a view has none, so the call names it |
| | `export()` | the group's rows exported to Excel, the server's report of them | the export — which on a pivot exports the pivot |

What the buttons act on is the platform's: `filterControlsVisible` shows and hides the PLATFORM's filter controls — its
FILTERCONTROLS buttons and its panel's editors, and where the platform has none for the group — the view draws the
conditions and `FILTERCONTROLS(o)`, or the toolbar turns the platform's filters off (`showFilters = FALSE`) — it stays
`false` and its change is refused, saying why. Controls a view draws itself are its own to show, its [filter
controls](#filter-controls). The view type switch switches the platform's table: where a view draws the rows,
`listViews` is empty, `listView` is `null`, and a change is refused. A group whose rows a view draws has the default
`GRID` list view type: one the form gives another (`OBJECTS o = Order PIVOT`, and `MAP`, `CALENDAR`, `CUSTOM`) is
refused when the form is built, since the server starts such a group as the platform's view of that kind needs it —
give what draws the rows, the group's grid or its tree, `custom = NULL` for the platform to draw them there.

Inside a [batch](How-to_Custom_view_controller.md#batch) the members that ask the server are refused. The settings of
the platform's table are not there — a dialog of the platform's, over the table it draws. A toolbar placed back with
`custom = NULL` keeps them.

### A tree's toolbar and filter controls {#tree-toolbar}

A [tree](#trees) has one toolbar and one set of filter controls for all its groups, `TOOLBARSYSTEM(TREE t)` and
`FILTERCONTROLS(TREE t)`. Where a view draws them, they are on [the tree's node](#trees), `data.t`, whoever draws the
tree's rows:

```lsf
DESIGN catalogue {
    NEW header FIRST {
        custom = 'CatalogueHeader';
        MOVE TOOLBARSYSTEM(TREE t); // the tree stays the platform's; its toolbar is the view's
    }
}
```

```jsx
function CatalogueHeader({data, controller}) {
    const buttons = controller.t; // t, the tree's name
    return <div>
        <button onClick={() => buttons.expandCurrent()}>Expand the current node</button>
        <button onClick={() => buttons.expandTree()}>Expand the tree</button>
        <button onClick={() => buttons.export()}>Excel</button>
        <label>
            <input type="checkbox" checked={data.t.filterControlsVisible}
                   onChange={e => buttons.filterControlsVisible.change(e.target.checked)}/> filters
        </label>
    </div>;
}
```

The toolbar has the buttons of the platform's tree toolbar as members, whatever its `show…` flags say — they are the
platform's alone, as a group's [toolbar](#toolbar)'s are —, and the two that open the tree only where the platform draws
the tree's rows:

| on the node | member | what it does | the platform's button |
| --- | --- | --- | --- |
| `filterControlsVisible` | `filterControlsVisible.change(shown)` | whether the platform's filter controls of the tree are shown, as on a group's [toolbar](#toolbar) | the filter button |
| | `export()` | the tree exported to Excel, the server's report of it | the export |
| | `expandCurrent()` | over the platform's tree: its selected node opened, and every node under it — a node with children only | expand current object |
| | `expandTree()` | over the platform's tree: every node of the selected node's group under the nodes open above it, and of the groups below — all of the tree when the selected node is the top group's | expand whole tree |

Where a view draws the tree's rows, the toolbar has not the two that open it: the tree opens through members it has
already — its current node through the tree's own [`expandCurrent()`](#trees), on the same node where the view draws
both, the tree from a group down through that group's `expandAll()` — all of it through the top group's —, one member
for each. So which members open a tree depends on who draws its rows, and a view that draws the tree's toolbar in
another container than the tree's rows has none there: the view that draws the rows has them. They only open — the
platform's tree closes nothing from its toolbar. Inside a [batch](How-to_Custom_view_controller.md#batch)
`expandCurrent()` and `expandTree()` go with it, as `expandAll()` does, and `export()` is refused.

The tree's filter controls are a part of the tree the same way, with the state and members of a group's
[filter controls](#filter-controls), over the conditions of all its groups.

### The platform's toolbar and filters over a view's rows {#platform-toolbar-and-filters}

Where a view draws a group's rows, the platform still draws what the design leaves to it of the group's toolbar and
filters — `TOOLBARSYSTEM(o)`, and what `FILTERS(o)` and `FILTERCONTROLS(o)` hold — when they are outside the view's
containers, or placed back with `custom = NULL`:

```lsf
DESIGN items {
    NEW cards {
        custom = 'ItemCards';
        MOVE GRID(i); // the view draws the rows; BOX(i) keeps the toolbar and the filters
    }
}
```

They work over the view's rows as over the standard grid, with what those rows can tell them:

- the toolbar has the buttons that need no table: the filter button, the count of the rows, the export and the manual
  update — not the view type switch, the settings or the sum of a column;
- the filter panel's conditions can be on the columns the view carries, by their captions, and a new one starts on [the
  cell a condition is added on](#filter-controls). The panel states them as the group's list, the one a `FILTER` action
  sets, and the view's rows follow it;
- where the view draws the conditions too — it gets the group's `filters` —, the platform builds no panel: its controls
  and the toolbar's filter button give the [commands over the view's conditions](#custom-filter-panel);
- the keys of the platform's grid are not there, so the controls open from the toolbar's filter button — the
  `filterControlsVisible` member of the view's [toolbar](#toolbar), where the view draws it —, and where the design hides the
  toolbar, nothing opens them: they start hidden, as over the standard grid, and stay so.

A [tree](#trees) a view draws (`MOVE GRID(TREE t)`) is given the same, one for all its groups, as the platform's own
tree has it — `TOOLBARSYSTEM(TREE t)`, and what `FILTERS(TREE t)` and `FILTERCONTROLS(TREE t)` hold. The toolbar has the
tree's buttons: over the view's rows the one that expands the current object opens what the tree's
[`expandCurrent()`](#trees) opens, the one that expands the tree opens, as over the platform's tree, every node of the
current node's group under the nodes open above it and of the groups below — nothing before a node is current; all of
the tree is the top group's `expandAll()` —, and the export exports the tree. The panel's conditions can be on the
columns of all the tree's groups, each condition of its column's group, and a new one starts on [the cell a condition is
added on](#filter-controls).

What the view draws itself the platform does not build: a `custom` on `BOX(o)` draws the whole group.

### A component's own view {#component-views}

A table, a toolbar, filter controls, a filter group and a property's filter — `GRID(o)`, a tree's `GRID(TREE t)`,
`TOOLBARSYSTEM`, `FILTERCONTROLS`, `FILTERGROUP(fg)` and `FILTER(<property>)` — take a React component in `custom` as a
container does, and the component then draws, in their place, what they show of their object group or tree: the rows,
the toolbar's buttons, the filter controls, the filter selected in the filter group, the property filter's condition.
The rest of the form stays as it is:

```lsf
DESIGN items {
    GRID(i) { custom = 'ItemCards'; } // the toolbar and the filters over the rows stay the platform's
    TOOLBARSYSTEM(j) { custom = 'OrderToolbar'; } // over the platform's grid of j
}
```

The component gets what a container holding that component alone would get — `NEW cards { custom = 'ItemCards';
MOVE GRID(i); }` —: the part of the group or of the tree on its node, under the same names ([the table at the
top](#the-component)) — the rows and the sortings on `data.i` with `controller.i.change(row)`, the toolbar's state and
members on `data.j` and `controller.j`, a tree's rows on its groups' nodes and its own on `data.<tree>`, a filter group
on `data.<group>.<filterGroup>`, a property's filter on `data.<group>.filter.<property>`. Nothing else is on the top
level:

```jsx
export function ItemCards({data, controller}) {
    const items = data.i;
    return <div className="cards">
        {items.list.map(row =>
            <div key={row.key} className={row.isCurrent ? 'current' : ''} onClick={() => controller.i.change(row)}>
                {row.name.value}
            </div>)}
    </div>;
}
```

The component's own design attributes are its view's, as they are of the platform's view of it: its size and its
place in its container, its class, and its `SHOWIF`, which hides the view — so its data has no [entry](#display-options)
of the component itself, which a container around it would have. Inside what a React container draws, the component
has a view of its own, and the container places it with `<Platform name="GRID(i)"/>`, its entry in the container's
data, and its parts in the data of its own view. A property the platform draws in the rows it draws is placed per row
with [`<Platform name row/>`](#live-editor); it places nothing else.

Some combinations are refused when the form is built:

- a component that shows nothing there — the table, the toolbar or the filter controls of a group in `PANEL` view,
  which has no rows, and a filter of its property, which has no user filters to be among; a toolbar hidden with
  `visible = FALSE`; a filter group with no filters or of no object — would give its component nothing to draw;
- a table of a group in another list view type than `GRID` — `PIVOT`, `MAP`, `CALENDAR`, `CUSTOM` —, as any view of
  its rows is ([Toolbar](#toolbar));
- a table the design moves components into — `MOVE PROPERTY(note(i))` inside `GRID(i) { ... }`, the
  [record](../paradigm/Form_design.md#record) a `CUSTOM`, `MAP` or `CALENDAR` view pops up for a row: its view places
  no view of the platform's, so a container around the table, `NEW cards { custom = 'ItemCards'; MOVE GRID(i); }`,
  draws or places what the record holds;
- a value other than a component name: `''`, an HTML template and a property lay out the children of a container.

The attributes of the platform's table — a table's `valueClass` given by a property, a tree's `hierarchicalCaption` —
mean nothing to its own view, as to a table a React container draws: the projection carries neither.
