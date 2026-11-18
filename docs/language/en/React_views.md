---
slug: "/React_views"
title: 'React views'
---

A *React view* is a [custom view](../paradigm/Custom_views.md) drawn by a React component in the web client instead of the platform, as part of a form's [interactive view](../paradigm/Interactive_view.md) or of the [navigator design](../paradigm/Navigator_design.md). The articles of this section describe what such a component receives and what it calls.

A [`DESIGN`](DESIGN_statement.md) container can be rendered by a React component instead of the standard layout. The component receives a projection of the form state and draws the container's whole subtree itself. A table, a toolbar, filter controls, a filter group and a property's filter can be drawn by a React component of their own the same way, instead of the platform's view of them ([A component's own view](React_component_views.md#component-views)). Outside a form, a React component can draw [a navigator window](React_windows.md#navigator-window), [the forms window](React_windows.md#forms-window), [the log window](React_windows.md#log-window) and [the window layout](React_windows.md#window-layout).

This is a web-client feature only. The desktop client deserializes the container and renders its regular (non-React) subtree, and draws a component with a React component of its own with the platform's view, so the design stays usable in both clients.

### Kinds of React views {#kinds}

React views are of the following kinds:

- a container of the form design: its `custom` in `DESIGN` names the component, and the component draws the container's whole subtree ([Component selection](#selecting-the-component));
- [a component's own view](React_component_views.md#component-views): the `custom` of a tree's table, a toolbar, filter controls, a filter group or a property's filter names the component, and the component draws, in their place, what they show of their object group or tree;
- [a React component in `CUSTOM`](React_component_views.md#form-custom): the `CUSTOM` option of a property or of an object group names the component, and the component draws one cell of the property or the rows of the group;
- [a window or the window layout](React_windows.md): the `CUSTOM` option of the `WINDOW` statement, or of the `NAVIGATOR` statement for the layout, names the component, and the component draws a navigator window, the forms window or the log window, or lays the windows out.

A React view on a form reads the [form projection](React_projection.md) and calls the members of its [controller](React_view_controller.md). The components and hooks of the platform for drawing rows, images and captions are described in [Rendering in React](React_rendering.md); placing what the platform draws, in [Components the platform draws](React_platform_components.md); focus, form loading, keys and mouse bindings, live data and a hidden view, in [Form behavior in React](React_form_behavior.md); the parts of an object group a view draws, in [Trees, sortings and selection in React](React_trees_sortings_selection.md) and [Filters and toolbar in React](React_filters_and_toolbar.md). The form controller and the navigator controller, which any view written in JavaScript is given, are described in [Form and navigator controllers](Form_and_navigator_controllers.md).

### Component selection {#selecting-the-component}

In `DESIGN`, set the container's `custom` attribute to the component name as a string literal matching `[A-Z][A-Za-z0-9_$]*` (a bare identifier starting with an uppercase letter):

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

The value form selects the renderer: a string literal matching `[A-Z][A-Za-z0-9_$]*` names a React component, while an empty string `''`, an HTML template string, or a property gives the [classic (non-React) custom container](DESIGN_statement.md#containerprops). Here the object `o` is rendered by the `OrderBoard` React component instead of the standard table. A component that is not a container takes a React component name alone — see [A component's own view](React_component_views.md#component-views).

### Window size and base size {#base-size}

A form opened as a window (`SHOW ... FLOAT`; for `DIALOG` the window is the default location) gets its size from its content at the moment of opening and keeps it: the component's first drawing, of the form's first data, is measured, and what it draws later - more rows, data that arrives afterwards - is not. So a container whose content grows after the opening is given a base size: `size = (900, 600)` or the separate `width` and `height` attributes. Without it, the window keeps the size of the first drawing, and the content drawn later pushes the OK / Close buttons past the window's edge. A tab (`WINDOW`) is sized by the forms window, so no base size is needed there.

Another way is to leave the React container without a base size and make the window itself dynamic: in the form's own `DESIGN`, set its main container to `size = (-1, -1)`, or only `height = -1` — then the width is measured and fixed at opening while the height stays dynamic. In each dynamic direction the window follows the component's content, including content drawn later. This suits a form with no tables (their height stops fitting the rows) and content of moderate height: the window is centered only at opening, and its height is not capped at the screen height — a window taller than the screen scrolls as a whole.

The base size also bounds the height of the container itself, wherever the form opens. Without it the container is as tall as what the component drew, and the containers above it grow with it, so a component that draws more than fits on the form stretches the whole form, and the form scrolls as a whole — in a tab, its main container. A base height (`height` or `size`) separates the container's size from the content's: the container gets that height, expands from it into the form's free space by its extension coefficient (`fill`), and content that does not fit scrolls inside it (`overflowVert` is `auto` by default). It stretches the form only when it does not fit there itself — like the base size of any component.

### The component {#the-component}

`OrderBoard` is a named export from a `.jsx` [module](Client_JavaScript_modules.md) under `src/main/web`. The examples of the React articles use JSX. For a project [using files without a build](Client_JavaScript_modules.md#without-the-build), ship the same component as a `.jsx` file — it is transformed on the server when served. `import` and `export` are not available there, so the `export` keyword is dropped and the component works against the platform-provided `window.React` — or write it with `React.createElement` in a plain `.js`. Either file is placed under `src/main/resources/web/init` (auto-loaded) or under `src/main/resources/web` and registered with `onWebClientInit`.

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

A component that places children the platform draws and reads nothing from `props.data` does what a classic custom container already does: an HTML template positions the same children through its `<Platform:name>` places, without a React runtime and without a host node per child. When the platform draws every child of the container, `props.data` carries no group or property values, because such a child is not projected — only the children's `{ caption, image, hidden }` entries in `props.data`.

A template is given no data at all. It places what the platform draws, by name, and everything else in it is written out: it cannot read the caption or the image of a child, or of the container itself. The captions it shows are drawn by the platform: a property child is drawn in parts (unless it is declared `inline = FALSE`) — its caption and its comment are places of their own, `<Platform:PROPERTY(qty).caption>` and `<Platform:PROPERTY(qty).comment>`, which the template can put inside markup of its own, a heading say —, any other child carries its caption inside its placed view, and the container's own caption and image are drawn by the platform beside the container rather than inside the template.

Both name their children, so neither is extended from `DESIGN` alone: adding a child means editing the JSX or the template string. A template string belongs to the module that declared it and can only be replaced whole.
