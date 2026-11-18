---
slug: "/React_rendering"
title: 'Rendering in React'
---

A [React view](React_views.md) can draw the rows of an [object group](../paradigm/Interactive_view.md#objects), and the images and captions the platform projects, with components and hooks of the platform: they re-render only the rows that changed, lay the rows out in cells and let the page of rows follow the scroll.

`List`, `BucketScope`, `useBucket`, `Buckets` and `useSeekOnScroll` are built on the group and row shape of a form projection — `data.<group>.list` / `.byKey` / `.keys` —, so they serve only a view of a form that draws the rows of that group, and no other kind of root ([Projection reading](React_projection.md#reading)). `Image` and `Caption` draw the value they are given under a root of any kind.

### Row rendering {#rendering-rows}

Use `window.lsfusion.List` to render the rows of a group with per-row render economy. It is a runtime global, so to write it as a JSX tag bind it to a local capitalized name first. Without an alias, call it through `React.createElement`. The alias is read where the module runs, which for a compiled bundle and for a `.jsx` resource is before anything renders, so it can sit at the top of the module. In a resource written as `.js`, which is served as it is, read the platform's components and hooks inside the component instead — the module runs before the first view mounts, and they are installed at that mount:

```jsx
const List = window.lsfusion.List;
// ...
<List group="o" component={Row} />
// or, without an alias:
React.createElement(window.lsfusion.List, { group: 'o', component: Row })
```

`group` is the group object SID. `List` reads the group's rows out of the view's own projection by that name, the way [`BucketScope`](#bucketing) does, so it is given the name rather than the node. `keys` places some of those rows in a visual block, in the order given. For example, inside a `BucketScope` for `o` whose `bucketOf` uses a server-computed row property, each cell draws its own rows:

```jsx
function OrderCell({cellKey}) {
    const keys = window.lsfusion.useBucket(cellKey);
    return <List group="o" keys={keys} component={Row} />;
}
```

Selecting or ordering rows is done on the server, through [filters](React_filters_and_toolbar.md#filters) and [sortings](React_trees_sortings_selection.md#orders), rather than by filtering or sorting the loaded page in JavaScript.

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

### Row bucketing {#bucketing}

When the view lays a group's rows out as a matrix rather than a list — a calendar, a kanban board, a timetable, a seating chart — each row belongs to a derived cell (day × employee, status column, and so on). `window.lsfusion.BucketScope` maintains a cell → rows index over one group, and each cell subscribes to only its own membership.

`<BucketScope group bucketOf bucketDeps>` wraps the grid markup. `group` is the group object SID. A group whose rows this view does not draw throws, as it does in [`List`](#rendering-rows). `bucketOf(row, rowKey)` computes the row's cell key from the row's property values — a string (any value is coerced to a string), an array of keys to place the row into several cells, or `null` for none. `bucketDeps` lists the outside values `bucketOf` closes over — like a hook dependency array, the index is rebuilt when they change. Keep the array's length constant.

`useBucket(cellKey)` returns the array of row keys currently in that cell, in the group's display order, and subscribes the component to only that cell. Call it once per cell component, with that cell's fixed key (the usual hook rules). An empty cell always returns the same frozen empty array. The cell component resolves each row key to a row component that subscribes to its own row via `useData(d => d.<g>.byKey[rowKey])`.

The view keeps the layout: it supplies the cell keys — so empty cells exist and render too, e.g. as drop targets — and the cell markup. The platform keeps the index and the render economy: moving a row between cells re-renders only the old and the new cell; editing a value that does not change the row's cell re-renders only that row's own component; every other cell keeps its previous array reference and its `React.memo` skips. The plain alternative — grouping `data.<g>.list` into cells by hand on each render — rebuilds every cell's array every time, so any change re-renders the whole board.

When the cells form a flat list and each cell's markup lives in one component, the `<Buckets group cells bucketOf component/>` form does the mapping itself, the way `List` does for rows: one memoized wrapper per key in `cells`, and the cell component receives `cellKey`, `rowKeys`, `index`, and the pass-through props. Keep the explicit `<BucketScope>` + `useBucket` markup when the view itself lays out the grid — a two-axis matrix, axis headers, pinned columns:

```jsx
const { Buckets } = window.lsfusion;
const STATUSES = ['new', 'inProgress', 'done'];

// Card subscribes to its own row
const Column = ({ cellKey, rowKeys }) => (
    <div className="column">{rowKeys.map(k => <Card key={k} rowKey={k} />)}</div>
);

<Buckets group="t" cells={STATUSES} bucketOf={t => t.status.value} component={Column} />
```

Use bucketing for placing one group's rows into derived cells where only the membership matters — pivots, calendars, kanban boards, timetables, drag-and-drop grids. It does not compute per-cell aggregates: `useBucket` returns row keys, not sums or counts, and the cell component re-renders only when that cell's row-key array changes — live aggregates are what the [pivot table view type](../paradigm/Interactive_view.md#property) provides. For a plain one-to-one list of rows use `List`, and grouping only works over the group's own projected values.

### Scroll tracking {#seek-on-scroll}

A view that lays rows out in their own order — a card flow, a feed — can keep the page instead of reading all rows and let it follow the scroll, the way the standard table does. `useSeekOnScroll(controller.<group>)` returns a function that marks each row's element. The hook watches which of them are on screen as the view's own box scrolls — the nearest element that scrolls the rows, looked for from a row up to the element the view is drawn in and no further — and follows the table's own rules: while the current record is on screen, scrolling changes nothing; when it leaves, it is reseated on the visible edge it left through, which is also what requests the next page; on a page change the reseated row keeps its on-screen position, so nothing jumps under the eye; a current record moved from outside — a click, a programmatic seek — is scrolled into view instead. The pact rests on the same contract the table itself lives by, and it is the developer's to keep: the page must hold more rows than the viewport can show — set `PAGESIZE` on the [`OBJECTS`](Object_blocks.md) block accordingly. The window then always extends a page beyond the current record, so it leaves the viewport — and reseats, pulling the next page — before the scroller runs out of room at a loaded edge:

```jsx
const seekRef = useSeekOnScroll(controller.o, { enabled: follow });
...
{rows.map(row => <div key={row.key} ref={seekRef(row)}>...</div>)}
```

Options: `enabled` (default `true`) suspends the tracking, `threshold` (`0.6`) is how much of an element must be visible to count as on screen, `settle` (`250` ms) is how long the rows on screen must stay the same before the view seeks — the wait starts over whenever a row comes on screen or leaves it, not on every scroll event —, and `onSeek(row)` is called for each issued seek. Use one `useSeekOnScroll` per scrolling element. The rows delivered for the new position replace `list`, and everything built from it — values and [placed lsFusion views](React_platform_components.md#placing) alike — follows.

The hook moves the scroll position of that box and switches off the browser's own scroll anchoring there, so it touches nothing outside the element the view is drawn in: everything there — a platform container, a box another view shares, the page, a portal's box — belongs to something else. Rows that scroll only in such an element leave the hook doing nothing at all — no seek, and no scroll into view of a current moved out of sight — and it says so once in the console. Give the rows a scrolling box of the view's own — a bounded height and `overflow: auto`.

### Image rendering {#image}

An image the platform projects — a property of an image class, the icon of a navigator element or of an action — is drawn by `Image`:

```jsx
const { Image } = window.lsfusion;

<Image value={e.image} className="menu-icon"/>
<Image value={row.photo.value} className="avatar"/>
```

The projected value is either an address or a ready element, since a font icon has no address at all, and `Image` draws whichever it was given. A view that wants the address itself — for a CSS background, say — still has it in the value. `Image` is there so that no view has to insert the platform's markup by hand. A missing image draws nothing.

### Caption rendering {#caption}

A caption the platform projects — a form's, a navigator element's, the one a message carries — is drawn by `Caption`:

```jsx
const { Caption } = window.lsfusion;

<Caption value={e.caption} className="tab-caption"/>
```

A caption is either plain text or markup the platform built, and `Caption` draws whichever it was given, by the same rule the platform draws a caption of its own by: the value is markup when a tag appears anywhere in it. Printed as text, such a caption would show its own markup, so a view that prints `{e.caption}` itself is right only while the caption is plain text. A missing caption draws nothing.

### Performance tiers {#performance-tiers}

The two ways of [projection reading](React_projection.md#reading) compose into tiers, and each following one is only needed when the previous one's re-render becomes the cost:

1. a small view — `props.data`, render everything;
2. a longer list — `props.data` plus row components memoized at module level (`React.memo` bails out on the unchanged rows' stable references), or [`List`](#rendering-rows) which does exactly that;
3. a big board — subscriptions down the tree: the root renders memoized columns and hands them no `data`, a column subscribes to its [bucket](#bucketing), a row to itself, so one edited value re-renders the root and one card.
