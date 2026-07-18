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

`props.data` is the form projection. It contains a group only when that group's box is nested inside the custom container the view renders: `props.data.<g>` is `{ list, byKey, keys, meta }` for each such group object SID `g`, where `list` is the array of rows in display order, `byKey` maps a row's key string to the same row object, and `keys` is the array of those key strings in the same order. `list`, `byKey` and `keys` are always present: they are empty when the group has no rows — a panel-only group, or one before its rows first arrive — so the view reads `props.data.<g>.list` directly without guarding a missing field. A group whose box is outside the container — or `REMOVE`'d from the design — is **absent** from `props.data` (`props.data.<g>` is `undefined`), so to feed a group's data to the view keep its box inside the custom container. A group's panel properties are members of the group itself, keyed by integration SID, and each form-level (no-group) property is a member of `props.data` directly, under its integration SID. Actions are projected the same way as properties — an action drawn on a group is a field of each row (a list action) or of the group node (a panel action), and its `meta` is alongside a property's, so `controller.changeProperty('<group>.<action>', row)` runs it. `meta` holds the group's [display options](#display-options). Each row carries:

| Field | Meaning |
| --- | --- |
| `key` | A stable public id for the row — use it as the React key |
| `isCurrent` | Whether the row is the current (selected) one |
| `<integrationSID>` | The value of each form property, keyed by the property's integration SID |
| `objects` | An opaque row handle the controller uses to address the row |
| `meta` | The row's [display options](#display-options), absent when the platform computed none |

`key`, `isCurrent`, `objects` and `meta` are reserved row field names; `list`, `byKey`, `keys` and `meta` are reserved on the group; `components` and `meta` are reserved at the top level. Inside a group's `meta`, `count` and `customOptions` are reserved, and inside a row's `meta`, `row` is reserved. A form whose projected integration SID takes a reserved name, or where two projected items claim the same value or metadata name at one data level, is rejected with an explicit error when it is built.

A property value in a row is converted to a JS value depending on the property's class:

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

```jsx
function Row(props) {
    const r = props.row;
    return (
        <div className={r.isCurrent ? "order order-current" : "order"}>
            <span>{r.number}</span>
            <span>{r.sum}</span>
        </div>
    );
}
```

### Display options {#display-options}

For every property drawn on a form the platform computes semantic options that determine how it is shown — its caption, image, background and text colors, whether it is read-only or disabled, its comment, the text shown in an empty cell, its tooltip. They come from the property's design and from the data-dependent options of the [properties and actions block](../language/Properties_and_actions_block.md) (`HEADER`, `IMAGE`, `BACKGROUND`, `FOREGROUND`, `READONLYIF`, `OPTIONS` and the rest), so an option that depends on the data is recomputed per row. Native element classes and fonts are not projected: a React component owns its CSS. `background` and `foreground` are projected — they are `BACKGROUND` / `FOREGROUND` highlighting, a data-dependent business signal, not a theme the component's own CSS should own. For a property the React container draws itself, the semantic result is projected into `data` under `meta`, so the view does not have to recompute it.

The projection follows what an option describes — a whole column, one cell, or the row:

| Where | What it holds |
| --- | --- |
| `data.<g>.meta[<integrationSID>]` | For a property shown in the table — the options of its column, one value for the whole column: `caption`, `image`, `footer` and the design values of its semantic options. For a panel property of the group — that property's own options, read for the current object |
| `data.<g>.list[i].meta[<integrationSID>]` | The options of one cell of a table property, computed for that row: `readOnly`, `disabled`, `background`, `foreground` and the rest |
| `data.<g>.list[i].meta.row` | The options of the row itself: `background`, `foreground`, `selected` |
| `data.<g>.meta` | `count` — how many rows have been read; `customOptions` — the group's custom options |
| `data.meta[<integrationSID>]` | The options of a form-level (no-group) property |

The column entry carries the option's **design value** — the one written on the property, the same for the whole column — and the value the form delivers for the column overrides it. The row entry carries only what the form delivers **for that row**; a design value never appears there, because it is not per-row. So an option whose design value the property sets (`pattern`, `regexp`, `comment`, `placeholder`, `tooltip`, the colors, the caption, the image, …) is read from the column entry, and a row that the form computed a different value for overrides it. The options in force for a cell are therefore its column entry overridden by its row entry:

```jsx
const opts = { ...(props.data.o.meta[sid] || {}), ...((row.meta || {})[sid] || {}) };
```

The server computes a dynamic `IMAGE` for a property once at the column key (using the current object), while an action's dynamic `IMAGE` is computed for every row. The projection preserves that distinction: a property's delivered image is in the column entry; an action's delivered image is in its row entry.

An option the platform computed nothing for is absent — a property with no `BACKGROUND` has no `background` in its cell entry — and a row with no options at all has no `meta`.

`SHOWIF` controls the property itself, not an option in `meta`. When it hides a table column, that property's field and its `meta` entry are absent from every row and from the group column metadata. When it hides a panel or form-level property, the corresponding field and `meta` entry are absent from its object. Test for the property field with `Object.hasOwn()` when a present `null` value must be distinguished from a hidden property.

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
| `customOptions` | The property's custom options | parsed JSON value |
| `defaultValue` | The value editing starts with | string |

The row entry (`meta.row`) carries `background` and `foreground` — the colors of the row as a whole — and `selected`, which is `true` when the row is selected.

```jsx
function Row(props) {
    const r = props.row;
    const opts = { ...(props.colMeta.sum || {}), ...((r.meta || {}).sum || {}) };
    const rowOpts = (r.meta || {}).row || {};
    return (
        <div className="order" style={{ background: rowOpts.background }}>
            <span>{r.number}</span>
            <input value={r.sum} readOnly={!!opts.readOnly} disabled={!!opts.disabled}
                   style={{ background: opts.background, color: opts.foreground }} />
        </div>
    );
}
```

### Rendering rows

Use `window.lsfusion.List` to render the rows of a group with per-row render economy. It is a runtime global, so to write it as a JSX tag bind it to a local capitalized name first; without an alias, call it through `React.createElement`:

```jsx
const List = window.lsfusion.List;
// ...
<List data={props.data.o} component={Row} />
// or, without an alias:
React.createElement(window.lsfusion.List, { data: props.data.o, component: Row })
```

Render `List` as a component — through JSX or `React.createElement` — never by calling it as a plain function: it renders each row through a component that uses hooks, so it only works when React mounts it.

`List` keys each row by `row.key`, passes the row to the component as `props.row`, and renders each row through a memoized wrapper bound to that row, so on a change only the rows that actually changed re-render. The plain alternative maps the list directly:

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

A simpler variant of `window.lsfusion.List` is available as `<List simple/>`, or globally with `window.lsfusion.listSimple`. It maps the list and memoizes the row component instead, relying on the projection reusing the row reference of an unchanged row; the row component receives the same props.

### Bucketing rows into cells

When the view lays a group's rows out as a matrix rather than a list — a calendar, a kanban board, a timetable, a seating chart — each row belongs to a derived cell (day × employee, status column, and so on). `window.lsfusion.BucketScope` maintains a cell → rows index over one group, and each cell subscribes to only its own membership:

```jsx
const { BucketScope, useBucket, useFormData } = window.lsfusion;

const Shift = React.memo(({ rowKey }) => {
    const s = useFormData(d => d.ss.byKey[rowKey]);      // subscribes to its own row
    return s ? <button>{s.intervalS}</button> : null;
});

const Cell = React.memo(({ ck }) => {
    const rowKeys = useBucket(ck);                       // subscribes to its own cell
    return <div className="cell">{rowKeys.map(k => <Shift key={k} rowKey={k} />)}</div>;
});

export function Board(props) {
    // the view owns both axes: the days of the shown week and one board row per employee
    const days = weekOf(props.data.dates.scheduleFrom);
    const rows = props.data.boardEmployees;              // e.g. a pre-parsed JSON property: [{ id, ... }]
    return (
        <BucketScope group="ss" bucketDeps={[]}
                     bucketOf={s => dateKey(s.date) + '|' + (s.assignedTo ?? '0')}>
            <div className="grid">
                {rows.map(row => days.map(d =>
                    <Cell key={row.id + '/' + dateKey(d)} ck={dateKey(d) + '|' + row.id} />))}
            </div>
        </BucketScope>
    );
}
```

`<BucketScope group bucketOf bucketDeps>` wraps the grid markup. `group` is the group object SID. `bucketOf(row, rowKey)` computes the row's cell key from the row's property values — a string (any value is coerced to a string), an array of keys to place the row into several cells, or `null` for none. `bucketDeps` lists the outside values `bucketOf` closes over — like a hook dependency array, the index is rebuilt when they change; keep the array's length constant.

`useBucket(cellKey)` returns the array of row keys currently in that cell, in the group's display order, and subscribes the component to only that cell. Call it once per cell component, with that cell's fixed key (the usual hook rules). An empty cell always returns the same frozen empty array. The cell component resolves each row key to a row component that subscribes to its own row via `useFormData(d => d.<g>.byKey[rowKey])`, as above.

The view keeps the layout: it supplies the cell keys — so empty cells exist and render too, e.g. as drop targets — and the cell markup. The platform keeps the index and the render economy: moving a row between cells re-renders only the old and the new cell; editing a value that does not change the row's cell re-renders only that row's own component; every other cell keeps its previous array reference and its `React.memo` skips. The plain alternative — grouping `data.<g>.list` into cells by hand on each render — rebuilds every cell's array every time, so any change re-renders the whole board.

When the cells form a flat list and each cell's markup lives in one component, the `<Buckets group cells bucketOf component/>` form does the mapping itself, the way `List` does for rows: one memoized wrapper per key in `cells`, and the cell component receives `cellKey`, `rowKeys`, `index`, and the pass-through props. Keep the explicit `<BucketScope>` + `useBucket` markup when the view itself lays out the grid — a two-axis matrix, axis headers, pinned columns:

```jsx
const { Buckets } = window.lsfusion;
const STATUSES = ['new', 'inProgress', 'done'];

// Card subscribes to its own row, like Shift above
const Column = ({ cellKey, rowKeys }) => (
    <div className="column">{rowKeys.map(k => <Card key={k} rowKey={k} />)}</div>
);

<Buckets group="t" cells={STATUSES} bucketOf={t => t.status} component={Column} />
```

Use bucketing for placing one group's rows into derived cells where only the membership matters — pivots, calendars, kanban boards, timetables, drag-and-drop grids. It does not compute per-cell aggregates: `useBucket` returns row keys, not sums or counts, and the cell component re-renders only when that cell's row-key array changes — live aggregates are what the [pivot table view type](../paradigm/Interactive_view.md#property) provides. For a plain one-to-one list of rows use `List`, and grouping only works over the group's own projected values.

### Delegating children back to lsFusion

By default the component draws the container's whole subtree from `props.data`. A child can instead keep its own lsFusion view: set `delegate = TRUE` on it, and the component places that view with `<LsfComponent sid/>` rather than drawing it.

```lsf
DESIGN orders {
    board {
        custom = 'Board';
        MOVE BOX(o) { delegate = TRUE; }        // the standard grid, placed by the component
        MOVE PROPERTY(comment) { delegate = TRUE; }
    }
}
```

A delegated child is not projected into `props.data` — the platform builds its view, feeds it the property values, and renders it, exactly as in a standard container. The component only decides where it goes. Its caption and image are the exception: they go to the component through `props.data.components` instead of the child's own view.

`props.data.components` maps each delegated child's `sid` to its descriptor `{ caption, image }`, in `DESIGN` order. The `sid` (the key) is the design component's identifier, such as `BOX(o)` or `PROPERTY(comment)`; the descriptor carries the caption and image that a delegated child no longer draws in GWT. It is part of the projected `data`, so a dynamic caption or image re-renders it like any other data change.

`LsfComponent`, `LsfComponents` and `useLsfComponent` are runtime globals, like `List`, so bind them to local names before the examples below work: `const { LsfComponent, LsfComponents, useLsfComponent } = window.lsfusion;`.

```jsx
export function Board(props) {
    return <div className="board">
        {Object.keys(props.data.components).map(sid => <LsfComponent key={sid} sid={sid}/>)}
    </div>;
}
```

`<LsfComponents/>` does the same iteration, so a container that uses it places every delegated child without naming any:

```jsx
export function Board() {
    return <div className="board"><LsfComponents/></div>;
}
```

### Placing a delegated child

A delegated child's view is moved into a *host*: a DOM node React owns and never renders children into. React places the view relative to a node it owns, and it must keep owning it to go on rendering the surrounding tree. Which node that is, is the only difference between the two ways to place a child:

```jsx
// the platform creates the host — a <div> inside the section
<section className="board-panel"><LsfComponent sid="BOX(o)"/></section>

// the component's own element is the host — one node less
<section className="board-panel" ref={useLsfComponent('BOX(o)')}/>
```

`<LsfComponent>` is the shorter one, and it is what `<LsfComponents/>` places. `useLsfComponent(sid)` returns a ref callback, for an element the component renders anyway — a panel, a card, a grid cell — so the view goes straight into it.

Everything else is the same for both. The platform marks the host with the class `lsf-component` and with `data-lsf-sid`, whoever created it. Every host is styled so that the view fills it, whatever the child is, so the component sizes the host and the view follows:

```css
.board > .lsf-component[data-lsf-sid="BOX(o)"] { height: 260px; }
```

Sizing is the component's job, because a delegated child's `width`, `height`, `fill` and alignment attributes are **not** applied: those describe a position inside a standard container, and here the surrounding element is the component's own markup. Its `caption` and `image` are not drawn by the child either: they are handed to the component in `props.data.components`, so a component that places children itself draws them where it wants them.

`<LsfComponents/>` wraps each child in a `<div class="lsf-slot">` and, when the descriptor carries a caption or an image, draws them above the host in a `<div class="lsf-slot-caption">` (the image in a `<span class="lsf-slot-image">`). Its hosts are addressed from CSS as `.lsf-slot > .lsf-component[data-lsf-sid="..."]`. Iterate `props.data.components` yourself when a slot needs its own markup rather than its own style:

```jsx
Object.keys(props.data.components).map(sid => {
    const c = props.data.components[sid];
    return <section key={sid} className="slot">
        <h3>{c.caption}</h3>
        <LsfComponent sid={sid}/>
    </section>;
})
```

Three rules bound the placement:

- A property drawn on an object group that the component renders cannot be delegated, because that group has no lsFusion view to place. Delegate the group's `BOX` instead. The server rejects such a design.
- Each delegated child is placed by at most one host. A child no host places is not shown; a duplicate host reports itself in the page and in the console, and the first one keeps the child.
- The node `<LsfComponent>` renders holds the lsFusion view, so it must stay empty: give it a class or a style, never children.

A child the component stops rendering is reported to the server as not shown, and the server stops reading that child's data — the same gating an inactive tab or a collapsed container gets. Its group stops being read only when the child was the group's last visible place on the form. So a component that shows one child at a time renders only that child, rather than hiding the others with CSS: a CSS-hidden child is still shown as far as the server knows, and goes on being read. For the same reason the visibility of a delegated child belongs to the component alone — its `collapsible` attribute is ignored, and a scripted `COLLAPSE` / `EXPAND` on it is an error.

### Extending a delegating container

Because the children come from `DESIGN`, another module extends the container's content without touching the component:

```lsf
EXTEND FORM orders PROPERTIES(o) rating;
DESIGN orders {
    board { MOVE PROPERTY(rating) { delegate = TRUE; } }
}
```

The layout is not extensible this way: a React composition cannot be modified from `DESIGN`. To change the layout, replace the whole component with `custom = 'OtherBoard'`; the children stay as declared.

### Choosing between a React component and an HTML template

A component that places delegated children and reads nothing from `props.data` does what a classic custom container already does: an HTML template positions the same children through its `[sID]` slots, without a React runtime and without a placeholder node per child. When every child of the container is delegated, `props.data` carries no group or property values, because a delegated child is not projected — only `props.data.components` with the children's descriptors.

A React component earns its place when the layout is computed — the children are placed conditionally, the grid template is derived from the data read through `useFormData`, or the markup comes from a component library — or when `<LsfComponents/>` is used, since it places a child a later module adds without anyone editing the component. An HTML template has neither: adding a child there means editing the template string, which belongs to the module that declared it.

### Interactivity

To read and change form state from the component — selecting a row, changing a property, calling actions — use `props.controller`. Its methods are described in [How-to: Custom view controller](How-to_Custom_view_controller.md).
