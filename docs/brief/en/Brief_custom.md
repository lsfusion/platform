---
slug: "/Brief_custom"
title: 'Brief: custom views'
---

## Choosing a custom view {#choosing-a-custom-view}

A [custom view](../paradigm/Custom_views.md) hands drawing to the application's client code when the standard table, pivot table (`PIVOT`), calendar (`CALENDAR`) or map (`MAP`) does not fit: a kanban board, a card feed, a timetable, or a different layout or interaction. [React views](../language/React_views.md) suit new views, while classic render functions remain available for existing JavaScript views.

| Place | What the client code draws | When to use it |
| --- | --- | --- |
| A `DESIGN` container's `custom` | The container's whole subtree | A region of a form with its own layout and interaction |
| [A component's own `custom`](../language/React_component_views.md#component-views) | A tree's table, a toolbar, filter controls, a filter group or a property's filter, in that component's place | Replace just that part of the form |
| [A property's `CUSTOM`](../language/React_component_views.md#form-custom) | One value of one object, in a table cell or on a panel | A widget for one field |
| [An object group's `CUSTOM`](../language/React_component_views.md#form-custom) | The rows of one group, where its table stands | Another view of one list, with the platform's toolbar and filters around it |
| A `WINDOW`'s `CUSTOM` or `NAVIGATOR CUSTOM` | A window's content or the arrangement of the windows | A menu, a forms window, a message panel or the application's window layout |

These are web-client views. On forms the desktop client keeps the standard views and the container's regular subtree. A container's `custom` can also be an HTML template: it places platform-drawn children by name and receives no data. A React component is used when the drawing needs to read data or provide interaction of its own.

## Form projection {#form-projection}

A React component receives `props.data`, the [form projection](../language/React_projection.md): the properties, object-group parts and design entries that its view draws or places. Each React container has a projection of its own. Ordinary `DESIGN` containers can lay out several such regions on one form.

Data reaches a custom view through object groups, with server-side paging and filters: the group's rows in `props.data.<g>.list` for a React container, `props.data.list` for a React component in `CUSTOM` registered as `reactView`, and the `update` list for a classic `CUSTOM` object-group view. A JSON property carries one object's value, not a pageable object group.

A projected property carries its value and display attributes. The projection also carries the current object and, where the view draws them, sortings, filters, selection and toolbar state. What remains drawn by the platform is placed by the component rather than redrawn from property values.

## View and form controllers {#controllers}

A custom view receives data and a local controller for what it shows. A React component receives that controller as `props.controller`: its [members](../language/React_view_controller.md) mirror the projection and change property values, run form actions, select current objects, and change sortings, filters and tree nodes. The form remains responsible for selecting and ordering the rows and for calculations over them. Changes go into the form's change session, as edits in the standard table do.

The [form controller](../language/Form_and_navigator_controllers.md) is reached as `controller.form`, in both React and classic views and in editors. It changes what another container or the platform draws by name, groups edits in a `batch`, and provides server calls (`exec`, `change`, `eval`, `evalAction`). A form's `CUSTOMS` lists actions and properties callable from this form without exposing them through the external HTTP API. [Server calls](../language/Form_and_navigator_controllers.md#calling-the-server) specify the authorization and parameter formats.

Outside a form the [navigator controller](../language/Form_and_navigator_controllers.md#navigator-controller) activates navigator elements and provides server calls. A call there has a session of its own, rather than the session of an open form.

## Rendering and form behavior {#rendering-and-form-behavior}

The platform supplies components and hooks for a React view, so client code can draw its own interface while using the form's data and behavior:

| Article | Capabilities to reach for |
| --- | --- |
| [Rendering in React](../language/React_rendering.md) | `List` for rows, `Buckets` for placing rows into visual cells, `useSeekOnScroll` for paging with the scroll, `Image` and `Caption` for the platform's images and captions |
| [Components the platform draws](../language/React_platform_components.md) | `<Platform>` places a child or a row's property that keeps its own view and editor, `useShown` reports which regions are shown |
| [Form behavior in React](../language/React_form_behavior.md) | Focus and form loading, keyboard and mouse bindings, live data through the form's `SCHEDULE`, and hidden views |
| [Trees, sortings and selection in React](../language/React_trees_sortings_selection.md) | Tree nodes, server-side sortings, selected rows and columns, copying and pasting |
| [Filters and toolbar in React](../language/React_filters_and_toolbar.md) | User filters, property filters (`FILTER`), view filters, filter controls, `FILTERGROUP` and toolbar operations |

## Windows and window layout {#windows-and-window-layout}

[Windows in React](../language/React_windows.md) use `props.data` for the window's content and `props.controller` for its operations. `CUSTOM` on the [`WINDOW` statement](../language/WINDOW_statement.md) selects the window's renderer, and `EXTEND WINDOW ... CUSTOM` changes the renderer of an already declared window.

| View | What it receives and draws |
| --- | --- |
| Navigator window | Its navigator elements and the navigator controller, whose `activate` selects a folder or runs an element. A React component draws from the data, or an HTML template places the platform's buttons |
| `FORMS` window — `System.forms` or one declared by the application | Its open forms and operations for selecting and closing them. A React component places the forms with `<Platform>` and draws the controls around them |
| `System.log` | Its logged messages and the pin operation. A React component places the platform's message markup with `<Platform>` |
| `NAVIGATOR CUSTOM` | The windows and the navigator controller. A React component sizes and places the windows with `<Platform>`, replacing their standard arrangement by `POSITION` |

The forms and log windows accept only a React component: they receive what the window holds instead of navigator elements. Other `NATIVE` windows contain no navigator elements and accept neither a component nor a template. Custom windows and the window layout are drawn only by the desktop web client. The mobile web client and the desktop client keep their standard interface.

## Classic render and edit functions {#classic-functions}

`CUSTOM` can hand drawing to a classic JavaScript [render function](../language/Render_functions.md) or to a React component explicitly registered as `reactView`. An object group uses `CUSTOM renderFunction [OPTIONS optionsExpr]` in [object blocks](../language/Object_blocks.md), a property uses `CUSTOM renderFunction [CHANGE [editFunction]]` in the [properties and actions block](../language/Properties_and_actions_block.md).

A classic render function returns `render`, `update` and optional `clear`: the platform gives them an element, a local controller and, on update, the group's rows or the property's one value. The group's controller reads property values and provides row and property changes, incremental updates through `diff` and server-side view filters. A property's controller changes its value and looks up suggestions. Both reach server calls through `controller.form`.

An [edit function](../language/Edit_functions.md) replaces the editing of one value, independently of its display: it can extend the standard text input, draw an editor in the cell, or open a window editor. It is named after `CHANGE` in a property's `CUSTOM`, or in the `CUSTOM` block of `INPUT`. Its editor controller commits or cancels the edit. Committed values go through the property's ordinary change channel. Use it when the field needs an editor of its own while the rest of the view stays standard.

## Client JavaScript modules {#client-modules}

[Client JavaScript modules](../language/Client_JavaScript_modules.md) hold React components, classic render and edit functions, and functions called by `INTERNAL CLIENT` actions.

| Project | Where the code lives and how it is reached |
| --- | --- |
| With the build | `.js`, `.jsx`, `.ts` and `.tsx` under `src/main/web`; each file outside `lib` becomes an auto-loaded bundle, shared helpers in `lib` are imported. Named exports are auto-registered under the names the lsFusion code uses |
| Without the build | `.js` or `.jsx` under `src/main/resources/web`; `web/init` auto-loads files independent of load order, other files are listed in `onWebClientInit[STRING]`. JSX is transformed when served. Scripts share globals rather than import local modules |

The platform supplies React and ReactDOM. A React component for a container, a window or `NAVIGATOR CUSTOM` is selected by an identifier starting with an uppercase letter. A component used in a property's or object group's `CUSTOM` needs explicit registration as `reactView` even when it is a named export. Classic functions use the same `window.lsfusion.custom` registry, with names on `window` as a fallback for existing scripts. CSS modules provide component-local styles, and a bundle's CSS is loaded with it.
