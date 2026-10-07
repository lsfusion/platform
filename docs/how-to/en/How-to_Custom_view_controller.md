---
slug: "/How-to_Custom_view_controller"
title: 'How-to: Custom view controller API'
---

A custom view written in JavaScript communicates with the form through a *controller* object — with it the view sets the current object of a group, changes property values, looks up suggestions, and calls actions or scripts on the server. There are three kinds of custom view. A React view gets the controller of its own projection — the members below, beside the `props.data` they mirror. A classic view and an `INTERNAL CLIENT` action get the form's, which carries only what every controller of the form does: the form-level verbs (`exec` / `eval` / `evalAction` / `change`) and the [editing declaration](#editing) (`startEditing` / `stopEditing` / `isEditing`).

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

Rows a classic view receives carry the same `key` and `objects` as React rows (see [Row identity](#row-identity-contract)). An object group's local `changeProperty` also *delegates*: for a property that is not one of its own columns it is passed on to the form and resolved form-wide, so the view can change a property it does not display. The form controller itself takes no rows: its `change` names a property, then the values of its parameters and the new value.

An [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action is a fourth entry point: its bound JavaScript function receives the form controller as the argument after the call parameters. The [custom value editor](How-to_Custom_components_properties.md#custom-editor)'s controller (`CHANGE`) also exposes the form controller as its `form` field.

### The form controller

There is one controller **per projection**: a React view's `props.controller` is its own container's, and it names exactly what that container's `props.data` shows. The object a classic view and a custom cell editor are given as `controller.form`, and an `INTERNAL CLIENT` action as its argument, is the FORM's, and carries only the form-level verbs (`exec` / `eval` / `evalAction` / `change`): the members below say that `data.<group>.<property>` is there, and an object with no data beside it has nothing to say that about. It **mirrors the shape of `props.data`**: its members are the container's entries, made once, when the form opens, each at the same path as its data — what a view reads as `data.o.qty` it changes through `controller.o.qty`, and `data.total` through `controller.total`. Each object group a part of which this view draws is a member of the controller under its group SID, each property whose VALUE this view carries is a member of that group under its integration name, and each property of the empty group (the form level) is a member of the controller itself. A form in which one of these names would stand for two properties of one container is refused when it is built. The member *is* the address — there is no second, string-addressed set of verbs saying the same thing, and a name typed wrong is a member that does not exist rather than a string the platform has to validate.

A view reads and changes only its own container's parts: its controller changes what its `props.data` carries, and nothing else. What containers share is the form itself — its current objects and its session — so a filter or a current object changed in one container shows in another, as it does in the standard client.

Optional arguments are bracketed.

| member | what it does | returns |
| --- | --- | --- |
| `<group>.change(row)` | set the group's current object — only where the group's rows are drawn | — |
| `<group>.orders.change(order)` | change one of the group's [sortings](How-to_Custom_React_views.md#orders), leaving the rest and their priority — only where the group's rows are drawn | — |
| `<group>.orders.change(orders)` | an array states them all, its order being the priority | — |
| `<group>.<property>.change(value[, row])` | set the value — on the current object, or on a given row | — |
| `<group>.<property>.exec([row])` | run the property's change event, or the action — on the current object, or on a given row | — |
| `<group>.<property>.getValues([object,] value[, mode], ok[, fail][, count])` | a capped server suggestion list | — (via `ok`) |
| `<property>.change(value)` / `.exec()` / `.getValues(...)` | the same, for a property of the empty group — with no row | — |
| `<group>.expand(row)` / `.collapse(row)` / `.toggle(row)` | open or close one node of a [tree](How-to_Custom_React_views.md#trees) — a node is a row of a group; only where the tree's rows are drawn | — |
| `<group>.expandAll()` / `.collapseAll()` | open every node of the group and of the groups below it — the whole tree on its top group — or close them; where the tree's rows are drawn | — |
| `<group>.filters.change(condition)` | change one of the group's user filter conditions, leaving the rest — where `FILTERS(<group>)` is | — |
| `<group>.filters.change(conditions)` | an array states them all: replace the whole condition list | — |
| `properties.change([{property, object, value}])` | several property changes in ONE request | — |
| `exec(action, ...params)` | run a named action | `Promise` |
| `eval(script, ...params)` | run an lsf script with a typed `run` | `Promise` |
| `evalAction(script, ...params)` | run an action body (`$1`, `$2`, … params) | `Promise` |
| `change(property, ...keyParams, value)` | set a global property | `Promise` |
| `startEditing(element)` | declare that the user is editing in `element` | — |
| `stopEditing(element)` | declare that they are not — only if `element` is the declared one | — |
| `isEditing(element)` | whether that declaration stands | `boolean` |

```js
controller.o.note.change('checked');   // the current row of group `o`
controller.o.sum.change(100, row);     // ... a given row
controller.o.edit.exec(row);           // an action: run it on that row
controller.o.change(row);              // the group's current object
controller.o.orders.change([]);        // its sortings: an array states them all — here, none
controller.o.filters.change({property: 'name', value: 'an'});   // one condition — data.o.filters is where it lands
controller.o.customer.getValues(text, 'objects', ok, fail);
controller.total.change(500);          // a property of the empty group
```

The members are **what this view can change** - the entries it carries, minus what the PLATFORM draws: a property declared `LSF` has an entry (an ordinary column entry, so the view can draw the column header) and no member, because its value is drawn and edited by its own renderer, which this view places with `<Lsf name row/>`, and an `lsf` child has only the entry that labels it. So a group is a member of the controller of every container that draws a part of it, and a property is a member there when THAT container carries its value: a grid property where the rows are drawn, a panel property where the property itself stands. A member is there as long as its entry is — from the start, `hidden` or not: a `SHOWIF` that hides the property leaves the member in place, and a call through it is refused while the entry is `hidden`, with an error saying the property is not shown by the form now. A property's change event is the one a user fires by editing the cell, so a property the form is not showing has nobody to fire it for. `<group>.change(row)` is there only where the group's rows are drawn — the current object is chosen among them — and so is `<group>.orders.change(...)`, since a sorting is the order those rows are drawn in; and a row is taken only there: by `<group>.change` itself, and by the member of a list property, which lives where the rows are. The member of a panel property and a member of the empty group take no row: they act on the current objects. What a view cannot see it has no member for either: the rest of the form is reached the way anything outside this surface is — `change` / `exec` / `eval` / `evalAction`, which take lsf names and code rather than projection names, and which every controller carries. Where the state a view keeps names a property — a sorting, a user filter's condition — it names it by its integration name, not by a member: a state is data, written in the shape `props.data` publishes it, and it may name a property no member stands for, such as an `lsf` column. A sorting is `{property: 'sum', desc: true}`, and its name is looked up only among what this view carries; a condition is `{property: 'sum', compare: '>', value: 100}`, and its name is looked up among the group's properties, as `FILTERS(<group>)` need not be anywhere near a column ([Filters](How-to_Custom_React_views.md#filters)). A call, by contrast, names what it changes by its member — in the batch too. A property grouped in columns is a member nowhere, for the same reason it is not projected: its values are addressed by a row-and-column key. (The classic `changeProperty` a CUSTOM object group is given is another surface, older than this one, and goes on naming the whole form.) The mutating members return nothing: the new state arrives with the next form update. The server-calling methods on the controller itself (`exec` / `eval` / `evalAction` / `change`) return a `Promise`.

Running an action through its member — `controller.o.edit.exec(row)` — is the same request as clicking it on the form: synchronous, blocking input until it completes and showing the [busy indicator](../paradigm/Interactive_view.md#busy) after its delay, unless the action is drawn with `NOWAIT` (the `syncType` [property option](../language/Property_options.md)). The controller's own `exec`, `eval`, `evalAction` and `change` go as asynchronous requests. So a refresh on a timer is not made through the controller — see [Live data](How-to_Custom_React_views.md#live-data).

`properties.change` is the one call that is not reached through the member it changes: a batch spans properties, and groups, so no single member can own it, and it hangs on the controller itself. It is a shortcut for those members, not a second way in — it says exactly what they say. It states its changes as a list of `{property, object, value}`, or one of them alone, and `property` is the MEMBER itself — `controller.o.qty`, `controller.total` — never a name: an entry whose `property` is not a property member of this view's controller is refused. `object` is the row, taken as the member's own calls take it — only for a list property. Without it, the change is made for the current object. `value` left out runs the property's change event, exactly as the member's `exec()` does — the only form an action's entry takes. A `value` given as `undefined` is refused, since `null` is what clears. A member whose entry is `hidden` is refused here too.

A name that is not a JavaScript identifier is addressed with brackets: a property with `EXTID 'unit price'` is `controller.o['unit price'].change(v)`. A group SID is always an identifier — a group of several objects, whose SID would be their names joined with dots, is refused when the form is built until it is named (`OBJECTS pair = (d = X, t = Y)`).

A name that would shadow a member of the surface itself is refused when the form is **built**, the same way [the projection's own reserved names](How-to_Custom_React_views.md) are: a group SID or a property of the empty group coinciding with a controller method (`exec`, `eval`, `evalAction`, `change`, `startEditing`, `stopEditing`, `isEditing`, `properties`), or with each other — a controller is one namespace for its own projection, so a group and a property of the empty group the same container projects cannot both be `total`, while two different containers may — and a group property named like one of the group's own members — `change` on every group node, `expand`, `collapse`, `toggle`, `expandAll` and `collapseAll` on a group of a tree where its rows are drawn, `orders` where the group's rows are drawn, and `filters` where its user filters are. The fix is to rename it, or give it an `EXTID`.

The same two groups also differ along two more axes — whether they are gated, and how an object is addressed in them:

| group | methods | gate | how an object is passed |
| --- | --- | --- | --- |
| editing the form | `<group>.change` / `<property>.change` / `<property>.exec` / `properties.change` | none | the target row — a data row (`row`), a raw handle (`row.objects`), or its key; an object as a value (FK) — its id |
| calling the server | `exec` / `eval` / `evalAction` / `change` on the controller itself | `@@api` / admin rights / the form's `CUSTOMS` | an object — its id |

A custom view normally reads state from `props.data` and changes it through the form-edit members — including running an action drawn on the form with `controller.<action>.exec()`. The server-call methods (`exec` / `eval` / `evalAction` / `change`) are an escape hatch, used only for what the form does not express — ad-hoc server computation, a global write, or creating an object.

Editing the form goes through the ordinary edit channel and is not gated. The server calls are (see [Calling the server](How-to_Custom_components_objects.md#calling-the-server)). The edited row is addressed by a handle, while any other object — an FK value or an action parameter — is passed as its numeric id (an lsFusion object cannot be passed from JS).

#### Changing the current object and property values

`controller.<group>.change(row)` sets the current object of that group. It is there only in the container that draws the group's rows, and the `row` is a data row of the group, a raw `objects` handle, or the key the projection gave it (see [the identity rules](#row-identity-contract) below).

`controller.<group>.<property>.change(value)` changes the property for the group's current object, and `.change(value, row)` for the given row. The arguments come in this order, and nothing is guessed from them: the value first — it is required, and `null` clears it — then the row, which is optional. `.exec()` runs the property's change event, the one a user fires by editing the cell, on the current object — for an action, it runs the action — and `.exec(row)` does it on the given row. An action has no value to set, so `.change` on one is refused: call `exec`. A row is taken only by the member of a list property, which lives where the group's rows are drawn. The member of a panel property and a member of the empty group take none, and act on the current objects.

```js
function orderView(props) {
    const controller = props.controller;
    return (
        <div>
            <button onClick={() => controller.o.note.change('checked')}>Mark</button>
            {props.data.o.list.map(row =>
                <div key={row.key} onClick={() => controller.o.change(row)}>
                    {row.number.value}
                </div>)}
        </div>
    );
}
```

A value passed to `change` reaches a property's change handler only when it requests a value from the user — the [default handler](../paradigm/Form_events.md#default), or a handler with a [value request](../paradigm/Value_request_REQUEST.md): the passed value is taken instead of the user's input. `null` is a value like any other. An action has no value to set: its member's `change` is refused, and it is run with `exec`, which passes none — `controller.o.edit.exec(row)` — so an action that asks the user for a value cannot be given one through its member.

`properties.change(entries)` applies several changes at once, in one request. Each entry names its property by the member itself. An entry with no `object` changes the current object, and one with no `value` runs the change event, as `exec` does:

```js
controller.properties.change([{property: controller.o.note, value: 'checked'},
                              {property: controller.o.qty, object: row, value: 5}]);
```

A built-in primitive-class object group — a `DATE` navigator, for instance — is moved to a value by writing the object's value (`controller.<g>.VALUE.change(d)` with a real JS `Date`), by `controller.<g>.change(row)` with a row from `props.data.<g>.list` (which carries the `objects` handle), or, when the group is filtered by a data property, by changing that filter property. A date value goes through a conversion that assumes a JS `Date` (an unchecked cast): a non-`Date` argument — a date-input *string*, a timestamp — throws `getFullYear is not a function`, so pass an actual `Date`, e.g. `new Date(year, month - 1, day)`.

A property's `.change` and `properties.change` behave the same way, and the format depends on what is set as the value:

| value | how it is passed |
| --- | --- |
| a primitive | directly: a number for numeric types, a string, a JS `Date` for `DATE` / `TIME` / `DATETIME` / `ZDATETIME`, a boolean, `null` to clear |
| `JSON` | a JS object or array, serialized as JSON |
| an object (FK value) | the target object's id — `row.key` of its row (for a single-object group it already is its numeric id), or an id-valued property on the form (e.g. `LONG(obj)`) — not a handle |

:::info
Passing a handle (`otherRow.objects`) as an FK value silently sets it to `NULL`, with no error. A handle is only for the row argument (the edited row, the `object` of a batch entry) and for `<group>.change`. To set an FK, pass the target object's id.
:::

The format is the same in the read direction: an object property's value arrives in the data row as this same numeric id, so it can be compared with the target row's `row.key` (in a single-object group) or passed back as an FK value without conversion.

Like a user edit in the standard table, the change is made in the form's [change session](../paradigm/Change_sessions.md): the view sees the new value in `props.data` at once, while it reaches the database when the session is applied — with the Save button or an action with the [`APPLY`](../language/APPLY_operator.md) operator. If the edited property is marked with the [`APPLY` option](../language/Properties_and_actions_block.md#options) on the form, its standard change handler applies the change at once (by default, by committing the whole form session), so `.change` needs no separate save. The details are described with the option. For a simple edit from a view — a move, a resize, an in-place value edit — this is preferable to a separate server action. The server action (`controller.exec`) stays for what a property change cannot express: creating an object (`NEW`), multi-step logic, opening a form.

```js
// move an object to another parent and edit a primitive in one call:
// the FK value is the target object's id (row.key of the target row), the primitive value is passed directly
controller.properties.change([{property: controller.i.parent, object: item, value: targetColumn.key},
                              {property: controller.i.value, object: item, value: 5}]);
```

#### Looking up values

`getValues` asks the server for a capped suggestion list for a property — a suggestion list, not a full `SELECT DISTINCT`. The result is delivered to the `ok` callback as `{ data: [ { displayString, rawString, objects }, ... ], more }`. `more` is `true` on an answer the server gave before it was sure the form's unsaved changes do not affect it: `ok` is then called once more, with the final list.

```js
controller.<group>.<property>.getValues([object,] value[, mode], ok, fail[, count]);
```

- `value` — the typed query to match against.
- `object` — an optional row, taken only by the member of a list property (a data row, a raw handle, or the row's key — a key only when `mode` is given too, since before `value` alone a string is read as the `value` query), that scopes the lookup to that row. Omit it for the current object.
- `mode` — one of:

  | `mode` | result | `item.objects` |
  | --- | --- | --- |
  | `'objects'` (default) | the matching `OBJECTS` for the property — an object picker | a raw `objects` handle for that object |
  | `'values'` | the distinct values of the property | `null` (use `displayString` / `rawString`) |
  | `'change'` | the property's edit-time suggestions | depends on the property |

  `'change'` reflects how the property is *edited* — a custom `INPUT` list, a `notNull` constraint, or a custom change action — rather than the distinct values already present. For a property that cannot be changed in this context (for example, a read-only property, or a computed property without a change action) there are no such suggestions, and `fail` is called, whereas `'values'` still returns its distinct values.

- `ok(result)` / `fail()` — success and failure callbacks.
- `count` — raises the number of items requested, for paging.

Pass `item.objects` from an `'objects'` result straight back as a row, to act on the picked object: to the group's `.change`, to make it the current object, or as the `row` of a list property's `.change(value, row)` or `.exec(row)`. It is never a value: a handle passed as an FK value silently sets it to `NULL` (see above), and an FK is set by the object's id:

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

- `exec(action, ...params)` — runs a named action. Resolves to its `RETURN` value. `action` is the action's name — an entry of the form's `CUSTOMS` by the name it has there, or an action of the project by its [name](../language/IDs.md), with the namespace and the signature in brackets when they are needed to pick one: `runReport[Order]` among overloaded actions. Not the `saveFilters()` a script would write: that shape answers "Action was not found".
- `eval(script, ...params)` — runs an lsf script that defines its own `run` action (typed parameters).
- `evalAction(script, ...params)` — runs an action body wrapped into a `run` action, with parameters referenced as `$1`, `$2`, ….
- `change(property, ...keyParams, value)` — changes a global property. The last argument is the value, the preceding ones are the keys. When the property's value is an object, the value is its id, and the platform assigns the object with that id — the object picker opens only for interactive editing.

Parameters are passed as plain JS values (a number, string, boolean, `Date`, or an object/array for a `JSON` parameter). An lsFusion object is passed as its numeric id. When an action parameter is typed by a class, the platform resolves the id to the object of that class — no manual lookup is needed. A row handle is not an object reference here: for a class-typed parameter the call fails, so pass the id.

That id is the `row.key` of the object's row (see [Row identity](#row-identity-contract)) or an id-valued property on the form. It has to be a number: a key read out of `data.<group>.byKey` or `data.<group>.keys` is a *string*, because keys of a JS object are always strings, and a string in a class-typed parameter is rejected with `Number is required`.

```js
const total = await controller.exec('recalc', orderId);
const doubled = await controller.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.change('archived', orderId, true);
```

A call made after the form has been closed *rejects* with a `Form is closed` error — it never hangs — so an `await` on a closed form lands in the `catch` branch.

#### Declaring that the user is editing {#editing}

While a form is editing, a binding whose `editing` scope is the default does not fire for an event that came from inside the element being edited: `ENTER` does not move the focus to the next component, `ESCAPE` does not close a modal window, and a shortcut bound with [`CHANGEKEY`](../language/Property_options.md) does not run. A binding declared with `editing=all` fires regardless.

The form knows the editors it created itself. A field a view drew for itself it does not know, so the view declares it:

- `startEditing(element)` — from now on the user is editing in `element`;
- `stopEditing(element)` — the user is no longer editing in `element`, and the declaration is cleared only when `element` is the declared one;
- `isEditing(element)` — whether `element` is the declared one.

There is one declaration per form: declaring another element replaces the previous one. While the form's own editor is open it takes precedence over the declaration, which is read again once that editing ends.

While the declaration stands, the bindings above do not fire for events from `element` or from anything inside it. A view that declares nothing keeps the behaviour it had before: `ENTER` moves the focus to the next component and `ESCAPE` closes the window, from a field the view drew as from anywhere else.

```jsx
function Note({ controller }) {
    return <textarea onFocus={e => controller.startEditing(e.target)}
                     onBlur={e => controller.stopEditing(e.target)}/>;
}
```

This example declares on focus. A property's own editor starts editing on the first character typed, not on focus.

Declaring on a wrapper covers every field inside it — including a nested [`lsf` child](How-to_Custom_React_views.md#lsf-child) and the editor the platform renders there. Declare on a narrower element when that is not wanted.

Removing the declaration is the view's to do, and not only on blur: clear it when the element is removed or the view unmounts. A declaration left behind keeps those bindings from firing for events inside that element for as long as the form is open.

The declaration does not start property editing: there is no value to commit or to cancel, and `ENTER` saves nothing. To edit a *property* from the view, place the property with [`<Lsf name/>`](How-to_Custom_React_views.md#lsf-child) — the platform renders its own editor there, and nothing is declared.

A single key the view needs regardless of the editing state — a shortcut of its own — is a different matter: stop the event in the handler (`stopPropagation`), and the form does not receive it.

### The navigator controller {#navigator-controller}

An [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action placed in [`NAVIGATOR`](../language/NAVIGATOR_statement.md) receives a controller as well, but a navigator one: there is no form, so it has none of the form methods above. It has the four server calls — `exec`, `eval`, `evalAction`, `change` — which run in the navigator's own session, a new one per call that is never applied, so a `change` made here is not kept. And it has `activate`:

- `activate(canonicalName[, event])` — does what clicking that navigator element does: selects the folder, or runs the action, opening its form the same optimistic way. `canonicalName` is the element's [canonical name](../language/IDs.md). `event` is the event of the click, React's own or the browser's, and can be omitted when activating from code that has none.

Activating an element that does not exist, or one hidden by its own `SHOWIF`, throws. Running an action reports no completion, just as a click does not.

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
- the **key** the projection gave the row — `row.key`, or a key out of `data.<group>.keys` / `byKey`. A row is taken only where its group's rows are drawn, so there is always this view's `byKey` to look the key up in: it is looked up among the rows of the member's own group, and a key naming no row there fails with an error. This is what lets a view hand back what it was handed: `byKey` is a JS object, and `Object.keys()` on it yields strings, so a key that has been through it no longer carries the type `row.key` was written with. A classic view has no such index, so its `changeProperty` takes the row or its handle. Where a row and a value are told apart by the argument alone — the `value` query of `getValues` — a bare key is read as the value, so pass the row itself there.

For a single-object group of a custom class the value of `row.key` numerically equals that object's id, so it can be passed as the target object's id to a server call or as an FK value — no separate property for the row's own id is needed.

If an explicit row argument is none of these, the platform does not silently fall back to the current row: the call fails with an error naming what was expected rather than acting on another row. Nor does it accept a row of a *different* group: `controller.<group>.change(row)`, a list property member's `change(value, row)` / `exec(row)` and a `properties.change` entry all say whose row is meant — the member's group — and a row of another group carries only that group's objects, so the rest would be taken from the current ones and the call would land on a row of this group nobody named. Inside a tree a row of a group BELOW is not another group's row in this sense: its key is the whole path down to it, so it names one row of every group above it, and it is accepted there — exactly as `<group>.change` accepts it.
