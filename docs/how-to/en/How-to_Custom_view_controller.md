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
| `changeProperties(properties, objects, values)` | several `changeProperty` calls from parallel arrays | — |
| `getPropertyValues(property[, object], value[, mode], ok[, fail][, count])` | a capped server suggestion list | — (via `ok`) |
| `exec(action, ...params)` | run a named action | `Promise` |
| `eval(script, ...params)` | run an lsf script with a typed `run` | `Promise` |
| `evalAction(script, ...params)` | run an action body (`$1`, `$2`, … params) | `Promise` |
| `change(property, ...keyParams, value)` | set a global property | `Promise` |

The mutating methods (`changeObject` / `changeProperty` / `changeProperties`) return nothing — the new state arrives with the next form update; the server-calling methods (`exec` / `eval` / `evalAction` / `change`) return a `Promise`. When a property's integration name is not unique across the form, qualify the group directly in the name — `'groupSID.property'`; a group named this way has priority. When an object is passed, the property is resolved by that object's own group, so the group needs to be named only for a change with no object (the current object, or the two-argument `changeProperty(property, value)`). A bare, unqualified name drawn on more than one group must not be used — such a call fails with an error instead of silently changing the wrong group.

The same two groups also differ along two more axes — whether they are gated, and how an object is addressed in them:

| group | methods | gate | how an object is passed |
| --- | --- | --- | --- |
| editing the form | `changeObject` / `changeProperty` / `changeProperties` | none | the target row — a data row (`row`) or a raw handle (`row.objects`); an object as a value (FK) — its id |
| calling the server | `exec` / `eval` / `evalAction` / `change` | `@@api` / admin rights / the form's `CUSTOMS` | an object — its id |

A custom view normally reads state from `props.data` and changes it through the form-edit methods — including running an action drawn on the form with `changeProperty('action')`. The server-call methods (`exec` / `eval` / `evalAction` / `change`) are an escape hatch, used only for what the form does not express — ad-hoc server computation, a global write, or creating an object.

Editing the form goes through the ordinary edit channel and is not gated; the server calls are (see [Calling the server](How-to_Custom_components_objects.md#calling-the-server)). The edited row is addressed by a handle; any other object — an FK value or an action parameter — is passed as its numeric id (an lsFusion object cannot be passed from JS).

#### Using the structured shorthand {#structured-shorthand}

The form controller also mirrors the shape of `props.data`: each object group is a member of the controller under its group SID, each property drawn on that group is a member of the group under its integration name, and each form-level (no-group) property is a member of the controller directly. This is an alternative to the string-addressed methods — the group and property are written as a path instead of packed into a `'group.property'` string:

| shorthand | equivalent |
| --- | --- |
| `controller.<group>.<property>.change([object,] [value])` | `changeProperty('<group>.<property>', [object,] [value])` |
| `controller.<group>.<property>.getValues(value[, mode], ok[, fail][, count])` | `getPropertyValues('<group>.<property>', value[, mode], ok[, fail][, count])` |
| `controller.<group>.change(object)` | `changeObject('<group>', object)` — set the group's current object |
| `controller.<property>.change(value)` / `.getValues(...)` | `changeProperty('<property>', value)` / `getPropertyValues('<property>', …)` — a form-level property |

```js
controller.o.note.change('checked');   // changeProperty('o.note', 'checked')
controller.o.sum.change(row, 100);     // changeProperty('o.sum', row, 100) — on a given row
controller.o.edit.change(row);         // exec the action 'edit' on a given row
controller.o.change(row);              // changeObject('o', row) — set the group's current object
controller.o.customer.getValues(text, 'objects', ok, fail);
controller.total.change(500);          // a form-level property: changeProperty('total', 500)
```

At each node `.change(...)` mutates that node — a property's value, or a group's current object; a property's `.change(...)` reuses `changeProperty`'s value-or-row guess and `.getValues(...)` reuses `getPropertyValues`'s lookup modes, since each is a plain forward to the flat method. The shorthand is form-wide — every object group and every drawn property carrying an integration name; `props.data` carries the subset inside the view's own container. A name that would shadow an existing member is kept as that member, not the accessor: a group SID or form-level property coinciding with a controller method (`changeProperty`, `exec`, `change`, …), a form-level property coinciding with a group SID, or a group property named `change`. Address it with the string form instead.

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

`changeProperties(properties, objects, values)` applies several changes at once from parallel arrays — `properties[i]` is changed to `values[i]` for `objects[i]` (an entry may be `null` for the current object). Each name is qualified the same way as in a single `changeProperty` — by a `'groupSID.property'` name or the passed object's group.

```js
controller.changeProperties(['note', 'qty'], [null, row], ['checked', 5]);
```

A built-in primitive-class object group — a `DATE` navigator, for instance — is moved to a value by writing the object's value (`changeProperty('VALUE', d)` with a real JS `Date`), by `changeObject` to a row from `props.data.<g>.list` (which carries the `objects` handle), or, when the group is filtered by a data property, by changing that filter property. `changeProperty` runs a date value through a conversion that assumes a JS `Date` (an unchecked cast): a non-`Date` argument — a date-input *string*, a timestamp — throws `getFullYear is not a function`, so pass an actual `Date`, e.g. `new Date(year, month - 1, day)`.

`changeProperty` and `changeProperties` behave the same way; the format depends on what is set as the value:

| value | how it is passed |
| --- | --- |
| a primitive | directly: a number for numeric types, a string, a JS `Date` for `DATE` / `TIME` / `DATETIME` / `ZDATETIME`, a boolean, `null` to clear |
| `JSON` | a JS object or array, serialized as JSON |
| an object (FK value) | the target object's id — `row.key` of its row (for a single-object group it already is its numeric id), or an id-valued property on the form (e.g. `LONG(obj)`) — not a handle |

:::info
Passing a handle (`otherRow.objects`) as an FK value silently sets it to `NULL`, with no error. A handle is only for the `object` argument (the edited row) and for `changeObject`; to set an FK, pass the target object's id.
:::

If the edited property is marked `APPLY` on the form (the edit is applied at once), `changeProperty` commits the change immediately. For a simple edit from a view — a move, a resize, an in-place value edit — this is preferable to a separate server action; the server action (`exec`) stays for what a property change cannot express: creating an object (`NEW`), multi-step logic, opening a form.

```js
// move an object to another parent and edit a primitive in one call:
// the FK value is the target object's id (row.key of the target row), the primitive value is passed directly
controller.changeProperties(['parent', 'value'], [item, item], [targetColumn.key, 5]);
```

#### Looking up values

`getPropertyValues` asks the server for a capped suggestion list for a property. The result is delivered to the `ok` callback as `{ data: [ { displayString, rawString, objects }, ... ], more }`; `more` is `true` when the list was truncated, so it is a suggestion list, not a full `SELECT DISTINCT`.

```js
controller.getPropertyValues(property[, object], value[, mode], ok, fail[, count]);
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

`exec`, `eval`, `evalAction` and `change` each run on the server and return a `Promise`. They are subject to the same authorization gate and convert the result to a JS value the same way as a classic view's server calls — see [Calling the server](How-to_Custom_components_objects.md#calling-the-server) for the gate, parameter binding, and the result-to-JS conversion table.

- `exec(action, ...params)` — runs a named action; resolves to its `RETURN` value.
- `eval(script, ...params)` — runs an lsf script that defines its own `run` action (typed parameters).
- `evalAction(script, ...params)` — runs an action body wrapped into a `run` action, with parameters referenced as `$1`, `$2`, ….
- `change(property, ...keyParams, value)` — changes a global property; the last argument is the value, the preceding ones are the keys.

Parameters are passed as plain JS values (a number, string, boolean, `Date`, or an object/array for a `JSON` parameter). An lsFusion object is passed as its numeric id; when an action parameter is typed by a class, the platform resolves the id to the object of that class — no manual lookup is needed. A row handle is not an object reference here: for a class-typed parameter the call fails, so pass the id.

```js
const total = await controller.exec('recalc', orderId);
const doubled = await controller.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.change('archived', orderId, true);
```

A call made after the form has been closed *rejects* with a `Form is closed` error — it never hangs — so an `await` on a closed form lands in the `catch` branch.

### Row identity {#row-identity-contract}

A method that targets a row accepts one of:

- a data row object the view received (from the React `props` / the classic `update` list);
- a spread or `Object.assign` clone of such a row — the enumerable `objects` handle is copied with it, so the clone resolves to the same object;
- a raw `objects` handle — `row.objects`, or `item.objects` from a `getPropertyValues` `'objects'` result.

For addressing a row, a bare `row.key` is *not* accepted: `key` is a display / React-key / diff token, not a resolution input. For a single-object group of a custom class, though, the value of `row.key` numerically equals that object's id, so it can be passed as the target object's id to a server call or as an FK value — no separate property for the row's own id is needed. The field names `key`, `isCurrent` and `objects` are reserved on a row — an application property or column with one of these integration names would be overwritten.

If an explicit object argument resolves to neither a row nor a raw handle, the platform does not silently fall back to the current row: `changeProperty` and `getPropertyValues` fail with an error rather than acting on another row, while `changeObject` does nothing.
