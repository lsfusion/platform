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

`OrderBoard` is a named export from a `.jsx` module under `src/main/web`; how the module is compiled and registered is covered in [How-to: Custom client JS modules](How-to_Custom_client_JS_modules.md).

The component is a plain function that receives `props.data` and `props.controller`:

```jsx
export function OrderBoard(props) {
    const orders = props.data.o.list;
    return <div className="order-board">{orders.length} orders</div>;
}
```

`props.data` is the form projection. For each form group object SID `g`, `props.data.<g>` is `{ list, byKey }`, where `list` is the array of rows in display order and `byKey` maps a row's key string to the same row object. Each row carries:

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

`List` keys each row by `row.key`, passes the row to the component as `props.row`, and memoizes the row component, so on a change only the rows that actually changed re-render. The plain alternative maps the list directly:

```jsx
props.data.o.list.map(r => <Row key={r.key} row={r} />)
```

Why per-row economy matters. When any single row changes, `props.data.<g>.list` is rebuilt as a new array reference, but the projection keeps the same object reference for every row that did not change (structural sharing) — only the rows whose contents changed get a new row object. A plain `list.map(r => <Row row={r}/>)` re-creates the `Row` element for every entry on any single-row change, so React re-renders all of them. The React `key` does not change this: it lets React preserve each row's element identity, DOM, and component state across renders, but it is not a render bail-out. The React Compiler (`reactCompiler=true`) does not help either — it memoizes the `.map` as one reactive scope keyed by the array reference, which has just changed, and it does not wrap the row children in `React.memo`, so every row still re-renders.

`window.lsfusion.List` adds the missing per-row bail-out: it renders each row through a stable `React.memo`-wrapped component and passes the reused row reference, so a row re-renders when its row reference (or another prop passed to it) changes. To do the same by hand without `window.lsfusion.List`, declare the memoized row component once at module level and key by `row.key`:

```jsx
const MRow = React.memo(Row);
// ...
props.data.o.list.map(r => <MRow key={r.key} row={r} />)
```

A `React.memo(Row)` created inside the component on each render is a new component type every time, which defeats the memoization and re-renders every row.

### Interactivity

To read and change form state from the component — selecting a row, changing a property, calling actions — use `props.controller`. Its methods are described in [How-to: Custom view controller](How-to_Custom_view_controller.md).
