---
slug: "/React_views"
title: 'React views'
---

A *React view* is a [custom view](../paradigm/Custom_views.md) drawn by a React component in the web client instead of the platform, as part of the [form design](../paradigm/Form_design.md) or of the [navigator design](../paradigm/Navigator_design.md). The articles of this section describe what such a component receives and what it calls.

A [`DESIGN`](DESIGN_statement.md) container can be rendered by a React component instead of the standard layout. The component receives a projection of the form state and draws the container's whole subtree itself. A table, a toolbar, filter controls, a filter group and a property's filter can be drawn by a React component of their own the same way, instead of the platform's view of them ([A component's own view](React_component_views.md#component-views)). Outside a form, a React component can draw [a navigator window](React_windows.md#navigator-window), [the forms window](React_windows.md#forms-window), [the log window](React_windows.md#log-window) and [the window layout](React_windows.md#window-layout).

This is a web-client feature only. The desktop client deserializes the container and renders its regular (non-React) subtree, and draws a component with a React component of its own with the platform's view, so the design stays usable in both clients.

### Kinds of React views {#kinds}

React views are of the following kinds:

- a container of the form design: its `custom` in `DESIGN` names the component, and the component draws the container's whole subtree ([Component selection](#selecting-the-component));
- [a component's own view](React_component_views.md#component-views): the `custom` of a tree's table, a toolbar, filter controls, a filter group or a property's filter names the component, and the component draws, in their place, what they show of their object group or tree;
- [a React component in `CUSTOM`](React_component_views.md#form-custom): the `CUSTOM` option of a property or of an object group names the component, and the component draws one cell of the property or the rows of the group;
- [a window or the window layout](React_windows.md): the `CUSTOM` option of the `WINDOW` statement, or of the `NAVIGATOR` statement for the layout, names the component, and the component draws a navigator window, the forms window or the log window, or lays the windows out.

A React view on a form reads the [form projection](React_projection.md) and calls the members of its [controller](React_view_controller.md).

The rest is described in other articles:

- the components and hooks of the platform for drawing rows, images and captions, in [Rendering in React](React_rendering.md);
- placing what the platform draws, in [Components the platform draws](React_platform_components.md);
- focus, form loading, keys and mouse bindings, live data and a hidden view, in [Form behavior in React](React_form_behavior.md);
- the parts of an object group a view draws, in [Trees, sortings and selection in React](React_trees_sortings_selection.md) and [Filters and toolbar in React](React_filters_and_toolbar.md);
- the form controller and the navigator controller, which any view written in JavaScript is given, in [Form and navigator controllers](Form_and_navigator_controllers.md).

### Component selection {#selecting-the-component}

The container's [`custom` attribute](DESIGN_statement.md#containerprops) names the component:

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

Here the object `o` is rendered by the `OrderBoard` React component instead of the standard table. For a component that is not a container, see [A component's own view](React_component_views.md#component-views).

### Window size and base size {#base-size}

The [window size](../paradigm/Form_design.md#windowSize) and the container's [base size](../paradigm/Form_design.md#components) are set through the [`size`, `width` and `height` attributes](DESIGN_statement.md#component-properties). For example, `size = (900, 600)` gives the React container a base size; `size = (-1, -1)` on the form's main container makes the window dynamic in both directions, and `height = -1` makes only its height dynamic. The `fill` and `overflowVert` attributes control expansion and vertical overflow. A dynamic window suits a form with no tables and content of moderate height.

### The component {#the-component}

`OrderBoard` is a named export from a `.jsx` [module](Client_JavaScript_modules.md) under `src/main/web`. The examples of the React articles use JSX.

For a project [using files without a build](Client_JavaScript_modules.md#without-the-build), ship the same component as a `.jsx` file — it is transformed on the server when served. `import` and `export` are not available there, so the `export` keyword is dropped and the component works against the platform-provided `window.React` — or write it with `React.createElement` in a plain `.js`. Either file is placed under `src/main/resources/web/init` (auto-loaded) or under `src/main/resources/web` and registered with `onWebClientInit`.

The component is a plain function that receives `props.data` and `props.controller`:

```jsx
export function OrderBoard(props) {
    const orders = props.data.o.list;
    return <div className="order-board">{orders.length} orders</div>;
}
```

The component is first drawn with the first projection of its container, once the form's first data has arrived, and never before it: `props.data` has the shape described in [Form projection](React_projection.md) from the very first render, so nothing in it needs a guard against a projection that has not arrived yet.

A component that draws one row is given the row as `props.row` when [`List`](React_rendering.md#rendering-rows) renders it:

```jsx
function Row(props) {
    const r = props.row;
    return (
        <div className={r.current ? "order order-current" : "order"}>
            <span>{r.number.value}</span>
            <span>{r.sum.value}</span>
        </div>
    );
}
```

### Runtime globals {#globals}

The components and hooks of the platform are runtime globals in `window.lsfusion`, and so is `custom.register`, which registers a component for `CUSTOM`. Each is described in its section:

| Name | Section |
| --- | --- |
| `useData`, `useController` | [Projection reading](React_projection.md#reading) |
| `List` | [Row rendering](React_rendering.md#rendering-rows) |
| `BucketScope`, `useBucket`, `Buckets` | [Row bucketing](React_rendering.md#bucketing) |
| `useSeekOnScroll` | [Scroll tracking](React_rendering.md#seek-on-scroll) |
| `Image` | [Image rendering](React_rendering.md#image) |
| `Caption` | [Caption rendering](React_rendering.md#caption) |
| `Platform`, `usePlatform`, `useShown` | [Child placement](React_platform_components.md#placing) |
| `useVisible` | [A hidden view](React_form_behavior.md#hidden-view) |
| `custom.register` | [A React component in `CUSTOM`](React_component_views.md#form-custom) |

### A React component and an HTML template {#choosing-between-a-react-component-and-an-html-template}

For a component that only places children and reads nothing from `props.data`, compare [HTML template places](DESIGN_statement.md#template-places). When the platform draws every child of the container, `props.data` carries no group or property values, because such a child is not projected — only the children's `{ caption, image, hidden }` entries in `props.data`.

The React component names its children too: adding a child requires editing the JSX, not just `DESIGN`.