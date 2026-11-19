---
slug: "/React_component_views"
title: 'Component views and CUSTOM'
---

Like a container ([React views](React_views.md)), a tree's table, a toolbar, filter controls, a filter group and a property's filter of the [form design](../paradigm/Form_design.md) can be drawn by a React component of their own instead of the platform's view of them. A React component can also be the render function that the `CUSTOM` option gives a property or an object group: it draws one cell of the property, or the rows of the group as one of its [view types](../paradigm/View_type_VIEWTYPE.md).

### A component's own view {#component-views}

A component's [`custom` attribute](DESIGN_statement.md#component-properties) names its React view:

```lsf
DESIGN items {
    GRID(TREE t) { custom = 'CategoryTree'; } // the toolbar and the filters over the rows stay the platform's
    TOOLBARSYSTEM(j) { custom = 'OrderToolbar'; } // over the platform's grid of j
}
```

The `custom` of the table of an object group, `GRID(i)`, is another thing: it is the group's `CUSTOM`, the render
function of one of the views the platform shows the group's rows in ([A React component in `CUSTOM`](#form-custom)).

The component gets what a container holding that component alone would get — `NEW bar { custom = 'OrderToolbar';
MOVE TOOLBARSYSTEM(j); }` —: the part of the group or of the tree on its node, under the same names ([the table of parts and
nodes](React_projection.md#parts)), for example:

- the toolbar's state and members on `data.j` and `controller.j`;
- a tree's rows on its groups' nodes and its own on `data.<tree>`;
- a filter group on `data.<group>.<filterGroup>`;
- a property's filter on `data.<group>.filter.<property>`.

Nothing but the nodes of the group, or of the tree and its groups, is on the top level.

The component's own design attributes are its view's, as they are of the platform's view of it: its size and its
place in its container, its class, and its `SHOWIF`, which hides the view — so its data has no [entry](React_projection.md#display-options)
of the component itself, which a container around it would have.

Inside what a React container draws, the component
has a view of its own, and the container places it with `<Platform name="TOOLBARSYSTEM(j)"/>`, its entry in the container's
data, and its parts in the data of its own view. A property the platform draws in the rows it draws is placed per row
with [`<Platform name row/>`](React_platform_components.md#live-editor); it places nothing else.

[Restrictions on `custom`](DESIGN_statement.md#component-properties).

The attributes of the platform's table — a table's `valueClass` given by a property, a tree's `hierarchicalCaption` —
mean nothing to its own view, as to a table a React container draws: the projection carries neither.

### A React component in `CUSTOM` {#form-custom}

The `CUSTOM` option of a property in the [`PROPERTIES`](Properties_and_actions_block.md) block and of an object group in the [`OBJECTS`](Object_blocks.md) block specifies a render function, and that function can be a React component. The property or the group remains the platform's: it stays where the form's design puts it, and the component draws only what the render function would draw — one cell of the property, or the rows of the group.

#### Registration of the component {#registration}

The web client tells a component from a classic render function of a [property](Render_functions.md#property) or of an [object group](Render_functions.md#object-group) by the way the name is registered: a component is registered with the kind `reactView`, and any other name is a classic function, whatever its case. The name itself does not say it here, as it does for the `custom` of a container: a classic function may be named as a component is.

A named export of a compiled module is registered without a kind, and a function of a `.jsx` file is not registered at all, so the module registers the component explicitly:

```jsx
function Quantity({data, controller}) {
    return <button disabled={data.readOnly || data.disabled}
                   onClick={() => controller.change((data.value ?? 0) + 1)}>
        {data.value}
    </button>;
}

window.lsfusion.custom.register('Quantity', Quantity, 'reactView');
```

```lsf
FORM items
    OBJECTS i = Item
    PROPERTIES(i) READONLY name
    PROPERTIES(i) quantity CUSTOM 'Quantity'
;
```

#### The component of a property {#property-component}

The component of a property draws one cell: a cell of the table, or the value of the property on a panel.

- `data` describes that cell — its `value`, `readOnly`, `disabled`, `loading`, `background`, `foreground`, `placeholder`, `pattern`, `regexp`, `regexpMessage`, `defaultValue`, `valueTooltip` and `options`, and the `type` and `askConfirm` of the property — the fields [the entry of a panel property a view draws](React_projection.md#display-options) has, with the same meaning, an option the platform computed nothing for being absent.
- `controller` is the property's [member](React_view_controller.md#members) for that cell — `change(value)`, `exec()` and `getValues(query, {mode, count})`, each returning the `Promise` of its request — with the form's controller as `controller.form`.

A call takes no row: in a table it changes the row of the cell, whichever row is current, and on a panel the current object of the form. The component changes the value by these calls only ([`CUSTOM` editing semantics](Properties_and_actions_block.md#options)). Whether the cell may be changed is checked by the server, as for a classic render function, so a change of a read-only cell is not made.

A table reuses the element of a cell for another row when its rows are sorted or scrolled. The component stays mounted then and keeps its state: it receives the `data` of the other row, and its `controller`, which stays the same object, changes the row the cell shows at the moment of the call — after an `await` too.

A view that draws the rows itself places such a property as any other property the platform draws, with [`<Platform name row/>`](React_platform_components.md#live-editor), and the component is drawn [in that view's tree](#embedded).

#### The component of a group {#group-component}

The component of a group draws the rows of the group where the platform's table would, as one of the view types the toolbar switches. For the table's `custom` attribute, see [`DESIGN`](DESIGN_statement.md#component-properties).

The component is given its one group, with no name above it:

- `data` is the group's node, as a container that draws the group's rows has it under the group's name ([Projection reading](React_projection.md#reading)) — its rows (`data.list`, `data.byKey`, `data.keys`), the entries of its properties, its [sortings](React_trees_sortings_selection.md#orders) and [view filters](React_filters_and_toolbar.md#view-filters), and the `OPTIONS` of the group as `data.options`;
- `controller` is the group's [member](React_view_controller.md#members) — `change(row)`, `orders.change(...)`, `viewFilters.change(...)` and a member per property —, with the form's controller as `controller.form` and `controller.batch`.

[`List`](React_rendering.md#rendering-rows) and [`BucketScope`](React_rendering.md#bucketing) are given no `group` there. A property the form gives a view of its own — `NOCUSTOM`, or a `CUSTOM` render function — is not drawn by the component but placed by it in each row, by its name alone: [`<Platform name="quantity" row={row}/>`](React_platform_components.md#live-editor); the component places nothing else.

The table takes the size of what the component draws, as any `CUSTOM` view does, and the toolbar switches it with the platform's other views of the rows. The toolbar and the user filters of the group remain the platform's, which draws them around the component, so `data` and `controller` carry nothing for them.

A property whose name is a [reserved name](React_projection.md#reserved-names) of a row or of a group's node where its rows are drawn, or `form` or `batch`, is not given, and neither is a property grouped in columns (`COLUMNS`): the form is not refused for it, as the server does not know that the render function is a component.

Inside a container a React view draws, the grid of such a group is drawn by the platform, and the view places it with `<Platform name="GRID(i)"/>`.

### A React component inside a React view {#embedded}

A child the platform draws can itself be drawn by a React component: a property or a group whose `CUSTOM` is a [component](#form-custom), a table that has such a property, a container or [another component](#component-views) with a `custom` of its own.

#### A component in the view's tree {#view-tree}

When a view of the form places such a child with [`<Platform>`](React_platform_components.md#placing) — whole, or its cell in a [row](React_platform_components.md#live-editor) —, the component is drawn in the React tree of that view, where the `<Platform>` stands, and not in a React root of its own.

- A context the view provides around the `<Platform>` reaches the component, however many components the placed child holds — a table has one in every row. `props.data` and `props.controller`, and so `useData` and `useController` inside the component, remain the component's own.
- An event in the component bubbles through the React handlers of the elements around the `<Platform>`. The handlers of the component, and then those handlers of the view, run before the platform handles the event in the cell — selecting the row, handling a key —, so `stopPropagation()` in any of them keeps the event from the platform, and in the component from the view's handlers too.
- A component that throws while it draws shows its message in its own place, and the view around it stays. A component that suspends shows the fallback of the nearest `Suspense` above it, which, unless the component has one of its own, is one of the view's, around the whole placed child.
- The component is mounted while its place is: when the view stops rendering the `<Platform>`, gives it another name, or a `Suspense` around it shows its fallback, the component is unmounted and loses its state, and it is mounted anew when the child is placed again. A child no `<Platform>` places is not drawn. What the platform keeps for a child that is not placed — a row's [editor](React_platform_components.md#live-editor) while its row is not rendered — is its own view, not the state of a component it draws there.

A child whose place is rendered but hidden is still placed: hiding its place does not stop the server reading it.

#### A component in a place with `ownRoot` {#own-root}

Where the place itself has to go and come back — a tab strip that renders only the active child so that the others are not read, a row of a list that is not rendered while it is scrolled away — and the components inside have to keep their state meanwhile, give the place `ownRoot`, at the price of the context and the `Suspense` boundaries the view has around the place:

```jsx
<Platform key={active} name={active} ownRoot/>
// or: usePlatform(active, { ownRoot: true })
```

Every component in what such a place holds is drawn in a React root of its own instead, and stays mounted, with its state, while no place holds the child, until the form is closed. No context and no `Suspense` boundary of the view around the `<Platform>` reaches it then; an event in it still reaches the view's handlers around the place, after the platform has handled it.

A whole child that is not placed is [not read](React_platform_components.md#placing): the component keeps the `data` it had until the child is placed again, and its effects and timers go on running with that `data`. A row's editor that is not placed goes on receiving its `data`, and is there until its row leaves the rows of the group.

Give a place `ownRoot` when the child is first placed, and do not change it: a root of its own, once made, is kept wherever the child is placed later, and a component made later — the cell of a new row — is drawn in a root of its own where its place has `ownRoot`, and in the tree of the view that places it where it has not.

A component in such a place that is itself a view draws what it places with its own `<Platform>` in its own tree.

#### A component outside the views of its own form {#outside-view}

Where no view of its own form places it — in a table or on a panel the platform lays out — the component is drawn in a React root of its own, one for each cell, and no context reaches it.

A form in the [forms window](React_windows.md#forms-window) and a window in the [window layout](React_windows.md#window-layout) keep their own roots too: an open form that no `<Platform>` shows stays open, and its components keep their state. So does a form shown inside another form, in a container or in a cell that a view of that form places — a component is drawn in the tree of a view of its own form only —, and what a form shows outside its place: the content of a popup container, a record of a table.
