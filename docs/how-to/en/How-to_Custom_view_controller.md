---
slug: "/How-to_Custom_view_controller"
title: 'How-to: Custom view controller API'
---

A custom view written in JavaScript talks back to the form through a *controller* object. In a [React custom view](How-to_Custom_React_views.md) the controller is `props.controller`; the same controller is also passed to a JavaScript function bound by an [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action. With it the view sets the current object of a group, changes property values, looks up suggestions, and calls actions or scripts on the server.

Properties and actions are addressed by their integration name — the name on the form (or the alias / `NEW` / `DELETE` integration name of a button), the same name the [external JSON/REST API](How-to_Integration.md) uses.

### Changing the current object and property values

`controller.changeObject(groupSID, object)` sets the current object of the group `groupSID`. The `object` is a data row of that group, or a raw `objects` handle (see [the identity contract](#row-identity-contract) below) — not a bare `row.key`.

`controller.changeProperty(property, value)` changes `property` for the group's current object. To target a specific row, pass it in between: `controller.changeProperty(property, object, value)`, where `object` is a data row or a raw handle. When `property` is an action (or any property with no editable value), the value is omitted: `controller.changeProperty('edit')` execs it on the current object, `controller.changeProperty('edit', object)` on the given row.

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

`controller.changeProperties(properties, objects, values)` applies several changes at once from parallel arrays — `properties[i]` is changed to `values[i]` for `objects[i]` (an entry may be `null` for the current object). An optional fourth array `groupSIDs` scopes each property to a group when its integration name is not unique across the form.

```js
controller.changeProperties(['note', 'qty'], [null, row], ['checked', 5]);
```

### Looking up values

`controller.getPropertyValues` asks the server for a capped suggestion list for a property. The result is delivered to the `ok` callback as `{ data: [ { displayString, rawString, objects }, ... ], more }`; `more` is `true` when the list was truncated, so it is a suggestion list, not a full `SELECT DISTINCT`. In a classic `GRID` custom view the same lookup is exposed as `getValues(property, value, ok, fail)`, equivalent to `getPropertyValues` in the default `'objects'` mode; the form-level / React controller uses `getPropertyValues`.

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

### Calling the server

`exec`, `eval`, `evalAction` and `change` each run on the server and return a `Promise`. They behave exactly as described in [How-to: Custom Components (objects)](How-to_Custom_components_objects.md#calling-the-server) — the same authorization gate and the same conversion of the result to a JS value.

- `controller.exec(action, ...params)` — runs a named action; resolves to its `RETURN` value.
- `controller.eval(script, ...params)` — runs an lsf script that defines its own `run` action (typed parameters).
- `controller.evalAction(script, ...params)` — runs an action body wrapped into a `run` action, with parameters referenced as `$1`, `$2`, ….
- `controller.change(property, ...keyParams, value)` — changes a global property; the last argument is the value, the preceding ones are the keys.

```js
const total = await controller.exec('recalc', orderId);
const doubled = await controller.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.change('note', orderId, 'checked');
```

A call made after the form has been closed *rejects* with a `Form is closed` error — it never hangs — so an `await` on a closed form lands in the `catch` branch.

### Row identity contract {#row-identity-contract}

A method that targets a row accepts one of:

- a data row object the view received (from the React props / the `update` list);
- a spread or `Object.assign` clone of such a row — the enumerable `objects` handle is copied with it, so the clone resolves to the same object;
- a raw `objects` handle — `row.objects`, or `item.objects` from a `getPropertyValues` `'objects'` result.

A bare `row.key` is *not* accepted: `key` is a display / React-key / diff token, not a resolution input. The field names `key`, `isCurrent` and `objects` are reserved on a row — an application property or column with one of these integration names would be overwritten.

If an explicit object argument resolves to neither a row nor a raw handle, the platform does not silently fall back to the current row and does not throw: `changeProperty` logs a console error and skips that change, and `changeObject` is a no-op.

### Classic CUSTOM compatibility

The classic [render/update custom components](How-to_Custom_components_objects.md) keep their own `controller` with the `render`/`update`/`isCurrent`/`getDiff` helpers and the [`controller.change`](How-to_Custom_components_properties.md#handling-user-actions) event method. Their rows now also carry the public `key` and the enumerable `objects` handle, so a row from such a view can be passed wherever a row is expected. A `controller.changeProperty(property, ...)` on a property that is *not* a column of that view's own grid is delegated to the form controller, which resolves the property form-wide — so a classic view can change a property it does not display.
