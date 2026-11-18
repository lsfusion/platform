---
slug: "/Render_functions"
title: 'Render functions'
---

A *render function* is a JavaScript function drawing a [custom view](../paradigm/Custom_views.md): instead of the platform, it draws the rows of an object group or the value of a property of a form in the [interactive view](../paradigm/Interactive_view.md). It is named by the `CUSTOM` option of the group in the [`OBJECTS`](Object_blocks.md#objects) block, or of the property in the [`PROPERTIES`](Properties_and_actions_block.md#options) block or among the [property options](Property_options.md) of its declaration. The `custom` attribute of the group's table `GRID(o)` or of the property's component in the [form design](../paradigm/Form_design.md) ([`DESIGN`](DESIGN_statement.md)) names it too. The function returns an object of the functions the platform calls: `render`, `update` and the optional `clear`. These places can also name a React component instead of such a function ([A React component in `CUSTOM`](React_component_views.md#form-custom)). Only the web client calls render functions.

### The render function {#render-function}

For example, the render function `itemCards` of an object group returns an object of two functions, `render` and `update`:
```js
function itemCards() {
    return {
        render: (element, controller) => {
            ...
        },
        update: (element, controller, list) => {
            ...
        }
    }
}
```

It is the function that is registered: the platform itself calls _itemCards()_ and takes _render_ and _update_ from the result, so the object with these functions cannot be registered without the wrapping function. Its name is looked up first in the registry `window.lsfusion.custom`, where the loaded [client JavaScript modules](Client_JavaScript_modules.md#named-exports-and-auto-registration) register their exports, and then among the names on the global `window` object. A name registered with the kind `reactView` is a React component.

The function _render_ takes as input the controller and the element inside which the new elements necessary to display the data are to be created.

For an object group the signature is `render(element, controller, event)`. When the view is installed, it receives the browser event that opened the form if that event is still available, or `null`. The cell of a property receives just `render(element, controller)`.

To update the displayed values, the platform will call the _update_ function each time, and the same _element_ will be passed to it, as in the _render_ function, and the _list_ of objects. For a property, `update` is passed its [value](#property) instead of the list.

The object may also contain the optional `clear` function, which the platform calls when it clears the view of an [object group](#diff) or the cell of a [property](#property-controller). If a group's `clear` throws, the client reports the error and finishes clearing the view.

The older declaration of a group's render function with parameters, `function itemCards(element, list, controller)`, is deprecated since 5.0 and will be removed in 8.0. The platform calls it directly on each update. Its replacement is the parameterless function above, with initialization in `render`, the new list in `update`, and cleanup in `clear`.

### An object group's rows {#object-group}

_list_ receives only the read page, not the whole set of objects: for an object group with the `CUSTOM` view type its default size is 1000 objects. For the view to receive all objects of the group, specify the `PAGESIZE 0` option (read all objects) in the [`OBJECTS`](Object_blocks.md) block.

The names of the object fields correspond to the names of the properties on the form. The property values are converted to JS values in the same way as in the rows of a [React view](React_projection.md#values): for example, values of the date and time classes are passed as `Date`, and `JSON` — as a parsed object. A property whose value is an object arrives as that object's numeric id — the same id an object is passed by in [FK writes and server calls](Form_and_navigator_controllers.md#value-formats). Each object also carries the same `key` and `objects` as a row of a React view ([Row identity](Form_and_navigator_controllers.md#row-identity-contract)).

For a property grouped in columns, a column with a nonempty key has a field named `<property>_<index>`, where `index` is its zero-based position among that property's column keys. A column hidden by `SHOWIF` has no field in the row. The names `key` and `objects` are reserved for row identity.

As its fourth argument, `options`, `update` receives the value of the group's [`OPTIONS`](Object_blocks.md#objects) option (a parsed JSON).

### An object group's controller {#controller-methods}

The methods of the local controller passed to _render_ and _update_, internal helpers aside (optional arguments are bracketed). Here `property` is the integration name of a property added to the form in this object group, and `object` is an object from the _list_:

| method | what it does |
| --- | --- |
| `isCurrent(object)` | whether the object is the group's current one |
| `changeObject(object[, rendered[, element]])` | set the group's current object (see below); given an `element`, show the table's [record](../paradigm/Form_design.md#record) next to it; the `Promise` of its request |
| `changeProperty(property[, object][, value])` | set a property value, or exec an action — on the current object or a given one (see below); the `Promise` of its request |
| `batch(fn)` | run `fn`, the `changeProperty` and view filter calls made in it sent together, in one request; the `Promise` of that request |
| `changeDateProperty(property, object, year, month, day)` | deprecated since 6.0, to be removed in 8.0. Use `controller.changeProperty(property, object, new Date(year, month - 1, day))`. The old method calls an unqualified `changeProperty` and throws a `ReferenceError` if that name is undefined |
| `changeProperties(properties, objects, newValues)` | deprecated since 7.0. Changes one cell per index in the parallel arrays of column names, rows and values and returns the batch's `Promise`. Use `batch(fn)` with one `changeProperty` call per cell. The old method skips unknown columns |
| `getValue(property, object)` | the current property value for the object |
| `getCaption(property)` | the property caption |
| `isPropertyReadOnly(property, object)` | whether the property can be edited: `null` — editable, `false` — read-only |
| `getBackground(property, object)` / `getForeground(property, object)` | the cell background / text color |
| `getGroupObjectBackgroundColor(object)` | the row background color, or `null` if unset. A truthy `object.color` supplies the color as a string and takes precedence. Passing `null` or omitting the object throws a `TypeError` |
| `getGroupObjectForegroundColor([object])` | the row text color, or `null` if unset. Omitting the object or passing `null` selects the current row for this lookup |
| `getDisplayBackgroundColor(color[, isCurrent])` / `getDisplayForegroundColor(color)` | the supplied color (`'#rrggbb'`) adjusted for the current theme, or `null` for `null`. The background method ignores `isCurrent` |
| `getFont(property, object)` | the cell font |
| `getPlaceholder(property, object)`, `getPattern(property, object)`, `getRegexp(property, object)`, `getRegexpMessage(property, object)`, `getTooltip(property, object)`, `getValueTooltip(property, object)` | the values of the property's design attributes of the same name |
| `getCaptionClass(property)`, `getGridClass(property, object)`, `getValueClass(property, object)` | the CSS classes set by the design attributes of the same name |
| `getChangeKey(property, object)` / `getChangeMouse(property, object)` | the key combination / mouse event set on the form to change the property |
| `getPropertyCustomOptions(property, object)` | the value of the property's `OPTIONS` option (a parsed JSON) |
| `getPropertyValues(property, value[, mode], ok[, fail[, count]])` | a server suggestion list for the property's values |
| `getValues(property, value, ok[, fail])` | shorthand for `getPropertyValues(property, value, ok, fail)` in the default `'objects'` mode |
| `getObjects(object)` | the row's `objects` handle. A handle passed directly is returned as is. A bare key or an object without a platform handle gives `null` |
| `createObject(object, objects)` | a shallow copy of `object` with the `key` and `objects` of the supplied row or handle. If `objects` identifies no row, the copy has neither field |
| `getObjectsString(object)` | the string used to match rows in `diff`. For a row or handle it equals `String(row.key)`. For an unresolved object it is `String(object.key)`, and for a primitive or `null` it is `String(object)` |
| `isList()` | `true` for an object group's view |
| `isTabFocusable()` | whether the view's input belongs in the Tab order. Always `true` for a group |
| `diff(list, fnc[, noDiffObjects][, removeFirst])` / `clearDiff()` | compute the list changes (see below) |
| `setBooleanViewFilter(property, pageSize)` | filter the view by the condition "the property value is true"; the `Promise` of its request |
| `setDateIntervalViewFilter(startProperty, endProperty, pageSize, start, end)` | filter the view by a date interval; the `Promise` of its request |
| `getColorThemeName()` | the current color theme name: `'LIGHT'` or `'DARK'` |
| `form` | the [form controller](Form_and_navigator_controllers.md) |

When the second parameter of `changeObject` (_rendered_) is not specified, it is assumed to be _false_, which means that the server must eventually call the _update_ function with a new list of objects (probably the same one). Pass `rendered = true` when the view has already drawn the new current object. An explicit non-null object that cannot be resolved leaves the current object in place. In this case, as when the object is already current, the call completes without a request. If a row is current, passing `null` as `object` sends a request that fails because the server requires a row key.

`changeProperty` reads its arguments by their count, and a single argument by that argument alone: with none it runs the property's change event (for an action, the action) on the current object; with one, the argument is the row, and the change event runs on it, when it is a data row or an `objects` handle, or when the property is an action drawn in an object group; anything else is the value, set for the current object; with two, the first is the row and the second the value. The value formats are the same as in the [form controller](Form_and_navigator_controllers.md#value-formats)'s `<group>.<property>.change`. A property that is not one of this group's columns is passed by `changeProperty` to the form, which resolves it by its name alone, the way a view names a property everywhere: `<group>.<property>` is a property of that group, an unqualified name one of the form level (the empty group) — a property of an object group is named with its group, a row passed or not, and a row passed with an unqualified name is a mistake.

For a column of this group, `changeProperty(property, object, value[, type[, index]])` also updates the list remembered by `diff`, if any. By default it replaces the named field of matching rows. With `'add'` it sets the field on a copy of the object and inserts the copy at the zero-based `index` (at the end when omitted). With `'remove'` it removes the matching row, which must be in the list. These arguments affect only the remembered list. An explicit row argument that cannot be resolved causes the column change to return `null` and log the reason on the console. For a change delegated to the form, an unknown or ambiguous name, a property grouped in columns, or a row that cannot be used with that name throws at the call.

`batch(fn)` makes several changes together: each `changeProperty` call in `fn` is made at once, and they are all sent in one request when `fn` returns — for example, a Gantt chart view changes both dates at once when a task bar is dragged:

```js
controller.batch(() => {
    controller.changeProperty('start', task, newStart);
    controller.changeProperty('end', task, newEnd);
});
```

A batch carries changes only: inside `fn`, `getPropertyValues` is refused — its failure callback called, the reason said on the console —, and so is a server call on the form's controller (`controller.form.exec`, `eval`, `evalAction`, `change`), its promise rejected; make them before the batch or after it; and changes only through `changeProperty` and the view filters (`setBooleanViewFilter`, `setDateIntervalViewFilter`), which go with the batch — moving the focus or changing the DOM inside `fn` makes the platform send requests of its own, which a batch is not made for. A function returning a Promise is refused — but not the batch's own, which a `changeProperty` made in `fn` returns —, as in the [form controller](Form_and_navigator_controllers.md#batch)'s `batch`.

`getPropertyValues` uses the same `mode` values as the form controller's `<group>.<property>.getValues`, but the property is resolved among this group's columns, and the lookup runs for its current object. The result is delivered to the `ok` callback as `{ data: [ { displayString, rawString, objects }, ... ], more }`. `more` is `true` while more answers are to come — the final list of an answer the server gave before it was sure the form's unsaved changes do not affect it, or a later lookup's answer. An earlier lookup's answer that comes after a later one's is dropped:

```js
controller.getPropertyValues('name', query, result => { ... });
```

Both lookup methods return no value. In the answer, `displayString` is the suggestion's display text, `rawString` its raw text, and `objects` depends on the [lookup mode](Form_and_navigator_controllers.md#looking-up-values). An unknown column, an unknown mode or a failed lookup calls `fail()` if supplied. An unknown mode also logs the reason on the console. In `getPropertyValues`, a positive `count` requests more suggestions. The default is `0`, which the group's `getValues` shorthand always uses.

<a className="lsdoc-anchor" id="diff"/>

To update the DOM only for changed values, the _controller_ has a _diff_ method, which takes the new _list_ of objects and a handler function. The method compares the passed list with the list from the previous call (the first call compares with an empty one) and delivers the changes as a sequential script transforming the old list into the new one: the handler is called for every change and receives its type (`'add'`, `'update'` or `'remove'`), the change position in the list being transformed (for `'add'` and `'update'` it coincides with the object's position in the new list), and the object itself (the removed one for `'remove'`, the new one for the other types). The passed list is then remembered. Two optional flags refine the comparison: with `noDiffObjects` one object's row is never turned into another's — such a pair yields `'remove'` and `'add'` instead of `'update'`. With `removeFirst` the handler receives all removals first — with positions in the previous list — and only then the additions and updates.
Example:

```js
controller.diff(list, (type, index, object) => {
    switch (type) {
        case 'add': ...; break;
        case 'update': ...; break;
        case 'remove': ...; break;
    }
}, true, true);
```

The _clearDiff_ method resets the remembered list — it is called in the component's optional _clear_ function, invoked when the view is cleared with the same _element_ and _controller_, so that the next rendering starts from an empty state.

`setBooleanViewFilter` and `setDateIntervalViewFilter` set a server view filter and the page size `pageSize`: the next _update_ call receives an already filtered list. `setBooleanViewFilter` keeps the objects whose `property` value is true. `setDateIntervalViewFilter` keeps the objects whose period from the `startProperty` value to the `endProperty` value intersects the interval from `start` to `end` (the values are JS `Date`s, and when `endProperty` is `null`, both ends of the period are taken from `startProperty`). This is how the built-in calendar view reads only the events of the visible date range.

The display getters let the component use the design attributes set on the form: for example, a chart view builds its datasets taking the column captions and colors from `getCaption`, `getBackground` and `getForeground`.

The column getters that take an object use the current row if it is omitted or `null`. For an unknown column the value and design attribute getters return `null`, as does `isPropertyReadOnly`. A view filter naming an unknown column leaves the filter and page size in place.

The request `Promise` of a group change, view filter or batch resolves when the client finishes processing the server's answer, or immediately if there is no request to send. It rejects if the request fails or the form is closed (`Form is closed`).

Editing the form — `changeObject`, `changeProperty`, `batch` and the view filters — goes through the ordinary edit channel and is not gated. The server calls of the form's controller, `controller.form`, are gated ([Authorization gate](Form_and_navigator_controllers.md#access)).

### A property's value {#property}

A property's render function draws one value — that of its own property, in a cell of a table or on a panel. `update` receives it as its third argument, `value`:
```js
update: function (element, controller, value) {
    element.text.innerText = value || '';
}
```

A value of any class arrives converted to a JS value in the same way as in the rows of a [React view](React_projection.md#values), and, files aside, is passed to `change` in the same shape.

The fourth argument of `update` is `null` for an ordinary `CUSTOM` cell renderer.

Given together with [`SELECT`](Properties_and_actions_block.md#options), the render function draws the selection. The `value` passed to `update` contains the property's options, as for the platform's own selection element. The wrapping function is called with an object `{multi, notNull, html, changeValue}`: `multi` means a selection of several values, `notNull` means a value has to be selected, `html` means the options' views are HTML, and `changeValue` means a pick is passed to the property's explicit change as the option's view.

### A property's controller {#property-controller}

The methods of the controller passed to the _update_ function, internal helpers aside (optional arguments are bracketed):

| method | what it does |
| --- | --- |
| `change([value])` | pass a value change (see below); with no argument the change action is called without a value |
| `change(value, renderValueSupplier)` | in a property cell, pass `value` to the change action and display `renderValueSupplier(oldValue)` while the request is pending |
| `changeValue([value])` | deprecated since 5.0, to be removed in 8.0. Use `change([value])` |
| `changeProperty(property, object, newValue[, type[, index[, changeValue]]])` | change a field of an item in the cell's array. Passes `{property, objects: object.objects, value: newValue}` to `change`, or the explicit `changeValue` when supplied. The displayed array is updated as described below |
| `getValues(value, ok[, fail[, count]])` | a server suggestion list for the typed `value` — the same suggestions as when the property is edited normally |
| `getPropertyValues(property, value, ok[, fail[, count]])` | calls the cell's `getValues` with the query `property + ':' + value`. For the default change action of a property built by the [`JSON` operator](JSON_operator.md), `property` is the integration name of the field to look up |
| `isReadOnly()` | whether the property can be edited: `null` — editable, `false` — read-only, `true` — disabled |
| `isList()` | `false` for a property, including one drawn in a table cell |
| `isTabFocusable()` | whether the view's input belongs in the Tab order. On a panel this follows the property's focusability, in a table cell it is `false` |
| `getObjects(object)` | the array item's `objects` field, or `undefined` if absent. Passing `null` or omitting the item throws a `TypeError` |
| `createObject(object, objects)` | a shallow copy of the item. A platform row or handle supplies both `key` and `objects`. Any other `objects` value replaces just that field |
| `getObjectsString(object)` | the identity used by `diff` and `changeProperty`. For a platform row or handle it includes the identities of the group's objects as well as their key. Passing `null` or omitting the item throws a `TypeError`. For another object with a defined `key`, it returns `String(object.key)`. Otherwise it returns `null` if `object.objects` is missing, `undefined` or `null`. For any other field value it returns `JSON.stringify(object.objects)` (for example, `JSON.stringify(() => 1)` returns `undefined`), or `'{}'` if serialization throws |
| `diff(list, fnc[, noDiffObjects][, removeFirst])` | compute the changes of a value array — like the [`diff` method of an object group](#diff); the items are matched by their `objects` field |
| `getColorThemeName()` | the current color theme name: `'LIGHT'` or `'DARK'` |
| `form` | the [form controller](Form_and_navigator_controllers.md) |

A value passed by the _change_ method goes into the change handling of the property in the same way as a value entered by the standard editor — for a data property that is writing the value into it. When the passed object goes into the default change action of a property with a custom-class value listed in the `CHECKED BY` block of a [constraint](CONSTRAINT_statement.md), it must be among the values the filter leaves: otherwise the change is refused. If an action is specified after the property with the keyword **ON CHANGE**, it is executed instead of the standard change handling, and the value passed by the _change_ method is substituted for the user input in its [value request](../paradigm/Value_request_REQUEST.md).

The requests of the `change` method and of the server calls through `controller.form` are executed on the server in the order of the calls.

The cell's `change`, `changeValue` and `changeProperty` return no value. For `changeProperty`, the displayed array starts empty if the old value is `null`. By default the named field is replaced on matching items. The `'add'` and `'remove'` operations and the insertion position `index` work as for the object group's `changeProperty`. An explicit `changeValue` changes only the value sent to the action.

The `ok` handler of `getValues` receives the result in the same format as a custom object group's [`getPropertyValues`](#controller-methods). Both cell lookups return no value and use the same `count` and failure callback. To pass a count without a failure callback, use `controller.getPropertyValues('name', query, ok, undefined, 20)`. A lookup during a batch calls `fail()` if supplied and logs the reason on the console.

In a table footer or pivot cell drawn with the property's renderer, changes and lookups do nothing and neither lookup callback is called. When the renderer draws a user filter's value, `change(value, renderValueSupplier)` commits `renderValueSupplier(oldValue)` as the filter value, where `oldValue` is the current filter value. Neither `getValues` nor `getPropertyValues` calls `ok` or `fail` in a user filter.

The _render_ function and the optional _clear_ function, invoked when the cell is cleared, receive a reduced controller as the second argument. It provides `isList()`, `isTabFocusable()` and `clearDiff()`, which resets the list remembered by `diff`. The full controller's `isReadOnly()` gives the state at the time of `update`.
