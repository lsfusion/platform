---
slug: "/React_windows"
title: 'Windows in React'
---

Outside a form, a React component can draw a [window](../paradigm/Navigator_design.md) instead of its standard drawing — a navigator window, the forms window, the log window — and can lay the windows out on the desktop instead of the standard layout. Like a [React view](React_views.md) of a form, such a component receives its data in `props.data` and a controller in `props.controller`, and places what the platform draws with `<Platform name/>`.

### A navigator window {#navigator-window}

A React component can also draw a navigator window — the menu itself — instead of the standard toolbar. The [`WINDOW`](WINDOW_statement.md) is given the component name, and the elements placed in that window become its data:

```lsf
WINDOW appMenu VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CUSTOM 'AppMenu';

NAVIGATOR {
    NEW FOLDER sales 'Sales' WINDOW appMenu PARENT {
        NEW orders;
        NEW invoices;
    }
}
```

A component can also be given to a window that already exists, the standard toolbar included — then the navigator is not restructured at all, and only its drawing changes:

```lsf
EXTEND WINDOW System.toolbar CUSTOM 'AppMenu';
```

The window is then not given the classes with which the standard toolbar lays its strip out: with the vertical navbar, `System.logo`, `System.root` and `System.system` do not align their items to the start, and `System.logo` does not size its icon to the height of the horizontal strip — the component's own markup keeps the layout it gives it.

`props.data` is the window's own elements, keyed by [canonical name](../paradigm/Naming.md#canonicalname) — canonical names contain dots, so they are map keys rather than fields:

| Field | Meaning |
| --- | --- |
| `root` | The canonical names of the elements drawn at the top level, in display order |
| `byName` | Each element by canonical name |

and each element carries:

| Field | Meaning |
| --- | --- |
| `name` | Its own canonical name, so a component that was handed the entry alone can still address the element |
| `caption`, `elementClass` | Its caption and CSS class, the current ones — a `HEADER` or `CLASS` expression is applied as it changes. The caption is drawn with [`<Caption value={e.caption}/>`](React_rendering.md#caption) |
| `image` | Its icon, drawn with `<Image value={e.image}/>` — see [Drawing an image](React_rendering.md#image) — or `null` |
| `folder` | Whether it is a folder rather than an action |
| `hidden` | Whether `SHOWIF` currently hides it. Unlike the standard toolbar, which simply omits such an element, the projection keeps it, so the component decides how to treat it |
| `selected` | Whether it is the selected element **of this window** |
| `children` | The canonical names of its children that this window draws |

An element declared [`NOCUSTOM`](NAVIGATOR_statement.md) is drawn by the platform, so what it draws is not projected: its entry carries `name`, `caption`, `image`, `hidden`, `children` and `platformDrawn: true`, and none of the rest. While its `hidden` is `true`, `<Platform>` hides the [place](#standard-button) of its button.

The window is given the elements it is responsible for: an element that crossed into another window, and a subtree gated out because its parent is not selected, are absent — the same rule that decides what the standard toolbar draws. What that rule keeps, the projection keeps, hidden or not. The component is drawn once the navigator has worked out the window's elements, and a window with none is drawn too, with `root` empty. `props.controller` is the navigator controller, whose [`activate`](Form_and_navigator_controllers.md#navigator-controller) does what clicking the element does: selects a folder, or runs an action.

So a window can receive elements the module never declared. A folder's `WINDOW` names the window of its children, and a child that lands in another window is drawn there while the folder is selected. In the stock application this is how Administration works: the folder itself sits in `System.system`, and its sections are declared with `WINDOW toolbar PARENT`, so while it is selected in the top bar they, and their children, are drawn by `System.toolbar` — after `EXTEND WINDOW System.toolbar CUSTOM`, by your component. A component that draws only the elements it knows about loses such sections. Everything the window was given is reached through `root` and `children`.

Only the desktop web client draws the component. The mobile web client and the desktop client render their standard menu for that window, so the navigator stays usable in all of them.

#### The standard button {#standard-button}

An element the component does not want to draw itself is placed with `<Platform name/>` — the tag that places a design child on a form, where `name` is the element's canonical name. The platform puts its own button inside, with its icon, caption, tooltip and click behavior, but not with what the standard toolbar's layout adds to it: its caption is not hidden until the mouse is over the window, as in an unpinned navigator, and it has no indent for its depth. Where the button goes, and how deep it looks, is up to the component. The [node itself](React_platform_components.md#custom-placement) is left as the component rendered it, without the marks a form's host is given.

The element must be declared `NOCUSTOM` in the [`NAVIGATOR`](NAVIGATOR_statement.md) — the crossing back is declared on the navigator side, the way `custom = NULL` declares it on a form:

```lsf
NAVIGATOR {
    Sale.sales NOCUSTOM;
}
```

A `NOCUSTOM` element no `<Platform>` names is not shown, as on a form. Five mistakes are reported in the page: a name no navigator element has, a name that is not `NOCUSTOM`, which the component is expected to draw from the projection instead, a name of an element another window draws, a name a second `<Platform>` places again, and a `<Platform>` given a `row` even though a button is placed whole. A name the window merely does not draw right now — an element `SHOWIF` hides, one gated out because its parent is not selected — is not a mistake and is not reported: the place waits, and is filled as soon as the window draws that element. Meanwhile the place of an element `SHOWIF` hides is hidden, as its entry's `hidden` says, and the place of one gated out, which has no entry, stays empty. Which window draws an element is not such a case: waiting for a button another window owns would wait for good.

#### An HTML template instead of a component {#navigator-template}

A window whose menu only needs its own markup is given an HTML template instead of a component name — the [same template](WINDOW_statement.md) a `custom` container takes, where a `<Platform:name>` place takes the element's standard button. The name is resolved the way every other name in the module is, so nothing has to be spelled out in full, and the place is written open — closing it is an error:

```lsf
EXTEND WINDOW System.toolbar CUSTOM
    '<div class="menu"><div class="menu-head">Master data</div><Platform:items><Platform:partners></div>';
```

The element has to be declared before the template that gives it a place — a `NAVIGATOR` block that creates it goes above. A name that resolves to nothing is an error when the module is read, so a misspelled name stops the application instead of leaving the menu an item short.

An element the template gives no place is not drawn, so it names everything it shows, and `NOCUSTOM` is not needed on any of them — a template places nothing but the platform's own drawings.

The template may also be computed: given a property instead of a literal, it is recomputed as the property's value changes and the window is drawn again from the new markup, so a menu can rearrange itself without a component. Nothing resolves a computed template, so its places name their elements in full, by canonical name. A component is still what a menu needs when the markup depends on what the component itself reads.

### The forms window {#forms-window}

A React component can also draw the window the forms open in, instead of the standard tabs. The [`WINDOW`](WINDOW_statement.md) `System.forms` is given the component name, and the open forms become its data - and so is any other `FORMS` window the application declares, which gets the forms opened into it with `SHOW ... WINDOW windowName`:

```lsf
EXTEND WINDOW System.forms CUSTOM 'FormsBoard';
```

The component never draws a form: it renders a place for the form it wants shown, and the platform moves that form's own view into it. `<Platform name/>` is that place, and `name` is the name the projection gives the form. It places the form's view the same way it places a [design child](React_platform_components.md#placing) and a [navigator element](#standard-button). Like a design child's [host](React_platform_components.md#custom-placement), the place carries the marks — the class `lsf-platform` and `data-lsf-sid` with the form's name — and the form's view fills it. A form whose place changes size is told so.

Three mistakes are reported in the page: a name no open form has, a second `<Platform>` for the same form, and a `<Platform>` given a `row` even though a form is placed whole.

`props.data` is the open forms and the state of the window itself. The component is drawn as soon as the window is, before any form opens — `open` is then empty — so what it shows while nothing is open is its own to draw:

| Field | Meaning |
| --- | --- |
| `open` | The names of the open forms, in the order the standard strip shows them in |
| `byName` | Each form by name |
| `editMode` | The name of the chosen edit mode — the one the platform's mode button chooses, and the one kept between sessions. A mode switched on by holding Ctrl, Shift or Alt lasts while the key is held and does not change `editMode` |
| `editModes` | Every edit mode, in the order the mode button offers them |
| `fullScreen` | Whether System.forms is expanded to the full screen, the page drawing it alone; the component of another forms window always gets `false` |

and each form carries:

| Field | Meaning |
| --- | --- |
| `name` | Its own name, so a component that was handed the entry alone can still address the form |
| `canonicalName` | The [canonical name](../paradigm/Naming.md#canonicalname) of the form, for a component that draws a particular form its own way |
| `caption`, `image` | Its caption and icon, the current ones — the caption a tab shows. Drawn with [`<Caption value={e.caption}/>`](React_rendering.md#caption) and [`<Image value={e.image}/>`](React_rendering.md#image) |
| `selected` | Whether it is this window's current form — the one its standard strip would show. The keyboard works in it when this window has the keyboard. With several `FORMS` windows, only one can have it at a time |
| `loading` | Whether the form has not arrived from the server yet. Until it does, `caption` is the one the request that opened it carried and `image` is empty; the rest of the entry is there as always |
| `blocked` | Whether a form opened from this one is on top of it. Such a form cannot be closed, which is why the standard tab disables its close button |

and each edit mode in `editModes` carries what the mode button shows it with:

| Field | Meaning |
| --- | --- |
| `name` | Its own name — the one `setEditMode` takes, and the one `editMode` is compared with |
| `caption`, `image` | The mode's caption and icon. Drawn with [`<Caption value={m.caption}/>`](React_rendering.md#caption) and [`<Image value={m.image}/>`](React_rendering.md#image) |

`props.controller` has the [navigator controller](Form_and_navigator_controllers.md#navigator-controller) methods — `exec`, `eval`, `evalAction`, `change`, `activate` — for calls in the navigator's session. It also does what only the platform can do to an open form: `select(name)` makes it the current one, the way clicking its tab does, and `close(name)` asks it to close, the way its close button does. Closing is a request — a form with unsaved changes asks the user first and may stay open — so a form leaves `open` when it is actually closed, not when `close` is called.

`select`, `close`, `closeAll`, `setEditMode` and `toggleFullScreen` return nothing, so `await controller.close(name)` does not wait for the form to close. Read changes from `props.data`: `select`, `setEditMode` and `toggleFullScreen` take effect at once.

`select` and `close` refuse a name no open form has, `close` refuses a blocked form, and `setEditMode` refuses a mode not in `editModes`. The call throws an `Error` with the reason in its `message`.

A form the component places nowhere stays **open and hidden**, which is what a background tab already is: the standard strip only hides the forms it does not show. So a component that places only the current form gives an application without tabs, where every form opened so far is still there, and placing one again shows it as it was left. A form is closed only through `close(name)` or `closeAll()`, or the way it is closed without a component view.

In a forms window drawn by a React component, form shortcuts act on the current form even if the component places it nowhere. The component should therefore place the current form.

The platform's own toolbar — edit mode, full screen, the mobile menu — is System.forms', in its standard tab strip, so a component that draws System.forms does not show it, and no other window has it. What the toolbar does, a component of any forms window does through the controller: `setEditMode(name)` chooses the edit mode, one for the whole page, the way the mode button does, where `name` is the name of a mode in `editModes`, and `toggleFullScreen()` expands System.forms to the full screen and brings it back, the way the full screen button and ALT+F11 do. The full screen is System.forms' whichever window's component asks for it: the page draws System.forms alone then, without the navigator and the other windows, so the window of another component is not drawn, and that component never sees `fullScreen` as `true`. What the standard tab's context menu offers is here too: `closeAll()` asks every open form to close, starting from the last one in that order. Closing each of them is a request as well, so a form that did not close stays open and the rest are closed. The chosen mode and the full screen are state the platform holds itself, which is why it projects them: a mode chosen by the component and a full screen entered with ALT+F11 are both seen the same way, in `props.data`. The mobile menu is not something a component needs: on the mobile web client the platform draws the forms window.

Hiding the close button of an inactive tab is how the standard strip follows the user's setting *Do not show close button on inactive tab* (`dontShowCloseButtonOnInactiveTab`), so `System.forms` drawn by a component is not given the class that hides it, `dont-show-close-button`: the class would hide the close buttons the component draws with the standard tabs' class names too. Whether its tabs show a close button is the component's to decide. The mobile web client, which draws the standard strip, keeps the class.

A forms window clips what is drawn in it. Nor is a high `z-index` enough: the window's own element is a stacking context (`position: relative; z-index: 0`), so a layer inside it can never rise above the neighbouring windows. A portal into the page body leaves both behind.

Only the desktop web client draws the component. The mobile web client and the desktop client keep their standard forms window.

### The log window {#log-window}

A React component can also draw the window the messages to the user appear in, instead of the standard panel. The [`WINDOW`](WINDOW_statement.md) `System.log` is given the component name, and the logged messages become its data:

```lsf
EXTEND WINDOW System.log CUSTOM 'MessageLog';
```

The component never draws a message: a message is markup the platform built — the text with its icon, or the table an action reported beside it — so the component renders a place for each message it wants shown, and the platform moves that markup in. `<Platform name/>` is that place, and `name` is the name the projection gives the message. It places the message's markup the same way it places a [design child](React_platform_components.md#placing), a [navigator element](#standard-button) and an [open form](#forms-window). Like a navigator button's, the [place](React_platform_components.md#custom-placement) is left as the component rendered it, without the marks a form's host gets.

Three mistakes are reported in the page: a name no logged message has, a second `<Platform>` for the same message, and a `<Platform>` given a `row` even though a message is placed whole.

`props.data` is the logged messages, and the component is drawn before the first one arrives, with `messages` empty:

| Field | Meaning |
| --- | --- |
| `messages` | The names of the logged messages, newest first — the order the standard panel shows them in |
| `byName` | Each message by name |

and each message carries:

| Field | Meaning |
| --- | --- |
| `name` | Its own name, so a component that was handed the entry alone can still place the message |
| `caption` | The caption of the action that logged the message, drawn with [`<Caption value={m.caption}/>`](React_rendering.md#caption) |
| `time` | When the message arrived, in milliseconds — what the standard panel prints on its date line |
| `failed` | Whether the message is an error — the panel's only distinction between two messages, and the one a component styles from |

A message is markup, so what the projection carries is only what the standard panel writes itself beside that markup. The name is the platform's own: two messages can carry the same text, caption and second, so nothing about a message identifies it.

`props.controller` has the [navigator controller](Form_and_navigator_controllers.md#navigator-controller) methods — `exec`, `eval`, `evalAction`, `change`, `activate` — for calls in the navigator's session. It also does the one thing only the platform can do to the log: `togglePin()` toggles the window's pin mode, the way the platform's pin button does. `togglePin` returns nothing and reports no completion, just as a click does not. Pinning is a setting of the display environment, shared with the desktop client, so the component asks the platform to change it. It is not told the current mode either, since the client is not — an unpinned window is unpinned by a class the server computes into the window.

A message the component places nowhere is still logged and still in the projection: nothing drops one, exactly as the standard panel keeps every message it printed. So a component that shows only the last message, or only the failed ones, loses nothing — an earlier message placed again is shown as it was. There is no clearing and no dismissing, in the projection or in the controller, because the platform has none.

The window itself is still the platform's: the component draws inside it, so in the standard layout the window is hidden, popped out over the form and revealed when a message arrives exactly as before. When the windows are laid out by a [component of their own](#window-layout), the window is shown where that component places it.

Only the desktop web client draws the component. The desktop client keeps its standard message panel, and the mobile web client, whose layout has no message window at all, is left as it was.

### The window layout {#window-layout}

A React component can also lay the windows out on the desktop - which window is where, and how large -, instead of the standard layout, which places them by their [`POSITION`](WINDOW_statement.md). The component is given with the `CUSTOM` option of the [`NAVIGATOR`](NAVIGATOR_statement.md) statement:

```lsf
NAVIGATOR CUSTOM 'AppLayout';
```

The component never draws a window: it renders a place for each window it wants shown, and the platform moves that window's own view in, drawn as without the component - a navigator window's buttons, or what the window's own [component](#navigator-window) draws, the forms open in a `FORMS` window, the messages of `System.log`. `<Platform name/>` is that place, and `name` is the window's [canonical name](../paradigm/Naming.md#canonicalname). It places the window the same way it places a [design child](React_platform_components.md#placing) and an [open form](#forms-window), with the [same marks on the place](React_platform_components.md#custom-placement): the window's view fills the place, and the component sizes the place. A window whose place changes size is told so, as when a border of the standard layout is dragged, so a table, a pivot table's chart or a map in a form measures itself again.

Three mistakes are reported in the page: a name no window has, a second `<Platform>` for the same window, and a `<Platform>` given a `row` even though a window is placed whole.

`props.data` describes every window, `System.forms` included:

| Field | Meaning |
| --- | --- |
| `windows` | Array of the windows' canonical names: navigator windows, `System.log`, application `FORMS` windows, then `System.forms`. Navigator and application `FORMS` windows follow declaration order within each module and module initialization order between modules |
| `byName` | Entries under the windows' canonical names, such as `data.byName['System.toolbar']` |

Each entry in `byName` has:

| Field | Meaning |
| --- | --- |
| `name` | Its own canonical name, so a component that was handed the entry alone can still place the window |
| `caption` | Its caption - the one the standard layout writes on the tab of windows that share a place. A window declared without a caption, as the system windows are, has its name |
| `hidden` | Whether the standard layout would not show the window now: `HIDE WINDOW` hides it, or it is a navigator window with nothing to draw - the selected folder has no elements for it -, other than `System.root` and a window with a `CUSTOM`. While the entry says so, `<Platform>` hides the place. The component reads it to fold its own markup around the place |
| `vertical` | Whether the window draws its buttons in a column. The "Navbar: Vertical" design setting turns `System.logo`, `System.root` and `System.system` so, whoever lays them out |

`props.controller` has the [navigator controller](Form_and_navigator_controllers.md#navigator-controller) methods — `exec`, `eval`, `evalAction`, `change`, `activate` — for calls in the navigator's session. The windows are drawn and changed by the platform, and nothing about their arrangement is the platform's.

A window the component places nowhere is not shown, and lives on: its navigator buttons are refreshed, a form opened into it is open - hidden, as a background tab is -, its messages are logged. Placed later, it is shown as it is then. So a component that places only the windows it knows hides a window another module adds. A component meant to take any window places the rest by `data.windows`.

What the standard layout does with its own panels is not done under a component: the user dragging the border between windows and the sizes kept between sessions, the tabs of windows that share a place, the vertical navbar turning the top strip into a column, the unpinned navigator and log popping out over the form. The pin modes are the standard layout's way of laying the navigator and the log out, so a window the component lays out is not given the classes that collapse it and pop it out over the form or show the top strip's captions only on hover: pinning there changes no layout. The full screen (`ALT+F11`) is as without a component: `System.forms` is drawn alone over the page, and the component is hidden meanwhile - still mounted, its state kept. It is not told of the full screen: `fullScreen` is projected to the [component](#forms-window) of `System.forms` alone. [`Service.resetWindowsLayout`](../paradigm/System_Service.md) leaves the full screen and puts back the windows' declared visibility, and it does not reach sizes the component keeps itself.

Only the desktop web client draws the component. The mobile web client keeps its own page - `System.forms` and the navigation menu -, and the desktop client its own layout of the windows.
