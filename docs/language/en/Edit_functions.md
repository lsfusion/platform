---
slug: "/Edit_functions"
title: 'Edit functions'
---

An *edit function* is a JavaScript function responsible for editing a value in the web client, as a [render function](Render_functions.md) is for displaying it. Its name is given in a property's [`CUSTOM` option](Properties_and_actions_block.md#options), after the `CHANGE` keyword, and in the `CUSTOM` block of the [`INPUT` operator](INPUT_operator.md), for the value that operator [requests from the user](../paradigm/Primitive_input_INPUT.md). In the desktop client the value is edited by the standard mechanism.

### The edit function {#custom-editor}

By default the value is edited by the property's standard mechanism — a text input, for instance. The `CHANGE` keyword of the [`CUSTOM` value view](Properties_and_actions_block.md) replaces or extends that mechanism with an editor of your own, written in JavaScript:

```lsf
PROPERTIES(o) address CUSTOM CHANGE 'googleAutocomplete'
```

The editor function, like the display function, is registered as a wrapper and returns an object of several functions. The editor kind is determined by which rendering function that object contains:

| function | editor |
| --- | --- |
| `renderInput(element, controller, value)` | extends the standard text input; `element` is that input itself |
| `renderDialog(element, controller, value)` | a window editor |
| `render(element, controller, value)` | an editor replacing the cell content |

`value` is the current property value converted to a JS value. Besides the rendering function, the object may contain:

- `getValue(element)` — returns the value to commit. The string `'canceled'` cancels the edit. For an input editor, when this function is absent, the text of the input itself is taken.
- `clear(element, cancel)` — cleanup when the edit finishes. `cancel` is true on cancellation.
- `onBrowserEvent(event, element)` — handling browser events during the edit.

### The editor's controller {#editor-controller}

The editor controller provides:

| method | what it does |
| --- | --- |
| `commit([value])` | commit the edit: the passed value or, with no argument, the current one (from `getValue` or the input) |
| `cancel()` | cancel the edit |
| `setDeferredCommitOnBlur(true)` | defer the commit on focus loss — for picking from a suggestion list rendered outside `element` |
| `getColorThemeName()` | the current color theme name |
| `form` | the [form controller](Form_and_navigator_controllers.md) |

The committed value goes through the property's ordinary change channel — the same way as a value entered by the standard editor.

For example, the built-in `googleAutocomplete` editor extends an address input with Google Maps suggestions:

```js
function googleAutocomplete() {
    return {
        renderInput: (element, controller) => {
            // the suggestion list is rendered outside element, and picking from it ends with a focus loss,
            // so the commit on focus loss is deferred until the value is set
            controller.setDeferredCommitOnBlur(true);

            new google.maps.places.Autocomplete(element, { types: ['address'] });
        },
        clear: (element, cancel) => {
            // remove the suggestion list elements added to <body>
            $(".pac-container").remove();
        }
    };
}
```
