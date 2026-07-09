---
slug: "/How-to_Custom_components_server_calls"
title: 'How-to: Пользовательские компоненты (вызовы сервера)'
---

Пользовательское представление обращается к серверу через методы `exec` / `eval` / `evalAction` / `change` контроллера формы; механика — авторизационная проверка, сессии, преобразование значений — описана в разделе [Вызов сервера](How-to_Custom_components_objects.md#calling-the-server), а сигнатуры методов — в [API контроллера формы](How-to_Custom_view_controller.md#calling-the-server). Здесь — сквозной пример: один компонент CUSTOM, который через блок `CUSTOMS` формы вызывает действия и свойства со всеми основными видами параметров.

### Задача

Нарисуем список товаров карточками (представление CUSTOM группы объектов). Кнопки на карточках и на панели над ними должны покрыть:

| Случай | Вызов из JS | На стороне lsFusion |
| --- | --- | --- |
| действие: объект + примитивы (INTEGER, STRING, DATE) | `exec('addToCart', item.key, 2, 'из витрины', new Date())` | `addToCart[Item, INTEGER, STRING[100], DATE]` |
| действие с результатом | `await exec('preview', item.key, 15.0)` → число | `preview[Item, NUMERIC[5,2]]` с `RETURN` |
| действие с параметром JSON | `exec('addSet', [{item, quantity}, ...])` | `addSet[JSON]` + `IMPORT JSON FROM` |
| изменение глобального свойства | `change('discount', 15.0)` | первичное свойство `discount[]` |
| изменение свойства с ключом-объектом | `change('archived', item.key, true)` | первичное свойство `archived[Item]` |
| действие формы (для контраста — без проверки доступа) | `changeProperty('edit', item)` | обычный `EDIT` на форме |

### Доменная логика

```lsf
CLASS Item 'Товар';
CLASS CartLine 'Строка корзины';

name 'Наименование' = DATA ISTRING[100] (Item) NONULL CHARWIDTH 20 IN id;
price 'Цена' = DATA NUMERIC[14,2] (Item);
archived 'Скрыт' = DATA BOOLEAN (Item);

discount 'Скидка, %' = DATA NUMERIC[5,2] ();

item 'Товар' = DATA Item (CartLine) NONULL DELETE;
nameItem 'Товар' (CartLine l) = name(item(l));
quantity 'Кол-во' = DATA INTEGER (CartLine);
note 'Комментарий' = DATA STRING[100] (CartLine);
needBy 'Нужно к' = DATA DATE (CartLine);

// exec: объект (приходит числовым id) + примитивы INTEGER, STRING, DATE
addToCart 'В корзину' (Item i, INTEGER q, STRING[100] c, DATE d) {
    NEW l = CartLine {
        item(l) <- i;
        quantity(l) <- q;
        note(l) <- c;
        needBy(l) <- d;
    }
}

// exec: объект + NUMERIC, результат возвращается клиенту через RETURN
preview 'Цена со скидкой' (Item i, NUMERIC[5,2] pct) {
    RETURN NUMERIC[14,2](price(i) * (100 - pct) / 100);
}

// exec: параметр JSON — клиент передаёт обычный JS-массив объектов
addSet 'Добавить набор' (JSON items) {
    IMPORT JSON FROM items FIELDS LONG item, INTEGER quantity DO
        NEW l = CartLine {
            item(l) <- GROUP MAX i AS Item IF LONG(i) = item;
            quantity(l) <- quantity;
        }
}

FORM shop 'Витрина'
    OBJECTS i = Item CUSTOM 'itemBoard' PAGESIZE 0
    PROPERTIES(i) READONLY name, price
    PROPERTIES(i) NEWSESSION new = NEW, edit = EDIT GRID, DELETE GRID
    FILTERS NOT archived(i)

    OBJECTS l = CartLine
    PROPERTIES(l) READONLY nameItem, quantity, note, needBy
    PROPERTIES(l) DELETE

    PROPERTIES discount()

    // что JS-компоненту можно вызывать на этой форме без @@api
    CUSTOMS addToCart[Item, INTEGER, STRING[100], DATE], addSet[JSON],
            preview[Item, NUMERIC[5,2]], discount[], archived[Item]
;

DESIGN shop {
    NEW pane FIRST {
        fill = 1;
        horizontal = TRUE;
        MOVE BOX(i) { fill = 2; }
        MOVE BOX(l) { fill = 1; }
    }
}

NAVIGATOR {
    NEW shop;
}
```

Ключевые места:

- `CUSTOM 'itemBoard'` — группу объектов рисует JS-функция _itemBoard_ вместо таблицы; `PAGESIZE 0` — компонент получает все объекты, а не первую страницу.
- `CUSTOMS` перечисляет, что контроллеру этой формы можно вызывать без `@@api`: для `exec` нужна запись-действие, для `change` — запись-свойство. Классы параметров указаны явно (пустые скобки — без параметров), поэтому запись продолжит указывать на то же действие или свойство, когда позже появится перегрузка.
- `FILTERS NOT archived(i)` — реакция на `change('archived', …)` видна сразу: карточка исчезает.

### Компонент

Файлы `shopboard.js` и `shopboard.css` кладутся в папку _resources/web_ (путь без сборки — обычный файл `.js`, без упаковки; см. [How-to: Пользовательские клиентские модули JS](How-to_Custom_client_JS_modules.md)) и регистрируются в действии `onWebClientInit[]`:

```lsf
onWebClientInit() + {
    onWebClientInit('shopboard.js') <- 10;
    onWebClientInit('shopboard.css') <- 11;
}
```

```js
function itemBoard() {
    return {
        render: (element, controller) => {
            let toolbar = document.createElement("div");
            toolbar.classList.add("board-toolbar");

            // change: глобальное свойство, примитив NUMERIC
            let discountBtn = document.createElement("button");
            discountBtn.innerText = "Скидка 15%";
            discountBtn.onclick = () => controller.form.change('discount', 15.0);

            // exec: параметр JSON — массив {item: id, quantity} одним вызовом
            let setBtn = document.createElement("button");
            setBtn.innerText = "По одному каждого";
            setBtn.onclick = () => controller.form.exec('addSet',
                (element.lastList || []).map(it => ({ item: it.key, quantity: 1 })));

            toolbar.append(discountBtn, setBtn);

            let cards = document.createElement("div");
            cards.classList.add("item-cards");

            element.cards = cards;
            element.lastList = [];
            element.append(toolbar, cards);
        },

        update: (element, controller, list) => {
            element.lastList = list;
            while (element.cards.lastElementChild)
                element.cards.removeChild(element.cards.lastElementChild);

            for (let item of list) {
                let card = document.createElement("div");
                card.classList.add("item-card");
                if (controller.isCurrent(item))
                    card.classList.add("item-card-current");

                let title = document.createElement("div");
                title.classList.add("item-card-title");
                title.innerText = item.name + " — " + item.price;
                card.appendChild(title);

                // exec: объект (item.key = числовой id) + INTEGER + STRING + DATE
                let buy = document.createElement("button");
                buy.innerText = "В корзину";
                buy.onclick = async (e) => {
                    e.stopPropagation();
                    try {
                        await controller.form.exec('addToCart', item.key, 2, 'из витрины', new Date());
                    } catch (ex) {
                        console.error(ex);
                    }
                };
                card.appendChild(buy);

                // exec: действие с RETURN — Promise разрешается значением
                let previewBtn = document.createElement("button");
                previewBtn.innerText = "Со скидкой?";
                previewBtn.onclick = async (e) => {
                    e.stopPropagation();
                    let p = await controller.form.exec('preview', item.key, 15.0);
                    previewBtn.innerText = "≈ " + p;
                };
                card.appendChild(previewBtn);

                // change: свойство с ключом-объектом — карточка исчезнет (FILTERS NOT archived)
                let hideBtn = document.createElement("button");
                hideBtn.innerText = "Скрыть";
                hideBtn.onclick = (e) => {
                    e.stopPropagation();
                    controller.form.change('archived', item.key, true);
                };
                card.appendChild(hideBtn);

                // канал правки формы — без проверки доступа, запись в CUSTOMS не нужна
                card.onclick = () => { if (!controller.isCurrent(item)) controller.changeObject(item); };
                card.ondblclick = () => controller.changeProperty('edit', item);

                element.cards.appendChild(card);
            }
        }
    }
}
```

```css
.board-toolbar { display: flex; gap: 8px; margin-bottom: 8px; }
.item-cards { display: flex; flex-wrap: wrap; gap: 8px; align-content: flex-start; }
.item-card { border: 1px solid #ccc; border-radius: 6px; padding: 8px; display: flex; flex-direction: column; gap: 4px; min-width: 170px; }
.item-card-current { border-color: #4a90d9; box-shadow: 0 0 0 1px #4a90d9; }
.item-card-title { font-weight: bold; }
```

Открытая форма:

![](../images/How-to_Custom_components_server_calls.png)

После «Скидка 15%», «По одному каждого», «В корзину» на карточке кофе и «Скрыть» на карточке сахара:

![](../images/How-to_Custom_components_server_calls_result.png)

### Параметры и результат

Вызовы передают параметры позиционно, обычными значениями JS: `addToCart` получает объект его числовым id — `item.key` строки карточки — а примитивы `INTEGER`, `STRING` и `DATE` напрямую (для `DATE` — именно объект `Date`); `addSet` получает обычный JS-массив в параметр `JSON`. `Promise` вызова `preview` разрешается числом, которое возвращает `RETURN` действия.

### Сессии

На форме вызовы выполняются в сессии формы: строка корзины и «скрытие» товара видны сразу, но в базу попадут только при сохранении формы. Когда изменение затрагивает собственный список компонента — здесь скрытие карточки, — платформа вызывает его `update`, и компонент пересоздаёт DOM целиком, поэтому временное состояние в узлах DOM — текст «≈ 807.5» на кнопке — стирается следующим таким обновлением; чтобы результат отображался устойчиво, его кладут в свойство формы.

### Проверка доступа

`CUSTOMS` открывает перечисленные записи только контроллеру этой формы и не ограничивает значения аргументов, которые передаёт компонент, поэтому каждая запись в списке должна оставаться безопасной при произвольном id и произвольных значениях. Двойному щелчку с `edit` запись не нужна: действие, нарисованное на форме, выполняется через канал правки формы (`changeProperty`), который не проходит проверку доступа, — но и не принимает дополнительных аргументов, только строку-цель. Как только нужен аргумент, которого нет на форме, — количество, процент, массив — это `exec` плюс запись в `CUSTOMS`.
