---
slug: "/React_form_behavior"
title: 'Form behavior in React'
---

A [React view](React_views.md) draws a part of a form in the [interactive view](../paradigm/Interactive_view.md), and the form's behavior covers that part as well: the focus, the indication that the form waits for the server, keys and mouse bindings, the form's `SCHEDULE` event and the hiding of a view with the containers around it.

### Interactivity {#interactivity}

To read and change form state from the component — selecting a row, changing a property, calling actions — use `props.controller`. Its methods are described in [React view controller](React_view_controller.md).

A component that draws a field of its own also tells the form when the user is editing in it, so that `ENTER` does not move the focus to the next component and `ESCAPE` does not close the window while they are typing — see [Declaring that the user is editing](Form_and_navigator_controllers.md#editing). A component that draws no field of its own has nothing to declare.

The focus in what a view draws is the view's own: the platform has no field there to move it to. So `defaultComponent` on a component the view draws does nothing, and a scripted [`ACTIVATE PROPERTY`](ACTIVATE_operator.md) of a property it draws — a panel one, or a column of the rows it draws — is an error. `ACTIVATE PROPERTY` of a property the platform draws and the view places with [`<Platform>`](React_platform_components.md#placing) is no error. A view that wants one of its own fields focused focuses it itself, with `autoFocus` or the element's `focus()`.

<a className="lsdoc-anchor" id="form-loading"/>

While the form waits for the server, the platform shows it on the form's Refresh button (`formRefresh`), and where React draws that button, the button's entry says so: `data.formRefresh.loading` is `true`. The design moves the button into the view's container — `MOVE PROPERTY(formRefresh())` —, and the view draws from the entry what it needs: the button, or only an indicator. What it shows are the form's asynchronous requests, whichever part of the form sent them — a property changed with a value, the current object, sortings and filters changed, a tree's node opened, a [batch](Form_and_navigator_controllers.md#batch), the controller's own `exec`, `eval`, `evalAction` and `change`, a [scheduled](#live-data) run —, from the moment the first of them has waited 20 milliseconds until the answer to the last of them comes. A request answered sooner shows nothing, so that the page does not flicker. A synchronous request does not set it — the run of an action through its member outside a batch is one, unless the platform runs the action asynchronously, as it runs one drawn with `NOWAIT`: it blocks input, and the platform shows its [busy indicator](../paradigm/Interactive_view.md#busy) instead. The entry also says `loading` while the button's own run waits, as an action's does. A Refresh button the platform draws — placed with [`<Platform>`](React_platform_components.md#placing) or anywhere else on the form — shows it itself, and a form with no Refresh button shows it nowhere. Only the view that draws the button is drawn again when it changes:

```lsf
DESIGN orders {
    board {
        MOVE PROPERTY(formRefresh());
    }
}
```

```jsx
const { useData } = window.lsfusion;

function Busy() {
    return useData(d => d.formRefresh.loading) ? <span className="spinner"/> : null;
}
```

### Keys and mouse bindings {#keys}

The keys and mouse bindings the design gives what a view draws work as in the standard form: the key and the mouse binding of a property or an action ([`CHANGEKEY`, `CHANGEMOUSE`](Property_options.md)), the keys of the form's own buttons — actions of the `System` module: `ESCAPE` closes the form, `ctrl ENTER` is OK — and the key of a filter of a [filter group](React_filters_and_toolbar.md#filter-groups). The platform binds them itself, and the view does nothing for them. A key works while the form shows its component — its entry is not `hidden` — and the view that draws it is shown, whatever element of the form the user presses it in, and it does what the platform's key does:

- an action runs, as a click on its button. An action in the rows a view draws runs on the current row; with no current row the key is left to the other bindings. An action with `CONFIRM` asks first, in the platform's dialog: the key is the platform's, while a call of the action's member asks nothing — the view asks itself where the entry says [`askConfirm`](React_projection.md#display-options);
- a property whose change asks no value — its `ON CHANGE` handler runs an action — runs that handler;
- a filter's key selects the filter, as the platform's widget does.

The key of an action or a property the form shows read only (`READONLY`, `READONLYIF`) is not taken while it is: it is left to the other bindings.

A mouse binding is taken as in the standard form, where its modes let it: by default on a click in a cell of a table the platform draws, not of a panel (the `panel` and `cell` modes). What a view draws is no cell of the platform's: a click there takes a mouse binding only where its `cell` mode allows one outside a cell.

The binding modes of a key work as anywhere: with `showing=all` the key works while the form shows its component, whether the view is on the page or not, and a key of a property of an object group with `group=only` asks for the focus in a view of that group the platform draws — what a view draws belongs to no group.

The platform does not bind:

- the key of a property or an action that asks a value in a cell right away — the default change of a data property, an action that starts by asking a value (`INPUT`), a field a barcode scanner types into: the platform opens the editor for it in a cell, and that field is the view's. A view that needs such a key handles it itself;
- the keys of the standard grid — `F3` and the other keys of its filters, the typing that starts a quick filter, `ENTER` down a column: over the rows a view draws there is no such grid, and the view uses the members of its [filter controls](React_filters_and_toolbar.md#filter-controls) and `filters.change`.

A view of the platform's that a view places with [`<Platform name/>`](React_platform_components.md#placing) keeps its keys, as it does anywhere: they work while its host is shown, and a host the view hides — `<Platform>` hides it while the entry of what it holds says `hidden` — takes them with it.

A key pressed in a field the view draws reaches the form like any other key. A field where the user types declares it ([`controller.form.startEditing`](Form_and_navigator_controllers.md#editing)), so that `ENTER`, `ESCAPE` and the keys bound with the default `editing` mode step aside; a key the view needs for itself it stops in its own handler (`stopPropagation`).

### Live data {#live-data}

Data that keeps changing while the form is open — quotes, a queue, a monitor — is refreshed by the form itself, not by a timer in the component. The [`SCHEDULE` event](../paradigm/Form_events.md) of the form runs an action every given number of seconds, and the platform delivers what changed through `props.data` like any other change:

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

`System.formRefresh[]` re-reads everything the form shows, so the response carries the whole form on every run. There is no refresh of a single object group: `System.forceUpdate[STRING]` only applies the pending update of a group in manual update mode (the `enableManualUpdate` design attribute) and does nothing for a group updated automatically. So a live board is kept as a form of its own, with only the properties it draws, and the period is no shorter than the data needs.

The scheduled run differs from a request the component would make itself. A timer of the component's own that calls `controller.refresh.exec()` polls the server the way a click on the form would — as a synchronous request that blocks input until it completes, unless the action is drawn with `NOWAIT` — and, as the third point shows, goes on polling while the form is hidden.

- **It does not block the user.** The web client sends it as an asynchronous request, without blocking input and without the [busy indicator](../paradigm/Interactive_view.md#busy) a synchronous request gets, so the user keeps working while the form is re-read.
- **It runs only while the form is on screen.** For a form in a background tab, a form the [forms window](React_windows.md#forms-window) component places nowhere, or a form under a modal dialog, the run is skipped. The timer keeps counting, and the next run comes once the form is shown again.
- **A hidden form stays mounted.** A background tab and an unplaced form are hidden, not closed, so the component is still mounted and a `setInterval` of its own keeps firing — and keeps calling the server — while the form's schedule is paused. That is why the refresh belongs to the form, not to the component. A timer the component does need (an animation, a countdown) is cleared in the effect's cleanup as usual, but that cleanup runs when the form closes, not when it is hidden: what stops it while the view is hidden is [`useVisible()`](#hidden-view).

### A hidden view {#hidden-view}

The platform hides a view without taking it down: in a tab of a `tabbed` container that is not selected, in a collapsed container, in a `popup` container that is not open, under a `SHOWIF` — of the view's container or of a container above it —, and in a form that is not on screen: in a background tab, one the [forms window](React_windows.md#forms-window) component does not place, or one in another window while System.forms is expanded to the full screen. The component stays mounted and keeps its state, and what it draws is not seen.

While the form hides the view's container, the server does not read what the view draws, as it does not read a hidden part of any form: the rows of a group stay as they were last sent, and a property whose value changes meanwhile reads as `null` — unless a part of the form that is shown reads the same. The first answer after the container is shown brings the current values. A form that is not on screen is read as usual.

What the component does on its own goes on all the while: a timer, a request, an animation. `useVisible()` says whether the view is on screen now — whether the element its root is mounted in is displayed: in the page, with no element above it under `display: none`, which is how the platform hides one — and draws the component again when that changes, so work of the component's own is done while it is `true`:

```jsx
const { useVisible } = window.lsfusion;

export function Clock() {
    const visible = useVisible();
    const [now, setNow] = React.useState(() => new Date());
    React.useEffect(() => {
        if (!visible)
            return undefined;
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, [visible]);
    return <span>{now.toLocaleTimeString()}</span>;
}
```

It does not say whether the view is scrolled into sight. What the component draws from its data needs no such check: a hidden view is drawn again with the current data once it is shown, as the platform's own views are. What it measures does: an element under a hidden container has no size, so a view that sizes what it draws by its own box — a chart, a map — measures it again once it is visible.

The tabs and the collapsible containers around a view are a part of the form, and the view reads and changes them as it does the rest of the form — through the properties and actions the design gives it: [`ACTIVE TAB`](ACTIVE_operator.md) says whether a tab is selected, [`ACTIVATE TAB`](ACTIVATE_operator.md) selects it, [`COLLAPSE CONTAINER`](COLLAPSE_operator.md) and [`EXPAND CONTAINER`](EXPAND_operator.md) collapse and expand a container.

The actions run on the server, so a tab is selected when the server answers, not when the view calls the action. Whether a container is collapsed is no property: the form's [`ON COLLAPSE` and `ON EXPAND`](Event_block.md) events run when the user collapses or expands it. A container the view draws itself has no tab and no collapse of the platform's — the view shows it or not —, so `ACTIVATE TAB`, `COLLAPSE CONTAINER` and `EXPAND CONTAINER` on it are an error. `SHOW ... IN` also requires a destination container the platform draws, with `custom = NULL` and placement through `<Platform>` inside a React view ([Placing a child the platform draws](React_platform_components.md#placing)).
