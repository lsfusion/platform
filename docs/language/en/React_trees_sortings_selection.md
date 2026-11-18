---
slug: "/React_trees_sortings_selection"
title: 'Trees, sortings and selection in React'
---

A [React view](React_views.md) that draws the rows of an object group gets in `props.data` the same state of those rows as the platform's table has, and changes it through `props.controller`: the hierarchy of an [object tree](../paradigm/Interactive_view.md#tree) and which of its nodes are open, the [sortings](../paradigm/Interactive_view.md#userfilters), and the selected rows and columns. It also copies and pastes the selected cells as that table does.

### Trees {#trees}

A group of a tree is projected like any other group: what a container has of it is what its own components put there,
and where the TREE's rows are drawn that is `list`, `byKey`, `keys` and the rows themselves. Each of those rows says
where it sits in the hierarchy:

```js
{
    key, current, objects, ...cells,
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
it comes from drawing the tree from its roots down.

`hasChildren` is what the server says the node has, loaded or not — not a design attribute, but the tree's live
state.

`expanded` says the node is open: its children are loaded — or a view asked to open or close it and the rows of the
answer have not arrived yet, and then it says what was asked, as a changed value is shown before the answer and as the
platform's own tree turns its expander glyph at the click. A node is closed at once, while its children stay in `list`
until the answer takes them away, so a view that draws a node's children only under an `expanded` node hides
them at once. Only a node with children opens: one whose `hasChildren` is false stays closed, as it does in the
platform's tree. It is live state as well.

If an expand or collapse request fails without updating the rows, `expanded` keeps the requested state until the server sends rows for the node's group or the immediately following group in the tree in response to that request or a later one.

`loading` is `true` while the node's children are being loaded: a view asked to open it, and the rows of the answer
have not arrived — as a cell's `loading` says a change of it waits for its answer. Closing a node loads nothing, and
neither does opening one whose `hasChildren` is false; the field is absent but while it is `true`.

Rows arrive as the tree is opened — past the roots, a group of a tree returns only the rows under nodes that are
currently expanded, which is why, before anything is opened, its lower groups project an empty list.

A row of a group below carries the path above it, so a row made current — through `change(row)`, or by the server —
makes the rows on that path current in their groups as well. `current` follows the path up, as the platform's tree
selects one node. The groups below it have no current row — the server clears their current objects when a row above is
chosen. Every row in those groups has `current` set to `false` until a row is chosen there again.

`controller.<g>.expand(row)` / `.collapse(row)` / `.toggle(row)` open and close one node: a node is a row of a group, so
they are the group's. Each takes a row (the row, its `objects` handle, or its key) and returns the `Promise` of its request: the row's
`expanded` changes at once, and its children arrive as rows. `toggle` asks for the opposite of what the row's `expanded`
says now, so two toggles before the answer open the node and close it again. `controller.<g>.expandAll()` opens every
node of the group and of the groups below it: on the tree's top group, the whole tree; on a group below, its nodes under
the nodes open above it. `controller.<g>.collapseAll()` closes the same nodes and every node below them, so a node
opened again afterwards shows its children closed. Their rows' `expanded` changes at once too. A group outside a tree has no such members at all: the five are
installed on a group of a tree and nowhere else, and they are reserved on that group's node, as `list` is.

These verbs are members of the group's node, so a container has them exactly where it has that group's rows: the tree
React draws. A tree the platform draws is opened by the platform's own expander, and no react container has node verbs
for it; a view that draws its [toolbar](React_filters_and_toolbar.md#tree-toolbar) opens it through the toolbar's two buttons.

```lsf
FORM catalogue
    TREE t c = Category PARENT parent(c), i = Item
    PROPERTIES(c) name
    PROPERTIES(i) name
    FILTERS category(i) == c
;
```

What is a tree's, one for all its groups, is on a node of the tree's own, `data.<tree>` and `controller.<tree>`, where
`<tree>` is the tree's name in the form, the `t` of `TREE t` (a tree with no name gets a generated one, so a tree a view
draws a part of is named): at the top level, beside its groups' nodes. A container has it wherever it draws a part of
the tree: the tree's rows — as a group's node is where its rows are — with its [sortings](#orders), its [user
filters](React_filters_and_toolbar.md#filters), its [toolbar and filter controls](React_filters_and_toolbar.md#tree-toolbar). It carries no property, so a property of a group
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
[batch](Form_and_navigator_controllers.md#batch) it goes with the batch, so the row a `change(row)` made current earlier
in the same batch is the node it opens.

### Sortings {#orders}

As after a change of the [filters](React_filters_and_toolbar.md#filters), after a change of the sortings the server
reads rows around the current row.

A view that draws the rows of a group not in a tree carries `orders` — the group's sortings, in priority order — beside
`list` and `keys`: a sorting is the order the rows are drawn in, so it is projected where they are, and a container
holding only a panel property of the group has neither. Where it is there it is always there, and empty when the group
is sorted by nothing. Each entry is an object:

| Field | Meaning |
| --- | --- |
| `property` | The sorted property's integration SID — on a [tree](#trees)'s node, with its group's name, as below |
| `desc` | Whether it sorts descending |

The same shape the [`ORDER ... FROM`](FILTER_ORDER_operators.md) operator applies and
`ORDERS ... TO` reads, with one difference: `orders` names a property the way the rest of the node does — by its
integration SID — while those operators name it by its SID on the form (`sum(o)`).

A sorting names a property of the group, as a [condition](React_filters_and_toolbar.md#filters) does, not an entry of the node: the property need
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

A [tree](#trees)'s sortings are the tree's, as its [user filters](React_filters_and_toolbar.md#filters) are: where a view draws the tree's rows,
`data.<tree>.orders` lists the sortings of all its groups, group by group, top down — each group's in its own priority
order, as no group's sortings outrank another's —, each property named by its group's name and its own, `c.name`; its
groups' nodes have no `orders`. `controller.<tree>.orders.change(...)` changes them the same way: one order merges into
its group's sortings, and an array states the tree's whole list — each group gets the entries on its own properties, and
a group none of them names stops being sorted. Each group's list is sent as a group's is.

`orders` describes the sorting the user can change, whoever stated it. A **fixed** sorting is not part of it and cannot
be: `ORDERS ... FIXED`, and an `ORDERS` entry that is an expression rather than a property drawn on the form, are
applied on the server and never sent to the client at all. Neither is the implicit ordering by the group's own objects
that makes a read stable. So the rows can be ordered by more than `orders` says, and a view cannot show an indicator for
the part it is not told about.

`orders` is what the form has: its own `ORDERS` when it opens, then every list the server reports — an
[`ORDER`](FILTER_ORDER_operators.md) action's too. It is rebuilt only when that list changes: it keeps
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
default `ORDERS` entry on it, and an [`ORDER`](FILTER_ORDER_operators.md) action naming it — the server
sorts the rows by it, and `orders` says so, as the standard grid's header shows it too: what is not offered is only the
click that would sort by it.

Asking to sort BY a property declared `noSort` is refused, and so is a list naming one property twice, a name the
group does not carry, and a property grouped in columns. Taking a `noSort` sorting off is stating an order with no
`desc`, which is allowed: what is refused is sorting by such a property, not stopping.

### Selected rows and columns {#selection}

A view that draws the rows of a group not in a tree selects rows and columns as the platform's table does. The selected
rows are a range the server keeps, in the group's current order ([selection](../paradigm/Selection_SELECT.md)), so the
range also holds rows the view has not read. A row in the range has `selected` in `list`; a `change` to a row changes
the range on the rows of `list` at once, before the server answers — unless `'extend'` runs it on from a current row
`list` does not have, which the answer brings. The columns are the list
properties the view draws in its rows, except actions, in the order of `properties`. The entry of a column — the one
its caption is in — has `current`, whether the column is the current one, and `selected`, whether the range of
selected columns holds it. A cell is selected where its row and its column are.

The members that change the selection are on the group's member where its rows are drawn, and only there:

| Member | What it does |
| --- | --- |
| `change(row[, {selection}])` | Makes the row current and changes the selected rows |
| `change({to: 'home'}[, {selection}])`, `change({to: 'end'}[, {selection}])` | Makes the first or last row of the whole list current, under the group's current filters and sortings, and changes the selected rows. The form's controller has the two edges of the list as `form.HOME` and `form.END` |
| `properties.change(column[, {selection}])` | Makes the column current, named by its property, and changes the selected columns |
| `selectAll()` | Selects all the rows the filters leave and all the columns. The current row and the current column stay |

Each returns the `Promise` of its request. What a call does to the selection, `selection` says:

| `selection` | Rows | Columns |
| --- | --- | --- |
| `'start'`, or not given | No row is selected, and a range starts from this row | This column alone is selected, and a range starts from it |
| `'extend'` | The rows from the row the range starts from to this one are selected | The columns from the column the range starts from to this one are selected |
| `'keep'` | The selected rows stay. While no row is selected, a range starts from this row | The selected columns stay |

The two ranges are independent: a change of the row leaves the selected columns as they are, and a change of the
column leaves the selected rows. The platform's table changes both by one move of the cell; a view does it by the two
calls, which a [batch](Form_and_navigator_controllers.md#batch) sends in one request — a click on a cell, for example:

```jsx
function Cell({ row, name, controller }) {
    const select = selection => controller.batch(() => {
        controller.i.change(row, { selection });
        controller.i.properties.change(name, { selection });
    });
    return <td onClick={e => select(e.shiftKey ? 'extend' : 'start')}>{row[name].value}</td>;
}
```

The view takes the keys of the platform's table itself, as it takes [the other keys of the table](React_form_behavior.md#keys):

```jsx
function onKeyDown(e, controller) {
    const selection = e.shiftKey ? 'extend' : 'start';
    if (e.ctrlKey && e.key === 'Home')
        controller.i.change(controller.form.HOME, { selection });
    else if (e.ctrlKey && e.key === 'End')
        controller.i.change(controller.form.END, { selection });
    else if (e.ctrlKey && e.key === 'a')
        controller.i.selectAll();
}
```

The server finds the first or last row when it reads the rows of the group, after all the changes of the request are
made, as it does for an action's `ACTIVATE FIRST` / `LAST`. So a call made after such a move in the same batch still
acts on the row that was current before it.

The form sees this selection as it sees the selection in the platform's table: the properties of
[`SELECT`, `SELECT ACTIVE` and `SELECT PROPERTY`](Object_group_operator.md) return it, the current column
is the active property ([`ACTIVE PROPERTY`](ACTIVE_operator.md)), and the form's
[`SELECT` and `SELECT PROPERTY` events](Event_block.md) occur.

A column the form does not show now — its entry is `hidden` — cannot be made current, and a name that is not a column
is refused. A group of a tree has no selection: its `change` takes a row only, with no `selection` but `'keep'`.
`useSeekOnScroll` moves the current row with `'keep'`, as a scroll of the platform's table
leaves its selection.

The selection is the view's that draws the rows. Another container changes it through the form's controller —
`controller.form.changeObject('<group>', row, {selection})`, `changeColumn('<group>', column, {selection})` and
`selectAll('<group>')` ([across containers](Form_and_navigator_controllers.md#across-containers)). Where the platform's
table draws the rows, the same calls change the table's own selection: a column is made current where the table shows
it now and lets the user move to it, and a property grouped in columns has no column of that name. While the table
shows the rows as another view — a pivot, a map, a calendar, a custom view — the calls are refused, as they are for a
group of a tree: `changeObject` takes a row there, with no `selection` but `'keep'`.

### Clipboard {#clipboard}

A view that draws the rows of a group not in a tree copies and pastes the [selected](#selection) cells as the
platform's table does, by two more members in the same place:

| Member | What it does |
| --- | --- |
| `copy()` | Returns the `Promise` of the text of the selected cells: a line for each row, with tabs between the values of its cells |
| `paste(text)` | Pastes the text into the selected cells and returns the `Promise` of its request |

The cells are the selected rows in the selected columns the form shows. While no row of `list` is `selected`, the row
is the current one, and while no column is selected, the column is the current one. The selected rows are the ones the
server keeps, so `copy` also returns the rows the view has not read, and `paste` changes them too; the text of the
current row `copy` returns with no request to the server.

`copy` returns each value as the platform's table writes it to the clipboard, and `paste` reads a text written so, by
the platform's table or by a spreadsheet. What `paste` does depends on the text:

| Text | What is pasted |
| --- | --- |
| One value | The value, into every one of these cells. With rows selected, the current row takes it too where it is not among the selected rows, as in the platform's table |
| Several values, in lines with tabs between the values | The values, into the cells from the current one on: the columns go into the current column and the columns after it, the lines into the current row and the rows after it in the group's order. Where the list has fewer rows, the server adds new objects — in a group of one object —, as it does for the platform's table |
| An empty text | Nothing |

Each cell is changed as a user's change of it: the property's [change event](Event_block.md) runs, and a
cell the form does not let change — a `READONLY` property, for example — is skipped, the other values staying in their
cells. A value that cannot be read as the cell's type is pasted as `NULL`. With no current row in `list` nothing is
pasted, and with no column `copy` returns an empty text.

`copy` and `paste` are calls of their own. A [batch](Form_and_navigator_controllers.md#batch) does not carry them,
and inside one they are refused. The platform's table asks the user before it pastes several values. `paste` does not
ask, so a view that needs the question asks it itself.

The view takes the browser's `copy` and `paste` events itself and gives the text to the clipboard when it has it:

```jsx
<div tabIndex={0}
     onCopy={e => {
         e.preventDefault();
         controller.i.copy().then(text => navigator.clipboard.writeText(text));
     }}
     onPaste={e => {
         e.preventDefault();
         controller.i.paste(e.clipboardData.getData('text/plain'));
     }}>
    ...
</div>
```

Another container copies and pastes through the form's controller, `controller.form.copy('<group>')` and
`paste('<group>', text)`, which are refused where the selection's calls are. On the platform's table a column of an
action, and of a property grouped in columns, is none of the cells, and several values are not pasted while such a
column is the current one.
