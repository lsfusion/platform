---
slug: "/How-to_Custom_React_views"
title: 'How-to: Custom React views'
---

This article collects typical tasks solved with React views, each with its solution. The views are specified in the Language articles, starting with [React views](../language/React_views.md): the data a component receives is described in [Form projection](../language/React_projection.md), and what it calls in [React view controller](../language/React_view_controller.md).

The components of the examples are written in JSX, each in a `.jsx` module ([The component](../language/React_views.md#the-component)). How the module is compiled and registered is covered in [Client JavaScript modules](../language/Client_JavaScript_modules.md). The classic (non-React) custom container is described in the [`DESIGN` statement](../language/DESIGN_statement.md#containerprops).

### "N of M": a count under the form's filter {#filtered-count}

We need to show how many rows the form's filter admits beside the rows a view draws: "6 of 23". The solution extends the form `orders` of [Component selection](../language/React_views.md#selecting-the-component).

The number of rows read is `list.length`, not how many rows the group's filters admit, so under paged reading the projection alone cannot show "6 of 23". A total kept current is a separate property that counts the rows under the form's current filter, including the applied user filters and view filters, with the [`FILTER` operator](../paradigm/Filter_FILTER.md), as in [How-to: Table status](How-to_Table_status.md), placed inside the container so that it reaches `data`:

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

### A feed that loads as it scrolls {#feed}

We need to show a group's rows as a feed that reads the next page as it scrolls, starts over after a change of the filters or sortings, and moves to its first or last row on a button. The solution uses the [`useSeekOnScroll`](../language/React_rendering.md#seek-on-scroll) hook and an edge of the list passed to the group's `change` ([The first and the last row](../language/React_view_controller.md#first-last-row)).

```jsx
const seekRef = useSeekOnScroll(controller.o, { enabled: follow });
...
{rows.map(row => <div key={row.key} ref={seekRef(row)}>...</div>)}
```

After a change of the filters or sortings, the server reads rows around the current row, as in the platform's table. A feed that should start over calls `controller.<g>.change(controller.form.HOME)` after the change in the same synchronous [batch](../language/Form_and_navigator_controllers.md#batch). In the [manual filter apply mode](../language/React_filters_and_toolbar.md#filter-controls), move to the start after applying the filters, so the move uses the new conditions.

The buttons to the first and the last row:

```jsx
<>
    <button onClick={() => controller.o.change(controller.form.HOME)}>First</button>
    <button onClick={() => controller.o.change(controller.form.END)}>Last</button>
</>
```

### A calendar with rows bucketed into cells {#calendar-board}

We need to show shifts as a calendar — a grid of days by employees with each shift in its cell — in which a change re-renders only the cells it touches. The solution uses `BucketScope` and `useBucket` ([Row bucketing](../language/React_rendering.md#bucketing)).

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

// a DATE value comes in props.data as a JS Date
function weekOf(from) {
    return Array.from({length: 7}, (_, i) => {
        const day = new Date(from);
        day.setDate(from.getDate() + i);
        return day;
    });
}

function dateKey(d) {
    const year = String(d.getFullYear()).padStart(4, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

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

### A page of several containers {#several-containers}

We need to build a page of several regions, each drawn by React, that follow one another: picking a category shows its items, and picking an item shows its details. The solution uses the [projection](../language/React_projection.md) each container gets.

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

FORM catalog 'Catalog'
    OBJECTS c = Category
    PROPERTIES(c) READONLY name

    OBJECTS i = Item
    PROPERTIES(i) READONLY name, archived, nameItem = name PANEL
    FILTERS category(i) = c
    FILTERGROUP archive
        FILTER 'Active' NOT archived(i) DEFAULT
    ORDERS name(i)

    OBJECTS d = Detail
    PROPERTIES(d) READONLY name, quantity
    FILTERS item(d) = i
;

DESIGN catalog {
    NEW page {
        horizontal = TRUE;
        NEW categories { fill = 1; custom = 'CategoryList'; MOVE BOX(c); }
        NEW items { fill = 2; custom = 'ItemList'; MOVE BOX(i); }
        NEW details { fill = 1; custom = 'DetailPanel'; MOVE PROPERTY(nameItem); MOVE BOX(d); }
    }
}
```

```jsx
export function CategoryList({data, controller}) {
    return <ul>{data.c.list.map(row =>
        <li key={row.key}>
            <button style={{fontWeight: row.current ? 'bold' : 'normal'}}
                    onClick={() => controller.c.change(row)}>{row.name.value}</button>
        </li>)}</ul>;
}

export function ItemList({data, controller}) {
    return <>
        <label>
            <input type="checkbox" checked={data.i.archive.value === null}
                   onChange={() => controller.i.archive.change(data.i.archive.value === null ? 0 : null)}/> archived too
        </label>
        <ul>{data.i.list.map(row =>
            <li key={row.key}><button onClick={() => controller.i.change(row)}>{row.name.value}</button></li>)}</ul>
    </>;
}

export function DetailPanel({data}) {
    return <>
        <h2>{data.i.nameItem.value}</h2>
        <ul>{data.d.list.map(row =>
            <li key={row.key}>{row.name.value}: {row.quantity.value}</li>)}</ul>
    </>;
}
```

Each container's `props.data` holds only the parts it draws: `CategoryList` sees `data.c`, `ItemList` sees `data.i` with its rows and the `archive` filter group, `DetailPanel` sees `data.d` and, as `data.i`, only the panel property `nameItem` moved there. A part of a group is projected where its component stands — the rows where the group's grid is, a panel property where the property itself is — so the current item's name reaches the detail panel as a panel property of `i`, added under an alias and `MOVE`d into the container that draws it, without the group's rows. A click in a list sets the current object (`controller.c.change(row)`), and the filters of the groups below follow it — the platform's own dependency between groups, so the containers need no shared JavaScript state. A container that does not draw a group's rows chooses its current object through the form's controller — `controller.form.changeObject('c', row)`, with a row of its own data or a lookup's `objects` handle —, as it sorts the rows another container draws ([across containers](../language/Form_and_navigator_controllers.md#across-containers)).

The named selection is a `FILTERGROUP`: `data.i.archive.value` is `0` for active items and `null` for all items, including archived ones. To show archived items too, call `controller.i.archive.change(null)`; to hide them, call `controller.i.archive.change(0)` ([Filter groups](../language/React_filters_and_toolbar.md#filter-groups)). For other ways to select rows, see [Choosing a filter mechanism](#choosing-a-filter-mechanism).

The regions are laid out by the standard containers: a horizontal parent, an extension coefficient (`fill`) on each column, and a base height on a column whose content scrolls inside it ([the base size](../language/React_views.md#base-size)).

### A tab strip over what the platform draws {#placed-tabs}

We need to show the children the platform draws as tabs: a strip of their captions and images over the one child shown. The solution uses the [child placement](../language/React_platform_components.md#placing), the child's entry, [`<Caption>`](../language/React_rendering.md#caption) and [`<Image>`](../language/React_rendering.md#image).

Here the form `orders` has two object groups, `o` and `i`. Their boxes are moved into the view's container, the platform draws them (`custom = NULL`), and `PlacedTabs` places the active one:

```lsf
DESIGN orders {
    NEW tabs FIRST {
        custom = 'PlacedTabs';
        MOVE BOX(o) { custom = NULL; }
        MOVE BOX(i) { custom = NULL; }
    }
}
```

```jsx
const { Platform, Caption, Image } = window.lsfusion;

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

Rendering only the active child, rather than hiding the others, is what makes the closed tabs stop being read — see [Child placement](../language/React_platform_components.md#placing). Put the tabs over a group's `BOX`, not its `GRID`: only a container and a property carry a caption and an image, so a placed `GRID(o)` has an entry with no caption — it says only whether the form hides the grid.

A tab strip over children of both kinds keeps one name per tab, the name it places the child by, and reads the child's entry by it — a design identifier at the top level, a property's name as its path:

```jsx
const TABS = ['BOX(o)', 'o.note'];   // a container, and a panel property of o the platform draws
const entryOf = (data, name) => {
    const dot = name.lastIndexOf('.');
    return name in data || dot < 0 ? data[name] : data[name.slice(0, dot)][name.slice(dot + 1)];
};
// the strip: <Caption value={entryOf(props.data, sid).caption}/>; the body: <Platform key={active} name={active}/>
```

### Status buttons for a `SELECT` property {#status-buttons}

We need the user to pick an order's status from a row of buttons, one button per status. The solution uses the entry of a property with a [selection](../language/React_projection.md#select) — its `select` field — and the `change` of the property's member.

```lsf
FORM orders 'Orders'
    OBJECTS o = Order PANEL
    PROPERTIES(o) number READONLY, nameStatus SELECT, tags SELECT
;

DESIGN orders {
    NEW card FIRST { custom = 'OrderCard'; MOVE BOX(o); }
}
```

```jsx
const { Caption } = window.lsfusion;

export function OrderCard(props) {
    const { data, controller } = props;
    return <div className="status">
        {data.o.nameStatus.select.list.map(option =>
            <button key={option.key} aria-pressed={!!option.selected}
                    onClick={() => controller.o.nameStatus.change(option)}>
                <Caption value={option.value}/>
            </button>)}
    </div>;
}
```

### A quote board refreshed by `SCHEDULE` {#live-data}

We need to show quotes that change while the form is open, re-read by the form every two seconds. The solution uses the form's `SCHEDULE` event running `formRefresh` ([Live data](../language/React_form_behavior.md#live-data)).

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

```jsx
export function QuoteBoard({data}) {
    return <table><tbody>{data.q.list.map(row =>
        <tr key={row.key}>
            <td>{row.symbol.value}</td>
            <td>{row.price.value}</td>
            <td>{row.changedAt.value?.toLocaleTimeString()}</td>
        </tr>)}</tbody></table>;
}
```

### Selecting a tab and collapsing a container {#tabs-and-collapse}

We need a view that selects a tab of the form, shows whether that tab is selected, and collapses a container of the form. The solution uses a property with [`ACTIVE TAB`](../language/ACTIVE_operator.md) and actions with [`ACTIVATE TAB`](../language/ACTIVATE_operator.md) and [`COLLAPSE CONTAINER`](../language/COLLAPSE_operator.md), moved by the design into the view's container ([A hidden view](../language/React_form_behavior.md#hidden-view)).

Here `lines` is a page of a `tabbed` container of the form `order`, and `details` a collapsible container of the same form:

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
export function Steps({data, controller}) {
    return <>
        <button aria-pressed={data.linesSelected.value === true} onClick={() => controller.selectLines.exec()}>Lines</button>
        <button onClick={() => controller.collapseDetails.exec()}>Hide details</button>
    </>;
}
```

### A menu of your own {#app-menu}

We need to draw the navigator's menu with a component of our own and keep the platform's button for some of the elements. The solution uses a [navigator window](../language/React_windows.md#navigator-window), [the standard button](../language/React_windows.md#standard-button) and the navigator controller's [`activate`](../language/Form_and_navigator_controllers.md#navigator-controller).

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
const { Caption } = window.lsfusion;

export function AppMenu({ data, controller }) {
    function item(name) {
        const e = data.byName[name];
        return <li key={name}>
            <button className={e.selected ? 'on' : ''}
                    onClick={ev => controller.activate(name, ev)}><Caption value={e.caption}/></button>
            {e.children.length > 0 && <ul>{e.children.map(item)}</ul>}
        </li>;
    }
    return <nav><ul>{data.root.map(item)}</ul></nav>;
}
```

The same menu with the platform's button for `Sale.sales`:

```jsx
const { Platform, Caption } = window.lsfusion;

export function AppMenu({ data, controller }) {
    function item(name) {
        const e = data.byName[name];
        return <li key={name}>
            {name === 'Sale.sales'
                ? <Platform name="Sale.sales" className="menu-main"/>
                : <button onClick={ev => controller.activate(name, ev)}><Caption value={e.caption}/></button>}
            {e.children.length > 0 && <ul>{e.children.map(item)}</ul>}
        </li>;
    }
    return <nav><ul>{data.root.map(item)}</ul></nav>;
}
```

```lsf
NAVIGATOR {
    Sale.sales NOCUSTOM;
}
```

A menu that is mostly standard is written this way as one `<Platform>` for each element the platform draws, and markup only for the elements the component draws itself.

### A forms strip of your own {#forms-board}

We need to draw the window the forms open in our own way: a strip that selects and closes the open forms, over the current form. The solution uses [the forms window](../language/React_windows.md#forms-window).

```lsf
EXTEND WINDOW System.forms CUSTOM 'FormsBoard';
```

```jsx
const { Platform, Caption } = window.lsfusion;

export function FormsBoard({ data, controller }) {
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

### A message log of your own {#message-log}

We need to draw the messages to the user as a log of our own, with the time of each message and a button that toggles the window's pin mode. The solution uses [the log window](../language/React_windows.md#log-window).

```lsf
EXTEND WINDOW System.log CUSTOM 'MessageLog';
```

```jsx
const { Platform, Caption } = window.lsfusion;

export function MessageLog({ data, controller }) {
    return <div className="log">
        <a className="log-pin" onClick={() => controller.togglePin()}>pin</a>
        {data.messages.map(name => {
            const m = data.byName[name];
            return <div key={name} className={m.failed ? 'log-failed' : 'log-ok'}>
                <div className="log-line">{new Date(m.time).toLocaleTimeString()} <Caption value={m.caption}/></div>
                <Platform name={name}/>
            </div>;
        })}
    </div>;
}
```

### An application shell {#app-layout}

We need to lay the application's windows out ourselves: a top bar, a menu column the user can fold, the forms area and the message log. The solution uses [the window layout](../language/React_windows.md#window-layout).

```lsf
NAVIGATOR CUSTOM 'AppLayout';
```

```jsx
const { Platform } = window.lsfusion;

export function AppLayout({ data }) {
    const [menu, setMenu] = React.useState(true);
    // the menu's column is there while the user has not folded it and the platform has something to show in it
    const showMenu = menu && !data.byName['System.toolbar'].hidden;
    return <div className={showMenu ? 'app' : 'app app-no-menu'}>
        <header className="app-top">
            <Platform name="System.logo"/>
            <Platform name="System.root" className="app-sections"/>
            <Platform name="System.system"/>
            <button onClick={() => setMenu(!menu)}>Menu</button>
        </header>
        {showMenu && <aside className="app-menu"><Platform name="System.toolbar"/></aside>}
        <main className="app-work"><Platform name="System.forms"/></main>
        {!data.byName['System.log'].hidden && <footer className="app-log"><Platform name="System.log"/></footer>}
    </div>;
}
```

```css
.app { display: grid; height: 100%; grid-template: "top top" auto "menu work" 1fr "log log" auto / 240px 1fr; }
.app.app-no-menu { grid-template: "top" auto "work" 1fr "log" auto / 1fr; }
.app-top { grid-area: top; display: flex; align-items: center; }
.app-menu { grid-area: menu; }
.app-work { grid-area: work; display: flex; flex-direction: column; min-height: 0; }
.app-log { grid-area: log; height: 120px; display: flex; }
.app-work > .lsf-platform, .app-log > .lsf-platform { flex: 1; }
```

### The application header {#application-header}

We need a header above the forms area with a search field that finds customers and opens the one clicked. The solution uses [projection reading](../language/React_projection.md#reading) with `useData` and `useController`, and the controller's [members](../language/React_view_controller.md#members).

A header with a search field that finds records is a form: it wants a live projection, a session to hold the query, ordering and paging, and a way to open what it finds. So it is built as a form and opened into a window of its own, a [`WINDOW ... FORMS`](../language/WINDOW_statement.md) above the forms area, with [`SHOW ... WINDOW windowName`](../language/SHOW_operator.md).

The query is a form property here because an empty query must find nothing. A search over a list the user browses uses a [property filter](../language/React_filters_and_toolbar.md#property-filters) by default.

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
    PROPERTIES(c) openCustomer GRID
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

Two things make it read as a header rather than as a document. `REMOVE TOOLBARBOX` takes the form's own toolbar away - there is nothing here for the user to save, cancel or close, and the buttons are drawn by that container. The results are drawn through a **portal into the page body**, not inside the container, and that is the second thing this kind of view must get right. A window is short by design, and it clips what its form draws - a list left inside a header band is cut off after the first row. Nor is a high `z-index` enough: the window's own element is a stacking context (`position: relative; z-index: 0`), so a layer inside it can never rise above the neighbouring windows. A portal leaves both behind. The layer is then positioned from the field's `getBoundingClientRect()` and has to follow it on resize and scroll, and go away on Escape.

`AUTOSIZE` on the window is what keeps the band from being a tall empty strip: the header is as tall as its form - here one input - while still stretching across, and the `fill = 1` container inside it fills that height rather than hugging its own content. A window is sized by the layout rather than by the form in it, so without it the band keeps the share of the height its `POSITION` asks for, empty or not. Note also that the client remembers a size the user dragged, and restores it over the declared one - `Service.resetWindowsLayout` is what puts the declared sizes back. Two things about `POSITION` are worth knowing before the header looks wrong. The numbers are not a rectangle the window is drawn in but the way the layout splits the space, so a header of `POSITION(20, 6, 80, 10)` becomes a band to the right of the toolbar and above `System.forms`. And unless the navigator is fully pinned its panel is an overlay that hangs over the windows beside it: a click aimed at the header then lands on the menu underneath and runs whatever that menu item runs.

What the form route does not give is freshness across connections: a form does not see what another connection committed until it re-reads, so a header that must show that adds [`EVENTS ON SCHEDULE PERIOD n formRefresh()`](../paradigm/Interactive_view.md) to the form. And the header is drawn by the desktop web client only: the mobile web client and the desktop client draw `System.forms` alone and open the header there, as a tab, so an application that serves them keeps the header's design usable as a plain form.

### A catalogue tree {#tree-catalogue}

We need to draw a tree of categories and their items ourselves, with nodes that open and close and the current row in bold. The solution uses the fields of a tree's rows, the `toggle` and `change` members of its groups and the tree's own node with `current` and `expandCurrent()` ([Trees](../language/React_trees_sortings_selection.md#trees)).

```lsf
FORM catalogue
    TREE t c = Category PARENT parent(c), i = Item
    PROPERTIES(c) name
    PROPERTIES(i) name
    FILTERS category(i) == c
;

DESIGN catalogue {
    GRID(TREE t) { custom = 'Tree'; }
}
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

### Sortable column headers {#sortable-headers}

We need column headers that sort the rows on a click and show the direction of the sorting. The solution uses the group's `orders` and their `change` ([Sortings](../language/React_trees_sortings_selection.md#orders)). The form is `orders` of [Component selection](../language/React_views.md#selecting-the-component).

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

### Choosing a filter mechanism {#choosing-a-filter-mechanism}

We need to choose how a view selects and orders the rows it shows: by the group's current object, [user filters](../language/React_filters_and_toolbar.md#filters), a property filter, a filter group, view filters, a sorting or a form property.

Rows are selected and ordered on the server. Choose the mechanism by the situation:

| Situation | Mechanism |
| --- | --- |
| A record or category chosen from a list | The group's current object, `controller.<g>.change(row)` |
| A search the user types over one or several properties | A [property filter](../language/React_filters_and_toolbar.md#property-filters), `controller.<g>.filter.<p>.change(...)`; for several properties, a combined property or adjacent filters joined by `or` and stated together |
| Conditions built by the view's filter editor | The group's user filters, `controller.<g>.filters.change(...)` |
| A sorting the user picks | The group's [sortings](../language/React_trees_sortings_selection.md#orders), `controller.<g>.orders.change(...)` |
| A fixed set of named selections | A [filter group](../language/React_filters_and_toolbar.md#filter-groups), `FILTERGROUP` |
| A condition imposed by the view that the user must not see or reset | The group's [view filters](../language/React_filters_and_toolbar.md#view-filters), `controller.<g>.viewFilters.change(...)` |
| A value other form logic reads, an empty value that must select no rows, or a predicate user filters cannot state | A form property (normally `DATA LOCAL`) and `FILTERS` / `ORDERS` over it |
| Which region or tab is shown | React state (`useState`, [`<Platform>`](../language/React_platform_components.md#placing), [`useShown`](../language/React_platform_components.md#placing)) |

### A condition editor {#condition-editor}

We need a filter condition editor of our own in place of the conditions the platform draws: the user picks a property, types a value and adds the condition. The solution uses [a custom filter panel](../language/React_filters_and_toolbar.md#custom-filter-panel) and the group's [user filters](../language/React_filters_and_toolbar.md#filters) with their `filterProperties`.

```lsf
DESIGN items {
    FILTERS(i) { custom = 'FilterPanel'; }
}
```

A view that draws `FILTERS(<group>)` alone builds a condition editor from `filterProperties`, with no property of the group drawn there:

```jsx
export function FilterPanel({data, controller}) {
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

### Search by one property {#property-search}

We need a search field over one property of a list. The solution uses a [property filter](../language/React_filters_and_toolbar.md#property-filters).

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

```jsx
export function ItemSearch({data, controller}) {
    if (data['FILTER(name(i))'].hidden) return null;
    const filter = data.i.filter.name;
    return <label>{filter.caption}
        <input value={filter.value ?? ''} onChange={e => controller.i.filter.name.change(e.target.value)}/>
    </label>;
}
```

### Search by several properties {#several-properties-search}

We need one search field over several properties. The solution uses [property filters](../language/React_filters_and_toolbar.md#property-filters) and a [batch](../language/Form_and_navigator_controllers.md#batch).

One input can search several properties without a `DATA LOCAL` search property. One option is a combined property, declared `FILTER` (here `name` and `description` are string properties of `Item`):

```lsf
searchText 'Search' (Item i) = CONCAT ' ', name(i), description(i);

FORM items
    OBJECTS i = Item
    PROPERTIES(i) searchText FILTER
;

DESIGN items {
    NEW search FIRST {
        custom = 'CombinedItemSearch';
        MOVE FILTER(searchText(i));
    }
}
```

```jsx
export function CombinedItemSearch({data, controller}) {
    const filter = data.i.filter.searchText;
    return <input value={filter.value ?? ''}
                  onChange={e => controller.i.filter.searchText.change(e.target.value)}/>;
}
```

[`CONCAT`](../language/CONCAT_operator.md) skips `NULL` operands and places a space between the others. The search can match across the boundary between fields, so this differs from "name contains text OR description contains text". The combined property must be in `PROPERTIES`, but need not be displayed: its `FILTER(searchText(i))` component must remain in the design.

For a separate match in either property, declare their filters next to one another and state both in one synchronous `batch`:

```lsf
FORM items
    OBJECTS i = Item
    PROPERTIES(i) name FILTER, description FILTER
;

DESIGN items {
    NEW search FIRST {
        custom = 'NameOrDescriptionSearch';
        MOVE FILTER(name(i));
        MOVE FILTER(description(i));
    }
}
```

```jsx
export function NameOrDescriptionSearch({data, controller}) {
    const change = text => controller.batch(() => {
        controller.i.filter.name.change(text, {compare: '_', negation: false, or: true});
        controller.i.filter.description.change(text, {compare: '_', negation: false, or: false});
    });
    return <input value={data.i.filter.name.value ?? ''} onChange={e => change(e.target.value)}/>;
}
```

At form opening, empty property filters occupy the start of the condition list in declaration order, so these two conditions are adjacent. Always change and clear both together: `or` joins a condition to the next **active** one, so clearing only the second can join the first to an unrelated condition. Replacing the whole list with `filters.change([...])` moves empty property filters to its end, and does not guarantee that this pair stays adjacent. Use the paired variant only while the two conditions remain adjacent; otherwise use a combined property or explicitly state the entire condition list when the view owns it.

### Only the filters, or the whole group {#filters-or-whole-group}

We need to decide whether a component replaces only a group's filters ([Custom filter panel](../language/React_filters_and_toolbar.md#custom-filter-panel)) or the whole group.

Receiving a group's state does not make a component the only one drawing it. Which components are drawn is decided
by the design and by what is placed with `<Platform>`, not by what `data` contains — the state is the group's, whoever draws it. So
there are two recipes, and they read differently in the design rather than in the projection:

- **replace the filters** — `custom` on `FILTERS(o)`: the component *is* the filter area;
- **replace the whole group** — `custom` on `BOX(o)`: nothing else is drawn at all, and the component owns the
  filters along with the rows.

### A header over the standard grid {#grid-header}

We need a header of our own over a group's standard grid: the record count, export to Excel, the manual update mode and the view type. The solution uses the group's [toolbar](../language/React_filters_and_toolbar.md#toolbar), drawn by the view.

```lsf
DESIGN items {
    NEW header FIRST {
        custom = 'ItemHeader';
        MOVE TOOLBARSYSTEM(i); // the grid stays the platform's; its toolbar is the view's
    }
}
```

```jsx
export function ItemHeader({data, controller}) {
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

### A tree's header {#tree-header}

We need a header of our own over a tree the platform draws: opening the current node and the whole tree, export to Excel and showing the filter controls. The solution uses [a tree's toolbar and filter controls](../language/React_filters_and_toolbar.md#tree-toolbar).

```lsf
DESIGN catalogue {
    NEW header FIRST {
        custom = 'CatalogueHeader';
        MOVE TOOLBARSYSTEM(TREE t); // the tree stays the platform's; its toolbar is the view's
    }
}
```

```jsx
export function CatalogueHeader({data, controller}) {
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

### Item cards in `CUSTOM` {#item-cards}

We need to show a group's rows as cards, one of the view types the toolbar switches. The solution uses a [React component in `CUSTOM`](../language/React_component_views.md#form-custom) of the group.

```lsf
FORM itemCards
    OBJECTS i = Item CUSTOM 'ItemCards'
    PROPERTIES(i) READONLY name
    PROPERTIES(i) quantity
;
```

```jsx
function ItemCards({data, controller}) {
    return data.list.map(row =>
        <button key={row.key} disabled={row.current}
                onClick={() => controller.change(row)}>
            {row.name.value}: {row.quantity.value}
        </button>
    );
}

window.lsfusion.custom.register('ItemCards', ItemCards, 'reactView');
```

### A theme context {#theme-context}

We need a React context the view provides — a theme — to reach the components in the cells of a table the platform draws and the view places. The solution uses a [React component inside a React view](../language/React_component_views.md#embedded). `Quantity` here is a property's component in [`CUSTOM`](../language/React_component_views.md#form-custom).

```lsf
FORM items
    OBJECTS i = Item
    PROPERTIES(i) READONLY name
    PROPERTIES(i) quantity CUSTOM 'Quantity'
;

DESIGN items {
    NEW board FIRST {
        custom = 'Board';
        MOVE GRID(i) { custom = NULL; }
    }
}
```

```jsx
const { Platform } = window.lsfusion;
const Theme = React.createContext('light');

export function Board() {
    return <Theme.Provider value="dark">
        <Platform name="GRID(i)"/> {/* a table of the platform's, its quantity drawn by Quantity */}
    </Theme.Provider>;
}

function Quantity({data}) {
    const theme = React.useContext(Theme); // 'dark' in every cell of the table Board places
    return <span className={theme}>{data.value}</span>;
}

window.lsfusion.custom.register('Quantity', Quantity, 'reactView');
```

### Keeping the components' state {#keeping-state}

We need the components of a placed child to keep their state while the view does not show the child, as in a tab strip ([A React component inside a React view](../language/React_component_views.md#embedded)).

To keep the state of the components in a child while the child is not shown, keep its place rendered and hide it, rather than render only the place of the child that is shown: `<div hidden={name !== active}><Platform name={name}/></div>`. A child hidden so is still placed: hiding its place does not stop the server reading it.

Where the place itself has to go, `ownRoot` keeps the state instead.

### A React component or an HTML template {#react-or-template}

We need to choose whether a container is drawn by a React component or by an HTML template ([A React component and an HTML template](../language/React_views.md#choosing-between-a-react-component-and-an-html-template)).

Markup of your own that has to use a caption the application computes — as text of its own, or a container's caption — is the point at which a template stops being enough.

A React component earns its place when the layout is computed — the children are placed conditionally, the grid template is derived from the data read through `useData`, or the markup comes from a component library — or when the markup has to show something the projection carries, which for a container or a child the platform draws is its `caption` and its `image`.
