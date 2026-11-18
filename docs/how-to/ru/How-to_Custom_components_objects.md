---
slug: "/How-to_Custom_components_objects"
title: 'How-to: Пользовательские компоненты (объекты)'
---

Эта статья описывает классические JavaScript-представления для существующего кода. Для нового представления используйте React-компонент в контейнере `custom` или зарегистрированный как `reactView` для `CUSTOM` ([Представления React](../language/React_views.md)).

По умолчанию каждый объект на форме с видом представления GRID отображается на форме в виде плоской таблицы со столбцами.
Однако, в платформе существует возможность создавать свои собственные компоненты для визуализации списка объектов.

В качестве наглядного примера рассмотрим задачу по отображению в виде "плитки" списка товаров с изображениями.

### Доменная логика

Для начала создадим классы и свойства товаров, а также форму редактирования:

```lsf
CLASS Item 'Item';

name 'Name' = DATA STRING (Item) NONULL;
price 'Price' = DATA NUMERIC[12,2] (Item) NONULL;
picture '' = DATA IMAGEFILE (Item);

FORM item 'Item'
    OBJECTS i = Item PANEL
    PROPERTIES(i) name, price, picture
    
    EDIT Item OBJECT i
;

DESIGN item {
    OBJECTS {
        MOVE PROPERTY(picture(i)) {
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
    PROPERTIES(i) READONLY picture, price, name
    PROPERTIES(i) NEWSESSION new = NEW, edit = EDIT GRID, DELETE GRID
;

NAVIGATOR {
    NEW items;
}
```
При помощи ключевого слова **CUSTOM** указывается, что для отрисовки списка товаров должен использоваться не стандартный табличный интерфейс, 
а компоненты, создаваемые функцией _itemCards_. Эту функцию объявим в файле _itemcards.js_, который поместим в папку _resources/web_. Это путь без сборки — обычный файл `.js`, без JSX и упаковки. Где размещается пользовательский JS и о варианте со сборкой см. [Клиентские модули JavaScript](../language/Client_JavaScript_modules.md#without-the-build).
Она будет возвращать объект, состоящий из двух функций: _render_ и _update_ ([Функция отображения](../language/Render_functions.md#render-function)).

Напишем функцию _render_:
```js
render: (element, controller) => {
    let cards = document.createElement("div")
    cards.classList.add("item-cards");

    element.cards = cards;
    element.appendChild(cards);
},
```
В данном примере мы создаем новый _div_ _cards_, запоминаем его и добавляем внутрь _element_.

Напишем функцию _update_:
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
        cardImage.src = item.picture;
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

В данном примере используется самая простая схема обновления, но при необходимости ее можно оптимизировать путем обновления DOM только для изменившихся значений при помощи метода [_diff_](../language/Render_functions.md#diff) контроллера.

После удаления старых элементов для каждого объекта из массива _list_ создается свой _div_ _card_, в который помещаются нужные элементы отображения каждого свойства ([Строки группы объектов](../language/Render_functions.md#object-group)).

В самом конце функции добавляются обработчики нажатия кнопки мыши на карточку товара. 

По одиночному нажатию у контроллера вызывается метод _changeObject_, 
который изменяет текущий объект. Второй параметр (_rendered_) не указывается, поэтому сервер в итоге снова вызовет функцию _update_ ([Контроллер группы объектов](../language/Render_functions.md#controller-methods)).
Так как значение метода _isCurrent_ изменится, то повторное создание карточек товаров изменит текущий выделенный объект в интерфейсе.

По двойному нажатию вызывается метод _changeProperty_, который изменяет текущее значение свойства _edit_ для объекта, переданного вторым параметром.
Поскольку _edit_ является действием, то третий параметр - значение, на которое необходимо изменить текущее значение свойства, не передается, и вместо изменения будет произведен вызов этого действия. 
В данном случае будет открыта форма редактирования товара.

Чтобы объединить функции _render_ и _update_ в одну, создается функция _itemCards_, которая возвращает их внутри одного объекта:
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
