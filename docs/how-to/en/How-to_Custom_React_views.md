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

`OrderBoard` is a named export from a `.jsx` module under `src/main/web`; how the module is compiled and registered is covered in [How-to: Custom client JS modules](How-to_Custom_client_JS_modules.md). The examples here use JSX, which needs the build. For a project [without the build](How-to_Custom_client_JS_modules.md#without-the-build), write the same component with `React.createElement` against the platform-provided `window.React` instead of JSX, and place it under `src/main/resources/web/init` (auto-loaded) or under `src/main/resources/web` and register it with `onWebClientInit`.

The component is a plain function that receives `props.data` and `props.controller`:

```jsx
export function OrderBoard(props) {
    const orders = props.data.o.list;
    return <div className="order-board">{orders.length} orders</div>;
}
```

`props.data` is the form projection. It contains a group only when that group's box is nested inside the custom container the view renders: `props.data.<g>` is `{ list, byKey }` for each such group object SID `g`, where `list` is the array of rows in display order and `byKey` maps a row's key string to the same row object. Both are always present: `list` is an empty array and `byKey` an empty object when the group has no rows — a panel-only group, or one before its rows first arrive — so the view reads `props.data.<g>.list` directly without guarding a missing field. A group whose box is outside the container — or `REMOVE`'d from the design — is **absent** from `props.data` (`props.data.<g>` is `undefined`), so to feed a group's data to the view keep its box inside the custom container. Each row carries:

| Field | Meaning |
| --- | --- |
| `key` | A stable public id for the row — use it as the React key |
| `isCurrent` | Whether the row is the current (selected) one |
| `<integrationSID>` | The value of each form property, keyed by the property's integration SID |
| `objects` | An opaque row handle the controller uses to address the row |

`key`, `isCurrent`, and `objects` are reserved row field names, so a form property's integration SID must not be one of them.

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

Why per-row economy matters. When any single row changes, `props.data.<g>.list` is rebuilt as a new array reference, but the projection keeps the same object reference for every row that did not change (structural sharing) — only the rows whose contents changed get a new row object. A plain `list.map(r => <Row row={r}/>)` re-creates the `Row` element for every entry on any single-row change, so React re-renders all of them. The React `key` does not change this: it lets React preserve each row's element identity, DOM, and component state across renders, but it is not a render bail-out. The React Compiler (`reactCompiler=true`) does not help either — it memoizes the `.map` as one reactive scope keyed by the array reference, which has just changed, and it does not wrap the row children in `React.memo`, so every row still re-renders.

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

The view keeps the layout: it supplies the cell keys — so empty cells exist and render too, e.g. as drop targets — and the cell markup. The platform keeps the index and the render economy: moving a row between cells re-renders only the old and the new cell; editing a value that does not change the row's cell re-renders only that row's own component; every other cell keeps its previous array reference and its `React.memo` skips.

For a flat set of cells, the `<Buckets group cells bucketOf component/>` form renders one memoized wrapper per key in `cells`; the cell component receives `cellKey`, `rowKeys`, `index`, and the pass-through props.

Use bucketing for placing one group's rows into derived cells where only the membership matters — pivots, calendars, kanban boards, timetables, drag-and-drop grids. It does not compute per-cell aggregates: `useBucket` returns row keys, not sums or counts, and the cell component re-renders only when that cell's row-key array changes — live aggregates are what the [pivot table view type](../paradigm/Interactive_view.md#property) provides. For a plain one-to-one list of rows use `List`, and grouping only works over the group's own projected values.

### Interactivity

To read and change form state from the component — selecting a row, changing a property, calling actions — use `props.controller`. Its methods are described in [How-to: Custom view controller](How-to_Custom_view_controller.md).
