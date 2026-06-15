---
slug: "/How-to_Custom_view_controller"
title: 'How-to: API контроллера пользовательского представления'
---

Пользовательское представление, написанное на JavaScript, взаимодействует с формой через объект *контроллера*. В [React-представлении](How-to_Custom_React_views.md) контроллер доступен как `props.controller`; тот же контроллер передаётся и в JavaScript-функцию, привязанную действием [`INTERNAL CLIENT`](../language/INTERNAL_operator.md). С его помощью представление задаёт текущий объект группы, изменяет значения свойств, запрашивает подсказки и вызывает действия или скрипты на сервере.

Свойства и действия адресуются по их интеграционному имени — имени на форме (либо псевдониму / интеграционному имени `NEW` / `DELETE` кнопки), тому же имени, что использует [внешний JSON/REST API](How-to_Integration.md).

### Методы контроллера

Все методы контроллера; необязательные аргументы — в скобках:

| метод | что делает | возвращает |
| --- | --- | --- |
| `changeObject(groupSID, object)` | задаёт текущий объект группы | — |
| `changeProperty(property[, object][, value])` | задаёт значение либо выполняет действие — на текущем объекте или на переданной строке | — |
| `changeProperties(properties, objects, values[, groupSIDs])` | несколько вызовов `changeProperty` из параллельных массивов | — |
| `getPropertyValues(property[, object], value[, mode], ok[, fail][, count])` | ограниченный список подсказок с сервера | — (через `ok`) |
| `exec(action, ...params)` | выполняет именованное действие | `Promise` |
| `eval(script, ...params)` | выполняет lsf-скрипт с типизированным `run` | `Promise` |
| `evalAction(script, ...params)` | выполняет тело действия (параметры `$1`, `$2`, …) | `Promise` |
| `change(property, ...keyParams, value)` | задаёт глобальное свойство | `Promise` |

Изменяющие методы (`changeObject` / `changeProperty` / `changeProperties`) ничего не возвращают — новое состояние приходит со следующей проекцией `props.data`; вызывающие сервер методы (`exec` / `eval` / `evalAction` / `change`) возвращают `Promise`. Когда интеграционное имя свойства не уникально в пределах формы, его привязывают к группе через `groupSID`: четвёртым позиционным аргументом `changeProperty` (`changeProperty(property, object, value, groupSID)`), завершающим аргументом после `count` в `getPropertyValues` либо массивом `groupSIDs` в `changeProperties`. Каждый метод подробно разобран ниже.

### Изменение текущего объекта и значений свойств

`controller.changeObject(groupSID, object)` задаёт текущий объект группы `groupSID`. Здесь `object` — это строка данных этой группы либо непосредственный дескриптор `objects` (см. [правила идентификации строки](#row-identity-contract) ниже), но не голый `row.key`.

`controller.changeProperty(property, value)` изменяет `property` для текущего объекта группы. Чтобы адресовать конкретную строку, она передаётся посередине: `controller.changeProperty(property, object, value)`, где `object` — строка данных либо непосредственный дескриптор. Когда `property` — это действие (или свойство без изменяемого значения), значение опускается: `controller.changeProperty('edit')` выполняет его на текущем объекте, `controller.changeProperty('edit', object)` — на переданной строке.

```js
function orderView(props) {
    const controller = props.controller;
    return (
        <div>
            <button onClick={() => controller.changeProperty('note', 'checked')}>Отметить</button>
            {props.data.o.list.map(row =>
                <div key={row.key} onClick={() => controller.changeObject('o', row)}>
                    {row.number}
                </div>)}
        </div>
    );
}
```

В форме `changeProperty(property, X)` с двумя аргументами платформа определяет, значение `X` или строка: если свойство принимает значение и `X` разрешается в строку (строка данных либо непосредственный дескриптор), то `X` читается как строка и вызов выполняется на ней; иначе `X` — это значение, и изменяется текущий объект.

Когда интеграционное имя свойства не уникально в пределах формы, его привязывают к группе четвёртым позиционным аргументом `groupSID` — `controller.changeProperty(property, object, value, groupSID)` (та же привязка, что и у массива `groupSIDs` в `changeProperties`).

`controller.changeProperties(properties, objects, values)` применяет несколько изменений сразу из параллельных массивов — `properties[i]` изменяется на `values[i]` для `objects[i]` (элемент может быть `null` для текущего объекта). Необязательный четвёртый массив `groupSIDs` привязывает каждое свойство к группе, когда его интеграционное имя не уникально в пределах формы.

```js
controller.changeProperties(['note', 'qty'], [null, row], ['checked', 5]);
```

### Запрос значений

`controller.getPropertyValues` запрашивает у сервера ограниченный список подсказок для свойства. Результат передаётся в обработчик `ok` в виде `{ data: [ { displayString, rawString, objects }, ... ], more }`; `more` равно `true`, когда список усечён, поэтому это список подсказок, а не полный `SELECT DISTINCT`. В классическом пользовательском представлении `GRID` тот же запрос доступен как `getValues(property, value, ok, fail)` — это эквивалент `getPropertyValues` в режиме по умолчанию `'objects'`; на уровне формы / в контроллере React используется `getPropertyValues`.

```js
controller.getPropertyValues(property[, object], value[, mode], ok, fail[, count[, groupSID]]);
```

- `value` — вводимая строка запроса для сопоставления.
- `object` — необязательная строка (строка данных либо непосредственный дескриптор), ограничивающая запрос этой строкой; опускается для текущего объекта.
- `mode` — одно из:

  | `mode` | результат | `item.objects` |
  | --- | --- | --- |
  | `'objects'` (по умолчанию) | подходящие `OBJECTS` свойства — выбор объекта | непосредственный дескриптор `objects` этого объекта |
  | `'values'` | различные значения свойства | `null` (используйте `displayString` / `rawString`) |
  | `'change'` | подсказки свойства при редактировании | зависит от свойства |

  `'change'` отражает то, как свойство *редактируется* — список `INPUT`, ограничение `notNull` либо собственное действие изменения, — а не уже присутствующие различные значения. Для свойства, которое в этом контексте изменить нельзя (например, свойство только для чтения или вычисляемое свойство без действия изменения), возвращается пустой список, тогда как `'values'` по-прежнему возвращает его различные значения.

- `ok(result)` / `fail()` — обработчики успеха и ошибки.
- `count` — увеличивает число запрашиваемых элементов, для постраничной загрузки.
- `groupSID` — привязывает свойство к группе, когда его интеграционное имя не уникально в пределах формы (передаётся после `count`).

`item.objects` из результата режима `'objects'` можно передать напрямую обратно в `changeObject` или `changeProperty`, чтобы действовать с выбранным объектом:

```js
controller.getPropertyValues('customer', text, 'objects',
    result => result.data.forEach(item => console.log(item.displayString)),
    () => console.log('ошибка'));

// выбор первой подсказки как заказчика группы
controller.getPropertyValues('customer', text, 'objects', result => {
    const item = result.data[0];
    if (item) controller.changeObject('c', item.objects);
}, () => {});
```

### Вызов сервера

`exec`, `eval`, `evalAction` и `change` выполняются на сервере и возвращают `Promise`. Они работают в точности так, как описано в [How-to: Пользовательские компоненты (объекты)](How-to_Custom_components_objects.md#calling-the-server) — те же проверки доступа и то же преобразование результата в JS-значение.

- `controller.exec(action, ...params)` — выполняет именованное действие; разрешается его значением `RETURN`.
- `controller.eval(script, ...params)` — выполняет lsf-скрипт, определяющий собственное действие `run` (типизированные параметры).
- `controller.evalAction(script, ...params)` — выполняет тело действия, обёрнутое в действие `run`, с параметрами `$1`, `$2`, ….
- `controller.change(property, ...keyParams, value)` — изменяет глобальное свойство; последний аргумент — значение, предшествующие — ключи.

```js
const total = await controller.exec('recalc', orderId);
const doubled = await controller.eval('run(INTEGER a) { RETURN a * 2; }', 21); // 42
await controller.change('note', orderId, 'checked');
```

Вызов, сделанный после закрытия формы, *отклоняется* с ошибкой `Form is closed` — он никогда не зависает, поэтому `await` на закрытой форме попадает в ветку `catch`.

### Правила идентификации строки {#row-identity-contract}

Метод, адресующий строку, принимает одно из:

- объект строки данных, полученный представлением (из props React / списка `update`);
- spread- или `Object.assign`-клон такой строки — перечислимый дескриптор `objects` копируется вместе с ней, поэтому клон разрешается в тот же объект;
- непосредственный дескриптор `objects` — `row.objects` либо `item.objects` из результата `getPropertyValues` в режиме `'objects'`.

Голый `row.key` не принимается: `key` — это токен для отображения / ключа React / сравнения при сопоставлении, а не вход для разрешения. Имена полей `key`, `isCurrent` и `objects` зарезервированы в строке — свойство или колонка приложения с одним из этих интеграционных имён было бы перезаписано.

Если явный аргумент-объект не разрешается ни в строку, ни в непосредственный дескриптор, платформа не подставляет молча текущую строку и не выбрасывает исключение: `changeProperty` записывает ошибку в консоль и пропускает это изменение, а `changeObject` ничего не делает.

### Совместимость с классическим CUSTOM

Классические [пользовательские компоненты на render/update](How-to_Custom_components_objects.md) сохраняют собственный `controller` — с `isCurrent`, `getValue`, геттерами стиля и метаданных, `changeProperty` / `changeObject`, `diff` / `clearDiff` — плюс метод события [`controller.change`](How-to_Custom_components_properties.md#handling-user-actions) у компонентов-свойств. Их строки теперь тоже несут публичный `key` и перечислимый дескриптор `objects`, поэтому строку такого представления можно передавать всюду, где ожидается строка. Вызов `controller.changeProperty(property, ...)` для свойства, которое *не* является колонкой собственной таблицы этого представления, делегируется контроллеру формы, который разрешает свойство в пределах всей формы, — так классическое представление может изменить свойство, которое не отображает.

Тот же контроллер формы — со всеми перечисленными выше методами — доступен из классического представления и напрямую как `controller.form`. Представление [на React](How-to_Custom_React_views.md) для всей формы получает его как `props.controller`; классическое представление объекта/таблицы получает собственный контроллер таблицы и обращается к контроллеру формы через `controller.form`, вызывая `controller.form.exec(...)`, `controller.form.changeObject(...)`, `controller.form.getPropertyValues(...)` и т. д., когда вызов должен идти через форму, а не через собственную таблицу представления.
