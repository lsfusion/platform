---
slug: "/How-to_Custom_view_controller"
title: 'How-to: Custom view controller API'
---

A custom view written in JavaScript communicates with the form through a *controller* object — with it the view sets the current object of a group, changes property values, looks up suggestions, and calls actions or scripts on the server. There are three kinds of custom view; they obtain the controller differently, but all reach the same form-level controller, whose methods are documented on this page.

Properties and actions are addressed by their integration name — the name on the form (or the alias / `NEW` / `DELETE` integration name of a button), the same name the [external JSON/REST API](How-to_Integration.md) uses.

### How a view gets its controller

| view | declared | JavaScript entry point | the form controller is |
| --- | --- | --- | --- |
| [React view](How-to_Custom_React_views.md) | `DESIGN c { custom = 'Name'; }` | a component taking `props` (`data`, `controller`) | `props.controller` |
| [CUSTOM object group](How-to_Custom_components_objects.md) | `OBJECTS g = Cls CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |
| [CUSTOM property cell](How-to_Custom_components_properties.md) | `PROPERTIES p CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |

A **React view** renders an entire custom container, so the `controller` in its `props` *is* the form controller — the methods below are called on it directly.

A **CUSTOM object group** and a **CUSTOM property cell** are rendered by classic `render(element, controller)` / `update(element, controller, ...)` callbacks — `update` also receives the group's `list` of rows, or the cell's `value`. The `controller` they receive is a *local* controller scoped to that one group or cell: it adds the helpers those views need — value, current-row and styling getters and `diff` / `clearDiff` for an object group, the `change` edit event for a property cell — documented in [Custom components (objects)](How-to_Custom_components_objects.md) and [Custom components (properties)](How-to_Custom_components_properties.md). The local controller exposes the form controller as `controller.form`, so the form-wide methods below are reached through it:

```js
controller.form.changeObject('customer', row);
const total = await controller.form.exec('recalc', orderId);
```

Rows a classic view receives carry the same `key` and `objects` as React rows (see [Row identity](#row-identity-contract)), so they are accepted by the form-controller methods unchanged. An object group's local `changeProperty` also *delegates*: for a property that is not one of its own columns it is passed to the form controller and resolved form-wide, so the view can change a property it does not display.

An [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action is a fourth entry point: its bound JavaScript function receives the form controller as the argument after the call parameters.

### The form controller

These methods are the same wherever the form controller is reached — directly as `props.controller`, or as `controller.form`. Optional arguments are bracketed.

| method | what it does | returns |
| --- | --- | --- |
| `changeObject(groupSID, object)` | set a group's current object | — |
| `changeProperty(property[, object][, value])` | set a value, or exec an action — on the current object or a given row | — |
| `changeProperties(properties, objects, values[, groupSIDs])` | several `changeProperty` calls from parallel arrays | — |
| `getPropertyValues(property[, object], value[, mode], ok[, fail][, count])` | a capped server suggestion list | — (via `ok`) |
| `exec(action, ...params)` | run a named action | `Promise` |
| `eval(script, ...params)` | run an lsf script with a typed `run` | `Promise` |
| `evalAction(script, ...params)` | run an action body (`$1`, `$2`, … params) | `Promise` |
| `change(property, ...keyParams, value)` | set a global property | `Promise` |

The mutating methods (`changeObject` / `changeProperty` / `changeProperties`) return nothing — the new state arrives with the next form update; the server-calling methods (`exec` / `eval` / `evalAction` / `change`) return a `Promise`. When a property's integration name is not unique across the form, scope it with a `groupSID`: a fourth positional argument to `changeProperty` (`changeProperty(property, object, value, groupSID)`), the trailing argument after `count` on `getPropertyValues`, or the `groupSIDs` array on `changeProperties`.

#### Changing the current object and property values

`changeObject(groupSID, object)` sets the current object of the group `groupSID`. The `object` is a data row of that group, or a raw `objects` handle (see [the identity rules](#row-identity-contract) below) — not a bare `row.key`.

`changeProperty(property, value)` changes `property` for the group's current object. To target a specific row, pass it in between: `changeProperty(property, object, value)`, where `object` is a data row or a raw handle. When `property` is an action (or any property with no editable value), the value is omitted: `changeProperty('edit')` execs it on the current object, `changeProperty('edit', object)` on the given row.

```js
function orderView(props) {
    const controller = props.controller;
    return (
        <div>
            <button onClick={() => controller.changeProperty('note', 'checked')}>Mark</button>
            {props.data.o.list.map(row =>
                <div key={row.key} onClick={() => controller.changeObject('o', row)}>
                    {row.number}
                </div>)}
        </div>
    );
}
```

In the two-argument `changeProperty(property, X)` form the platform decides whether `X` is a value or a row: when the property accepts a value and `X` resolves to a row (a data row or a raw handle), it is read as the row and the call execs on it; otherwise `X` is the value and the call changes the current object.

`changeProperties(properties, objects, values)` applies several changes at once from parallel arrays — `properties[i]` is changed to `values[i]` for `objects[i]` (an entry may be `null` for the current object). The optional fourth array `groupSIDs` scopes each property to a group when its integration name is not unique across the form.

```js
controller.changeProperties(['note', 'qty'], [null, row], ['checked', 5]);
```

An object group of a built-in primitive class — a `DATE` navigator, for instance — is moved by the data its `FILTERS` depend on, not by writing the object's value. `changeObject` moves to an existing row from `props.data.<g>.list` (which carries the `objects` handle); to move the group to an arbitrary value (a date the user picks), set the filter property through a server action — `controller.exec('setDateFilter', ...)` — so the value is handled on the server and the group follows its `FILTERS`. Passing a raw value to `changeObject`, or writing the object's value with `changeProperty`, does not navigate the group, and a value that cannot be converted is dropped without an error.

#### Looking up values

`getPropertyValues` asks the server for a capped suggestion list for a property. The result is delivered to the `ok` callback as `{ data: [ { displayString, rawString, objects }, ... ], more }`; `more` is `true` when the list was truncated, so it is a suggestion list, not a full `SELECT DISTINCT`.

```js
controller.getPropertyValues(property[, object], value[, mode], ok, fail[, count[, groupSID]]);
```

- `value` — the typed query to match against.
- `object` — an optional row (data row or raw handle) that scopes the lookup to that row; omit it for the current object.
- `mode` — one of:

  | `mode` | result | `item.objects` |
  | --- | --- | --- |
  | `'objects'` (default) | the matching `OBJECTS` for the property — an object picker | a raw `objects` handle for that object |
  | `'values'` | the distinct values of the property | `null` (use `displayString` / `rawString`) |
  | `'change'` | the property's edit-time suggestions | depends on the property |

  `'change'` reflects how the property is *edited* — a custom `INPUT` list, a `notNull` constraint, or a custom change action — rather than the distinct values already present. For a property that cannot be changed in this context (for example, a read-only property, or a computed property without a change action) it returns an empty list, whereas `'values'` still returns its distinct values.

- `ok(result)` / `fail()` — success and failure callbacks.
- `count` — raises the number of items requested, for paging.
- `groupSID` — scopes the property to a group when its integration name is not unique across the form (pass it after `count`).

Pass `item.objects` from an `'objects'` result straight back into `changeObject` or `changeProperty` to act on the picked object:

```js
controller.getPropertyValues('customer', text, 'objects',
    result => result.data.forEach(item => console.log(item.displayString)),
    () => console.log('failed'));

// picking the first suggestion as the group's customer
controller.getPropertyValues('customer', text, 'objects', result => {
    const item = result.data[0];
    if (item) controller.changeObject('c', item.objects);
}, () => {});
```

#### Calling the server

`exec`, `eval`, `evalAction` and `change` each run on the server and return a `Promise`. They are subject to the same authorization gate and convert the result to a JS value the same way as a classic view's server calls — see [Calling the server](How-to_Custom_components_objects.md#calling-the-server) for the gate and the result-to-JS conversion table.

- `exec(action, ...params)` — runs a named action; resolves to its `RETURN` value.
- `eval(script, ...params)` — runs an lsf script that defines its own `run` action (typed parameters).
- `evalAction(script, ...params)` — runs an action body wrapped into a `run` action, with parameters referenced as `$1`, `$2`, ….
- `change(property, ...keyParams, value)` — changes a global property; the last argument is the value, the preceding ones are the keys.

```js
const total = await controller.exec('recalc', orderId);
const doubled = await controller.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.change('note', orderId, 'checked');
```

A call made after the form has been closed *rejects* with a `Form is closed` error — it never hangs — so an `await` on a closed form lands in the `catch` branch.

### Row identity {#row-identity-contract}

A method that targets a row accepts one of:

- a data row object the view received (from the React `props` / the classic `update` list);
- a spread or `Object.assign` clone of such a row — the enumerable `objects` handle is copied with it, so the clone resolves to the same object;
- a raw `objects` handle — `row.objects`, or `item.objects` from a `getPropertyValues` `'objects'` result.

A bare `row.key` is *not* accepted: `key` is a display / React-key / diff token, not a resolution input. The field names `key`, `isCurrent` and `objects` are reserved on a row — an application property or column with one of these integration names would be overwritten.

If an explicit object argument resolves to neither a row nor a raw handle, the platform does not silently fall back to the current row and does not throw: `changeProperty` logs a console error and skips that change, and `changeObject` is a no-op.
