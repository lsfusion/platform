---
slug: "/How-to_Custom_components_objects"
title: 'How-to: Пользовательские компоненты (объекты)'
---

По умолчанию каждый объект на форме с видом представления GRID отображается на форме в виде плоской таблицы со столбцами.
Однако, в платформе существует возможность создавать свои собственные компоненты для визуализации списка объектов.

В качестве наглядного примера рассмотрим задачу по отображению в виде "плитки" списка товаров с изображениями.

### Доменная логика

Для начала создадим классы и свойства товаров, а также форму редактирования:

```lsf
CLASS Item 'Item';

name 'Name' = DATA STRING (Item) NONULL;
price 'Price' = DATA NUMERIC[12,2] (Item) NONULL;
image '' = DATA IMAGEFILE (Item);

FORM item 'Item'
    OBJECTS i = Item PANEL
    PROPERTIES(i) name, price, image
    
    EDIT Item OBJECT i
;

DESIGN item {
    OBJECTS {
        MOVE PROPERTY(image(i)) {
            fill = 1;
        }
    }
}
```
Для каждого товара должны быть заданы наименование, цена и изображение.

### Интерфейс

Создадим форму со списком товаров. Для этого добавим на форму объект _Товар_, его свойства, а также действия по добавлению, редактированию и удалению:
```lsf
FORM items 'Items'
    OBJECTS i = Item CUSTOM 'itemCards'
    PROPERTIES(i) READONLY image, price, name
    PROPERTIES(i) NEWSESSION new = NEW, edit = EDIT GRID, DELETE GRID
;

NAVIGATOR {
    NEW items;
}
```
При помощи ключевого слова **CUSTOM** указывается, что для отрисовки списка товаров должен использоваться не стандартный табличный интерфейс, 
а компоненты, создаваемые функцией _itemCards_. Эту функцию объявим в файле _itemcards.js_, который поместим в папку _resources/web_. Это путь без сборки — обычный файл `.js`, без JSX и упаковки; где размещается пользовательский JS и о варианте со сборкой см. [How-to: Пользовательские клиентские JS-модули](How-to_Custom_client_JS_modules.md).
Она будет возвращать объект, состоящий из двух функций: _render_ и _update_.

Функция _render_ принимает на вход контроллер и элемент, внутри которого должны создаваться новые элементы, необходимые для отображения данных:
```js
render: (element, controller) => {
    let cards = document.createElement("div")
    cards.classList.add("item-cards");

    element.cards = cards;
    element.appendChild(cards);
},
```
В данном примере мы создаем новый _div_ _cards_, запоминаем его и добавляем внутрь _element_.

Для обновления отображаемых значений платформа будет каждый раз вызывать функцию _update_, в которую будет передан тот же _element_,
что и в функции _render_, а также список значений _list_:
```js
update: (element, controller, list) => {
    while (element.cards.lastElementChild) {
        element.cards.removeChild(element.cards.lastElementChild);
    }

    for (let item of list) {
        let card = document.createElement("div")
        card.classList.add("item-card");

        if (controller.isCurrent(item))
            card.classList.add("item-card-current");

        let cardImage = document.createElement("img")
        cardImage.classList.add("item-card-image");
        cardImage.src = item.image;
        card.appendChild(cardImage);

        let cardPrice = document.createElement("div")
        cardPrice.classList.add("item-card-price");
        cardPrice.innerHTML = item.price;
        card.appendChild(cardPrice);

        let cardName = document.createElement("div")
        cardName.classList.add("item-card-name");
        cardName.innerHTML = item.name;
        card.appendChild(cardName);

        element.cards.appendChild(card);

        card.onclick = function(event) {
            if (!controller.isCurrent(item)) controller.changeObject(item);
        }
        card.ondblclick = function(event) {
            controller.changeProperty('edit', item);
        }
    }
}
```
Так как функция _update_ вызывается каждый раз, когда изменяются данные, то первым делом происходит удаление всех ранее созданных элементов (а именно карточек товаров).

В данном примере используется самая простая схема обновления, но при необходимости ее можно оптимизировать путем обновления DOM только для изменившихся значений.
Для этой цели у _controller_ есть метод _getDiff_, в который параметром нужно передать новый список объектов _list_. 
Этот метод в качестве результата вернет объект с массивами _add_, _update_, _remove_, в которых будут храниться соответственно добавленные, изменившиеся и удаленные объекты.
Пример:
```js
let diff = controller.getDiff(list);
for (let object of diff.add) { ... }
for (let object of diff.update) { ... }
for (let object of diff.remove) { ... }
```

После удаления старых элементов для каждого объекта из массива _list_ создается свой _div_ _card_, в который помещаются нужные элементы отображения каждого свойства.
Названия полей объектов соответствуют названию свойств на форме. При помощи метода _isCurrent_ определяется, какой объект из списка является текущим.

В самом конце функции добавляются обработчики нажатия кнопки мыши на карточку товара. 

По одиночному нажатию у контроллера вызывается метод _changeObject_, 
который изменяет текущий объект. Второй параметр (_rendered_) не указывается (то есть считается равным _false_), что означает, что сервер должен в итоге вызвать функцию _update_ с новым списком объектов (возможно тем же). 
Так как значение метода _isCurrent_ изменится, то повторное создание карточек товаров изменит текущий выделенный объект в интерфейсе.

По двойному нажатию вызывается метод _changeProperty_, который изменяет текущее значение свойства _edit_ для объекта, переданного вторым параметром.
Поскольку _edit_ является действием, то третий параметр - значение, на которое необходимо изменить текущее значение свойства, не передается, и вместо изменения будет произведен вызов этого действия. 
В данном случае будет открыта форма редактирования товара.

Чтобы объединить функции _render_ и _update_ в одну, создается функция _itemCards_, которая возвращает их внутри одного объекта:
```js
function itemCards() {
    return {
        render: function (element, controller) => {
            ...
        },
        update: function (element, controller, list) {
            ...
        }
    }
}
```

Для завершения настройки дизайна создадим файл _itemcards.css_, которую также поместим в папку _resources/web_:
```css
.item-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    grid-auto-rows: 200px;
    grid-gap: 10px;
}

.item-card {
    cursor: pointer;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    align-items: center;
    padding: 8px;
}
.item-card-current {
    background-color: lightblue;
}

.item-card-image {
    flex: 1;
    min-height: 100px;
}

.item-card-price {
    font-weight: bold;
}

.item-card-name {
    color: gray;
}
```

Для того, чтобы при открытии страницы в браузере, загрузились созданные js и css файлы, нужно добавить их инициализацию в действии _onWebClientInit_ путем добавления имени файла в свойство _onWebClientInit(STRING)_. Числовое значение необходимо для задания порядка загрузки:
```lsf
onWebClientInit() + {
    onWebClientInit('itemcards.js') <- 1;
    onWebClientInit('itemcards.css') <- 2;
}
```

В результате получившаяся форма будет выглядеть следующим образом:
![](../images/How-to_Custom_components_objects.png)

### Вызов сервера {#calling-the-server}

Пользовательское представление объекта обращается к серверу через [контроллер формы](How-to_Custom_view_controller.md): он доступен как `controller.form` из локального контроллера представления, как `props.controller` в [React-представлении](How-to_Custom_React_views.md) либо как контроллер, передаваемый в функцию [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) (последним аргументом, после преобразованных параметров вызова). Его методы `exec` / `eval` / `evalAction` / `change` выполняются на сервере и возвращают `Promise`; их сигнатуры — в разделе [Вызов сервера](How-to_Custom_view_controller.md#calling-the-server). Далее в этом разделе — как ведут себя эти серверные вызовы: преобразование результата, сессии и авторизационная проверка.

Результат преобразуется в значение JS:

| Результат на сервере | Значение в JS |
| --- | --- |
| скалярное число, строка, логическое значение или дата | число, строка, логическое значение или `Date` |
| `JSON` | разобранный объект или массив |
| `JSONTEXT`, `XML` | исходная строка |
| файл — `EXPORT`, изображение или свойство файлового типа | строка со ссылкой для скачивания |
| отсутствующий результат или `NULL` | `undefined` |

Параметры передаются как обычные значения JS (число, строка, логическое значение, `Date` или объект/массив для параметра типа `JSON`) и привязываются позиционно. Ошибка — отсутствующее действие или свойство, ошибка в скрипте или исключение во время выполнения — отклоняет `Promise` с её сообщением.

В форме вызовы выполняются в сессии формы, поэтому изменение видно последующим вызовам и фиксируется при применении изменений формы. В навигаторе каждый вызов выполняется в своей сессии, поэтому изменение отбрасывается, если скрипт не зафиксирует его через `APPLY`, а чтение видит сохранённое в базе состояние.

По умолчанию эти вызовы ограничены так же, как внешний HTTP-API: при `enableAPI = 0` вызов разрешён, только если действие или свойство помечено [`@@api`](../language/Action_options.md) (что заодно открывает его по HTTP), либо у пользователя есть права администратора. Чтобы контроллер конкретной формы мог вызывать выбранные действия и свойства в обход этого ограничения, их перечисляют в блоке `API` формы — тогда для доступа достаточно того, что пользователь может открыть эту форму, и явного перечисления:

```lsf
FORM order 'Order'
    OBJECTS o = Order
    PROPERTIES(o) number, note
    API round, format = formatSum, taxRate
;
```

Теперь `controller.form.exec("round", 3.14159)`, `controller.form.exec("format", 1990, "USD")` и `controller.form.change("taxRate", 0.2)` работают на этой форме без `@@api` и `enableAPI` (React-представление вызывает то же на `props.controller`). Каждую запись можно переименовать псевдонимом (`format = formatSum`), снабдить префиксом `ACTION`, чтобы выбрать действие, и указать полностью с сигнатурой (`round[NUMERIC]`) для выбора перегрузки; `exec` требует действие, `change` — свойство. Параметры передаются вызывающей стороной позиционно как обычные значения — в фазе 1 записи это в основном такие примитивные вызовы. Блок меняет то, какие вызовы разрешены, а не то, как связываются параметры, и не ограничивает значения аргументов, которые передаёт вызывающая сторона, поэтому перечислять стоит только записи, безопасные при любых аргументах (привязка параметра к собственному объекту формы — фаза 2). `eval`/`evalAction` выполняют произвольный скрипт и остаются под ограничением.