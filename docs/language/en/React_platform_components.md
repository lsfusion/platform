---
slug: "/React_platform_components"
title: 'Components the platform draws'
---

Inside a React custom container, a [component](../paradigm/Form_design.md) of the design given `custom = NULL` or a custom view of its own is drawn by the platform. The [React view](React_views.md) does not draw such a component but places it: with `<Platform>` or `usePlatform` it says where in its own markup the component's view goes. In the same way a view places, in every row it draws, the editor of a table property the platform draws, and a window drawn by React places the platform's views it shows.

### Platform rendering {#crossing-back-to-lsfusion}

By default the component draws the container's whole subtree from `props.data`. A child with its own view, selected by [`custom`](DESIGN_statement.md#component-properties) or [`CUSTOM` / `NOCUSTOM`](Properties_and_actions_block.md#options), is placed with `<Platform name/>`.

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

#### Child entry {#child-entry}

A child the platform draws is not projected into `props.data` — the platform builds its view, passes it the property values, and renders it, exactly as in a standard container. The component only decides where it goes. Its caption and image - and a property's comment - are the exception: they go to the component in `data` instead of the child's own view.

What such a child projects depends on its kind:

- Such a property projects only `{ caption, image, comment, hidden }` — it has no `.value`, since the platform draws the value with the rest of the child's presentation. It projects that entry even with neither a caption, an image nor a comment — an empty one while the form shows the child.
- Such an action projects only `{ hidden }`: the platform displays its caption and image on its button.
- Such a container is projected the same way as any container the view draws: the component reads its `caption` and `image` from `data` and decides where the platform's view goes.

The entry holds `caption`, `image` - a property's `comment` too - and, while the form hides the child, `hidden` (an action's holds only `hidden`) — a table property's is its whole column entry — and it sits at the same place in `data` the [display options](React_projection.md#display-options) use, under the same name the child's value is keyed by:

| Child the platform draws | Where its entry is | Key |
| --- | --- | --- |
| A **table** property of an object group, [drawn per row](#live-editor) | `data.<g>.<integrationSID>`, an ordinary column entry alongside the group's other columns | The property's integration SID, `qty` |
| A **panel** property, or a property of the empty group | `data.<g>.<integrationSID>`, on its group's node (`data.<integrationSID>` for the empty group), where its value would be if React drew it | The property's integration SID, `note` |
| A container, or any other component | `data.<componentSID>`, always at the top level | Its design component identifier, `BOX(o)`, `GRID(o)` |

#### Child name {#child-name}

A property is named by its integration SID, table or panel, its entry and its place alike. Its entry is on its group's node, where the entry of a property React draws would be, and a `<Platform>` names a panel one the way that entry is reached: `<Platform name="o.note"/>` for `data.o.note`, `<Platform name="note"/>` for the empty group's `data.note`; it hides the host while that entry has `hidden` set.

A table one's per-row renderer is named the same way, `<Platform name="o.quantity" row={row}/>` for its column entry `data.o.quantity`, and the row identifies which renderer to place: it has to be a row of that group — a tree's row of a group below passes, narrowed to the path down to the group, as a group's `change(row)` takes it —, and the column entry does not hide the host.

A container or any other component is named by its design identifier, `<Platform name="BOX(o)"/>` and `data['BOX(o)']`. So the name a component places a child by is the path it reads the child's entry by.

#### Component entries {#component-entries}

The following components get such an entry, `data['<component SID>'] = { caption, image, hidden }`, at the top level of this view's `data`:

- every component the view draws, other than a property it includes — a container, one declared with `NEW` and a generated one (`PANEL(o)`, `TOOLBAR(o)`, …) alike, a grid (`GRID(o)`) or a tree (`GRID(TREE t)`), a toolbar (`TOOLBARSYSTEM(o)`);
- every component the platform draws that it places, other than a property.

What is inside a container the platform draws gets none: the platform draws that container whole, and the component never looks into it. The React custom container itself has no entry in its own `data`: nothing there draws its caption and image — the platform draws them around the container, or, for one another React view places, that view does, from its entry there.

Being part of the projected `data`, a dynamic caption or image re-renders the component like any other data change.

#### Visibility {#visibility}

An entry's `hidden` follows `SHOWIF` — the component's own, or a container's above it — and not whether the view shows the component: a tab the view does not show keeps its caption, and has no `hidden`. While a placed component's entry has it, `<Platform>` and `usePlatform` hide the host that places it — the host gets `hidden`, and stays: the component is still placed, so the server continues to read it and it becomes visible again when its `SHOWIF` allows it.

A React custom container obeys its own `SHOWIF` as any other component does: the platform hides its box. Inside what React draws, `tabbed`, `collapsed`, `popup` and `activated` do nothing: React lays that content out, and there is no tab strip or collapse header to apply them to — the view has each container's entry instead, and shows what it wants. A scripted `ACTIVATE TAB`, `COLLAPSE` or `EXPAND` on a component there is an error, as on a child the platform draws: its visibility is the view's.

[Opening a form inside a container (`SHOW ... IN`)](SHOW_operator.md#windowtype).

### Child placement {#placing}

A child the platform draws need not be a direct child of the container: it may sit inside the containers the view draws itself, however deep — the [record](../paradigm/Form_design.md#record) of a table the view draws, what a design puts into `GRID(<group>)`, included —, and the React custom container above it places it by its name all the same — so a design groups such pieces the way the view draws them, a page or a section each.

Inside a container the platform draws nothing is placed separately: the platform draws that container whole, with whatever is inside it — unless a React custom container inside it places a component, as it places its own.

#### Host {#host}

Such a child's view is moved into a *host*: a DOM node React owns and never renders children into. React places the view relative to a node it owns, and it must keep owning it to go on rendering the surrounding tree. Which node that is, is the only difference between the two ways to place a child:

```jsx
// the platform creates the host — a <div> inside the section
<section className="board-panel"><Platform name="BOX(o)"/></section>

// the component's own element is the host — one node less
<section {...usePlatform('BOX(o)', { className: 'board-panel' })}/>
```

`<Platform>` is the shorter one. `usePlatform(name, { className, row })` is for an element the component renders anyway — a panel, a card, a grid cell — so the view goes straight into it: it returns the props that make that element a host, the ref that moves the view in, the host's class and attribute, and `hidden` while it is set in the child's entry — and, as `children`, the [React components in the placed child](React_component_views.md#embedded).

Spread them onto the element, and give its class to `usePlatform` rather than to the element: a `className` written after the spread replaces the class value that includes `lsf-platform`.

Everything else is the same for both. The host has the class `lsf-platform` and the attribute `data-lsf-sid` with the child's name, whoever created it: `<Platform>` and `usePlatform` render them as props of their own, merged with the class the component gives the host, so a class that changes between renders keeps them. Every host is styled so that the view fills it, whatever the child is, so the component sizes the host and the view fills it at that size:

```css
.board > .lsf-platform[data-lsf-sid="BOX(o)"] { height: 260px; }
```

When the [host changes size](#custom-placement), the placed child — including a per-row renderer — is notified.

Sizing is the component's job, because a placed child's `width`, `height`, `fill` and alignment attributes are **not** applied: those describe a position inside a standard container, and here the surrounding element is the component's own markup.

#### Caption and image {#caption-and-image}

A placed child's `caption` and `image` are not drawn by the child — except an action's, which the platform displays on its button: they are passed to the component in `data`, so a component that places children itself draws them where it wants them — nothing draws them otherwise:

```jsx
<section className="board-panel">
    <h3><Image value={props.data['BOX(o)'].image}/><Caption value={props.data['BOX(o)'].caption}/></h3>
    <Platform name="BOX(o)"/>
</section>
```

Drawn with [`<Caption>`](React_rendering.md#caption) and [`<Image>`](React_rendering.md#image), for the reason those exist: a caption is not always plain text, and an image is either an address or a ready element.

This is what makes a **tab strip** possible, and it is what the entry is for. A component that shows one placed child at a time still has to name the ones it is not showing, and their captions are the only thing it has to name them with.

Two things make that work:

- the entry does not depend on whether the child is placed — every child the container places has one from the start, and its `hidden` follows `SHOWIF`, not whether the view shows the child, so a closed tab has a label;
- a caption continues to be read while the child itself is hidden, so a computed caption on a closed tab keeps up to date.

Only a container and a property have a caption and an image, so a placed `GRID(o)` has an entry with no caption — it indicates only whether the form hides the grid.

#### Placement rules {#placement-rules}

These rules bound the placement:

- Each child the platform draws is placed by at most one host. A child no host places is not shown. The platform reports a duplicate host on the page and in the console, and the child remains in the first host.
- A component inside the container that the platform builds a view of without its being given [`custom = NULL`](DESIGN_statement.md#component-properties) — a [filter group](React_filters_and_toolbar.md#filter-groups) that filters by no object, moved in — is shown by nobody, and the console says so once, with the fix. A component whose [part](React_projection.md#parts) a view draws — a group's or a tree's filter box, filter controls or toolbar, or a group's filter group — is not such a component: the view gets that part.
- The host has to be in the document. A host that is still outside the document once the render that created it is over releases the child: the view is then available for placement again, a `<Platform>` with the same name that IS in the page gets it, and the console reports the name of the child removed from the host. A host a portal appends in an effect is in the page by then and is placed as usual.
- The node `<Platform>` renders holds the lsFusion view, so it must stay empty: give it a class or a style, never children.

The platform reports a placement error in the host itself, not only in the console. For each of the following cases, the platform renders a message into the host, in an element of the class `lsf-view-error`:

- a name that names nothing inside the container (nor in a container it draws itself);
- a component the view draws itself;
- a second host for the same child;
- a name given a `row` that names no grid property the platform draws per row of a group this view draws;
- a `row` of another group;
- a `row` that is not a row.

The host's own class stays as the component gave it.

#### Hidden components {#hidden-components}

The view tells the server which of the children the platform draws it places — the set of them, not the mounts and unmounts that changed it: it is worked out after every change of placement and once after the component first draws, and only what changed is sent.

A child with a host — whether or not its view is already available — is placed. A child no `<Platform>` places counts as [a hidden component](../paradigm/Form_design.md#hidden), with its caption, image and comment available to the view for display outside that child.

So a component that shows one child at a time renders only that child, rather than hiding the others with CSS: a CSS-hidden child is still placed as far as the server knows, and goes on being read. For the same reason the visibility of a placed child belongs to the component alone — its `collapsible`, `collapsed` and `activated` attributes are ignored, and a scripted `COLLAPSE` / `EXPAND` or `ACTIVATE TAB` on it is an error.

A container the view draws itself — a page of a tab strip the view lays out, a section it collapses — the server reads as a shown one for as long as the view does not report its visibility: the server does not track what the view shows. The view reports it through `useShown(name, shown)`.

A container reported as not shown and everything inside it follow the [rules for reading hidden components](../paradigm/Form_design.md#hidden), including a platform-drawn child placed there, with or without a host. Its caption and image remain available to the view outside it. The container's `SHOWIF` stays the form's, and its entry's `hidden` reflects only the result of `SHOWIF`.

Call `useShown` where it stays mounted while the container comes and goes — in the strip, not in the page —, one hook per container, under the container's design name: unmounted, the hook leaves the container shown, which is what the server assumes of a container no hook names. `shown` left out is `true`. A name that is no container the view draws itself is reported in the console.

```jsx
// a tab of the strip: mounted for as long as the strip is, whichever page is shown
function Tab(props) {
    useShown(props.name, props.active);
    return <button role="tab" aria-selected={props.active} onClick={props.onClick}>
        <Caption value={props.entry.caption}/>
    </button>;
}
```

### An lsFusion editor in every row {#live-editor}

A list property is placed in the view that draws its group's rows, wherever the property itself is in the design. A placed child is drawn once. A grid property given `NOCUSTOM` in the `FORM` is drawn once per **row**, so a component can put a real lsFusion editor into every row it renders, instead of showing the value and having to build editing itself:

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

`NOCUSTOM` says the platform draws the property, in its standard view: it is a component rather than a value. A `CUSTOM` render function says so too, the platform drawing the value by that function, and so does a `SELECT` given with `NOCUSTOM`, the platform drawing the value as a selection from a list: a `SELECT` alone says what is drawn, not who draws it ([A selection](React_projection.md#select)).

The design says the same with the property's `custom`, as with its `caption`: `custom = NULL` is `NOCUSTOM`, and `PROPERTY(quantity(o)) { custom = 'starRating'; }` is `PROPERTIES(o) quantity CUSTOM 'starRating'`.

Where it is placed is not said — and cannot be: a grid property has no place of its own in the design, and its renderers are placed in the rows, so the component drawing the rows is the one that places them. That is the component this `MOVE BOX(o)` gave the group to. A `MOVE` of a grid property is refused when the form is built, `NOCUSTOM` or not.

The component names the property as its column entry is keyed, with its group, and gives the row:

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
- The caption is not drawn in the row, and neither is the comment: the per-row editors draw none of their own. Like a placed child's caption, both are provided in the group's column node — `data.o.quantity.caption`, `data.o.quantity.comment` — so the component puts them where they belong, usually once, in a header. An action's column entry has no caption and no image: the platform displays them on the action's buttons.
- A renderer exists for every row the group is currently showing, whether the component renders that row or not. A row scrolled out of view keeps its editor. The editor is removed only when the row is no longer in that set.
- Its key (`CHANGEKEY`) is one for all the rows, as a grid column's is: pressed anywhere in the form, it opens the editor in the current row, while that row's editor is placed — the editor of a row the component does not render takes no key.

Where no React custom container draws the group's rows, `NOCUSTOM` is simply the standard view: the platform's grid draws the property, without a custom render or editing function. A grid property grouped in columns is refused, with the property named, where a React view draws its rows: a per-row editor cannot address a row-and-column cell.

### Container extension with placed children {#extending-a-container-with-placed-children}

Another module adds a child to the container from `DESIGN`:

```lsf
EXTEND FORM orders PROPERTIES() rating;
DESIGN orders {
    board { MOVE PROPERTY(rating) { custom = NULL; } }
}
```

The component does not place it automatically. Every child the platform draws is placed by a `<Platform>` that names it, so a child no `<Platform>` names is not shown, and adding one this way means editing the JSX too — the `DESIGN` extension declares the child, the component decides where it goes.

The layout is not extensible from `DESIGN` either: a React composition cannot be modified there. To change the layout, replace the whole component with `custom = 'OtherBoard'`. The children stay as declared.

### Placement in different views {#custom-placement}

`<Platform>` and `usePlatform` place the following views. Where the place is marked, the view fills it, and `data-lsf-sid` holds the placed name:

| Custom view | What `<Platform name>` names | Does the place wait for an absent view? | Place's marks | Where `hidden` comes from | Size changes reported to the placed view | `props.controller` |
| --- | --- | --- | --- | --- | --- | --- |
| [A form's container](#placing) / [component](React_component_views.md#component-views) | A container's child (design SID or property path), or a per-row renderer (property path and `row`) | Yes, while a known view is absent | `lsf-platform`, `data-lsf-sid` | The child's or an ancestor container's `SHOWIF`. Not used per row | Yes, per-row renderers too | Its projection's [members and `form`](React_view_controller.md) |
| [A navigator window](React_windows.md#navigator-window) | Canonical name of a `NOCUSTOM` element of this window | Yes, while this window does not draw the element | None | The element's `SHOWIF` | No | The [navigator controller](Form_and_navigator_controllers.md#navigator-controller) |
| [The forms window](React_windows.md#forms-window) | An open form's `name` | No, even a loading form already has a view | `lsf-platform`, `data-lsf-sid` | No entry `hidden` | Yes | The [navigator controller](Form_and_navigator_controllers.md#navigator-controller)'s methods plus `select`, `close`, `closeAll`, `setEditMode`, `toggleFullScreen` |
| [The log window](React_windows.md#log-window) | A logged message's `name` | No | None | No entry `hidden` | No | The [navigator controller](Form_and_navigator_controllers.md#navigator-controller)'s methods plus `togglePin` |
| [The window layout](React_windows.md#window-layout) | A window's canonical name | No | `lsf-platform`, `data-lsf-sid` | `data.byName[name].hidden`: `HIDE WINDOW`, or an empty navigator window without a `CUSTOM`, except `System.root` | Yes | The [navigator controller](Form_and_navigator_controllers.md#navigator-controller) |
