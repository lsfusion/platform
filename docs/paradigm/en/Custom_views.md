---
slug: "/Custom_views"
title: 'Custom views'
---

A *custom view* is a part of the [form design](Form_design.md) or of the [navigator design](Navigator_design.md) drawn by *client code* instead of the platform: the application's own JavaScript code running in the web client. The desktop client uses the platform's standard view in its place.

### Kinds of custom views {#kinds}

Client code can draw the following parts of the interface:

| Part of the interface | Client code |
| --- | --- |
| A container of the [form design](Form_design.md), with everything inside it | A React component |
| A tree's table, a toolbar, filter controls, a filter group or a property's filter | A React component |
| The [rows of an object group](Interactive_view.md#objects) — the custom list view alongside the table, pivot table, map and calendar | A React component or a JavaScript function |
| A [property's value](Interactive_view.md#property) in a table cell or on a panel | A React component or a JavaScript function |
| A value editor for changing a property or [inputting a value](Primitive_input_INPUT.md) — an extension of the standard input, a separate window or a replacement of the cell content | A JavaScript function |
| A navigator window, the forms window, the log window or the layout of windows on the desktop | A React component, in the desktop web client only |

A form container can also be laid out with an HTML template: the platform puts child components into the places provided for them. Such a template can also lay out the elements of a navigator window.

### The form's state and its changes {#state}

The platform keeps the form's objects, the groups' current objects, filters, sortings, property values and [change session](Change_sessions.md). Client code receives data for the part of the form it draws: the read sets of objects, current objects and property values. It can also use the property captions, colors and availability set by the design.

A React component also receives the options of a property shown as a selection element and can ask the server for a property's asynchronous value list. Changes made by the handler of a [scheduled form event](Form_events.md#global) reach the custom view like any other change.

Client code changes the form's state as a user does: it selects the current object, changes a property value or runs an action drawn on the form. A React component can also change a table's sortings and user filters, select a filter in a filter group, run toolbar actions and expand or collapse tree nodes. When it draws the rows of an object group outside a tree, it can read and change the same row and column selection that the [selection properties](Selection_SELECT.md) report. Changing a property runs its change event, and the passed value replaces the user's input in a [value request](Value_request_REQUEST.md). The source of such a change is [custom client code](Form_events.md#source) (`CUSTOM`). Changes go through the ordinary form edit checks and require no access to the external API.

### Calls to the server {#server-calls}

Besides editing the form, client code can call actions and change properties on the server by name, and execute lsFusion code. On a form these calls run in its session: a change is visible to following calls and is saved with the form's changes. In the navigator each call runs in a new session.

By default such calls are checked as [access from an external system](Access_from_an_external_system.md): actions and properties exposed to the external API are allowed, as are calls by a user who can run code in the [interpreter](Interpreter.md). A form can additionally allow its client code to call selected actions and properties. Executing arbitrary code remains subject to the API access check.

### Components the platform draws inside {#platform-components}

A React component drawing a container can leave some of its contents to the platform. The platform then draws those components, and the React component places them inside its view. A child component with a view of its own — standard or another custom view — is drawn by that view.

### A hidden custom view {#hidden}

A container with a custom view is hidden like any design component: for example, in an unselected tab or inside a collapsed container. The component itself can select tabs and collapse or expand the containers around it by running form actions. While the form hides the container, the server does not read what its view draws unless a shown part of the form needs the same data. A form that is entirely off screen is read as usual.

Components the platform draws that the React view does not place are not read unless a shown part of the form needs the same data. Their captions, images and comments continue to be read only when those labels are displayed outside the hidden contents. An action's labels and a list property's labels follow the visibility of the action or property itself. The server considers a container the React component draws itself shown until the component reports that it hides it.

### Keys and editing {#keys}

The platform handles the key and mouse bindings of properties, actions and filter groups a React component draws, according to the modes of those bindings. Bindings that need a platform cell for value input are the exception. The standard table's keys do not act over rows the component draws.

While the user types into a field of client code, the code tells the form that editing is in progress. The confirmation and cancellation keys, and bindings disabled during editing, then leave input in that field alone. This declaration itself does not start changing a property or save the entered value.

### Modules of client code {#modules}

Client code is kept as files in the project. They can be compiled during the project build or used without a build, and are loaded into the web client when a page opens.

### Language

The [JavaScript function](../language/Render_functions.md#render-function) named by `CUSTOM 'itemCards'` draws the rows of the item group.

An [HTML template](../language/HTML_templates.md#form-container-places) specifies where to place a form container's child components.

### React {#react}

- [Component selection](../language/React_views.md#selecting-the-component) explains how the design assigns a React component to draw a form container.
- [A component's own view](../language/React_component_views.md#component-views) shows how to use a separate React component to draw a tree's table, a toolbar, filter controls, a filter group or a property's filter.
- [A React component in `CUSTOM`](../language/React_component_views.md#form-custom) shows how a React component draws the rows of an object group in its custom list view or draws a property's value.
- [Form projection](../language/React_projection.md) describes the state a React component receives for the part it draws, including object groups with their rows and current objects, property values, and the options of properties shown as selection elements.
- [Value lookup](../language/Form_and_navigator_controllers.md#looking-up-values) shows how a React component asks the server for a property's asynchronous value list.
- [Scheduled refresh](../language/React_form_behavior.md#live-data) explains how changes made by the `SCHEDULE` handler reach a React component like any other change.
- [The React view controller](../language/React_view_controller.md) describes how a React component changes current objects and property values and runs form actions.
- [Changes to the current object and property values](../language/React_view_controller.md#changing-values) explains how a React component triggers property change events with the `CUSTOM` source and pushes a supplied value into a value request without prompting the user.
- [Drawing trees](../language/React_trees_sortings_selection.md#trees) covers how a React component draws an object tree and expands or collapses its nodes.
- [Drawing and changing sortings](../language/React_trees_sortings_selection.md#orders) covers the sortings of an object group drawn by a React component.
- [Working with filters and the toolbar](../language/React_filters_and_toolbar.md) covers how a React component draws and changes user filters and filter groups, and how it draws the toolbar and runs its actions.
- [Reading and changing the selection](../language/React_trees_sortings_selection.md#selection) covers how a React component drawing an object group's rows outside a tree reads and changes the selected rows and columns as the platform's table does.
- [Form and navigator controllers](../language/Form_and_navigator_controllers.md) give a React component server calls and, on a form, changes to what other containers draw and batching of changes.
- [Placing platform components](../language/React_platform_components.md) shows how a React component drawing a container places the children the platform draws.
- [A hidden view](../language/React_form_behavior.md#hidden-view) describes how an unselected tab or a collapsed container hides what a React component draws and how the component selects tabs and collapses or expands containers around it through form actions.
- [Keys and mouse bindings](../language/React_form_behavior.md#keys) are handled by the platform for the properties, actions and filter groups a React component draws, with the exception of bindings that need a platform cell.
- [Client JavaScript modules](../language/Client_JavaScript_modules.md) describe the storage, build, loading and name lookup of React components.

### Examples

The examples assume that class `Item` with property `name[Item]` and class `Order` with properties `number[Order]`, `date[Order]`, `sum[Order]` are declared. The JavaScript function `itemCards` and the React component `OrderBoard` are loaded into the web client.

A JavaScript function draws the rows of the item group instead of the standard table:

```lsf
FORM items
    OBJECTS i = Item CUSTOM 'itemCards'
    PROPERTIES(i) name
;
```

A React component draws the whole container of the order group:

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

The `OrderBoard` component draws a line with the number and the sum of each order read:

```jsx
export function OrderBoard(props) {
    return <ul className="order-board">
        {props.data.o.list.map(r => <li key={r.key}>{r.number.value}: {r.sum.value}</li>)}
    </ul>;
}
```
