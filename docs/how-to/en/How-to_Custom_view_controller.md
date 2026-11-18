---
slug: "/How-to_Custom_view_controller"
title: 'How-to: Custom view controller API'
---

This article collects typical tasks a custom view solves through its controller. The controller is specified in the Language articles: the members of a React view's controller in [React view controller](../language/React_view_controller.md), and the form's controller, which a custom view of any kind gets, and the navigator's controller in [Form and navigator controllers](../language/Form_and_navigator_controllers.md).

### Changing the current object and a value from a component {#current-object-and-value}

We need a click on a row to make the row current, and a button to mark the current order. The solution uses the group's `change(row)` and the `change(value)` of the property's member ([Changing the current object and property values](../language/React_view_controller.md#changing-values)).

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

### Suggestions as the user types {#suggestions}

We need to show suggestions for a value the user types and to make a suggestion the group's current object. The solution uses `getValues` ([Looking up values](../language/Form_and_navigator_controllers.md#looking-up-values)).

A view can show the first answer at once, the final one when it comes, and the message of a rejection:

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

Listing the suggestions and picking the first one:

```js
const result = await controller.c.customer.getValues(text);
result.data.forEach(item => console.log(item.displayString));

// picking the first suggestion of the final answer as the group's customer
const item = (result.more ? await result.more : result).data[0];
if (item) controller.c.change(item.objects);
```

### Declaring editing for a field the view draws {#field-editing}

We need a note field the view draws itself, in which `ENTER` does not move the focus to the next component and `ESCAPE` does not close the window. The solution uses the [editing declaration](../language/Form_and_navigator_controllers.md#editing) on the form's controller.

```jsx
function Note({ controller }) {
    return <textarea onFocus={e => controller.form.startEditing(e.target)}
                     onBlur={e => controller.form.stopEditing(e.target)}/>;
}
```

This example declares on focus.
