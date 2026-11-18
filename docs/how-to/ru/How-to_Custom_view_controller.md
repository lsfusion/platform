---
slug: "/How-to_Custom_view_controller"
title: 'How-to: API контроллера пользовательского представления'
---

В этой статье собраны типовые задачи, которые пользовательское представление решает через свой контроллер. Контроллер описан в разделе «Язык»: члены контроллера представления React — в статье [Контроллер представления React](../language/React_view_controller.md), а контроллер формы, который получает пользовательское представление любого вида, и контроллер навигатора — в статье [Контроллеры формы и навигатора](../language/Form_and_navigator_controllers.md).

### Смена текущего объекта и значения из компонента {#current-object-and-value}

Нужно, чтобы щелчок по строке делал её текущей, а кнопка отмечала текущий заказ. Для решения используются `change(row)` группы и `change(value)` члена свойства ([Изменение текущего объекта и значений свойств](../language/React_view_controller.md#changing-values)).

```js
function orderView(props) {
    const controller = props.controller;
    return (
        <div>
            <button onClick={() => controller.o.note.change('checked')}>Отметить</button>
            {props.data.o.list.map(row =>
                <div key={row.key} onClick={() => controller.o.change(row)}>
                    {row.number.value}
                </div>)}
        </div>
    );
}
```

### Подсказки по мере ввода {#suggestions}

Нужно показывать подсказки к значению, которое вводит пользователь, и делать подсказку текущим объектом группы. Для решения используется `getValues` ([Запрос значений](../language/Form_and_navigator_controllers.md#looking-up-values)).

Представление может показать первый ответ сразу, окончательный — когда он придёт, а сообщение — если Promise отвергнут:

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
        showHint(e.message);   // например, слишком короткий запрос
}
```

Вывод подсказок и выбор первой из них:

```js
const result = await controller.c.customer.getValues(text);
result.data.forEach(item => console.log(item.displayString));

// выбор первой подсказки окончательного ответа как покупателя группы
const item = (result.more ? await result.more : result).data[0];
if (item) controller.c.change(item.objects);
```

### Объявление редактирования для поля представления {#field-editing}

Нужно поле заметки, которое рисует само представление и в котором `ENTER` не переводит фокус на следующий компонент, а `ESCAPE` не закрывает окно. Для решения используется [объявление редактирования](../language/Form_and_navigator_controllers.md#editing) на контроллере формы.

```jsx
function Note({ controller }) {
    return <textarea onFocus={e => controller.form.startEditing(e.target)}
                     onBlur={e => controller.form.stopEditing(e.target)}/>;
}
```

В этом примере объявление делается по фокусу.
