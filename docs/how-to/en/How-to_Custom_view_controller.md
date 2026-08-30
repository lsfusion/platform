---
slug: "/How-to_Custom_view_controller"
title: 'How-to: Custom view controller API'
---

A custom view written in JavaScript communicates with the form through a *controller* object — with it the view sets the current object of a group, changes property values, looks up suggestions, and calls actions or scripts on the server. There are three kinds of custom view. A React view gets the controller of its own projection — the members below, beside the `props.data` they mirror; a classic view and an `INTERNAL CLIENT` action get the form's, which carries the form-level verbs alone (`exec` / `eval` / `evalAction` / `change`).

Properties and actions are addressed by their integration name — the name on the form (or the alias / `NEW` / `DELETE` integration name of a button), the same name the [external JSON/REST API](How-to_Integration.md) uses. Unless an `EXTID` gives it one of its own, that name is the property's name on the form with everything from `(` cut off, so `f(a)` and `f(b)` drawn on one group are both `f`: an integration name is not, by itself, unique.

That is the name a view has, everywhere: `props.data` is keyed by it and every controller member is named by it. The form's own operators — `FILTER`, `ORDER` and the `READ` that writes their state back out — speak the other name, the property's SID on the form, in both directions. So a name a view was handed is not a name one of those operators takes: convert where the two meet, in the `lsf` code that calls the operator, rather than expecting either side to accept both.

### How a view gets its controller

| view | declared | JavaScript entry point | its controller is |
| --- | --- | --- | --- |
| [React view](How-to_Custom_React_views.md) | `DESIGN c { custom = 'Name'; }` | a component taking `props` (`data`, `controller`) | `props.controller` |
| [CUSTOM object group](How-to_Custom_components_objects.md) | `OBJECTS g = Cls CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |
| [CUSTOM property cell](How-to_Custom_components_properties.md) | `PROPERTIES p CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |

A **React view** renders an entire custom container, so the `controller` in its `props` is that container's own controller — the methods below are called on it directly.

A **CUSTOM object group** and a **CUSTOM property cell** are rendered by classic `render(element, controller)` / `update(element, controller, ...)` callbacks — `update` also receives the group's `list` of rows, or the cell's `value`. The `controller` they receive is a *local* controller scoped to that one group or cell: it adds the helpers those views need — value, current-row and styling getters and `diff` / `clearDiff` for an object group, the `change` edit event for a property cell — documented in [Custom components (objects)](How-to_Custom_components_objects.md) and [Custom components (properties)](How-to_Custom_components_properties.md). The local controller exposes the form controller as `controller.form`, so the form-level methods below are reached through it:

```js
const total = await controller.form.exec('recalc', orderId);
await controller.form.change('customerOrder', orderId, customerId);
```

Rows a classic view receives carry the same `key` and `objects` as React rows (see [Row identity](#row-identity-contract)), so the form controller accepts them unchanged. An object group's local `changeProperty` also *delegates*: for a property that is not one of its own columns it is passed to the form controller and resolved form-wide, so the view can change a property it does not display.

An [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action is a fourth entry point: its bound JavaScript function receives the form controller as the argument after the call parameters. The [custom value editor](How-to_Custom_components_properties.md#custom-editor)'s controller (`CHANGE`) also exposes the form controller as its `form` field.

### The form controller

There is one controller **per projection**: a React view's `props.controller` is its own container's, and it names exactly what that container's `props.data` shows. The object a classic view, a custom cell editor or an `INTERNAL CLIENT` action is given — `controller.form` — is the FORM's, and carries only the form-level verbs (`exec` / `eval` / `evalAction` / `change`): the members below say that `data.<group>.<property>` is there, and an object with no data beside it has nothing to say that about. It **mirrors the shape of `props.data`**: what a view reads as `data.<group>.<property>` it changes as `controller.<group>.<property>.change(...)`. Each object group a part of which this view draws is a member of the controller under its group SID, each property whose VALUE this view carries is a member of that group under its integration name, and each form-level (no-group) property is a member of the controller directly — as long as that name singles out one addressable draw, which is what the rest of this page is about. The member *is* the address — there is no second, string-addressed set of verbs saying the same thing, and a name typed wrong is a member that does not exist rather than a string the platform has to validate. Optional arguments are bracketed.

| member | what it does | returns |
| --- | --- | --- |
| `<group>.change(object)` | set the group's current object | — |
| `<group>.<property>.change([object,] [value])` | set a value, or exec an action — on the current object or a given row | — |
| `<group>.<property>.getValues([object,] value[, mode], ok[, fail][, count])` | a capped server suggestion list | — (via `ok`) |
| `<property>.change(...)` / `<property>.getValues(...)` | the same, for a form-level (no-group) property | — |
| `properties.change([{property, object, value}])` | several property changes in ONE request | — |
| `exec(action, ...params)` | run a named action | `Promise` |
| `eval(script, ...params)` | run an lsf script with a typed `run` | `Promise` |
| `evalAction(script, ...params)` | run an action body (`$1`, `$2`, … params) | `Promise` |
| `change(property, ...keyParams, value)` | set a global property | `Promise` |

```js
controller.o.note.change('checked');   // the current row of group `o`
controller.o.sum.change(row, 100);     // ... a given row
controller.o.edit.change(row);         // an action: exec it on that row
controller.o.change(row);              // the group's current object
controller.o.customer.getValues(text, 'objects', ok, fail);
controller.total.change(500);          // a form-level property
```

The members are **what this view can change** - what the form shows, minus what the PLATFORM draws: a property declared `LSF` has an entry (its caption, so the view can draw the column header) and no member, because its value is drawn and edited by its own renderer, which this view places with `<Lsf name row/>`. Otherwise they are what the form shows — the same thing `props.data` carries, and it is the same thing for the same reason: `.change()` is that property's own `ON CHANGE` event, the one a user fires by editing the cell, so a property the form is not showing has nobody to fire it on. So a group is a member of the controller of every container that draws a part of it, and a property is a member there when THAT container carries its value: a grid property where the rows are drawn, a panel property where the property itself stands. And while the form is showing it: hidden by `SHOWIF`, or a panel property whose group has no current object, it has no entry in `data` and no member here either, and it comes back when the entry does. `<group>.change(object)` is on every group node, wherever it is - moving the current object is the group's own state and a panel-only view has a stake in it - but a row is named there by the row object or its `objects` handle: a key STRING is looked up in the row index of the view that draws the rows, so it names nothing where they are not drawn. What a view cannot see it cannot name either: the rest of the form is reached the way anything outside this surface is — `change` / `exec` / `eval` / `evalAction`, which take lsf names and code rather than projection names, and which every controller carries. A property grouped in columns is a member nowhere, for the same reason it is not projected: its values are addressed by a row-and-column key. (The classic `changeProperty` a CUSTOM object group is given is another surface, older than this one, and goes on naming the whole form.) The mutating members return nothing: the new state arrives with the next form update. The server-calling methods (`exec` / `eval` / `evalAction` / `change`) return a `Promise`.

`properties.change` is the one call that names its properties rather than being reached through them: a batch spans properties, and groups, so no single member can own it, and it hangs at the level whose members those properties are. It is a shortcut for those members, not a second way in — it says exactly what they say, and a property with no member is refused here too. It states its changes as a list of `{property, object, value}`, or one of them alone. A `property` is `'property'` or `'groupSID.property'`. A bare name is read the way `controller.<name>` is: the form-level property of that name, if the form shows one; otherwise the one object group that draws it — and a name drawn on several groups fails with an error instead of changing the wrong one. `object` omitted means the property's group current object, and `value` omitted execs it, exactly as a member's `.change()` with no argument does.

A group SID is written as a member, so a SID that is not a JavaScript identifier — a group of several objects is named by all of them, `df.dt` — is addressed with brackets: `controller['df.dt'].name.change(v)`. The same for a property.

A name that would shadow a member of the surface itself is refused when the form is **built**, the same way the projection's own reserved names are: a group SID or a form-level property coinciding with a controller method (`exec`, `eval`, `evalAction`, `change`, `properties`), or with each other — a controller is one namespace for its own projection, so a group and a form property the same container projects cannot both be `total`, while two different containers may — and a group property named like one of the group's own members (`change`). The fix is to rename it, or give it an `EXTID`.

The same two groups also differ along two more axes — whether they are gated, and how an object is addressed in them:

| group | methods | gate | how an object is passed |
| --- | --- | --- | --- |
| editing the form | `<group>.change` / `<property>.change` / `properties.change` | none | the target row — a data row (`row`), a raw handle (`row.objects`), or, where this view draws that group's rows, its key; an object as a value (FK) — its id |
| calling the server | `exec` / `eval` / `evalAction` / `change` | `@@api` / admin rights / the form's `CUSTOMS` | an object — its id |

A custom view normally reads state from `props.data` and changes it through the form-edit members — including running an action drawn on the form with `controller.<action>.change()`. The server-call methods (`exec` / `eval` / `evalAction` / `change`) are an escape hatch, used only for what the form does not express — ad-hoc server computation, a global write, or creating an object.

Editing the form goes through the ordinary edit channel and is not gated; the server calls are (see [Calling the server](How-to_Custom_components_objects.md#calling-the-server)). The edited row is addressed by a handle; any other object — an FK value or an action parameter — is passed as its numeric id (an lsFusion object cannot be passed from JS).

#### Changing the current object and property values

`controller.<group>.change(object)` sets the current object of that group. The `object` is a data row of the group, a raw `objects` handle, or — where this view draws that group's rows — the key the projection gave it (see [the identity rules](#row-identity-contract) below).

`controller.<group>.<property>.change(value)` changes the property for the group's current object. To target a specific row, pass it first: `.change(object, value)`. When the property is an action the value is omitted: `.change()` execs it on the current object, `.change(object)` on the given row.

```js
function orderView(props) {
    const controller = props.controller;
    return (
        <div>
            <button onClick={() => controller.o.note.change('checked')}>Mark</button>
            {props.data.o.list.map(row =>
                <div key={row.key} onClick={() => controller.o.change(row)}>
                    {row.number}
                </div>)}
        </div>
    );
}
```

In the one-argument `.change(X)` form the platform decides whether `X` is a value or a row by `X` alone: it is read as the row when it resolves to one — a data row or a raw handle — and the call execs on it; in every other case `X` is the value and the call changes the current object. The property is not consulted, so the same argument always means the same thing: an id, a string, a number, a `Date`, `null` are values, whatever the property's own type or way of being edited. A bare key is a value here too, being indistinguishable from one — pass both arguments, `.change(key, value)`, to say that it is the row. The single exception is an action, which has no value to set: an argument that does not resolve to a row fails with an error naming what was expected — except `null`, which names the current object there, as it does wherever a row is passed.

`properties.change(entries)` applies several changes at once, in one request — an entry with no `object` names the property's group current object:

```js
controller.properties.change([{property: 'note', value: 'checked'},
                              {property: 'o.qty', object: row, value: 5}]);
```

A built-in primitive-class object group — a `DATE` navigator, for instance — is moved to a value by writing the object's value (`controller.<g>.VALUE.change(d)` with a real JS `Date`), by `controller.<g>.change(row)` with a row from `props.data.<g>.list` (which carries the `objects` handle), or, when the group is filtered by a data property, by changing that filter property. A date value goes through a conversion that assumes a JS `Date` (an unchecked cast): a non-`Date` argument — a date-input *string*, a timestamp — throws `getFullYear is not a function`, so pass an actual `Date`, e.g. `new Date(year, month - 1, day)`.

A property's `.change` and `properties.change` behave the same way; the format depends on what is set as the value:

| value | how it is passed |
| --- | --- |
| a primitive | directly: a number for numeric types, a string, a JS `Date` for `DATE` / `TIME` / `DATETIME` / `ZDATETIME`, a boolean, `null` to clear |
| `JSON` | a JS object or array, serialized as JSON |
| an object (FK value) | the target object's id — `row.key` of its row (for a single-object group it already is its numeric id), or an id-valued property on the form (e.g. `LONG(obj)`) — not a handle |

:::info
Passing a handle (`otherRow.objects`) as an FK value silently sets it to `NULL`, with no error. A handle is only for the `object` argument (the edited row) and for `<group>.change`; to set an FK, pass the target object's id.
:::

The format is the same in the read direction: an object property's value arrives in the data row as this same numeric id, so it can be compared with the target row's `row.key` (in a single-object group) or passed back as an FK value without conversion.

If the edited property is marked `APPLY` on the form (the edit is applied at once), `.change` commits the change immediately. For a simple edit from a view — a move, a resize, an in-place value edit — this is preferable to a separate server action; the server action (`exec`) stays for what a property change cannot express: creating an object (`NEW`), multi-step logic, opening a form.

```js
// move an object to another parent and edit a primitive in one call:
// the FK value is the target object's id (row.key of the target row), the primitive value is passed directly
controller.properties.change([{property: 'parent', object: item, value: targetColumn.key},
                              {property: 'value', object: item, value: 5}]);
```

#### Looking up values

`getValues` asks the server for a capped suggestion list for a property. The result is delivered to the `ok` callback as `{ data: [ { displayString, rawString, objects }, ... ], more }`; `more` is `true` when the list was truncated, so it is a suggestion list, not a full `SELECT DISTINCT`.

```js
controller.<group>.<property>.getValues([object,] value[, mode], ok, fail[, count]);
```

- `value` — the typed query to match against.
- `object` — an optional row (a data row or a raw handle — not a bare key, which would be read as the `value` query) that scopes the lookup to that row; omit it for the current object.
- `mode` — one of:

  | `mode` | result | `item.objects` |
  | --- | --- | --- |
  | `'objects'` (default) | the matching `OBJECTS` for the property — an object picker | a raw `objects` handle for that object |
  | `'values'` | the distinct values of the property | `null` (use `displayString` / `rawString`) |
  | `'change'` | the property's edit-time suggestions | depends on the property |

  `'change'` reflects how the property is *edited* — a custom `INPUT` list, a `notNull` constraint, or a custom change action — rather than the distinct values already present. For a property that cannot be changed in this context (for example, a read-only property, or a computed property without a change action) it returns an empty list, whereas `'values'` still returns its distinct values.

- `ok(result)` / `fail()` — success and failure callbacks.
- `count` — raises the number of items requested, for paging.

Pass `item.objects` from an `'objects'` result straight back into a `.change` to act on the picked object:

```js
controller.c.customer.getValues(text, 'objects',
    result => result.data.forEach(item => console.log(item.displayString)),
    () => console.log('failed'));

// picking the first suggestion as the group's customer
controller.c.customer.getValues(text, 'objects', result => {
    const item = result.data[0];
    if (item) controller.c.change(item.objects);
}, () => {});
```

#### Calling the server

`exec`, `eval`, `evalAction` and `change` each run on the server and return a `Promise`. They are subject to the same authorization gate and convert the result to a JS value the same way as a classic view's server calls — see [Calling the server](How-to_Custom_components_objects.md#calling-the-server) for the gate, parameter binding, and the result-to-JS conversion table. An end-to-end example of these calls from a CUSTOM view is in [How-to: Custom Components (server calls)](How-to_Custom_components_server_calls.md).

- `exec(action, ...params)` — runs a named action; resolves to its `RETURN` value. `action` is the action's [canonical name](../language/IDs.md) — its signature in brackets, `saveFilters[]` or `runReport[Order]` — not the `saveFilters()` a script would write; a name with the wrong shape answers "Action was not found".
- `eval(script, ...params)` — runs an lsf script that defines its own `run` action (typed parameters).
- `evalAction(script, ...params)` — runs an action body wrapped into a `run` action, with parameters referenced as `$1`, `$2`, ….
- `change(property, ...keyParams, value)` — changes a global property; the last argument is the value, the preceding ones are the keys. When the property's value is an object, the value is its id, and the platform assigns the object with that id — the object picker opens only for interactive editing.

Parameters are passed as plain JS values (a number, string, boolean, `Date`, or an object/array for a `JSON` parameter). An lsFusion object is passed as its numeric id; when an action parameter is typed by a class, the platform resolves the id to the object of that class — no manual lookup is needed. A row handle is not an object reference here: for a class-typed parameter the call fails, so pass the id.

```js
const total = await controller.exec('recalc', orderId);
const doubled = await controller.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.change('archived', orderId, true);
```

A call made after the form has been closed *rejects* with a `Form is closed` error — it never hangs — so an `await` on a closed form lands in the `catch` branch.

### The navigator controller {#navigator-controller}

An [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action placed in [`NAVIGATOR`](../language/NAVIGATOR_statement.md) receives a controller as well, but a navigator one: there is no form, so it has none of the form methods above. It has the four server calls — `exec`, `eval`, `evalAction`, `change` — which run in the navigator's own session, a new one per call that is never applied, so a `change` made here is not kept; and it has `activate`:

- `activate(canonicalName[, event])` — does what clicking that navigator element does: selects the folder, or runs the action, opening its form the same optimistic way. `canonicalName` is the element's [canonical name](../language/IDs.md); `event` is the event of the click, React's own or the browser's, and can be omitted when activating from code that has none.

Activating an element that does not exist, or one hidden by `SHOWIF`, throws — activation by name reaches exactly what the navigator shows. Running an action reports no completion, just as a click does not.

```js
window.openMonthlyReport = function (controller) {
    controller.activate('Reporting.monthly');
};
```

```lsf
openMonthlyReport 'Monthly report' () { INTERNAL CLIENT 'openMonthlyReport'; }

NAVIGATOR {
    NEW openMonthlyReport WINDOW system PARENT;
}
```

### Row identity {#row-identity-contract}

A method that targets a row accepts one of:

- a data row object the view received (from the React `props` / the classic `update` list);
- a spread or `Object.assign` clone of such a row — the enumerable `objects` handle is copied with it, so the clone resolves to the same object;
- a raw `objects` handle — `row.objects`, or `item.objects` from a `getValues` `'objects'` result;
- the **key** the projection gave the row — `row.key`, or a key out of `data.<group>.keys` / `byKey`. A key is looked up among the rows of the group the NAME settles: the group a member names, the group a `'<group>.<property>'` prefix names, or — for a property named on its own, and not shown at the form level — the single group it is drawn on. A key naming no row of that group fails with an error, and a name that settles no group (a property drawn on several groups) takes the row or its handle instead. The index it is looked up in is `byKey`, which belongs to the view that draws those ROWS: settling the group is not enough, so a key names a row only in a view that draws them, and a view holding just a panel property of the group — like a classic view on a form with no React container — passes the row or its handle. This is what lets a view hand back what it was handed: `byKey` is a JS object, and `Object.keys()` on it yields strings, so a key that has been through it no longer carries the type `row.key` was written with. Where a row and a value are told apart by the argument alone — the one-argument `.change(X)`, and the `value` query of `getValues` — a bare key is read as the value, so pass the row itself there, or say which it is by passing both arguments.

For a single-object group of a custom class the value of `row.key` numerically equals that object's id, so it can be passed as the target object's id to a server call or as an FK value — no separate property for the row's own id is needed. `key`, `isCurrent`, `objects`, `background`, `foreground` and `selected` are reserved row field names: a property or column whose integration name is one of them would overwrite the field, so a form that projects such a name is rejected when it is built. Give it an explicit `EXTID`, or rename it.

If an explicit object argument is none of the three, the platform does not silently fall back to the current row: the call fails with an error naming what was expected rather than acting on another row. Nor does it accept a row of a *different* group where the name says which group's row is meant — `controller.<group>.change(object)`, and `controller.<group>.<property>.change(object, ...)` or a `properties.change` entry naming the property as `'<group>.<property>'`: such a key carries only the other group's objects, so the rest would be taken from the current ones and the call would land on a row of this group nobody named. Inside a tree a row of a group BELOW is not another group's row in this sense: its key is the whole path down to it, so it names one row of every group above it, and it is accepted there — exactly as `<group>.change` accepts it. Where the name settles no group — the property is drawn on several groups, so naming it alone does not say which is meant — the row's own group is what settles it, and there is nothing to check it against.
