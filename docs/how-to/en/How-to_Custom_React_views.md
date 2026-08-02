---
slug: "/How-to_Custom_React_views"
title: 'How-to: Custom React form views'
---

A [`DESIGN`](../language/DESIGN_statement.md) container can be rendered by a React component instead of the standard layout. The component receives a projection of the form state and draws the container's whole subtree itself.

This is a web-client feature only. The desktop client deserializes the container and renders its regular (non-React) subtree, so the design stays usable in both clients.

### Selecting the component

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

The value form selects the renderer: a string literal matching `[A-Z][A-Za-z0-9_$]*` names a React component, while an empty string `''`, an HTML template string, or a property gives the classic (non-React) custom container described in [How-to: Custom Components (objects)](How-to_Custom_components_objects.md). Here the object `o` is rendered by the `OrderBoard` React component instead of the standard table.

### The component

`OrderBoard` is a named export from a `.jsx` module under `src/main/web`; how the module is compiled and registered is covered in [How-to: Custom client JS modules](How-to_Custom_client_JS_modules.md). The examples here use JSX. For a project [without the build](How-to_Custom_client_JS_modules.md#without-the-build), ship the same component as a `.jsx` file — it is transformed on the server when served; `import` is not available there, so the component works against the platform-provided `window.React` — or write it with `React.createElement` in a plain `.js`. Either file is placed under `src/main/resources/web/init` (auto-loaded) or under `src/main/resources/web` and registered with `onWebClientInit`.

The component is a plain function that receives `props.data` and `props.controller`:

```jsx
export function OrderBoard(props) {
    const orders = props.data.o.list;
    return <div className="order-board">{orders.length} orders</div>;
}
```

`props.data` is the form projection — its own for each `CUSTOM REACT` container, holding only what that container owns, so a second React container on the form gets a separate one. Every property, group and container it projects is an object in `data`: read a value as `.value` and its attributes as sibling fields, and read a list property's column attributes at `data.<group>.<prop>`. It contains a group only when that group's box is nested inside the custom container the view renders: `props.data.<g>` is `{ list, byKey, keys, count, options }` for each such group object SID `g`, where `list` is the array of rows in display order, `byKey` maps a row's key string to the same row object, and `keys` is the array of those key strings in the same order. `list`, `byKey` and `keys` are always present: they are empty when the group has no rows — a panel-only group, or one before its rows first arrive — so the view reads `props.data.<g>.list` directly without guarding a missing field. A group whose box is outside the container — or `REMOVE`'d from the design — is **absent** from `props.data` (`props.data.<g>` is `undefined`), so to feed a group's data to the view keep its box inside the custom container. A group's panel properties are members of the group itself, keyed by integration SID and present once the group has a current object, and each form-level (no-group) property is a member of `props.data` directly, under its integration SID. A list property's column attributes are a member of the group too, at `data.<g>.<prop>` — one entry per column — while its per-row value and cell attributes live on each row. Actions are projected the same way as properties — an action drawn on a group is a field of each row (a list action) or of the group node (a panel action), an object of its attributes like a property, so `controller.changeProperty('<group>.<action>', row)` runs it; its `value` field is there too but carries nothing. `count` and `options` are the group's own [display options](#display-options). Each row carries:

| Field | Meaning |
| --- | --- |
| `key` | A stable public id for the row — use it as the React key |
| `isCurrent` | Whether the row is the current (selected) one |
| `<integrationSID>` | Each list property, an object `{ value, ...cell attributes }` keyed by the property's integration SID — read its value as `.value` |
| `objects` | An opaque row handle the controller uses to address the row |
| `background`, `foreground`, `selected` | The row's own [display options](#display-options): its background and text colors and whether it is selected |

A property grouped in columns (`COLUMNS`) is not projected at all: it has neither a column entry nor a cell entry, and nothing is reported — its values are addressed by a row-and-column key the projection has no place for. Declare an ordinary property if the view has to read those values.

`key`, `isCurrent`, `objects`, `background`, `foreground` and `selected` are reserved row field names; `list`, `byKey`, `keys`, `count` and `options` are reserved on the group. There is no `meta` object anywhere. A form whose projected integration SID takes a reserved name, or where two projected items claim the same name at one data level, is rejected with an explicit error when it is built.

A property's `value` is converted to a JS value depending on the property's class:

| Property class | JS value |
| --- | --- |
| `BOOLEAN` | `true` / `false` (`false` instead of `NULL`) |
| `TBOOLEAN` | `true` / `false` / `null` |
| numeric classes | number |
| user classes | number — the object's internal id |
| date and time classes | `Date` |
| `JSON` | parsed JSON value |
| file classes | string with a download link |
| images | string with the image address or HTML |
| other classes | string |

Except for `BOOLEAN`, a `NULL` value is converted to `null`.

`list` contains only the read page, not all rows of the group. The view type of a group rendered by a React container remains the table, and the group is read page by page, but since the table itself is not displayed, the page size is not adjusted to the visible rows — the server default page size (50 objects) applies. For a view that shows all rows of the group — a calendar, a board, a map — specify the `PAGESIZE 0` option (read all objects) or an explicit page size in the [`OBJECTS`](../language/Object_blocks.md) block.

A view that lays rows out in their own order — a card flow, a feed — can instead keep the page and let it follow the scroll, the way the standard table does. `useSeekOnScroll(controller.<group>)` returns a function that marks each row's element; the hook watches which of them are on screen — wherever the scrolling happens, in the component's own box or a platform container above it — and follows the table's own rules: while the current record is on screen, scrolling changes nothing; when it leaves, it is reseated on the visible edge it left through, which is also what requests the next page; on a page change the reseated row keeps its on-screen position, so nothing jumps under the eye; a current record moved from outside — a click, a programmatic seek — is scrolled into view instead. The pact rests on the same contract the table itself lives by, and it is the developer's to keep: the page must hold more rows than the viewport can show — set `PAGESIZE` on the [`OBJECTS`](../language/Object_blocks.md) block accordingly. The window then always extends a page beyond the current record, so it leaves the viewport — and reseats, pulling the next page — before the scroller runs out of room at a loaded edge:

```jsx
const seekRef = useSeekOnScroll(controller.o, { enabled: follow });
...
{rows.map(row => <div key={row.key} ref={seekRef(row)}>...</div>)}
```

Options: `enabled` (default `true`) suspends the tracking, `threshold` (`0.6`) is how much of an element must be visible to count as on screen, `settle` (`250` ms) is how long the scroll must be still, and `onSeek(row)` is called for each issued seek. Use one `useSeekOnScroll` per scrolling element. The rows delivered for the new position replace `list`, and everything built from it — values and [placed lsFusion views](#lsf-child) alike — follows.

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

### Reading the projection {#reading}

The component reads the projection in one of two ways, and the choice is about who re-renders when data changes.

`props.data` is the whole snapshot. The component re-renders — with a fresh `data` — whenever anything in its scope changes, and renders everything it draws from the new snapshot. For a small view this is the whole story: no hooks, a plain function of the data, and the examples above are written this way. It is also the only way to read the scope as a whole — enumerate its top-level entries, read the containers.

`useData(selector)` subscribes the component to a slice: it re-renders only when the reference of `selector(data)` changes — so the selector must return something the projection already holds (a group node, a row, an entry, a value), never a new object or array built inside it, which would differ on every read and re-render without end. Because of structural sharing — an unchanged node, row or entry keeps its previous reference — this is what lets a component pay only for what it reads: a root subscribed to its group node (`useData(s => s.o)`) ignores the rest of the scope, a row component subscribed to its own row (`useData(s => s.o.byKey[rowKey])`) ignores the other rows. `useController()` returns the same `controller` the root receives as a prop — its identity never changes for the life of the root, so it is safe in dependencies and closures, and passing it down as a prop is equally fine.

`useData` and `useController` read whatever root the component is mounted under, and know nothing about forms in particular. `Lsf` and `useLsf` place a view the platform draws itself, so they serve a form container and a [navigator window](#navigator-window) alike. The helpers built on the group and row shape of a form projection — `List`, `BucketScope`, `useBucket`, `Buckets`, `useSeekOnScroll` — are form-only: they read `data.<group>.list` / `.byKey` / `.keys`, which another kind of root does not have.

The two compose into tiers, and each following one is only needed when the previous one's re-render becomes the cost:

1. a small view — `props.data`, render everything;
2. a longer list — `props.data` plus row components memoized at module level (`React.memo` bails out on the unchanged rows' stable references), or [`List`](#rendering-rows) which does exactly that;
3. a big board — subscriptions down the tree: the root subscribes to nothing, a column to its [bucket](#bucketing), a row to itself, so one edited value re-renders one card.

### Display options {#display-options}

For every property drawn on a form the platform computes semantic options that determine how it is shown — its caption, image, background and text colors, whether it is read-only or disabled, its comment, the text shown in an empty cell, its tooltip. They come from the property's design and from the data-dependent options of the [properties and actions block](../language/Properties_and_actions_block.md) (`HEADER`, `IMAGE`, `BACKGROUND`, `FOREGROUND`, `READONLYIF`, `OPTIONS` and the rest), so an option that depends on the data is recomputed per row. Native element classes and fonts are not projected: a React component owns its CSS. `background` and `foreground` are projected — they are `BACKGROUND` / `FOREGROUND` highlighting, a data-dependent business signal, not a theme the component's own CSS should own. For a property the React container draws itself, the semantic result is projected into `data` as sibling fields of the property's value, so the view does not have to recompute it.

The projection follows what an option describes — a whole column, one cell, or the row:

| Where | What it holds |
| --- | --- |
| `data.<g>.<integrationSID>` | For a property shown in the table — its column attributes, one entry for the whole column: `caption`, `image`, `footer`, `comment`, `tooltip`, `defaultValue`. For a panel property of the group — the property's `value` and its attributes, read for the current object |
| `data.<g>.list[i].<integrationSID>` | One cell of a table property — its `value` for that row and the cell attributes computed for that row: `readOnly`, `disabled`, `background`, `foreground` and the rest |
| `data.<g>.list[i]` | The row's own options, direct on the row: `background`, `foreground`, `selected` |
| `data.<g>` | `count` — how many rows have been read; `options` — the group's custom options |
| `data.<integrationSID>` | A form-level (no-group) property — its `value` and attributes |
| `data.<containerSID>` | The `caption` and `image` of a container the component can be asked to draw them for — one **declared** in the design (`NEW <name>`), or an `lsf` child like `BOX(o)` — keyed by its design component identifier, always at the top level, whatever the container is nested in. Such a container is always there, `{}` when it has neither. A generated container the author neither declared nor marked `lsf` (`BOX(g)`, `TOOLBAR(g)`, `PANEL(g)`, …) is not projected |

Each attribute is delivered at one point, already resolved: the server folds a property's static design value and its per-row `BACKGROUND` / `READONLYIF` / … result into a single effective value, so the view reads an attribute straight from where it lives and never merges a column base with a row override. A list property's whole-column attributes — `caption`, `image`, `footer`, `comment`, `tooltip`, `defaultValue` — are on its column node `data.<g>.<prop>`; its per-row value and the cell attributes that can vary by row are on the cell `data.<g>.list[i].<prop>`:

```jsx
const caption = props.data.o.sum.caption;   // a column attribute, once for the column
const cell = row.sum;                        // { value, background, readOnly, ... } for this row
```

The server computes a dynamic `IMAGE` for a property once at the column key (using the current object), while an action's dynamic `IMAGE` is computed for every row. The projection preserves that distinction: a property's image is on its column node `data.<g>.<prop>`; an action's image is on its row cell `data.<g>.list[i].<action>`.

An option the platform computed nothing for is absent — a property with no `BACKGROUND` has no `background` in its cell entry — and a row the platform computed no row options for has no `background`, `foreground` or `selected`.

`SHOWIF` controls the property itself. When it hides a table column, that property's entry is absent from every row's cell and from the group's column node. When it hides a panel or form-level property, its entry is absent from its object. Test for the property entry with `Object.hasOwn()` when a hidden property must be distinguished from one whose `value` is `null`.

| Option | What it is | JS value |
| --- | --- | --- |
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

Of these, `caption`, `image`, `footer`, `comment`, `tooltip` and `defaultValue` are the column's own attributes, on the column node `data.<g>.<prop>` — one per column; the rest (`background`, `foreground`, `readOnly`, `disabled`, `placeholder`, `pattern`, `regexp`, `regexpMessage`, `valueTooltip`, `options`) are cell attributes, on each row's cell `data.<g>.list[i].<prop>` next to its `value`. The row itself carries `background` and `foreground` — the colors of the row as a whole — and `selected`, which is `true` when the row is selected — directly, not inside any property entry.

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

Use `window.lsfusion.List` to render the rows of a group with per-row render economy. It is a runtime global, so to write it as a JSX tag bind it to a local capitalized name first; without an alias, call it through `React.createElement`. The alias is read where the module runs, which for a compiled bundle and for a `.jsx` resource is before anything renders, so it can sit at the top of the module. In a resource written as `.js`, which is served as it is, read the platform's components and hooks inside the component instead — the module runs before the first view mounts, and they are installed at that mount:

```jsx
const List = window.lsfusion.List;
// ...
<List data={props.data.o} component={Row} />   // the group node itself, not a copy of it
// or, without an alias:
React.createElement(window.lsfusion.List, { data: props.data.o, component: Row })
```

Render `List` as a component — through JSX or `React.createElement` — never by calling it as a plain function: it renders each row through a component that uses hooks, so it only works when React mounts it.

`List` keys each row by `row.key`, passes the row to the component as `props.row` — with `rowKey`, `index` and any other props given to `List` — and renders each row through a memoized wrapper bound to that row, so on a change only the rows that actually changed re-render. The plain alternative maps the list directly:

```jsx
props.data.o.list.map(r => <Row key={r.key} row={r} />)
```

Why per-row economy matters. When any single row changes, `props.data.<g>.list` is rebuilt as a new array reference, but the projection keeps the same object reference for every row that did not change (structural sharing) — only the rows whose contents changed get a new row object. A plain `list.map(r => <Row row={r}/>)` re-creates the `Row` element for every entry on any single-row change, so React re-renders all of them. The React `key` does not change this: it lets React preserve each row's element identity, DOM, and component state across renders, but it is not a render bail-out. The React Compiler does not help either — it memoizes the `.map` as one reactive scope keyed by the array reference, which has just changed, and it does not wrap the row children in `React.memo`, so every row still re-renders.

`window.lsfusion.List` adds the missing per-row bail-out: it renders each row through a stable memoized wrapper that tracks that one row, so a value change re-renders only the changed row. The list itself is not re-walked when a row's value changes — only when rows are added, removed, or reordered — so the cost of an update does not grow with the number of rows. To get a per-row bail-out by hand without `window.lsfusion.List`, declare the memoized row component once at module level and key by `row.key`:

```jsx
const MRow = React.memo(Row);
// ...
props.data.o.list.map(r => <MRow key={r.key} row={r} />)
```

A `React.memo(Row)` created inside the component on each render is a new component type every time, which defeats the memoization and re-renders every row.

A simpler variant of `window.lsfusion.List` is available as `<List simple/>`, or as the default for every `List` by setting `window.lsfusion.listSimple = true`. It maps the list and memoizes the row component instead, relying on the projection reusing the row reference of an unchanged row; the row component receives the same props.

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

`<BucketScope group bucketOf bucketDeps>` wraps the grid markup. `group` is the group object SID. `bucketOf(row, rowKey)` computes the row's cell key from the row's property values — a string (any value is coerced to a string), an array of keys to place the row into several cells, or `null` for none. `bucketDeps` lists the outside values `bucketOf` closes over — like a hook dependency array, the index is rebuilt when they change; keep the array's length constant.

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

### Crossing back to lsFusion

`custom` crosses from the platform to React; `lsf = TRUE` is the crossing back. By default the component draws the container's whole subtree from `props.data`. A child marked `lsf = TRUE` instead keeps its lsFusion view, and the component places that view with `<Lsf name/>` rather than drawing it.

```lsf
FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) READONLY number, date, sum
    PROPERTIES() comment = orderComment      // a form-level property, so its entry is data.comment
;

DESIGN orders {
    board {
        custom = 'Board';
        MOVE BOX(o) { lsf = TRUE; }        // the standard grid, placed by the component
        MOVE PROPERTY(comment) { lsf = TRUE; }
    }
}
```

An `lsf` child is not projected into `props.data` — the platform builds its view, feeds it the property values, and renders it, exactly as in a standard container. The component only decides where it goes. Its caption and image are the exception: they go to the component in `data` instead of the child's own view. An `lsf` property projects only `{ caption, image }` — it has no `.value`, since the platform draws the value with the rest of the child's presentation. It projects that entry even with neither, as `{}`. An `lsf` container is projected the same way as any container the design declares: the component reads its `caption` and `image` from `data` and decides where the platform's view goes.

The entry holds `caption` and `image`, and it sits at the same place in `data` the [display options](#display-options) use, under the same name the child's value is keyed by:

| `lsf` child | Where its entry is | Key |
| --- | --- | --- |
| A property of an object group | `data.<g>.<integrationSID>`, alongside the group's other column attributes | The property's integration SID, `qty` |
| A form-level (no-group) property | `data.<integrationSID>` | The property's integration SID, `note` |
| A container | `data.<containerSID>`, always at the top level | The container's design component identifier, `BOX(o)` |

A container is keyed by its design identifier because that is the only name it has; a property is keyed by its integration SID, the name its value is keyed by, not by the design identifier `PROPERTY(qty)`. The `name` passed to `<Lsf>` is a different name: it is the design identifier of the child in the container, so an `lsf` property is placed as `<Lsf name="PROPERTY(note)"/>` and read as `data.note`.

Every container the React scope owns or places gets an entry in `data` when it is declared in the design (`NEW <name>`) or marked `lsf` — except a container inside an `lsf` subtree, which the platform draws whole and the component never looks into. A generated box the component neither placed nor the author named (`TOOLBAR(g)`, `PANEL(g)`, …) gets none. Being part of the projected `data`, a dynamic caption or image re-renders the component like any other data change.

`Lsf` and `useLsf` are runtime globals, like `List`, so bind them to local names before the examples below work: `const { Lsf, useLsf } = window.lsfusion;`.

The component names each child it places, and draws the caption itself where it wants it:

```jsx
export function Board(props) {
    const data = props.data;
    return <div className="board">
        <h3>{data['BOX(o)'].caption}</h3>
        <Lsf name="BOX(o)"/>
        <h3>{data.comment.caption}</h3>
        <Lsf name="PROPERTY(comment)"/>
    </div>;
}
```

### Placing an lsf child {#lsf-child}

An `lsf` child's view is moved into a *host*: a DOM node React owns and never renders children into. React places the view relative to a node it owns, and it must keep owning it to go on rendering the surrounding tree. Which node that is, is the only difference between the two ways to place a child:

```jsx
// the platform creates the host — a <div> inside the section
<section className="board-panel"><Lsf name="BOX(o)"/></section>

// the component's own element is the host — one node less
<section className="board-panel" ref={useLsf('BOX(o)')}/>
```

`<Lsf>` is the shorter one. `useLsf(name)` returns a ref callback, for an element the component renders anyway — a panel, a card, a grid cell — so the view goes straight into it.

Everything else is the same for both. The platform marks the host with the class `lsf-view` and with `data-lsf-sid`, whoever created it. Every host is styled so that the view fills it, whatever the child is, so the component sizes the host and the view follows:

```css
.board > .lsf-view[data-lsf-sid="BOX(o)"] { height: 260px; }
```

Sizing is the component's job, because an `lsf` child's `width`, `height`, `fill` and alignment attributes are **not** applied: those describe a position inside a standard container, and here the surrounding element is the component's own markup. Its `caption` and `image` are not drawn by the child either: they are handed to the component in `data`, so a component that places children itself draws them where it wants them — nothing draws them otherwise:

```jsx
<section className="board-panel">
    <h3><span dangerouslySetInnerHTML={{ __html: props.data['BOX(o)'].image }}/>
        {props.data['BOX(o)'].caption}</h3>
    <Lsf name="BOX(o)"/>
</section>
```

`image` is a string with the image HTML, so it is inserted as HTML; `caption` is plain text.

These rules bound the placement:

- A property drawn in the **panel** of an object group that the component renders cannot be marked `lsf`, because that group has no lsFusion view to place it in. Set `lsf` on the group's `BOX` instead. (A property drawn in the **table** of such a group is the per-row case below, and is exactly what `LSF` is for.)
- A child of a container the component itself draws cannot be marked `lsf` either: that container has no view of its own, so nothing would place the child. Mark the container `lsf` as well, and it gets one.
- Each lsf child is placed by at most one host. A child no host places is not shown; a duplicate host reports itself in the page and in the console, and the first one keeps the child.
- The node `<Lsf>` renders holds the lsFusion view, so it must stay empty: give it a class or a style, never children.
- `lsf` may be set only on a direct child of a `CUSTOM REACT` container; anywhere else the form is rejected when it is built.

A placement that cannot work says so in the host itself, not only in the console: a name that names no child of the container, a child without `lsf`, a second host for the same child, a name that is not an `LSF` grid property, and a `row` that is not a row each render their message into the host and mark it with the class `lsf-view-error`.

A child the component stops rendering is reported to the server as not shown, and the server stops reading that child's data — the same gating an inactive tab or a collapsed container gets. Its group stops being read only when the child was the group's last visible place on the form. So a component that shows one child at a time renders only that child, rather than hiding the others with CSS: a CSS-hidden child is still shown as far as the server knows, and goes on being read. For the same reason the visibility of an lsf child belongs to the component alone — its `collapsible` attribute is ignored, and a scripted `COLLAPSE` / `EXPAND` on it is an error.

### A live editor in every row

An lsf child is drawn once. A grid property marked `LSF` in the `FORM` is drawn once per **row**, so a component can put a real lsFusion editor into every row it renders, instead of showing the value and having to build editing itself:

```lsf
FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) number READONLY, date
    PROPERTIES(o) quantity LSF, note LSF
;

DESIGN orders {
    NEW board {
        custom = 'OrderBoard';
        MOVE BOX(o);                  // React draws the rows, from data.o
        MOVE PROPERTY(quantity);      // its per-row editors are placed by the component
        MOVE PROPERTY(note);
    }
}
```

`LSF` says the property is a component rather than a value, and the `MOVE` says which container places it. The component names the property and the row:

```jsx
{data.o.list.map(row => (
  <tr key={row.key}>
    <td>{row.number.value}</td>
    <td><Lsf name="quantity" row={row}/></td>
    <td><Lsf name="note" row={row}/></td>
  </tr>
))}
```

Pass the row object out of the projected data — a row key alone cannot be resolved back to a row. The same property may be placed once per row, and only once per row.

What the author gets and what stays theirs:

- The editor is the platform's, with everything that implies: editing, `READONLYIF`, `BACKGROUND` and the other per-cell options, all read for **that row**.
- The property leaves the rows: it is a component now, so `row.quantity` is not there (its column entry stays, with the caption). Declare a second, ordinary property if the value is also wanted as data.
- The caption is not drawn in the row. Like an lsf child's, it arrives in the group's column node — `data.o.quantity.caption` — so the component puts it where it belongs, usually once, in a header.
- A renderer exists for every row the group is currently showing, whether the component renders that row or not. A row scrolled out of view keeps its editor; only a row leaving that set loses it.

The declaration is refused, with the property named, when it cannot work: on a grid property whose object group is not drawn by a `CUSTOM REACT` container (nothing would place the per-row editors), on a grid property grouped in columns (a per-row editor cannot address a row-and-column cell), and on a panel property of a group the component itself draws (there is no lsFusion view to place it in — set `lsf` on the group's box instead).

### Extending a container with lsf children

Another module adds a child to the container from `DESIGN`:

```lsf
EXTEND FORM orders PROPERTIES(o) rating;
DESIGN orders {
    board { MOVE PROPERTY(rating) { lsf = TRUE; } }
}
```

The component does not pick it up on its own. Every lsf child is placed by an `<Lsf>` that names it, so a child no `<Lsf>` names is not shown, and adding one this way means editing the JSX too — the `DESIGN` extension declares the child, the component decides where it goes. The layout is not extensible from `DESIGN` either: a React composition cannot be modified there. To change the layout, replace the whole component with `custom = 'OtherBoard'`; the children stay as declared.

### Choosing between a React component and an HTML template

A component that places lsf children and reads nothing from `props.data` does what a classic custom container already does: an HTML template positions the same children through its `<Lsf:name>` places, without a React runtime and without a host node per child. When every child of the container is `lsf`, `props.data` carries no group or property values, because an lsf child is not projected — only the children's `{ caption, image }` entries in `props.data`.

A template is given no data at all. It places what the platform draws, by name, and everything else in it is written out: it cannot read the caption or the image of a child, or of the container itself, and cannot put either into markup of its own. What it places carries its own caption, drawn by the platform inside the placed view, and the container's own caption and image are drawn by the platform beside the container rather than inside the template. So a heading of your own that has to show a caption the application computes is the point at which a template stops being enough.

Both name their children, so neither is extended from `DESIGN` alone: adding a child means editing the JSX or the template string. A React component earns its place when the layout is computed — the children are placed conditionally, the grid template is derived from the data read through `useData`, or the markup comes from a component library — or when the markup has to show something the projection carries, which for a container or an lsf child is its `caption` and its `image`. A template string belongs to the module that declared it and can only be replaced whole.

### Interactivity

To read and change form state from the component — selecting a row, changing a property, calling actions — use `props.controller`. Its methods are described in [How-to: Custom view controller](How-to_Custom_view_controller.md).

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

An element declared [`LSF`](../language/NAVIGATOR_statement.md) is drawn by the platform, so what it draws is not projected: its entry carries `name`, `caption`, `image`, `children` and `lsf: true`, and none of the rest.

The window is given the elements it is responsible for: an element that crossed into another window, and a subtree gated out because its parent is not selected, are absent — the same rule that decides what the standard toolbar draws. What that rule keeps, the projection keeps, hidden or not. `props.controller` is the navigator controller, whose [`activate`](How-to_Custom_view_controller.md#navigator-controller) does what clicking the element does: selects a folder, or runs an action.

Only the desktop web client draws the component. The mobile web client and the desktop client render their standard menu for that window, so the navigator stays usable in all of them.

#### The standard button {#standard-button}

An element the component does not want to draw itself is placed with `<Lsf name/>` — the tag that places a design child on a form, where `name` is the element's canonical name. The platform puts its own button inside, with its icon, caption, tooltip and click behavior; the node itself is left as the component rendered it, without the marks a form's host is given.

```jsx
const { Lsf } = window.lsfusion;

function AppMenu({ data, controller }) {
    return <nav>
        <Lsf name="Sale.sales" className="menu-main"/>
        {data.root.filter(name => name !== 'Sale.sales').map(name =>
            <button key={name} onClick={ev => controller.activate(name, ev)}>{data.byName[name].caption}</button>)}
    </nav>;
}
```

The element must be declared `LSF` in the [`NAVIGATOR`](../language/NAVIGATOR_statement.md) — the crossing back is declared on the navigator side, the way `lsf = TRUE` declares it on a form:

```lsf
NAVIGATOR {
    Sale.sales LSF;
}
```

An `LSF` element no `<Lsf>` names is not shown, as on a form. Four mistakes are reported in the page: a name no navigator element has, a name that is not `LSF`, which the component is expected to draw from the projection instead, a name of an element another window draws, and a name a second `<Lsf>` places again. A name the window merely does not draw right now — an element `SHOWIF` hides, one gated out because its parent is not selected — is not a mistake and is not reported: the place stays empty and waits, and is filled as soon as the window draws that element. Which window draws an element is not such a case: waiting for a button another window owns would wait for good.

A menu that is mostly standard is written this way as one `<Lsf>` for each element the platform draws, and markup only for the elements the component draws itself.

#### An HTML template instead of a component {#navigator-template}

A window whose menu only needs its own markup is given an HTML template instead of a component name — the [same template](../language/WINDOW_statement.md) a `custom` container takes, where an `<Lsf:name>` place takes the element's standard button. The name is resolved the way every other name in the module is, so nothing has to be spelled out in full, and the place is written open — closing it is an error:

```lsf
EXTEND WINDOW System.toolbar CUSTOM
    '<div class="menu"><div class="menu-head">Master data</div><Lsf:items><Lsf:partners></div>';
```

The element has to be declared before the template that gives it a place — a `NAVIGATOR` block that creates it goes above. A name that resolves to nothing is an error when the module is read, so a misspelled name stops the application instead of leaving the menu an item short.

An element the template gives no place is not drawn, so it names everything it shows, and `LSF` is not needed on any of them — a template places nothing but the platform's own drawings.

The template may also be computed: given a property instead of a literal, it is recomputed as the property's value changes and the window is drawn again from the new markup, so a menu can rearrange itself without a component. Nothing resolves a computed template, so its places name their elements in full, by canonical name. A component is still what a menu needs when the markup depends on what the component itself reads.

### The forms window {#forms-window}

A React component can also draw the window the forms open in, instead of the standard tabs. The [`WINDOW`](../language/WINDOW_statement.md) `System.forms` is given the component name, and the open forms become its data:

```lsf
EXTEND WINDOW System.forms CUSTOM 'FormsBoard';
```

```jsx
const { Lsf } = window.lsfusion;

function FormsBoard({ data, controller }) {
    const current = data.open.find(name => data.byName[name].selected);
    return <div className="board">
        <div className="board-bar">{data.open.map(name => {
            const e = data.byName[name];
            return <span key={name} className={e.selected ? 'on' : ''}>
                <a onClick={() => controller.select(name)}>{e.caption || '...'}</a>
                {!e.blocked && <a onClick={() => controller.close(name)}>x</a>}
            </span>;
        })}</div>
        {current && <Lsf key={current} name={current} className="board-current"/>}
    </div>;
}
```

The component never draws a form: it renders a place for the form it wants shown, and the platform moves that form's own view into it. `<Lsf name/>` is that place, and `name` is the name the projection gives the form. It places the form's view the same way it places a [design child](#lsf-child) and a [navigator element](#standard-button), and the same mistakes are reported in the page.

`props.data` is the open forms and the state of the window itself:

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

The platform's own toolbar — edit mode, full screen, the mobile menu — sits in the standard tab strip, so a window drawn by a component does not show it. What it does to the window itself the component does through the controller: `setEditMode(name)` chooses an edit mode, the way the mode button does, where `name` is the name of a mode in `editModes`, and `toggleFullScreen()` expands the forms window to the full screen and brings it back, the way the full screen button and ALT+F11 do. What the standard tab's context menu offers is here too: `closeAll()` asks every open form to close, starting from the last one in that order; closing each of them is a request as well, so a form that did not close stays open and the rest are closed. The chosen mode and the full screen are state the platform holds itself, which is why it projects them: a mode chosen by the component and a full screen entered with ALT+F11 are both seen the same way, in `props.data`. The mobile menu is not something a component needs: on the mobile web client the platform draws the forms window.

Only the desktop web client draws the component. The mobile web client and the desktop client keep their standard forms window.

### The log window {#log-window}

A React component can also draw the window the messages to the user appear in, instead of the standard panel. The [`WINDOW`](../language/WINDOW_statement.md) `System.log` is given the component name, and the logged messages become its data:

```lsf
EXTEND WINDOW System.log CUSTOM 'MessageLog';
```

```jsx
const { Lsf } = window.lsfusion;

function MessageLog({ data, controller }) {
    return <div className="log">
        <a className="log-pin" onClick={() => controller.togglePin()}>pin</a>
        {data.messages.map(name => {
            const m = data.byName[name];
            return <div key={name} className={m.failed ? 'log-failed' : 'log-ok'}>
                <div className="log-line">{new Date(m.time).toLocaleTimeString()} {m.caption}</div>
                <Lsf name={name}/>
            </div>;
        })}
    </div>;
}
```

The component never draws a message: a message is markup the platform built — the text with its icon, or the table an action reported beside it — so the component renders a place for each message it wants shown, and the platform moves that markup in. `<Lsf name/>` is that place, and `name` is the name the projection gives the message. It places the message's markup the same way it places a [design child](#lsf-child), a [navigator element](#standard-button) and an [open form](#forms-window).

`props.data` is the logged messages:

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

`props.controller` does the one thing only the platform can do to the log: `togglePin()` toggles the window's pin mode, the way the platform's pin button does. Pinning is a setting of the display environment, shared with the desktop client, so the component cannot do it itself; it is not told the current mode either, since the client is not — an unpinned window is unpinned by a class the server computes into the window.

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

The projected value is either an address or a ready element, since a font icon has no address at all, and `Image` draws whichever it was given. A view that wants the address itself — for a CSS background, say — still has it in the value; `Image` is there so that no view has to insert the platform's markup by hand. A missing image draws nothing.

### Drawing a caption {#caption}

A caption the platform projects — a form's, a navigator element's, the one a message carries — is drawn by `Caption`:

```jsx
const { Caption } = window.lsfusion;

<Caption value={e.caption} className="tab-caption"/>
```

A caption is either plain text or markup the platform built, and `Caption` draws whichever it was given, by the same rule the platform draws a caption of its own by: the value is markup when a tag appears anywhere in it. Printed as text, such a caption would show its own markup, so a view that prints `{e.caption}` itself is right only while the caption is plain text. A missing caption draws nothing.
