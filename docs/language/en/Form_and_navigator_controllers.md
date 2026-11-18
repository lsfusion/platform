---
slug: "/Form_and_navigator_controllers"
title: 'Form and navigator controllers'
---

JavaScript code works with a form open in the [interactive view](../paradigm/Interactive_view.md) through the form controller, and with the [navigator](../paradigm/Navigator.md) through the navigator controller.

A custom view written in JavaScript communicates with the form through a *controller* object — with it the view sets the current object of a group, changes property values, looks up suggestions, and calls actions or scripts on the server. There are three kinds of custom view. A React view gets the controller of its own projection — the [members](React_view_controller.md#members), beside the `props.data` they mirror — and the form's controller as its `form`; a classic view finds the form's controller as `controller.form`, and an `INTERNAL CLIENT` action is given it as an argument. The form's controller carries the server calls (`exec` / `eval` / `evalAction` / `change`), the [methods](#across-containers) that do what a view's members do to what another container or the platform draws (`changeOrders('i', …)`, `changeObject('i', row)`, …), the [editing declaration](#editing) (`startEditing` / `stopEditing` / `isEditing`) and the [batch](#batch).

Properties and actions are addressed by their integration name — the name on the form (or the alias / `NEW` / `DELETE` integration name of a button), the same name the [external JSON/REST API](../paradigm/Access_from_an_external_system.md#form) uses. Unless an `EXTID` gives it one of its own, that name is the property's name on the form with everything from `(` cut off, so `f(a)` and `f(b)` drawn on one group are both `f`: an integration name is not, by itself, unique.

That is the name a view has, everywhere: `props.data` is keyed by it and every controller member is named by it. The form's own operators — `FILTER`, `ORDER` and the `READ` that writes their state back out — speak the other name, the property's SID on the form, in both directions. So a name a view was handed is not a name one of those operators takes: convert where the two meet, in the `lsf` code that calls the operator, rather than expecting either side to accept both.

Optional arguments are bracketed.

### How a view gets its controller {#how-a-view-gets-its-controller}

| view | declared | JavaScript entry point | its controller is |
| --- | --- | --- | --- |
| [React view](React_views.md) | `DESIGN c { custom = 'Name'; }` | a component taking `props` (`data`, `controller`) | `props.controller` |
| [React component in CUSTOM](React_component_views.md#form-custom) | `OBJECTS g = Cls CUSTOM 'Name'` or `PROPERTIES p CUSTOM 'Name'` | a component registered with the kind `reactView`, taking `props` (`data`, `controller`) | `props.controller.form` |
| [Navigator window](React_windows.md#navigator-window) | `EXTEND WINDOW System.toolbar CUSTOM 'Name';` | a component taking `props` (`data`, `controller`) | `props.controller` — the [navigator controller](#navigator-controller) |
| [Forms window](React_windows.md#forms-window) | `EXTEND WINDOW System.forms CUSTOM 'Name';` | a component taking `props` (`data`, `controller`) | `props.controller` — the [navigator controller](#navigator-controller)'s methods plus `select`, `close`, `closeAll`, `setEditMode`, `toggleFullScreen` |
| [Log window](React_windows.md#log-window) | `EXTEND WINDOW System.log CUSTOM 'Name';` | a component taking `props` (`data`, `controller`) | `props.controller` — the [navigator controller](#navigator-controller)'s methods plus `togglePin` |
| [Window layout](React_windows.md#window-layout) | `NAVIGATOR CUSTOM 'Name';` | a component taking `props` (`data`, `controller`) | `props.controller` — the [navigator controller](#navigator-controller) |
| Classic CUSTOM object group | `OBJECTS g = Cls CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |
| Classic CUSTOM property cell | `PROPERTIES p CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |

In a form's React view, requests through controller members or `controller.form` return `Promise`s. The window operations `select`, `close`, `closeAll`, `setEditMode`, `toggleFullScreen`, `togglePin` and `activate` return nothing.

A **React view** renders an entire custom container, so the `controller` in its `props` is that container's own controller — its [members](React_view_controller.md#members) are called on it directly, and the form's controller is its `form`, `props.controller.form`.

A **CUSTOM object group** and a **CUSTOM property cell** are rendered by classic `render(element, controller)` / `update(element, controller, ...)` callbacks — `update` also receives the group's `list` of rows, or the cell's `value`. The `controller` they receive is a *local* controller scoped to that one group or cell: it adds the helpers those views need — value, current-row and styling getters and `diff` / `clearDiff` for an object group, the `change` edit event for a property cell. The local controller exposes the form controller as `controller.form`, so the form-level methods below are reached through it:

```js
const total = await controller.form.exec('recalc', orderId);
await controller.form.change('customerOrder', orderId, customerId);
```

Rows a classic view receives carry the same `key` and `objects` as React rows (see [Row identity](#row-identity-contract)). An object group's local `changeProperty` also *delegates*: for a property that is not one of its own columns it is passed on to the form's `changeProperty` and resolved form-wide, so the view can change a property it does not display. The form controller's `change` takes no rows: it names a property, then the values of its parameters and the new value.

An [`INTERNAL CLIENT`](INTERNAL_operator.md) action is a fourth entry point: its bound JavaScript function receives the form controller as the argument after the call parameters. The custom value editor's controller (`CHANGE`) also exposes the form controller as its `form` field.

### The form controller's methods {#the-form-controller}

| method | what it does | returns |
| --- | --- | --- |
| `form.exec(action, ...params)` | run a named action | `Promise` |
| `form.eval(script, ...params)` | run an lsf script with a typed `run` | `Promise` |
| `form.evalAction(script, ...params)` | run an action body (`$1`, `$2`, … params) | `Promise` |
| `form.mcp` | `exec`, `eval` and `evalAction` that make their call as the MCP server's eval tool makes one: the parameters are bound as the tool's `params`, and the call resolves with what the tool answers | the three calls |
| `form.change(property, ...keyParams, value)` | set a global property | `Promise` |
| `form.startEditing(element)` | declare that the user is editing in `element` | — |
| `form.stopEditing(element)` | declare that they are not — only if `element` is the declared one | — |
| `form.isEditing(element)` | whether that declaration stands | `boolean` |
| `form.changeObject(group, row)`, `form.changeOrders(table, …)`, … | what the members do, to what another container or the platform draws, named by strings — [across containers](#across-containers) | as the member |

### Calling the server {#calling-the-server}

The form's `exec`, `eval`, `evalAction` and `change` — `controller.form` in a React view — each run on the server and return a `Promise`. They are subject to the same authorization gate and convert the result to a JS value the same way as a classic view's server calls.

- `exec(action, ...params)` — runs a named action. Resolves to its `RETURN` value. `action` is the action's name — an entry of the form's `CUSTOMS` by the name it has there, or an action of the project by its [name](IDs.md), with the namespace and the signature in brackets when they are needed to pick one: `runReport[Order]` among overloaded actions. Not the `saveFilters()` a script would write: that shape answers "Action was not found".
- `eval(script, ...params)` — runs an lsf script that defines its own `run` action (typed parameters).
- `evalAction(script, ...params)` — runs an action body wrapped into a `run` action, with parameters referenced as `$1`, `$2`, ….
- `mcp` — `exec`, `eval` and `evalAction` that make their call as the MCP server's eval tool makes one: `controller.form.mcp.eval(script, ...params)`. The parameters are bound as the tool binds its `params`, and the call resolves with the object the tool answers with, `{results, status}`.
- `change(property, ...keyParams, value)` — changes a global property. The last argument is the value, the preceding ones are the keys. When the property's value is an object, the value is its id, and the platform assigns the object with that id — the object picker opens only for interactive editing.

Parameters are passed as plain JS values (a number, string, boolean, `Date`, or an object/array for a `JSON` parameter); a file — as the string of its text or as the object `{base64, fileName, extension}`, with `text` in place of `base64` for a text file. An lsFusion object is passed as its numeric id. When an action parameter is typed by a class, the platform resolves the id to the object of that class — no manual lookup is needed. A row handle is not an object reference here: for a class-typed parameter the call fails, so pass the id.

That id is the `row.key` of the object's row (see [Row identity](#row-identity-contract)) or an id-valued property on the form. A key read out of `data.<group>.byKey` or `data.<group>.keys` is a *string*, because keys of a JS object are always strings, and it can be passed as it is: a string given for a numeric or class-typed parameter is parsed as a number, an empty string gives `NULL`, and a string that is not a number is rejected.

```js
const total = await controller.form.exec('recalc', orderId);
const doubled = await controller.form.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.form.change('archived', orderId, true);
```

A call made after the form has been closed *rejects* with a `Form is closed` error — it never hangs — so an `await` on a closed form lands in the `catch` branch. The form counts as closed once the server has closed it. A call made while the close is still being processed — the client has taken the form off the page as an action that may close it was clicked, and the server has not answered yet — is sent and runs after the close's request: if the server refuses the close, the form comes back with what the call did, so input a view commits as the form closes is not lost.

### Changing what another container draws {#across-containers}

What another container draws, or the platform, a view changes through the form's controller, `controller.form`, by names, as strings, checked when the call is made. Each of its methods does what a view's member does — the same arguments after the names, the same checks and refusals, a refusal quoting the form's call (`controller.form.changeOrders('i'): order 0: unknown property 'x' of object group 'i'`) — to the form's state, whoever draws it: a container that draws the part shows the change before the call returns, as a call of its own member would, and the platform's table, tree, toolbar or filter panel shows it as it shows the form's own edits. A call made in a [batch](#batch) goes with it; one that asks the server is refused there, as a member's is. Each returns what its member returns.

| method | does what this member does | named by |
| --- | --- | --- |
| `changeObject(group, row[, {selection}])` | `<group>.change(row[, {selection}])` | a group's SID |
| `changeColumn(group, column[, {selection}])` / `selectAll(group)` | `<group>.properties.change(column[, {selection}])` / `<group>.selectAll()` — the [selection](React_trees_sortings_selection.md#selection) of the rows another container draws, or of the platform's table | the SID of a list group not in a tree |
| `copy(group)` / `paste(group, text)` | `<group>.copy()` / `<group>.paste(text)` — the [clipboard](React_trees_sortings_selection.md#clipboard) of the rows another container draws, or of the platform's table | the SID of a list group not in a tree |
| `expand(group, row)` / `collapse(group, row)` / `expandAll(group)` / `collapseAll(group)` | `<group>.expand(row)` / `.collapse(row)` / `.expandAll()` / `.collapseAll()` | the SID of a group of a tree |
| `expandCurrent(tree)` / `expandTree(tree)` | `<tree>.expandCurrent()` / its toolbar's `expandTree()` | a tree's name |
| `changeOrders(table, order)` / `changeOrders(table, orders)` | `<table>.orders.change(…)` | a list group's SID, or a tree's name |
| `changeFilters(table, condition)` / `changeFilters(table, conditions)` | `<table>.filters.change(…)` ([user filters](React_filters_and_toolbar.md#filters)) | a list group's SID, or a tree's name |
| `changeFilter(property, value[, options])` | `<group>.filter.<property>.change(value[, options])` — the condition of the property's [filter](React_filters_and_toolbar.md#property-filters) | `'<group>.<property>'` |
| `addFilter(table)` / `applyFilters(table)` / `resetFilters(table)` | the [filter controls](React_filters_and_toolbar.md#filter-controls)' `addFilter()` / `applyFilters()` / `resetFilters()` | a list group's SID, or a tree's name |
| `changeFilterControlsVisible(table, shown)` | the [toolbar](React_filters_and_toolbar.md#toolbar)'s `filterControlsVisible.change(shown)` | a list group's SID, or a tree's name |
| `changeFilterGroup(filterGroup, index)` | `<group>.<filterGroup>.change(index)` | the filter group's name |
| `changeViewFilters(group, conditions)` | `<group>.viewFilters.change(conditions)` — the [view filters](React_filters_and_toolbar.md#view-filters) of the rows another container draws; refused where the platform draws them | the SID of a list group not in a tree |
| `getCount(group)` / `getSum(group, property)` | `<group>.getCount()` / `.getSum(property)` — a `Promise` | a list group's SID |
| `changeManual(group, on)` / `update(group)` / `changeListView(group, name)` | `<group>.manual.change(on)` / `.update()` / `.listView.change(name)` | a list group's SID |
| `export(table)` | `<table>.export()` | a list group's SID, or a tree's name |
| `changeProperty(property[, row][, value])` | the classic `changeProperty`, its arguments read the same way: the value set, or the change event run, on the current object or on the row given | `'<group>.<property>'`, a property of the empty group by its name |
| `getValues(property, value[, {row, mode, count}])` | `<group>.<property>.getValues(value[, options])`, the [suggestion list](#looking-up-values) — a `Promise`; the row a data row or its `objects` handle, none for the current object, and none for a property of the empty group, which has no rows; a property the form hides is asked all the same, as `changeProperty` changes it | the same |

`changeFilter` is refused if the design removes the property's `FILTER` component or a container holding it. `changeFilters` can still state a condition on that property.

A row is named by what a view has of it: a data row, or its `objects` handle — of a row of its own data, or of a lookup's result (`getValues` in the `objects` mode). A key names a row only in the view that draws the rows, through its member, so the form's controller takes none. A group of a tree has no sortings, user filters or toolbar of its own: they are the tree's, named by the tree's name, each property in its sortings and conditions as `<group>.<property>`. The filter controls' commands go to the table's filter input, whoever draws the conditions and wherever the controls are, if anywhere; where the platform makes none — it draws the conditions, and its toolbar turns its filters off —, the apply and the reset go to the table's user filters themselves, and the add, with no row to add to, is refused. `changeFilterControlsVisible` works where the toolbar's button shows and hides the platform's controls. A current object chosen where the platform draws the rows is the form's current one at once — what the form is asked next is asked of that row —, and the platform's table moves to it with the server's answer.

```js
// a side panel sorting, filtering and choosing in the rows another container draws
controller.form.changeOrders('i', {property: 'price', desc: true});
controller.form.changeFilter('i.price', 100, {compare: '>='});
controller.form.changeFilterGroup('stock', 0);
controller.form.changeObject('i', found.objects);   // a row found by a lookup
```

### Looking up values {#looking-up-values}

`getValues` asks the server for a capped suggestion list for a property — a suggestion list, not a full `SELECT DISTINCT` — and returns the `Promise` of the answer, `{ data: [ { displayString, rawString, objects }, ... ] }`. A query too short for the server to search rejects it with the message the platform's editor shows then — "String too short to search", in the user's language —, as `form.exec` and `form.eval` reject with the server's message. An answer the server gave before it was sure the form's unsaved changes do not affect it carries `more` too — the `Promise` of the final answer.

```js
controller.<group>.<property>.getValues(value[, {row, mode, count}]);
controller.form.getValues('<group>.<property>', value[, {row, mode, count}]);   // the same, by the property's name
```

- `value` — the typed query to match against, a string.
- `row` — an optional row that scopes the lookup to that row: the member of a list property takes a data row, a raw handle or the row's key; the form's controller a data row or its `objects` handle of the named group, not a key. Omit it for the current object; a property of the empty group takes none. The form's controller asks for a property the form hides too, as `changeProperty` changes it, where a member refuses while its property is hidden.
- `mode` — one of:

  | `mode` | result | `item.objects` |
  | --- | --- | --- |
  | `'objects'` (default) | the matching `OBJECTS` for the property — an object picker | a raw `objects` handle for that object |
  | `'values'` | the distinct values of the property | `null` (use `displayString` / `rawString`) |
  | `'change'` | the property's edit-time suggestions | depends on the property |

  `'change'` reflects how the property is *edited* — a custom `INPUT` list, a `notNull` constraint, or a custom change action — rather than the distinct values already present. For a property that cannot be changed in this context (for example, a read-only property, or a calculated property without a change action) there are no such suggestions, and the `Promise` is rejected, whereas `'values'` still returns its distinct values.

- `count` — raises the number of items requested, for paging.

A query that is not a string, an option the call does not know, or one whose value is none of the above throws at the call. The server looks a form's values up one lookup at a time: one it is still running when a later one comes is cancelled, its `Promise` rejected with an `Error` whose `cancelled` is `true`. An answer can still come after a later lookup's, so a view that looks values up as the user types checks that an answer is for the query it shows.

Pass `item.objects` from an `'objects'` result straight back as a row, to act on the picked object: to the group's `.change`, to make it the current object, or as the `row` of a list property's `.change(value, row)` or `.exec(row)`. It is never a value: a handle passed as an FK value silently sets it to `NULL` (see [Value formats](#value-formats)), and an FK is set by the object's id.

### Batch {#batch}

`batch(fn)` runs `fn`, and sends the changes the members make in it together, when `fn` returns. A call in a batch is made when it is made, as outside one — read, checked and shown at once, so it reads the state the calls before it left: two sortings stated one at a time, or two toggles of one node, go on from each other. Only the requests wait: when `fn` returns, the members' changes all go in ONE request — the server makes them in the order the calls were made, property values, a group's current object and its selected rows and columns, its sortings, its user filters and view filters, its update mode and the view its rows are shown as, the filters selected in its filter groups, a tree's nodes alike, and answers once, reading the form again once for all of them. A change of the user filters that the [manual apply mode](React_filters_and_toolbar.md#filter-controls) holds back waits for the apply, a batch's too. An action run with `exec()` inside a batch runs as part of that request, which, like a property change, does not hold up the user. Inside `fn`, call members and the form's controller's methods only: moving the focus, changing the DOM or dispatching events to the platform's components there makes the platform send requests of its own — an editor losing the focus saves its value —, which a batch is not made for. A batch carries changes only: inside `fn`, `getValues` is refused, and so is a server call of the form's controller (`form.exec`, `form.eval`, `form.evalAction`, `form.change`), their promise rejected, and so are a toolbar's calls that ask the server — `getCount`, `getSum`, `export` — a tree's too — and a group's `copy` and `paste`, their `Promise` rejected with the reason —, and the form's controller's methods that stand for them; make them before the batch or after it. `filterControlsVisible.change` shows or hides the controls at once. The [filter controls](React_filters_and_toolbar.md#filter-controls)' `addFilter`, `applyFilters` and `resetFilters` go with the batch, as `filters.change` does, and a tree's `expandCurrent` — and over the platform's tree its toolbar's `expandTree` — open its nodes, going with the batch as `expandAll` does: the row a `change(row)` made current earlier in the batch is the node `expandCurrent` opens. The editor of a condition `filterControlsVisible.change` starts on the platform's empty panel, or `addFilter` on its panel, opens, and takes the focus, once the batch is sent. `props.data` does not change inside `fn`: what the batch changes shows when it ends.

A mistake in the arguments throws where it is made, in a batch and on a closed form too, and a throw inside `fn` — a refused call or the view's own — ends the batch: what was made before the throw is sent, as it would have gone outside a batch, and the throw reaches the view. A batch is the form's, whichever controller begins it — a React view's `batch`, the form's `form.batch`, a classic view's: a batch begun inside another is a part of it, sent when the outer one ends — what it made before a throw too —, and a member of another view, or a method of the form's controller, called in `fn` goes with it. Before `fn` runs, the form sends, in a request of its own, the row a user moved to in a table or a tree the platform draws, which the form sends a moment later otherwise — of every group of the form, so that it is sent whether the batch's request fails or not.

`fn` is run at once: a function returning a Promise — an `async` one, whose calls after an `await` would come when the batch is over — is refused, what it made before the `await` sent; await first, then begin the batch. The batch's own `Promise`, which a call made in `fn` returns, is not refused, so `fn` can be written as that call: `controller.batch(() => controller.o.note.change('checked'))`.

A batch is no transaction either: its changes are made in the form's session, as a user's edits are, and on the server one after another — a change that fails stops the rest, those made before it staying in the session; they reach the database when the session is applied.

When a change fails, the property values the batch changed are shown as they were before the batch, and those the server made before the failing change come with its next answer. The rest of what the batch changed — a group's current object, its current column and selected columns, its sortings, user filters and view filters, its update mode and the view its rows are shown as, the filters selected in its filter groups, the nodes of a tree it opened or closed — stays as the batch showed it, whether the server made it or not: the server sends these only when it changes them itself, so after a failure set them again where it matters — the user filters as another list than the batch's, the update mode and the view by way of another one: what the batch set is taken for what was sent, and is not sent again.

`batch(fn)` returns the `Promise` of its one request, and a mutating member's call made in `fn` returns that same `Promise`, so a failure rejects one `Promise`, not one per call. A batch that makes no request resolves at once. On a closed form it is rejected with `Form is closed`, and `fn` is not run.

`batch(fn)` sends several changes together. Each is a member's own call, as it is outside a batch:

```js
await controller.batch(() => {
    controller.o.note.change('checked');
    controller.o.qty.change(5, row);
});   // the Promise of the one request: both changes made and answered
```

### Value formats {#value-formats}

A property's `.change` behaves the same way in a batch and out of one, and the format depends on what is set as the value:

| value | how it is passed |
| --- | --- |
| a primitive | directly: a number for numeric types, a string, a JS `Date` or its ISO string for `DATE` / `TIME` / `DATETIME` / `ZDATETIME`, a boolean, `null` to clear |
| an interval, a `COLOR` | in the same string format [received by the view](React_projection.md#values): `<from>.<to>` with the bounds in milliseconds, `#rrggbb`. A value of any other shape is refused |
| `JSON` | a JS object or array, serialized as JSON |
| an object (FK value) | the target object's id — `row.key` of its row (for a single-object group it already is its numeric id), or an id-valued property on the form (e.g. `LONG(obj)`) — not a handle |

:::info
Passing a handle (`otherRow.objects`) as an FK value silently sets it to `NULL`, with no error. A handle is only for the row argument of a member's call and for `<group>.change`. To set an FK, pass the target object's id.
:::

The format is the same in the read direction: an object property's value arrives in the data row as this same numeric id, so it can be compared with the target row's `row.key` (in a single-object group) or passed back as an FK value without conversion.

```js
// move an object to another parent and edit a primitive together:
// the FK value is the target object's id (row.key of the target row), the primitive value is passed directly
controller.batch(() => {
    controller.i.parent.change(targetColumn.key, item);
    controller.i.value.change(5, item);
});
```

### Declaring that the user is editing {#editing}

While a form is editing, a binding whose `editing` scope is the default does not fire for an event that came from inside the element being edited: `ENTER` does not move the focus to the next component, `ESCAPE` does not close a modal window, and a shortcut bound with [`CHANGEKEY`](Property_options.md) does not run. A binding declared with `editing=all` fires regardless.

The form knows the editors it created itself. A field a view drew for itself it does not know, so the view declares it, on the form's controller:

- `startEditing(element)` — from now on the user is editing in `element`;
- `stopEditing(element)` — the user is no longer editing in `element`, and the declaration is cleared only when `element` is the declared one;
- `isEditing(element)` — whether `element` is the declared one.

There is one declaration per form: declaring another element replaces the previous one. While the form's own editor is open it takes precedence over the declaration, which is read again once that editing ends.

While the declaration stands, the bindings above do not fire for events from `element` or from anything inside it. A view that declares nothing keeps the behaviour it had before: `ENTER` moves the focus to the next component and `ESCAPE` closes the window, from a field the view drew as from anywhere else.

A property's own editor starts editing on the first character typed, not on focus.

Declaring on a wrapper covers every field inside it — including a nested [child the platform draws](React_platform_components.md#placing) and the editor the platform renders there. Declare on a narrower element when that is not wanted.

Removing the declaration is the view's to do, and not only on blur: clear it when the element is removed or the view unmounts. A declaration left behind keeps those bindings from firing for events inside that element for as long as the form is open.

The declaration does not start property editing: there is no value to commit or to cancel, and `ENTER` saves nothing. To edit a *property* from the view, place the property with [`<Platform name/>`](React_platform_components.md#placing) — the platform renders its own editor there, and nothing is declared.

A single key the view needs regardless of the editing state — a shortcut of its own — is a different matter: stop the event in the handler (`stopPropagation`), and the form does not receive it.

### The navigator controller {#navigator-controller}

An [`INTERNAL CLIENT`](INTERNAL_operator.md) action placed in [`NAVIGATOR`](NAVIGATOR_statement.md) receives a controller as well, but a navigator one: there is no form, so it has none of the form methods above. It has the four server calls — `exec`, `eval`, `evalAction`, `change`, and `options` for the first three — which run in the navigator's own session, a new one per call that is never applied, so a `change` made here is not kept. Each returns a `Promise`. And it has `activate`:

- `activate(canonicalName[, event])` — does what clicking that navigator element does: selects the folder, or runs the action, opening its form the same optimistic way. `canonicalName` is the element's [canonical name](IDs.md). `event` is the event of the click, React's own or the browser's, and can be omitted when activating from code that has none.

The React components of a [navigator window](React_windows.md#navigator-window), the [forms window](React_windows.md#forms-window), the [log window](React_windows.md#log-window) and the [window layout](React_windows.md#window-layout) get all five methods directly on `props.controller`. The forms window and the log also keep their own window operations.

Activating an element that does not exist, or one hidden by a `SHOWIF` — its own or that of a folder above it — throws. `activate` returns nothing and reports no completion, just as a click does not.

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
- the **key** the projection gave the row — `row.key`, or a key out of `data.<group>.keys` / `byKey`. A row is taken only where its group's rows are drawn, so there is always this view's `byKey` to look the key up in: it is looked up among the rows of the member's own group, and a key naming no row there fails with an error. This is what lets a view hand back what it was handed: `byKey` is a JS object, and `Object.keys()` on it yields strings, so a key that has been through it no longer carries the type `row.key` was written with. A classic view has no such index, so its `changeProperty` takes the row or its handle.

For a single-object group of a custom class the value of `row.key` numerically equals that object's id, so it can be passed as the target object's id to a server call or as an FK value — no separate property for the row's own id is needed.

If an explicit row argument is none of these, the platform does not silently fall back to the current row: the call fails with an error naming what was expected rather than acting on another row. Nor does it accept a row of a *different* group: `controller.<group>.change(row)` and a list property member's `change(value, row)` / `exec(row)` both say whose row is meant — the member's group — and a row of another group carries only that group's objects, so the rest would be taken from the current ones and the call would land on a row of this group nobody named. Inside a tree a row of a group BELOW is not another group's row in this sense: its key is the whole path down to it, so it names one row of every group above it, and it is accepted there — exactly as `<group>.change` accepts it.
