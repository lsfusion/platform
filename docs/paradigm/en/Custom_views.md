---
slug: "/Custom_views"
title: 'Custom views'
---

A *custom view* is a part of a form's [interactive view](Interactive_view.md) or of the [navigator design](Navigator_design.md) drawn by *client code* instead of the platform: the application's own JavaScript code running in the web client. The desktop client uses the platform's standard view in its place.

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

Client code changes the form's state as a user does: it selects the current object, changes a property value or runs an action drawn on the form. A React component can also change a table's sortings and filters and, for a list outside a tree, the selection of rows and columns. Changing a property runs its change event, and the passed value replaces the user's input in a [value request](Value_request_REQUEST.md). The source of such a change is [custom client code](Form_events.md#source) (`CUSTOM`). Changes go through the ordinary form edit checks and require no access to the external API.

### Calls to the server {#server-calls}

Besides editing the form, client code can call actions and change properties on the server by name, and execute lsFusion code. On a form these calls run in its session: a change is visible to following calls and is saved with the form's changes. In the navigator each call runs in a new session.

By default such calls are checked as [access from an external system](Access_from_an_external_system.md): actions and properties exposed to the external API are allowed, as are calls by a user with admin rights. A form can additionally allow its client code to call selected actions and properties. Executing arbitrary code remains subject to the API access check.

### Components the platform draws inside {#platform-components}

A React component drawing a container can leave some of its contents to the platform. The platform then draws those components, and the React component places them inside its view. A child component with a view of its own — standard or another custom view — is drawn by that view.

### A hidden custom view {#hidden}

A container with a custom view is hidden like any design component: for example, in an unselected tab or inside a collapsed container. While the form hides the container, the server does not read what its view draws unless a shown part of the form needs the same data. A form that is entirely off screen is read as usual.

Components the platform draws that the React view does not place are not read unless a shown part of the form needs the same data. Their captions, images and comments continue to be read only when those labels are displayed outside the hidden contents. An action's labels and a list property's labels follow the visibility of the action or property itself. The server considers a container the React component draws itself shown until the component reports that it hides it.

### Keys and editing {#keys}

The platform handles the key and mouse bindings of properties, actions and filter groups a React component draws, according to the modes of those bindings. Bindings that need a platform cell for value input are the exception. The standard table's keys do not act over rows the component draws.

While the user types into a field of client code, the code tells the form that editing is in progress. The confirmation and cancellation keys, and bindings disabled during editing, then leave input in that field alone. This declaration itself does not start changing a property or save the entered value.

### Modules of client code {#modules}

Client code is kept as files in the project. They can be compiled during the project build or used without a build, and are loaded into the web client when a page opens.

### Language

The [JavaScript function](../language/Render_functions.md#render-function) named by `CUSTOM 'itemCards'` draws the rows of the item group.

### React

- [React views](../language/React_views.md) describe what a React component drawing a custom view receives and calls.
- [Form and navigator controllers](../language/Form_and_navigator_controllers.md) give a React component server calls and, on a form, changes to what other containers draw and batching of changes.
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
