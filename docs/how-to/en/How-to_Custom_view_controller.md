---
slug: "/How-to_Custom_view_controller"
title: 'How-to: Custom view controller API'
---

A custom view written in JavaScript communicates with the form through a *controller* object — with it the view sets the current object of a group, changes property values, looks up suggestions, and calls actions or scripts on the server. There are three kinds of custom view. A React view gets the controller of its own projection — the members below, beside the `props.data` they mirror — and the form's controller as its `form`; a classic view finds the form's controller as `controller.form`, and an `INTERNAL CLIENT` action is given it as an argument. The form's controller carries the server calls (`exec` / `eval` / `evalAction` / `change`), the [methods](#across-containers) that do what a view's members do to what another container or the platform draws (`changeOrders('i', …)`, `changeObject('i', row)`, …), the [editing declaration](#editing) (`startEditing` / `stopEditing` / `isEditing`) and the [batch](#batch).

Properties and actions are addressed by their integration name — the name on the form (or the alias / `NEW` / `DELETE` integration name of a button), the same name the [external JSON/REST API](How-to_Integration.md) uses. Unless an `EXTID` gives it one of its own, that name is the property's name on the form with everything from `(` cut off, so `f(a)` and `f(b)` drawn on one group are both `f`: an integration name is not, by itself, unique.

That is the name a view has, everywhere: `props.data` is keyed by it and every controller member is named by it. The form's own operators — `FILTER`, `ORDER` and the `READ` that writes their state back out — speak the other name, the property's SID on the form, in both directions. So a name a view was handed is not a name one of those operators takes: convert where the two meet, in the `lsf` code that calls the operator, rather than expecting either side to accept both.

### How a view gets its controller

| view | declared | JavaScript entry point | its controller is |
| --- | --- | --- | --- |
| [React view](How-to_Custom_React_views.md) | `DESIGN c { custom = 'Name'; }` | a component taking `props` (`data`, `controller`) | `props.controller` |
| [CUSTOM object group](How-to_Custom_components_objects.md) | `OBJECTS g = Cls CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |
| [CUSTOM property cell](How-to_Custom_components_properties.md) | `PROPERTIES p CUSTOM 'name'` | `render` / `update` callbacks | `controller.form` |

A **React view** renders an entire custom container, so the `controller` in its `props` is that container's own controller — its members below are called on it directly, and the form's controller is its `form`, `props.controller.form`.

A **CUSTOM object group** and a **CUSTOM property cell** are rendered by classic `render(element, controller)` / `update(element, controller, ...)` callbacks — `update` also receives the group's `list` of rows, or the cell's `value`. The `controller` they receive is a *local* controller scoped to that one group or cell: it adds the helpers those views need — value, current-row and styling getters and `diff` / `clearDiff` for an object group, the `change` edit event for a property cell — documented in [Custom components (objects)](How-to_Custom_components_objects.md) and [Custom components (properties)](How-to_Custom_components_properties.md). The local controller exposes the form controller as `controller.form`, so the form-level methods below are reached through it:

```js
const total = await controller.form.exec('recalc', orderId);
await controller.form.change('customerOrder', orderId, customerId);
```

Rows a classic view receives carry the same `key` and `objects` as React rows (see [Row identity](#row-identity-contract)). An object group's local `changeProperty` also *delegates*: for a property that is not one of its own columns it is passed on to the form's `changeProperty` and resolved form-wide, so the view can change a property it does not display. The form controller's `change` takes no rows: it names a property, then the values of its parameters and the new value.

An [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action is a fourth entry point: its bound JavaScript function receives the form controller as the argument after the call parameters. The [custom value editor](How-to_Custom_components_properties.md#custom-editor)'s controller (`CHANGE`) also exposes the form controller as its `form` field.

### The form controller

There is one controller **per projection**: a React view's `props.controller` is its own container's, and it names exactly what that container's `props.data` shows. The object a classic view and a custom cell editor are given as `controller.form`, an `INTERNAL CLIENT` action as its argument and a React view as `props.controller.form` is the FORM's: it projects no data, so it has none of the members below — a member says that `data.<group>.<property>` is there —, and what it reaches it is given the name of, as a string, which the platform checks when it is called. A view's controller **mirrors the shape of `props.data`**: its members are the container's entries, made once, when the form opens, each at the same path as its data — what a view reads as `data.o.qty` it changes through `controller.o.qty`, and `data.total` through `controller.total`. Each object group a part of which this view draws is a member of the controller under its group SID, each property whose VALUE this view carries is a member of that group under its integration name, and each property of the empty group (the form level) is a member of the controller itself. A form in which one of these names would stand for two properties of one container is refused when it is built. The member *is* the address — a view's controller has no second, string-addressed set of verbs saying the same thing, and a name typed wrong is a member that does not exist rather than a string the platform has to validate.

A view reads only its own container's parts, and its controller's members change what its `props.data` carries. What another container draws, or the platform, a view changes through the form's controller, by names ([across containers](#across-containers)). What containers share is the form itself — its current objects, sortings, filters and session — so a filter or a current object changed in one container shows in another, as it does in the standard client.

Optional arguments are bracketed.

| member | what it does | returns |
| --- | --- | --- |
| `<group>.change(row)` | set the group's current object — only where the group's rows are drawn | `Promise` |
| `<group>.orders.change(order)` | change one of the group's [sortings](How-to_Custom_React_views.md#orders), leaving the rest and their priority — only where the group's rows are drawn. A tree's are `<tree>.orders`, on the tree's node where its rows are drawn, each property named `<group>.<property>` | `Promise` |
| `<group>.orders.change(orders)` | an array states them all, its order being the priority | `Promise` |
| `<group>.<property>.change(value[, row])` | set the value — on the current object, or on a given row | `Promise` |
| `<group>.<property>.exec([row])` | run the property's change event, or the action — on the current object, or on a given row | `Promise` |
| `<group>.<property>.getValues(value[, {row, mode, count}])` | a capped server suggestion list | `Promise` |
| `<property>.change(value)` / `.exec()` / `.getValues(...)` | the same, for a property of the empty group — with no row | `Promise` |
| `<group>.expand(row)` / `.collapse(row)` / `.toggle(row)` | open or close one node of a [tree](How-to_Custom_React_views.md#trees) — a node is a row of a group; only where the tree's rows are drawn | `Promise` |
| `<group>.expandAll()` / `.collapseAll()` | open every node of the group and of the groups below it — the whole tree on its top group — or close them; where the tree's rows are drawn | `Promise` |
| `<group>.filters.change(condition)` | change one of the group's user filter conditions, leaving the rest: the first on its property, what it does not state kept, as a property's filter does — where `FILTERS(<group>)` is. A tree's are `<tree>.filters`, where `FILTERS(TREE <tree>)` is, each property named `<group>.<property>` ([Filters](How-to_Custom_React_views.md#filters)) | `Promise` |
| `<group>.filters.change(conditions)` | an array states all the conditions it can name: replace the condition list — one on a property it names none by keeps its place —, a condition with no value comparing with `NULL` | `Promise` |
| `<group>.addFilter()` / `.applyFilters()` / `.resetFilters()` | the [filter controls](How-to_Custom_React_views.md#filter-controls)' add, apply and reset, as the platform's buttons — where `FILTERCONTROLS(<group>)` is. A tree's are `<tree>.addFilter()` and so on, where `FILTERCONTROLS(TREE <tree>)` is | `Promise` |
| `<group>.<filterGroup>.change(index)` | select one of a [filter group](How-to_Custom_React_views.md#filter-groups)'s filters by its index, `null` for none — where `FILTERGROUP(<filterGroup>)` is | `Promise` |
| `<group>.filter.<property>.change(value[, {compare, negation, or}])` | state the value of a [property's filter](How-to_Custom_React_views.md#property-filters) — in its condition, the first on the property, or, with none, in the filter's own place, with the comparison, the negation and the junction the options state; `null` takes that one off — where `FILTER(<property>)` is drawn | `Promise` |
| `<group>.getCount()` / `.getSum(property)` | the number of the group's rows and the sum of a column of them, of the marked rows only where the user has marked any — where `TOOLBARSYSTEM(<group>)` is, whatever the design's `show…` flags say — its [buttons](How-to_Custom_React_views.md#toolbar) | `Promise` |
| `<group>.export()` / `.update()` / `.manual.change(on)` / `.filterControlsVisible.change(shown)` / `.listView.change(name)` | export the rows, bring the changes the server holds back, turn the manual update on or off, show or hide the platform's filter controls, show the rows in another view type — where `TOOLBARSYSTEM(<group>)` is, whatever the design's `show…` flags say | `Promise` |
| `<tree>.expandCurrent()` | open the [tree](How-to_Custom_React_views.md#trees)'s current node and every node under it — the tree's own, on the tree's node where its rows are drawn; over the platform's tree, its toolbar's, where `TOOLBARSYSTEM(TREE <tree>)` is | `Promise` |
| `<tree>.export()` / `.filterControlsVisible.change(shown)` / `.expandTree()` | export the tree, show or hide the platform's filter controls of the tree — on the tree's node, where `TOOLBARSYSTEM(TREE <tree>)` is ([its toolbar](How-to_Custom_React_views.md#tree-toolbar)); over the platform's tree also open it from the selected node's group down | `Promise` |
| `batch(fn)` | run `fn`, the member changes it makes shown at once and sent together in one request when it returns, in the order made — the form's [batch](#batch), also `form.batch(fn)` | `Promise` |
| `form.exec(action, ...params)` | run a named action | `Promise` |
| `form.eval(script, ...params)` | run an lsf script with a typed `run` | `Promise` |
| `form.evalAction(script, ...params)` | run an action body (`$1`, `$2`, … params) | `Promise` |
| `form.change(property, ...keyParams, value)` | set a global property | `Promise` |
| `form.startEditing(element)` | declare that the user is editing in `element` | — |
| `form.stopEditing(element)` | declare that they are not — only if `element` is the declared one | — |
| `form.isEditing(element)` | whether that declaration stands | `boolean` |
| `form.changeObject(group, row)`, `form.changeOrders(table, …)`, … | what the members do, to what another container or the platform draws, named by strings — [across containers](#across-containers) | as the member |

```js
controller.o.note.change('checked');   // the current row of group `o`
controller.o.sum.change(100, row);     // ... a given row
controller.o.edit.exec(row);           // an action: run it on that row
controller.o.change(row);              // the group's current object
controller.o.orders.change([]);        // its sortings: an array states them all — here, none
controller.o.filters.change({property: 'name', value: 'an'});   // one condition — data.o.filters is where it lands
controller.o.customer.getValues(text, {mode: 'objects'});   // the Promise of its suggestions
controller.total.change(500);          // a property of the empty group
```

A member's `change` and `exec` do not ask the confirmation a property's design asks for (`CONFIRM` on an action, `askConfirm` in the design): the platform's own editor and button ask it, a view's call goes straight to the server. The property's entry says whether to ask, and what — `askConfirm` and `askConfirmMessage` ([display options](How-to_Custom_React_views.md#display-options)) —, so a view that wants the question asks it before it calls.

The members are **what this view can change** - the entries it carries, minus what the PLATFORM draws: a property declared `NOCUSTOM` - or given a `CUSTOM` render function in the `FORM` - has an entry (an ordinary column entry, so the view can draw the column header) and no member, because its value is drawn and edited by its own renderer, which this view places with `<Platform name row/>`, and any other child the platform draws has only the entry that labels it - an action the platform draws, panel or drawn per row, only its entry, with no caption and no image - they are its button's face; the platform's button runs it. So a group is a member of the controller of every container that has `data.<group>` - an empty one where the container only places what the platform draws of the group -, and a property is a member there when THAT container carries its value: a grid property where the rows are drawn, a panel property where the property itself stands. A member is there as long as its entry is — from the start, `hidden` or not: a `SHOWIF` that hides the property leaves the member in place, and a call through it is refused while the entry is `hidden`, with an error saying the property is not shown by the form now. A property's change event is the one a user fires by editing the cell, so a property the form is not showing has nobody to fire it for. `<group>.change(row)` is there only where the group's rows are drawn — the current object is chosen among them — and so is `orders.change(...)` — the group's, or a tree's on the tree's node —, since a sorting is the order those rows are drawn in; and a row is taken only there: by `<group>.change` itself, and by the member of a list property, which lives where the rows are. The member of a panel property and a member of the empty group take no row: they act on the current objects. What a view cannot see it has no member for either: the rest of the form is reached through the form's controller, `form` — its [methods across containers](#across-containers), and `change` / `exec` / `eval` / `evalAction`, which take lsf names and code rather than projection names. Where the state a view keeps names a property — a sorting, a user filter's condition — it names it by its integration name, not by a member: a state is data, written in the shape `props.data` publishes it, and it may name a property no member stands for, such as a column the platform draws. A sorting is `{property: 'sum', desc: true}` and a condition `{property: 'sum', compare: '>', value: 100}`, and either name is looked up among the group's properties, wherever they are drawn: a group's state is the group's, not one view's — `FILTERS(<group>)` need not be anywhere near a column, and the rows need not be drawn beside a property they are sorted by ([Sortings](How-to_Custom_React_views.md#orders), [Filters](How-to_Custom_React_views.md#filters)). A call, by contrast, names what it changes by its member. A property grouped in columns is a member nowhere, for the same reason it is not projected: its values are addressed by a row-and-column key. (The classic `changeProperty` — a CUSTOM object group's, and the form's controller's, `form.changeProperty` — is another surface, older than this one, and goes on naming the whole form.) The mutating members — a property's `change` and `exec`, and those that change a group's current object, sortings, user filters or filter group, or a tree's nodes, the filter controls' too — return the `Promise` of their request: the new state arrives with the next form update. A toolbar's `export()`, `update()`, `manual.change()` and `listView.change()` also return the `Promise` of their request. The form's server calls (`form.exec` / `form.eval` / `form.evalAction` / `form.change`) return a `Promise`, and so do a property's `getValues()`, which asks the server for suggestions, and a toolbar's `getCount()` and `getSum()` — and the form's controller's `getCount`, `getSum` and `getValues` —, which ask it for a number.

The `Promise` of a mutating member's call is resolved once its request is over — the server has run it to its end, any dialog it opened answered, and the form has taken in its answer — and rejected with an `Error` with the reason the request failed, which the platform shows the user, as it shows a failed edit, so a call whose `Promise` the view does not wait for reports nothing more. After the form has been closed, it is rejected with `Form is closed`; a call made while the close is still being processed is sent — see [Calling the server](#calling-the-server). A property's `change` or `exec` the platform makes no request for — a delete with no row to delete, a closing of the form the user does not confirm — resolves its `Promise` at once outside a batch; when the server fails to give a new object its id, the `Promise` is rejected with the reason, as a failed request's is. Calls on the user filters — including a change the [manual apply mode](How-to_Custom_React_views.md#filter-controls) holds back and an `applyFilters` with nothing waiting — wait for the table's latest filter request in flight. A tree's `expandCurrent` and `expandTree` likewise wait for its latest expansion request in flight. With none in flight they resolve at once, as do other calls that make no request outside a batch.

Running an action through its member — `controller.o.edit.exec(row)`, outside a `batch` — is the same request as clicking it on the form: synchronous, blocking input until it completes and showing the [busy indicator](../paradigm/Interactive_view.md#busy) after its delay, unless the action is drawn with `NOWAIT` (the `syncType` [property option](../language/Property_options.md)). The form's `exec`, `eval`, `evalAction` and `change` go as asynchronous requests. So a refresh on a timer is not made through the controller — see [Live data](How-to_Custom_React_views.md#live-data).

<a className="lsdoc-anchor" id="batch"/>

`batch(fn)` runs `fn`, and sends the changes the members make in it together, when `fn` returns. A call in a batch is made when it is made, as outside one — read, checked and shown at once, so it reads the state the calls before it left: two sortings stated one at a time, or two toggles of one node, go on from each other. Only the requests wait: when `fn` returns, the members' changes all go in ONE request — the server makes them in the order the calls were made, property values, a group's current object, its sortings, its user filters, its update mode and the view its rows are shown as, the filters selected in its filter groups, a tree's nodes alike, and answers once, reading the form again once for all of them. A change of the user filters that the [manual apply mode](How-to_Custom_React_views.md#filter-controls) holds back waits for the apply, a batch's too. An action run with `exec()` inside a batch runs as part of that request, which, like a property change, does not hold up the user. Inside `fn`, call members and the form's controller's methods only: moving the focus, changing the DOM or dispatching events to the platform's components there makes the platform send requests of its own — an editor losing the focus saves its value —, which a batch is not made for. A batch carries changes only: inside `fn`, `getValues` is refused, and so is a server call of the form's controller (`form.exec`, `form.eval`, `form.evalAction`, `form.change`), their promise rejected, and so are a toolbar's calls that ask the server — `getCount`, `getSum`, `export` — a tree's too —, their `Promise` rejected with the reason —, and the form's controller's methods that stand for them; make them before the batch or after it. `filterControlsVisible.change` shows or hides the controls at once. The [filter controls](How-to_Custom_React_views.md#filter-controls)' `addFilter`, `applyFilters` and `resetFilters` go with the batch, as `filters.change` does, and a tree's `expandCurrent` — and over the platform's tree its toolbar's `expandTree` — open its nodes, going with the batch as `expandAll` does: the row a `change(row)` made current earlier in the batch is the node `expandCurrent` opens. The editor of a condition `filterControlsVisible.change` starts on the platform's empty panel, or `addFilter` on its panel, opens, and takes the focus, once the batch is sent. `props.data` does not change inside `fn`: what the batch changes shows when it ends.

A mistake in the arguments throws where it is made, in a batch and on a closed form too, and a throw inside `fn` — a refused call or the view's own — ends the batch: what was made before the throw is sent, as it would have gone outside a batch, and the throw reaches the view. A batch is the form's, whichever controller begins it — a React view's `batch`, the form's `form.batch`, a classic view's: a batch begun inside another is a part of it, sent when the outer one ends — what it made before a throw too —, and a member of another view, or a method of the form's controller, called in `fn` goes with it. Before `fn` runs, the form sends, in a request of its own, the row a user moved to in a table or a tree the platform draws, which the form sends a moment later otherwise — of every group of the form, so that it is sent whether the batch's request fails or not.

`fn` is run at once: a function returning a Promise — an `async` one, whose calls after an `await` would come when the batch is over — is refused, what it made before the `await` sent; await first, then begin the batch. The batch's own `Promise`, which a call made in `fn` returns, is not refused, so `fn` can be written as that call: `controller.batch(() => controller.o.note.change('checked'))`.

A batch is no transaction either: its changes are made in the form's session, as a user's edits are, and on the server one after another — a change that fails stops the rest, those made before it staying in the session; they reach the database when the session is applied.

When a change fails, the property values the batch changed are shown as they were before the batch, and those the server made before the failing change come with its next answer. The rest of what the batch changed — a group's current object, its sortings and user filters, its update mode and the view its rows are shown as, the filters selected in its filter groups, the nodes of a tree it opened or closed — stays as the batch showed it, whether the server made it or not: the server sends these only when it changes them itself, so after a failure set them again where it matters — the user filters as another list than the batch's, the update mode and the view by way of another one: what the batch set is taken for what was sent, and is not sent again.

`batch(fn)` returns the `Promise` of its one request, and a mutating member's call made in `fn` returns that same `Promise`, so a failure rejects one `Promise`, not one per call. A batch that makes no request resolves at once. On a closed form it is rejected with `Form is closed`, and `fn` is not run.

A name that is not a JavaScript identifier is addressed with brackets: a property with `EXTID 'unit price'` is `controller.o['unit price'].change(v)`. A group SID is always an identifier — a group of several objects, whose SID would be their names joined with dots, is refused when the form is built until it is named (`OBJECTS pair = (d = X, t = Y)`).

A name that would shadow a member of the surface itself is refused when the form is **built**, the same way [the projection's own reserved names](How-to_Custom_React_views.md#reserved-names) are: a group SID, a tree's name where a view draws its rows or another part of the tree, or a property of the empty group coinciding with what the controller carries beside its members (`form`, `batch`), or with each other — a controller is one namespace for its own projection, so a group and a property of the empty group the same container projects cannot both be `total`, while two different containers may — and a group property named like one of the group's own members in the same container. The fix is to rename it, or give it an `EXTID`. A tree's sortings, its current row and its own `expandCurrent`, where its rows are drawn, and its user filters, toolbar and filter controls are on [the tree's node](How-to_Custom_React_views.md#trees), which carries no property.

The same two groups also differ along two more axes — whether they are gated, and how an object is addressed in them:

| group | methods | gate | how an object is passed |
| --- | --- | --- | --- |
| editing the form | `<group>.change` / `<property>.change` / `<property>.exec`; on the form's controller, `changeObject` / `changeOrders` / … ([across containers](#across-containers)) | none | the target row — a data row (`row`), a raw handle (`row.objects`), or its key (a member's only); an object as a value (FK) — its id |
| calling the server | `exec` / `eval` / `evalAction` / `change` on the form's controller, `form` | `@@api` / admin rights / the form's `CUSTOMS` | an object — its id |

A custom view normally reads state from `props.data` and changes it through the form-edit members — including running an action drawn on the form with `controller.<action>.exec()`. Its members change what the view draws — rows and their current row, sortings, filters, tree nodes and the toolbar. There is no member for a move to the first or last row of the whole list, which the server finds, or for a tab or container the platform draws: place a form action with [`ACTIVATE FIRST` / `LAST`](../language/ACTIVATE_operator.md), [`ACTIVATE TAB`](../language/ACTIVATE_operator.md), [`COLLAPSE CONTAINER`](../language/COLLAPSE_operator.md) or [`EXPAND CONTAINER`](../language/EXPAND_operator.md) in the view's container and run it through its member. The form's server calls (`exec` / `eval` / `evalAction` / `change`) are an escape hatch, used only for what the form does not express — ad-hoc server computation, a global write, or creating an object.

Editing the form goes through the ordinary edit channel and is not gated. The server calls are (see [Calling the server](How-to_Custom_components_objects.md#calling-the-server)). The edited row is addressed by a handle, while any other object — an FK value or an action parameter — is passed as its numeric id (an lsFusion object cannot be passed from JS).

#### Changing the current object and property values

`controller.<group>.change(row)` sets the current object of that group. It is there only in the container that draws the group's rows — another container sets it through the form's controller, `controller.form.changeObject('<group>', row)` ([across containers](#across-containers)) —, and the `row` is a data row of the group, a raw `objects` handle, or the key the projection gave it (see [the identity rules](#row-identity-contract) below).

`controller.<group>.<property>.change(value)` changes the property for the group's current object, and `.change(value, row)` for the given row. The arguments come in this order, and nothing is guessed from them: the value first — it is required, and `null` clears it — then the row, which is optional. `.exec()` runs the property's change event, the one a user fires by editing the cell, on the current object — for an action, it runs the action — and `.exec(row)` does it on the given row. An action has no value to set, so `.change` on one is refused: call `exec`. A row is taken only by the member of a list property, which lives where the group's rows are drawn. The member of a panel property and a member of the empty group take none, and act on the current objects. A [selection](How-to_Custom_React_views.md#select) is changed by what is picked — the forms are given there.

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

A member's call has no cell for the user to enter a value in, so it asks the user for none. Where the platform asks the user for the value before it runs the change handler — the [asynchronous input](../paradigm/Value_request_REQUEST.md) of the default handler, for one — the member's `exec` is refused: the view gives a property's value itself, with `change(value)`, and no member can give an action one. A value the handler requests only as it runs — an input in one branch of it, say — is cancelled, as the user cancels an input, and the handler goes on as it then does; the browser console says why.

`batch(fn)` sends several changes together. Each is a member's own call, as it is outside a batch:

```js
await controller.batch(() => {
    controller.o.note.change('checked');
    controller.o.qty.change(5, row);
});   // the Promise of the one request: both changes made and answered
```

A built-in primitive-class object group — a `DATE` navigator, for instance — is moved to a value by writing the object's value (`controller.<g>.VALUE.change(d)` with a real JS `Date`), by `controller.<g>.change(row)` with a row from `props.data.<g>.list` (which carries the `objects` handle), or, when the group is filtered by a data property, by changing that filter property. A date value goes through a conversion that assumes a JS `Date` (an unchecked cast): a non-`Date` argument — a date-input *string*, a timestamp — throws `getFullYear is not a function`, so pass an actual `Date`, e.g. `new Date(year, month - 1, day)`.

A property's `.change` behaves the same way in a batch and out of one, and the format depends on what is set as the value:

| value | how it is passed |
| --- | --- |
| a primitive | directly: a number for numeric types, a string, a JS `Date` for `DATE` / `TIME` / `DATETIME` / `ZDATETIME`, a boolean, `null` to clear |
| an interval, a `COLOR` | in the same string format [received by the view](How-to_Custom_React_views.md#the-component): `<from>.<to>` with the bounds in milliseconds, `#rrggbb`. A value of any other shape is refused |
| `JSON` | a JS object or array, serialized as JSON |
| an object (FK value) | the target object's id — `row.key` of its row (for a single-object group it already is its numeric id), or an id-valued property on the form (e.g. `LONG(obj)`) — not a handle |

:::info
Passing a handle (`otherRow.objects`) as an FK value silently sets it to `NULL`, with no error. A handle is only for the row argument of a member's call and for `<group>.change`. To set an FK, pass the target object's id.
:::

The format is the same in the read direction: an object property's value arrives in the data row as this same numeric id, so it can be compared with the target row's `row.key` (in a single-object group) or passed back as an FK value without conversion.

Like a user edit in the standard table, the change is made in the form's [change session](../paradigm/Change_sessions.md): the view sees the new value in `props.data` at once, while it reaches the database when the session is applied — with the Save button or an action with the [`APPLY`](../language/APPLY_operator.md) operator. If the edited property is marked with the [`APPLY` option](../language/Properties_and_actions_block.md#options) on the form, its standard change handler applies the change at once (by default, by committing the whole form session), so `.change` needs no separate save. The details are described with the option. For a simple edit from a view — a move, a resize, an in-place value edit — this is preferable to a separate server action. The server action (`controller.form.exec`) stays for what a property change cannot express: creating an object (`NEW`), multi-step logic, opening a form.

```js
// move an object to another parent and edit a primitive together:
// the FK value is the target object's id (row.key of the target row), the primitive value is passed directly
controller.batch(() => {
    controller.i.parent.change(targetColumn.key, item);
    controller.i.value.change(5, item);
});
```

#### Changing what another container draws {#across-containers}

What another container draws, or the platform, a view changes through the form's controller, `controller.form`, by names, as strings, checked when the call is made. Each of its methods does what a view's member does — the same arguments after the names, the same checks and refusals, a refusal quoting the form's call (`controller.form.changeOrders('i'): order 0: unknown property 'x' of object group 'i'`) — to the form's state, whoever draws it: a container that draws the part shows the change before the call returns, as a call of its own member would, and the platform's table, tree, toolbar or filter panel shows it as it shows the form's own edits. A call made in a [batch](#batch) goes with it; one that asks the server is refused there, as a member's is. Each returns what its member returns.

| method | does what this member does | named by |
| --- | --- | --- |
| `changeObject(group, row)` | `<group>.change(row)` | a group's SID |
| `expand(group, row)` / `collapse(group, row)` / `expandAll(group)` / `collapseAll(group)` | `<group>.expand(row)` / `.collapse(row)` / `.expandAll()` / `.collapseAll()` | the SID of a group of a tree |
| `expandCurrent(tree)` / `expandTree(tree)` | `<tree>.expandCurrent()` / its toolbar's `expandTree()` | a tree's name |
| `changeOrders(table, order)` / `changeOrders(table, orders)` | `<table>.orders.change(…)` | a list group's SID, or a tree's name |
| `changeFilters(table, condition)` / `changeFilters(table, conditions)` | `<table>.filters.change(…)` ([user filters](How-to_Custom_React_views.md#filters)) | a list group's SID, or a tree's name |
| `changeFilter(property, value[, options])` | `<group>.filter.<property>.change(value[, options])` — the condition of the property's [filter](How-to_Custom_React_views.md#property-filters) | `'<group>.<property>'` |
| `addFilter(table)` / `applyFilters(table)` / `resetFilters(table)` | the [filter controls](How-to_Custom_React_views.md#filter-controls)' `addFilter()` / `applyFilters()` / `resetFilters()` | a list group's SID, or a tree's name |
| `changeFilterControlsVisible(table, shown)` | the [toolbar](How-to_Custom_React_views.md#toolbar)'s `filterControlsVisible.change(shown)` | a list group's SID, or a tree's name |
| `changeFilterGroup(filterGroup, index)` | `<group>.<filterGroup>.change(index)` | the filter group's name |
| `getCount(group)` / `getSum(group, property)` | `<group>.getCount()` / `.getSum(property)` — a `Promise` | a list group's SID |
| `changeManual(group, on)` / `update(group)` / `changeListView(group, name)` | `<group>.manual.change(on)` / `.update()` / `.listView.change(name)` | a list group's SID |
| `export(table)` | `<table>.export()` | a list group's SID, or a tree's name |
| `changeProperty(property[, row][, value])` | the [classic `changeProperty`](How-to_Custom_components_objects.md#controller-methods), its arguments read the same way: the value set, or the change event run, on the current object or on the row given | `'<group>.<property>'`, a property of the empty group by its name |
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

#### Looking up values {#looking-up-values}

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

A query that is not a string, an option the call does not know, or one whose value is none of the above throws at the call. The server looks a form's values up one lookup at a time: one it is still running when a later one comes is cancelled, its `Promise` rejected with an `Error` whose `cancelled` is `true`. An answer can still come after a later lookup's, so a view that looks values up as the user types checks that an answer is for the query it shows. A view can show the first answer at once, the final one when it comes, and the message of a rejection:

```js
try {
    let result = await controller.i.name.getValues(query);
    show(result.data, result.more != null);
    if (result.more) {
        result = await result.more;
        show(result.data, false);
    }
} catch (e) {
    if (!e.cancelled)
        showHint(e.message);   // e.g. a query too short to search
}
```

Pass `item.objects` from an `'objects'` result straight back as a row, to act on the picked object: to the group's `.change`, to make it the current object, or as the `row` of a list property's `.change(value, row)` or `.exec(row)`. It is never a value: a handle passed as an FK value silently sets it to `NULL` (see above), and an FK is set by the object's id:

```js
const result = await controller.c.customer.getValues(text);
result.data.forEach(item => console.log(item.displayString));

// picking the first suggestion of the final answer as the group's customer
const item = (result.more ? await result.more : result).data[0];
if (item) controller.c.change(item.objects);
```

#### Calling the server

The form's `exec`, `eval`, `evalAction` and `change` — `controller.form` in a React view — each run on the server and return a `Promise`. They are subject to the same authorization gate and convert the result to a JS value the same way as a classic view's server calls — see [Calling the server](How-to_Custom_components_objects.md#calling-the-server) for the gate, parameter binding, and the result-to-JS conversion table. An end-to-end example of these calls from a CUSTOM view is in [How-to: Custom Components (server calls)](How-to_Custom_components_server_calls.md).

- `exec(action, ...params)` — runs a named action. Resolves to its `RETURN` value. `action` is the action's name — an entry of the form's `CUSTOMS` by the name it has there, or an action of the project by its [name](../language/IDs.md), with the namespace and the signature in brackets when they are needed to pick one: `runReport[Order]` among overloaded actions. Not the `saveFilters()` a script would write: that shape answers "Action was not found".
- `eval(script, ...params)` — runs an lsf script that defines its own `run` action (typed parameters).
- `evalAction(script, ...params)` — runs an action body wrapped into a `run` action, with parameters referenced as `$1`, `$2`, ….
- `change(property, ...keyParams, value)` — changes a global property. The last argument is the value, the preceding ones are the keys. When the property's value is an object, the value is its id, and the platform assigns the object with that id — the object picker opens only for interactive editing.

Parameters are passed as plain JS values (a number, string, boolean, `Date`, or an object/array for a `JSON` parameter). An lsFusion object is passed as its numeric id. When an action parameter is typed by a class, the platform resolves the id to the object of that class — no manual lookup is needed. A row handle is not an object reference here: for a class-typed parameter the call fails, so pass the id.

That id is the `row.key` of the object's row (see [Row identity](#row-identity-contract)) or an id-valued property on the form. It has to be a number: a key read out of `data.<group>.byKey` or `data.<group>.keys` is a *string*, because keys of a JS object are always strings, and a string in a class-typed parameter is rejected with `Number is required`.

```js
const total = await controller.form.exec('recalc', orderId);
const doubled = await controller.form.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.form.change('archived', orderId, true);
```

A call made after the form has been closed *rejects* with a `Form is closed` error — it never hangs — so an `await` on a closed form lands in the `catch` branch. The form counts as closed once the server has closed it. A call made while the close is still being processed — the client has taken the form off the page as an action that may close it was clicked, and the server has not answered yet — is sent and runs after the close's request: if the server refuses the close, the form comes back with what the call did, so input a view commits as the form closes is not lost.

#### Declaring that the user is editing {#editing}

While a form is editing, a binding whose `editing` scope is the default does not fire for an event that came from inside the element being edited: `ENTER` does not move the focus to the next component, `ESCAPE` does not close a modal window, and a shortcut bound with [`CHANGEKEY`](../language/Property_options.md) does not run. A binding declared with `editing=all` fires regardless.

The form knows the editors it created itself. A field a view drew for itself it does not know, so the view declares it, on the form's controller:

- `startEditing(element)` — from now on the user is editing in `element`;
- `stopEditing(element)` — the user is no longer editing in `element`, and the declaration is cleared only when `element` is the declared one;
- `isEditing(element)` — whether `element` is the declared one.

There is one declaration per form: declaring another element replaces the previous one. While the form's own editor is open it takes precedence over the declaration, which is read again once that editing ends.

While the declaration stands, the bindings above do not fire for events from `element` or from anything inside it. A view that declares nothing keeps the behaviour it had before: `ENTER` moves the focus to the next component and `ESCAPE` closes the window, from a field the view drew as from anywhere else.

```jsx
function Note({ controller }) {
    return <textarea onFocus={e => controller.form.startEditing(e.target)}
                     onBlur={e => controller.form.stopEditing(e.target)}/>;
}
```

This example declares on focus. A property's own editor starts editing on the first character typed, not on focus.

Declaring on a wrapper covers every field inside it — including a nested [child the platform draws](How-to_Custom_React_views.md#placing) and the editor the platform renders there. Declare on a narrower element when that is not wanted.

Removing the declaration is the view's to do, and not only on blur: clear it when the element is removed or the view unmounts. A declaration left behind keeps those bindings from firing for events inside that element for as long as the form is open.

The declaration does not start property editing: there is no value to commit or to cancel, and `ENTER` saves nothing. To edit a *property* from the view, place the property with [`<Platform name/>`](How-to_Custom_React_views.md#placing) — the platform renders its own editor there, and nothing is declared.

A single key the view needs regardless of the editing state — a shortcut of its own — is a different matter: stop the event in the handler (`stopPropagation`), and the form does not receive it.

### The navigator controller {#navigator-controller}

An [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action placed in [`NAVIGATOR`](../language/NAVIGATOR_statement.md) receives a controller as well, but a navigator one: there is no form, so it has none of the form methods above. It has the four server calls — `exec`, `eval`, `evalAction`, `change` — which run in the navigator's own session, a new one per call that is never applied, so a `change` made here is not kept. And it has `activate`:

- `activate(canonicalName[, event])` — does what clicking that navigator element does: selects the folder, or runs the action, opening its form the same optimistic way. `canonicalName` is the element's [canonical name](../language/IDs.md). `event` is the event of the click, React's own or the browser's, and can be omitted when activating from code that has none.

Activating an element that does not exist, or one hidden by a `SHOWIF` — its own or that of a folder above it — throws. Running an action reports no completion, just as a click does not.

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
