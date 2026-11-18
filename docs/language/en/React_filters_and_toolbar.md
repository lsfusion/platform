---
slug: "/React_filters_and_toolbar"
title: 'Filters and toolbar in React'
---

A [React view](React_views.md) can draw the [user filters](../paradigm/Interactive_view.md#userfilters), the
[filter groups](../paradigm/Interactive_view.md#filtergroup) and the toolbar of an object group or a tree: it reads
their state on the node of the group or the tree in `props.data` and changes it through the members of that group
or tree in `props.controller`. The platform still draws the ones the design leaves to it, over the rows a view draws
too. A view that draws the rows of a group not in a tree can also have the server filter them by conditions of its
own — the group's view filters.

### User filters {#filters}

`filters` is the group's list of user filter conditions, in the order they are applied: the ones the server filters the
group by, with the changes that wait to be sent: in the [manual apply mode](#filter-controls), those that wait for the
apply, and, in either mode, a value the platform's panel prepares — F3's — till its editor commits it. They are a part
of the group of their own, like its rows and its panel properties, and they are on the group's node where their
component is — where `FILTERS(<group>)` is: inside the group's box, which is where a `custom` on `BOX(<group>)` draws
the rows too, or `FILTERS(<group>)` itself when it has a `custom` of its own. There `filters` is always present, and
empty when the group is unfiltered; the group's node in any other container does not have it. A group in `PANEL` view
has none anywhere: the platform shows it no user filters either. Each condition is an object:

| Field | Meaning |
| --- | --- |
| `property` | The filtered property's integration SID — the group's one property of that name, wherever it is drawn; on a [tree](React_trees_sortings_selection.md#trees)'s node, with its group's name, as below |
| `compare` | The comparison operator: `=`, `>`, `<`, `>=`, `<=`, `!=`, `_` (contains), `@` (match) |
| `value` | The value compared against, converted like any other property value |
| `negation` | Whether the condition is negated |
| `or` | Whether this condition is joined to the **next** one by OR rather than AND — so `or` on the first of two makes the pair a disjunction |

This is the shape the [`FILTER`](FILTER_ORDER_operators.md) operator reads, so one vocabulary describes a
condition in `.lsf` and in the projection; `FILTERS` writes it too, with the value as a string and `or` only on a
condition joined by OR. Only the name differs: `FILTER` names a property by its name on the form, the projection by its
integration SID, and the two are different names when the property has an `EXTID`.

A condition names a property of the group, not an entry of a node: the filter area need not be where any column of
the group is drawn. So a name the group draws twice — a column `price` and a panel property `price` in another
container — would name no condition, and a form whose user filters a React view draws is refused when it is built
until they have explicit `EXTID`s.

`data.<g>.properties` lists the entries of the node it is on, so on a node that holds only the user filters it is
empty: the properties a condition can name are the group's, not the node's — `filterProperties` lists them.

The controller takes a condition in the same shape, so a projected condition can be handed back to it as it is. What a
condition leaves out of `compare`, `negation` and `or` is read as below: in an array, as for a new condition — the
property's default comparison (its `defaultCompare` in `filterProperties`), no negation, AND —; on its own, as the
condition it takes the place of has it. One on a file property is the exception: its `value` is a link to the file, not
the file a condition compares with, so the controller refuses one that has a value; one with none is read as any other
with none.

`controller.<group>.filters.change(condition)` — a member of the group where `filters` is — changes **one** condition
and leaves the group's other conditions, and their order, alone: it takes the place of the FIRST condition on the same
property — a range is two conditions on one property, and its other bound stays —, keeping what it does not state of
that condition's comparison, negation and `or`; when there is none, it takes the place of the property's
[filter](#property-filters), where the group has one, else is appended — the default way, either way. So
`{property: 'price', value: 300}` turns `price > 100 OR …` into `price > 300 OR …`. A condition with no `value` —
absent, `null`, `''`, or one its type reads as `NULL`: `false` of a `BOOLEAN` property (a `TBOOLEAN`'s `false` is a
value) — takes that first condition off, alone. This is what a [property filter](#property-filters)'s `change` does, and
F3 in a grid over the conditions a view draws ([Custom filter panel](#custom-filter-panel)) — but those two, with
no condition on the property, compare as its filter compared last, and F3 on an empty cell states the condition
comparing with `NULL`: a `FILTER` being a user filter given in advance, its value and a condition added by hand are
stated by one rule.

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

An empty input takes the condition on `name` off — the first one, which the input shows —, keeping the other conditions:
its `value` is `''`, which is no value.

`filters` follows the group's list as it is changed and as the server reports it — a list a `FILTER` action sets, say —
and is rebuilt only then: it keeps its identity across renders in which nothing states its conditions anew — a change to
a property value, to the rows or to the current object leaves it as it was — so a view can compare it across renders and
memoize on it.

A change through the controller goes into the group's list, and `filters` shows it straight away. It is sent at once,
before the rows filtered by it come back — but in the [manual apply mode](#filter-controls), where it waits for the
apply, as a change of the platform's filter panel does; until it is answered, a list the server reports for the group in
the answer to an earlier request does not replace it. A view that sends a search only after a pause in the typing keeps
what is being typed in its own state and calls `change` when the pause comes.

After a change of the filters or sortings, the server reads rows around the current row, as in the platform's
table.

`controller.<group>.filters.change(conditions)` — the same thing the projection calls `data.<group>.filters`, changed
where it is read — replaces the list at once: an array states all the conditions a view can name — a condition on a
property it names none by, below, keeps its place —, a single condition states one. For clearing what a view can name
(`controller.i.filters.change([])` — the [filter controls](#filter-controls)' `resetFilters()` clears every condition,
at once), or when the view genuinely owns the entire condition set. The array is the list of conditions, as `FILTER`
reads one in `.lsf`: an element with no `value` is a condition comparing with `NULL` — "is empty", with `negation` "is
not empty" —, so a condition `filters` lists with a `null` `value` — one F3 states on an empty cell, say — is taken back
as it is, a `BOOLEAN` property's too, listed with `false`, which reads back as `NULL`. A property filtered by no more is
one the array leaves out.

Either form of `change` reports an author's mistake instead of dropping a condition: an unknown property, an unknown
comparison or `IN ARRAY`, which no condition compares by, an unknown field in a condition, a `negation` or `or` that
is not a boolean, a condition on an action or on a property grouped in columns.

A condition set elsewhere is not listed when it is on a property with no integration SID (`NOEXTID`), which the
projection has no name for, on an action, which is not something a form is filtered by, or on a property grouped in
columns, as a condition here names a property, not one of its columns. The console says which and why.

Handing `data.<group>.filters` back through the array form therefore leaves those conditions where they are, as changing
ONE condition never touches the conditions on other properties.

A tree has one filter box for all its groups, `FILTERS(TREE <tree>)`, and its user filters are on [the tree's
node](React_trees_sortings_selection.md#trees). `filters` there lists the conditions of all its groups, those of the upper groups first, and names each
property by its group's name and its own, `c.name`, as a [`<Platform>`](React_platform_components.md#crossing-back-to-lsfusion) names a property of a group.
`controller.<tree>.filters.change` takes a condition named the same way: the name is read up to its first dot, the
group's — a group's name has none, and an `EXTID` with a dot, which a property the platform draws may carry, is taken
back as `filters` gives it. A list it states is the tree's: `controller.t.filters.change([])` clears the conditions it
can name of all its groups in one request, and a single condition is merged into the conditions of all of them.

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
Nor is a file property: a condition on one is listed in `filters` all the same, and is stated with no value alone, as
above.

### Filter controls {#filter-controls}

The filter controls a view draws are the same whoever draws the conditions — the view, which gets `filters`, or the
platform's panel, `FILTERS(o)` placed back with `custom = NULL` —, and each does what the platform's button does there:

| on the node | member | over [conditions the view draws](#custom-filter-panel) | over the platform's panel |
| --- | --- | --- | --- |
| | `addFilter()` | the condition F3 puts in | a new condition on the same cell, its editor open, as the panel's add does — or the cell's value put into the property filter a view draws on that column, where it shows none |
| `filtersPending`, `filtersManual` | `applyFilters()` | what waits sent — nothing, with nothing waiting | the same, the conditions typed in included |
| | `resetFilters()` | the list cleared | the panel's conditions reset |

`filtersManual` is the manual apply mode: the setting that makes the user filters apply changes by hand
(`userFiltersManualApplyMode`) is on — the controls a view draws always have the apply button; the platform's have it
while they are shown, which the toolbar's filter button does. In it a change of the group's conditions waits for the
apply, whoever states it — the view, a [property filter](#property-filters), F3, the platform's panel —; the reset is
sent at once all the same, and so are a pivot's drill-down and a row the platform's panel deletes. The apply sends the
group's list as it stands: a value typed into an editor the view draws and kept in the view's own state is not in the
list until the view states it (`filters.change`, a property filter's `change`), so a view states it before the apply,
or the apply does not send it. `filtersPending` is whether anything waits: the list differs from the one sent — in the
manual apply mode till the apply, and in either mode while the platform's panel holds a value it prepared, F3's, which
its editor has not committed. The setting is off by default: a change is sent at once, `filtersManual` stays `false`,
and a view may as well not draw `FILTERCONTROLS(o)` at all — the group is filtered the same. Where the view draws the
conditions too, `resetFilters()` clears them all at once, the conditions on a property the view names none by
included, while `filters.change([])` clears the ones it can name and is sent as any change is: members of two
components may do much the same thing, each component's part repeating its component.

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
the [current node](React_trees_sortings_selection.md#trees)'s group that the form shows and a condition can be on, in that group's current row.
With no `FILTERS(o)` in the design `addFilter` is refused, as the platform has no such button, and F3 does nothing.

Inside a [batch](Form_and_navigator_controllers.md#batch) the commands go with it, as `filters.change` does.

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
| `canClear` | Whether none may be selected, as the platform's widget offers it: a group of one filter is its check box, always clearable; a group of several is its list, with "(All)" unless `NONULL`. Where it is `false`, `change(null)` is refused |

The server applies the filter selected and reports it, so `value` is what the server holds — the default filter when the
form opens, a `FILTERGROUP` action's choice — and a filter a view selects shows at once. `change` takes the index of one
of `filters`, or `null`, and anything else is refused. It is a part of a [batch](Form_and_navigator_controllers.md#batch)
as `filters.change` is, and asks the server even for the filter shown — after a batch that failed, it is set again. A
filter's key keeps working while the form shows the filter group — its descriptor's `hidden` — and the view that draws
it is shown, the host a container places that view in included, as the platform's widget takes it: it selects the
filter, and in a group of one filter — the platform's check box —, pressed again, takes it off.

A filter group is its group's — the last group for the objects its filters take, the one a
[filter](../paradigm/Form_structure.md#filters) of the form is applied to —, a group of a [tree](React_trees_sortings_selection.md#trees) included: it is
on that group's node, `data.<group>.<filterGroup>`, never on the tree's, which carries what the platform has one of for
the whole tree — its user filters, filter controls, toolbar and sortings —, though the platform's design puts a tree's
filter groups in the tree's `FILTERGROUPS(TREE <tree>)`.

### Property filters (`FILTER`) {#property-filters}

A property declared a [user filter](../paradigm/Interactive_view.md#userfilters) — `PROPERTIES(i) name FILTER`, or
`USER` in the [filters block](Filters_and_sortings_block.md) — has a filter of its own among the user filters
of its group: a component the design names `FILTER(name(i))`, by the property's name on the form, which the platform's
filter panel shows as a condition row there from the start if the component and every container holding it remain in
the form design. A property filter a view draws is a part of the group of its property there, as a filter group is: on
the group's node, under `filter`, by the property's integration name — one object for the filters the view draws, with
no prototype, as `byKey` is:

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
{caption: 'Name', type: 'string', active: false, compare: '_', negation: false, or: false, value: null}

controller.i.filter.name.change('ab');                     // filtered by it
controller.i.filter.name.change('ab', {negation: true});   // ... by its negation
controller.i.filter.name.change(null);                     // ... and not any more
```

| Field | Meaning |
| --- | --- |
| `caption` | The property's caption in the design, `null` where the design gives none |
| `type` | The type of a condition's value: `string`, `number`, `date`, `boolean` or `json` — the FILTER type, by which `value` is read and written |
| `active` | Whether the filter has a condition — one comparing with `NULL` too, whose `value` is `null` as with none (`false` on a `BOOLEAN` property) |
| `compare`, `negation`, `or` | The comparison of the filter's condition, spelled as `filters` spells it, whether the condition is negated, and whether it is joined to the next one by OR, as `filters` says it; while there is no condition, those of the condition it showed last — at first, the property's default comparison, `false` and `false` |
| `value` | The value of the filter's condition, `null` while there is none |

The filter's condition is the first condition on its property among the group's user filters — as the platform's row of
the filter takes the first condition on its column —, whoever states it: the filter, `filters.change`, the platform's
filter panel, a `FILTER` action. `change(value, options)` states its value as `filters.change` states one condition: in
that condition's place, or, where there is none, in the filter's own. The options, `{compare, negation, or}`, any of
them, state the condition's comparison — spelled as `filters` spells it —, its negation and its junction, as the
platform's row does with the filter controls shown; what they leave out stays as the filter has it — as its condition
has it, or, with none, as it compared last (above). An option `change` does not know, a comparison that is no comparison
of a condition — an unknown one, `IN ARRAY` — and a negation or `or` that is not `true` or `false` are refused, with no
value too, which then takes nothing off. No value — absent, `null`, `''`, or one its type reads as `NULL`: `false` of a
`BOOLEAN` property — takes that condition off, whatever the options say, and that one alone: a range's other bound
stays, and is the filter's condition then, as when the platform's row is emptied. The filter itself is never removed. It
goes with a [batch](Form_and_navigator_controllers.md#batch) as `filters.change` does. The condition of a filter on a
file property can only be taken off: its `value` is a link to the file, not the file a condition compares with.

A property filter with no condition is not in `filters`, but it keeps its place among the group's conditions, and a
condition it is given is put in that place: at form opening these places come first, in declaration order. `or` joins a
condition to the next one in `filters`, skipping such places. Replacing the whole list — with `filters.change([...])` or
by a `FILTER` action — moves them after its conditions.

A property filter does not depend on who draws the group's conditions: the view that draws it, which then gets
`filters` too, or another view, or the platform's filter panel — its condition is one of the group's list either way,
sent as any change is: at once, or, in the [manual apply mode](#filter-controls), on the apply, the filter showing the
change meanwhile. Over the platform's panel the filter is the panel's own row of it, which the view draws: the panel
shows no row of its own for it. It is a row of the panel as any other is: F3 on a column whose first row it is puts
the cell's value into it, its comparison, negation and junction kept — React drawing the row, no editor opens —, and
so, while it shows no condition, do Alt+F3 and the add of the filter controls on that column, typing there starting
nothing; while it shows one, the add and typing start a new row of the panel's. A filter of a property of a
[tree](React_trees_sortings_selection.md#trees)'s group is on its group's node, as a filter group of a tree's group
is. Its [entry](React_projection.md#display-options), `data['FILTER(name(i))']`, says whether the form shows the
filter — its `SHOWIF`, or a container's above it.

A property filter with `custom = NULL` is the platform's: its row of the group's user filters, as a classic form shows
it, which the view places with `<Platform name="FILTER(name(i))"/>` as any child the platform draws
([placing](React_platform_components.md#placing)). It is that row whoever draws the group's conditions — the platform's filter panel, or a view,
over whose conditions it is the one row the platform draws —, and so is a property filter that stands outside React:

```lsf
DESIGN items {
    FILTERS(i) { custom = 'ItemFilters'; } // the conditions: a view
    FILTER(name(i)) { custom = NULL; }     // ... and the platform's row of the name filter, which the view places
}
```

A value committed in the row is the filter's, sent as any change is; a condition the view states on the property shows
in it, and F3 on its column puts the cell's value into it — on a property `filterProperties` lists, or on a file one: F3
over a view's conditions starts no condition on another ([Custom filter panel](#custom-filter-panel)). Where the
toolbar turns the platform's filters off (`showFilters = FALSE`), nobody draws such a filter, as in a form with no React
view. A filter with a React component of its own — `FILTER(name(i)) { custom = 'NameSearch'; }` — draws its part in its
own view ([A component's own view](React_component_views.md#component-views)).

Some combinations are refused when the form is built:

- a filter of a group in `PANEL` view, which has no user filters: one a view draws, and one given `custom = NULL` in a
  view;
- a filter a view draws on a property its condition cannot name or give a value to: one with no integration name
  (`NOEXTID`), one grouped in columns, an action;
- a filter the platform draws over the conditions a view draws, on an action, which is never filtered by.

### Custom filter panel {#custom-filter-panel}

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
  and the toolbar's filter button is there only for the rows of the property filters the platform draws, whose selectors
  it shows and hides;
- F3 in the grid gives the value of its [current cell](#filter-controls) to that cell's property, as a value is given a
  [property filter](#property-filters): in the place of the first condition on the property, its comparison, negation
  and `or` kept, or, where there is none, in the place of the property's filter, compared as it compared last, else at
  the end, compared the property's default way. It is stated at once, as F3 and Enter state a value on the platform's
  panel — there is no editor here to type a value in, the component edits it —, and sent as any change is: at once, or,
  in the [manual apply mode](#filter-controls), on the apply. Alt+F3 makes a new condition the only one. None is started
  where no row is current — an empty grid, or a pivot, a map, a calendar or a custom view, which have no cell —, or on a
  cell whose property `filterProperties` does not list — a file property's cell excepted: F3 states a condition on it,
  which a view states with no value alone —, or that shows another value than it is filtered by;
- Shift+F3 and Escape in the grid clear the list, at once, in the manual apply mode too;
- typing in a column starts no condition (a grid with a quick search searches, as it does anyway), and the apply button
  is the filter controls': the platform's in `FILTERCONTROLS(o)`, while they are shown, or the component's, where it
  draws them — in the [manual apply mode](#filter-controls) a change waits for it.

The property filters the design declares in the box are the component's: it draws them as [property filters](#property-filters).

These commands are there wherever a view draws the FILTERS box of a group in `LIST` view, whatever its
`TOOLBARSYSTEM(o)` says: `showFilters`, like the toolbar's other `show…` flags, is the platform's alone, and turns off
only what the platform draws of the filters — its panel, its controls in FILTERCONTROLS and the toolbar's filter button.
With `showFilters = FALSE` and the conditions drawn by a view, the [filter controls](#filter-controls) a view draws and
the keys of the grid work all the same; with the conditions the platform's, there are no commands, and the members of
the filter controls a view draws refuse. A tree's commands are the same, over the conditions of all its groups, its
[node's](React_trees_sortings_selection.md#trees) `filters`, and so is its toolbar's `showFilters`.

`props.controller.<g>.filters` changes them, so the panel both shows the current conditions and sets them: the
search box of [`ItemBoard`](#filters), without its list, is such a panel.

The conditions the component sets are the group's own: a `FILTER` action, a pivot's drill-down and this component all
change one list, so they cannot disagree.

`TOOLBARSYSTEM(o)`, `FILTERCONTROLS(o)`, `FILTERGROUP(f)` and a [property's filter](#property-filters), `FILTER(name(o))`, are not containers, but each takes a `custom` of its own, and a React component then draws it in its place ([A component's own view](React_component_views.md#component-views)).

Receiving a group's state does not make a component the only one drawing it. Which components are drawn is
decided by the design and by what is placed with `<Platform>`, not by what `data` contains — the state is the
group's, whoever draws it.

A component on a box beside `FILTERS(o)` — `TOOLBARLEFT(o)`, say — gets no part of the filters: the group's node
there, if it has one, has no `filters`.

### View filters {#view-filters}

A view that draws the rows of a group not in a tree can have the server filter them by conditions of its own — the
group's view filters, as a calendar filters its group by the period it shows. They are a list apart from the group's
[user filters](#filters), which the user changes and a `FILTER` action states, and the server applies them together
with those, with the form's own `FILTERS` and with its filter groups. `viewFilters` lists them beside `list` and
`keys`, where the rows are drawn: always there, and empty until the view states any. Each entry is a condition in the
shape [`filters`](#filters) gives one — `property`, `compare`, `value`, `negation`, `or` —, on a property of the group
named the same way, wherever it is drawn.

`controller.<group>.viewFilters.change(conditions)` — a member in the same place, and only there — states the whole
list, an array, read as an array of `filters.change` is: what a condition leaves out of `compare` and `negation` is the
default, and one with no `value` compares with `NULL`. An empty array takes them all off. The conditions are all applied
together, so one with `or: true` is refused, and so is what `filters.change` refuses in a condition.

```jsx
export function MeetingList({data, controller}) {
    const meetings = data.e;
    const show = first => controller.e.viewFilters.change([       // the meetings of one month: two conditions on the date
        {property: 'date', compare: '>=', value: first},
        {property: 'date', compare: '<', value: new Date(first.getFullYear(), first.getMonth() + 1, 1)}
    ]);
    const stated = meetings.viewFilters[0];                       // what the view stated is read back out of the projection
    return (
        <div>
            <button onClick={() => show(new Date(2031, 0, 1))}>January</button>
            <button onClick={() => show(new Date(2031, 1, 1))}>February</button>
            <button onClick={() => controller.e.viewFilters.change([])}>All</button>
            <span>{stated ? 'from ' + stated.value.toLocaleDateString() : 'all meetings'}</span>
            <ul>{meetings.list.map(row => <li key={row.key}>{row.name.value}</li>)}</ul>
        </div>
    );
}
```

A change through the controller is sent at once, as the group's whole list, and `viewFilters` shows it straight away,
before the rows filtered by it come back. The server does not report view filters, so `viewFilters` is what a call
stated last: it is rebuilt only by one, keeping its identity across renders otherwise, and after a request that failed
it still lists what that request stated — state the list again where it matters. No page size goes with them: the
group's stays as the form gives it (`PAGESIZE`).

The view filters are the view's that draws the rows. Another container states them through the form's controller,
`controller.form.changeViewFilters('<group>', conditions)`
([across containers](Form_and_navigator_controllers.md#across-containers)); for a group whose rows the platform draws
the call is refused — the view types the platform's table shows such rows in state their own, a calendar the period it
shows —, as it is for a group of a tree.

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

Every member, and what its button shows, is there whatever the design's flags say — `showFilters`, `showViews`,
`showCalculateSum`, … are the platform's alone, which draw its own toolbar; a view draws what it wants —, and so is the
name each takes on the node: where a view draws the toolbar, no property of the group takes `getSum`, whatever
`showCalculateSum` says. `visible` is not one of these flags: a toolbar the design hides (`visible = FALSE`) is no part
of any view — nothing of it on the node, no members, no names taken.

| on the node | member | what it does | the platform's button |
| --- | --- | --- | --- |
| `manual` | `manual.change(on)` | the manual update turned on or off | the manual update |
| `updatePending` | `update()` | the changes the server holds back while the update is manual, brought | the update |
| `filterControlsVisible` | `filterControlsVisible.change(shown)` | whether the platform's filter controls are shown — the buttons of its FILTERCONTROLS and the condition editors of its FILTERS panel and of the property filters' rows it draws; shown on a panel with no conditions, with one to fill in | the filter button |
| `listView`, `listViews` | `listView.change(name)` | the rows of the platform's table shown in another view type, one of `listViews`: `grid`, `pivot`, and `custom`, `map` or `calendar` where the group has one | the view type switch |
| | `getCount()` | the number of the group's rows — of the marked ones only, where the user has marked any, as the platform counts them —, as a `Promise` of the server's answer, a request as `getValues` is | the count |
| | `getSum(property)` | the sum of a column — over the marked rows only, where the user has marked any, as the platform sums it —, as a `Promise`: a number property of the group, named as a [sorting](React_trees_sortings_selection.md#orders) names one, not one grouped in columns | the sum, of the current column — a view has none, so the call names it |
| | `export()` | the group's rows exported to Excel, the server's report of them | the export — which on a pivot exports the pivot |

What the buttons act on is the platform's: `filterControlsVisible` shows and hides the PLATFORM's filter controls — its
FILTERCONTROLS buttons, its panel's editors and the selectors of the property filters' rows it draws —, and where the
platform has none for the group — it draws neither the `FILTERS(o)` panel nor `FILTERCONTROLS(o)` nor a property
filter's row, the view drawing them, or the design removing them or a container holding them, or the toolbar turns the
platform's filters off (`showFilters = FALSE`) — it stays `false` and its change is refused, saying why. Controls a view
draws itself are its own to show, its [filter controls](#filter-controls). The view type switch switches the platform's
table: where a view draws the rows, `listViews` is empty, `listView` is `null`, and a change is refused. A group whose
rows a view draws has the default `GRID` list view type. The grid of a group the form gives another
(`OBJECTS o = Order PIVOT`, and `MAP`, `CALENDAR`, `CUSTOM`) is drawn by the platform in that view type, and a React
container around it places the grid with `<Platform name="GRID(o)"/>`, as it places a grid given `custom = NULL`. A
`custom` of the grid itself (`GRID(o) { custom = 'OrderCards'; }`) is the group's `CUSTOM`, and puts the group in the
`CUSTOM` view type ([A React component in `CUSTOM`](React_component_views.md#form-custom)).

The toolbar's `export()`, `update()`, `manual.change()` and `listView.change()` return the `Promise` of their request.
Inside a [batch](Form_and_navigator_controllers.md#batch) the members that ask the server return a rejected `Promise` with the reason. The settings of
the platform's table are not there — a dialog of the platform's, over the table it draws. A toolbar placed back with
`custom = NULL` keeps them.

### A tree's toolbar and filter controls {#tree-toolbar}

A [tree](React_trees_sortings_selection.md#trees) has one toolbar and one set of filter controls for all its groups, `TOOLBARSYSTEM(TREE t)` and
`FILTERCONTROLS(TREE t)`. Where a view draws them, they are on [the tree's node](React_trees_sortings_selection.md#trees), `data.t`, whoever draws the
tree's rows:

```lsf
DESIGN catalogue {
    NEW header FIRST {
        custom = 'CatalogueHeader';
        MOVE TOOLBARSYSTEM(TREE t); // the tree stays the platform's; its toolbar is the view's
    }
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
already — its current node through the tree's own [`expandCurrent()`](React_trees_sortings_selection.md#trees), on the same node where the view draws
both, the tree from a group down through that group's `expandAll()` — all of it through the top group's —, one member
for each. So which members open a tree depends on who draws its rows, and a view that draws the tree's toolbar in
another container than the tree's rows has none there: the view that draws the rows has them. They only open — the
platform's tree closes nothing from its toolbar. Inside a [batch](Form_and_navigator_controllers.md#batch)
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

A [tree](React_trees_sortings_selection.md#trees) a view draws (`MOVE GRID(TREE t)`) is given the same, one for all its groups, as the platform's own
tree has it — `TOOLBARSYSTEM(TREE t)`, and what `FILTERS(TREE t)` and `FILTERCONTROLS(TREE t)` hold. The toolbar has the
tree's buttons: over the view's rows the one that expands the current object opens what the tree's
[`expandCurrent()`](React_trees_sortings_selection.md#trees) opens, the one that expands the tree opens, as over the platform's tree, every node of the
current node's group under the nodes open above it and of the groups below — nothing before a node is current; all of
the tree is the top group's `expandAll()` —, and the export exports the tree. The panel's conditions can be on the
columns of all the tree's groups, each condition of its column's group, and a new one starts on [the cell a condition is
added on](#filter-controls).

What the view draws itself the platform does not build: a `custom` on `BOX(o)` draws the whole group.
