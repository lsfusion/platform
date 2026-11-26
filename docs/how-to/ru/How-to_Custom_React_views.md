---
slug: "/How-to_Custom_React_views"
title: 'How-to: Пользовательские представления на React'
---

В этой статье собраны типовые задачи, решаемые представлениями React, — каждая со своим решением. Сами представления описаны в разделе «Язык», начиная со статьи [Представления React](../language/React_views.md): данные, которые получает компонент, — в статье [Проекция формы](../language/React_projection.md), а то, что он вызывает, — в статье [Контроллер представления React](../language/React_view_controller.md).

Компоненты примеров написаны на JSX, каждый — в модуле `.jsx` ([Компонент](../language/React_views.md#the-component)). Как этот модуль компилируется и регистрируется, описано в статье [Клиентские модули JavaScript](../language/Client_JavaScript_modules.md). Классический (не React) пользовательский контейнер описан в [инструкции `DESIGN`](../language/DESIGN_statement.md#containerprops).

### «N из M»: количество под фильтром формы {#filtered-count}

Нужно показать рядом со строками, которые рисует представление, сколько строк пропускает фильтр формы, — «6 из 23». Решение дополняет форму `orders` из раздела [Выбор компонента](../language/React_views.md#selecting-the-component).

Число прочитанных строк — `list.length`, а не число строк, пропущенных фильтрами группы, поэтому при постраничном чтении по одной проекции «6 из 23» не показать. Поддерживаемый актуальным итог — отдельное свойство, которое считает строки в текущем отборе формы, включая применённые пользовательские фильтры и фильтры представления, [оператором `FILTER`](../paradigm/Filter_FILTER.md), как в [How-to: Состояние таблиц](How-to_Table_status.md), и размещено внутри контейнера, чтобы попасть в `data`:

```lsf
filteredCount 'Orders' = GROUP SUM 1 IF [ FILTER orders.o](Order o);

EXTEND FORM orders PROPERTIES() filteredCount;
DESIGN orders { BOX(o) { MOVE PROPERTY(filteredCount()); } }
```

```jsx
const { o, filteredCount } = props.data;
<div>{o.list.length} из {filteredCount.value ?? 0}</div>
```

Если вместо этого нарисовать свойство для группы (`PROPERTIES() filteredCount DRAW o PANEL`), `MOVE` не нужен: его место по умолчанию — в `PANEL(o)`, внутри `BOX(o)`. Оно становится членом узла группы и читается как `data.o.filteredCount.value`.

### Список с загрузкой при прокрутке {#feed}

Нужно показывать строки группы списком, который при прокрутке загружает следующую страницу, после смены фильтров или сортировок начинается заново, а по кнопкам переходит к своей первой или последней строке. Для решения используются хук [`useSeekOnScroll`](../language/React_rendering.md#seek-on-scroll) и край списка, переданный в `change` группы ([Первая и последняя строка](../language/React_view_controller.md#first-last-row)).

```jsx
const seekRef = useSeekOnScroll(controller.o, { enabled: follow });
...
{rows.map(row => <div key={row.key} ref={seekRef(row)}>...</div>)}
```

После смены фильтров или сортировок сервер читает строки вокруг текущей строки, как в таблице платформы. Список, который должен переходить к первой строке, после изменения вызывает `controller.<g>.change(controller.form.HOME)` в том же синхронном [пакете](../language/Form_and_navigator_controllers.md#batch). В [ручном режиме применения фильтров](../language/React_filters_and_toolbar.md#filter-controls) переходите к началу после применения фильтров, чтобы переход использовал новые условия.

Кнопки перехода к первой и последней строке:

```jsx
<>
    <button onClick={() => controller.o.change(controller.form.HOME)}>Первая</button>
    <button onClick={() => controller.o.change(controller.form.END)}>Последняя</button>
</>
```

### Календарь с раскладкой строк по ячейкам {#calendar-board}

Нужно показывать смены календарём — сеткой дней по сотрудникам, где каждая смена стоит в своей ячейке, — так, чтобы изменение перерисовывало только затронутые ячейки. Для решения используются `BucketScope` и `useBucket` ([Раскладка строк по ячейкам](../language/React_rendering.md#bucketing)).

```jsx
const { BucketScope, useBucket, useData } = window.lsfusion;

const Shift = React.memo(({ rowKey }) => {
    const s = useData(d => d.ss.byKey[rowKey]);          // подписка на свою строку
    return s ? <button>{s.intervalS.value}</button> : null;
});

const Cell = React.memo(({ ck }) => {
    const rowKeys = useBucket(ck);                       // подписка на свою ячейку
    return <div className="cell">{rowKeys.map(k => <Shift key={k} rowKey={k} />)}</div>;
});

// значение DATE приходит в props.data как Date JavaScript
function weekOf(from) {
    return Array.from({length: 7}, (_, i) => {
        const day = new Date(from);
        day.setDate(from.getDate() + i);
        return day;
    });
}

function dateKey(d) {
    const year = String(d.getFullYear()).padStart(4, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

export function Board(props) {
    // обеими осями владеет представление: дни показанной недели и одна строка доски на сотрудника
    const days = weekOf(props.data.dates.scheduleFrom.value);
    const rows = props.data.boardEmployees.value;        // например, предразобранное JSON-свойство: [{ id, ... }]
    return (
        <BucketScope group="ss" bucketDeps={[]}
                     bucketOf={s => dateKey(s.date.value) + '|' + (s.assignedTo.value ?? '0')}>
            <div className="grid">
                {rows.map(row => days.map(d =>
                    <Cell key={row.id + '/' + dateKey(d)} ck={dateKey(d) + '|' + row.id} />))}
            </div>
        </BucketScope>
    );
}
```

### Страница из нескольких контейнеров {#several-containers}

Нужно построить страницу из нескольких областей, которые рисует React и которые следуют друг за другом: выбор категории показывает её товары, а выбор товара — его детали. Для решения используется [проекция](../language/React_projection.md), своя у каждого контейнера.

Страница из нескольких областей — список категорий, элементы выбранной категории, детали текущего элемента — это не один компонент над всей формой: каждая область — свой контейнер `custom` над своей частью формы, а раскладывают их обычные контейнеры `DESIGN`. Тогда:

- изменение перерисовывает только области, чьи проекции изменились;
- область в неактивной вкладке или свёрнутом контейнере не читается, пока не показана, — если от неё не зависит видимая область;
- другой модуль по-прежнему может дополнить страницу из `DESIGN`.

Общее у областей — состояние формы, а не компонента:

```lsf
CLASS Category;
name = DATA ISTRING[100] (Category);

CLASS Item;
name = DATA ISTRING[100] (Item);
category = DATA Category (Item);
archived = DATA BOOLEAN (Item);

CLASS Detail;
item = DATA Item (Detail);
name = DATA ISTRING[100] (Detail);
quantity = DATA INTEGER (Detail);

FORM catalog 'Catalog'
    OBJECTS c = Category
    PROPERTIES(c) READONLY name

    OBJECTS i = Item
    PROPERTIES(i) READONLY name, archived, nameItem = name PANEL
    FILTERS category(i) = c
    FILTERGROUP archive
        FILTER 'Active' NOT archived(i) DEFAULT
    ORDERS name(i)

    OBJECTS d = Detail
    PROPERTIES(d) READONLY name, quantity
    FILTERS item(d) = i
;

DESIGN catalog {
    NEW page {
        horizontal = TRUE;
        NEW categories { fill = 1; custom = 'CategoryList'; MOVE BOX(c); }
        NEW items { fill = 2; custom = 'ItemList'; MOVE BOX(i); }
        NEW details { fill = 1; custom = 'DetailPanel'; MOVE PROPERTY(nameItem); MOVE BOX(d); }
    }
}
```

```jsx
export function CategoryList({data, controller}) {
    return <ul>{data.c.list.map(row =>
        <li key={row.key}>
            <button style={{fontWeight: row.current ? 'bold' : 'normal'}}
                    onClick={() => controller.c.change(row)}>{row.name.value}</button>
        </li>)}</ul>;
}

export function ItemList({data, controller}) {
    return <>
        <label>
            <input type="checkbox" checked={data.i.archive.value === null}
                   onChange={() => controller.i.archive.change(data.i.archive.value === null ? 0 : null)}/> и архивные
        </label>
        <ul>{data.i.list.map(row =>
            <li key={row.key}><button onClick={() => controller.i.change(row)}>{row.name.value}</button></li>)}</ul>
    </>;
}

export function DetailPanel({data}) {
    return <>
        <h2>{data.i.nameItem.value}</h2>
        <ul>{data.d.list.map(row =>
            <li key={row.key}>{row.name.value}: {row.quantity.value}</li>)}</ul>
    </>;
}
```

В `props.data` каждого контейнера только те части, которые он рисует:

- `CategoryList` видит `data.c`;
- `ItemList` — `data.i` со строками и группой фильтров `archive`;
- `DetailPanel` — `data.d`, а в `data.i` — только перенесённое к нему панельное свойство `nameItem`.

Часть группы проецируется туда, где стоит её компонент, — строки туда, где грид группы, панельное свойство туда, где само свойство, — поэтому имя текущего элемента попадает в панель деталей панельным свойством `i`, добавленным под алиасом и перенесённым `MOVE` в рисующий его контейнер, без строк группы.

Щелчок в списке устанавливает текущий объект (`controller.c.change(row)`), и фильтры нижестоящих групп следуют за ним — это собственная зависимость групп платформы, поэтому общего состояния на JavaScript контейнерам не нужно. Контейнер, который не рисует строки группы, выбирает её текущий объект через контроллер формы — `controller.form.changeObject('c', row)` со строкой своих данных или дескриптором `objects` из запроса, — так же, как сортирует строки, которые рисует другой контейнер ([между контейнерами](../language/Form_and_navigator_controllers.md#across-containers)).

Именованный отбор задаёт `FILTERGROUP`: `data.i.archive.value` равно `0` для активных элементов и `null` для всех, включая архивные. Чтобы показывать и архивные элементы, вызовите `controller.i.archive.change(null)`, а чтобы скрыть их — `controller.i.archive.change(0)` ([Группы фильтров](../language/React_filters_and_toolbar.md#filter-groups)). Другие способы отбора строк описаны в разделе [Выбор механизма отбора](#choosing-a-filter-mechanism).

Области раскладывают стандартные контейнеры: горизонтальный родитель, коэффициент расширения (`fill`) у каждой колонки и базовая высота у колонки, содержимое которой прокручивается внутри неё ([базовый размер](../language/React_views.md#base-size)).

### Полоса вкладок над тем, что рисует платформа {#placed-tabs}

Нужно показывать дочерние компоненты, которые рисует платформа, вкладками: полосой их заголовков и изображений над одним показанным дочерним компонентом. Для решения используются [размещение дочернего компонента](../language/React_platform_components.md#placing), его запись, [`<Caption>`](../language/React_rendering.md#caption) и [`<Image>`](../language/React_rendering.md#image).

Здесь у формы `orders` две группы объектов, `o` и `i`. Их боксы переносятся в контейнер представления, рисует их платформа (`custom = NULL`), а `PlacedTabs` размещает активный:

```lsf
DESIGN orders {
    NEW tabs FIRST {
        custom = 'PlacedTabs';
        MOVE BOX(o) { custom = NULL; }
        MOVE BOX(i) { custom = NULL; }
    }
}
```

```jsx
const { Platform, Caption, Image } = window.lsfusion;

const TABS = ['BOX(o)', 'BOX(i)'];

export function PlacedTabs(props) {
    const [active, setActive] = React.useState(TABS[0]);
    return <div className="placed-tabs">
        <div className="placed-tabs-strip" role="tablist">
            {TABS.map(sid => <button key={sid} role="tab" aria-selected={sid === active}
                                     onClick={() => setActive(sid)}>
                <Image value={props.data[sid].image}/><Caption value={props.data[sid].caption}/>
            </button>)}
        </div>
        <Platform key={active} name={active} className="placed-tabs-body"/>
    </div>;
}
```

Отрисовка только активного дочернего компонента, а не сокрытие остальных, — это и есть то, из-за чего закрытые вкладки перестают читаться, см. [Размещение дочернего компонента](../language/React_platform_components.md#placing). Вкладки нужно делать над `BOX` группы, а не над её `GRID`: заголовок и изображение есть только у контейнера и у свойства, поэтому в записи размещённого `GRID(o)` нет заголовка — она говорит только о том, скрывает ли форма грид.

Полоса вкладок над детьми обоих видов держит одно имя на вкладку — то, по которому она размещает дочерний компонент, — и по нему же читает его запись: идентификатор дизайна — на верхнем уровне, имя свойства — как путь:

```jsx
const TABS = ['BOX(o)', 'o.note'];   // контейнер и панельное свойство o, которое рисует платформа
const entryOf = (data, name) => {
    const dot = name.lastIndexOf('.');
    return name in data || dot < 0 ? data[name] : data[name.slice(0, dot)][name.slice(dot + 1)];
};
// полоса: <Caption value={entryOf(props.data, sid).caption}/>; тело: <Platform key={active} name={active}/>
```

### Кнопки статуса для свойства `SELECT` {#status-buttons}

Нужно, чтобы пользователь выбирал статус заказа рядом кнопок — по кнопке на каждый статус. Для решения используются запись свойства с [выбором](../language/React_projection.md#select) — её поле `select` — и `change` члена свойства.

```lsf
FORM orders 'Orders'
    OBJECTS o = Order PANEL
    PROPERTIES(o) number READONLY, nameStatus SELECT, tags SELECT
;

DESIGN orders {
    NEW card FIRST { custom = 'OrderCard'; MOVE BOX(o); }
}
```

```jsx
const { Caption } = window.lsfusion;

export function OrderCard(props) {
    const { data, controller } = props;
    return <div className="status">
        {data.o.nameStatus.select.list.map(option =>
            <button key={option.key} aria-pressed={!!option.selected}
                    onClick={() => controller.o.nameStatus.change(option)}>
                <Caption value={option.value}/>
            </button>)}
    </div>;
}
```

### Табло котировок по `SCHEDULE` {#live-data}

Нужно показывать котировки, которые меняются, пока форма открыта, и которые форма перечитывает каждые две секунды. Для решения используется событие формы `SCHEDULE`, которое выполняет `formRefresh` ([Обновление по расписанию](../language/React_form_behavior.md#live-data)).

```lsf
FORM quotes 'Котировки'
    OBJECTS q = Quote
    PROPERTIES(q) READONLY symbol, price, changedAt
    EVENTS ON SCHEDULE PERIOD 2 formRefresh()
;

DESIGN quotes {
    BOX(q) {
        custom = 'QuoteBoard';
    }
}
```

```jsx
export function QuoteBoard({data}) {
    return <table><tbody>{data.q.list.map(row =>
        <tr key={row.key}>
            <td>{row.symbol.value}</td>
            <td>{row.price.value}</td>
            <td>{row.changedAt.value?.toLocaleTimeString()}</td>
        </tr>)}</tbody></table>;
}
```

### Выбор вкладки и сворачивание контейнера {#tabs-and-collapse}

Нужно, чтобы представление выбирало вкладку формы, показывало, выбрана ли эта вкладка, и сворачивало контейнер формы. Для решения используются свойство с [`ACTIVE TAB`](../language/ACTIVE_operator.md) и действия с [`ACTIVATE TAB`](../language/ACTIVATE_operator.md) и [`COLLAPSE CONTAINER`](../language/COLLAPSE_operator.md), которые дизайн перемещает в контейнер представления ([Скрытое представление](../language/React_form_behavior.md#hidden-view)).

Здесь `lines` — вкладка контейнера формы `order` с `tabbed`, а `details` — сворачиваемый контейнер той же формы:

```lsf
linesSelected = ACTIVE TAB order.lines;
selectLines() { ACTIVATE TAB order.lines; }
collapseDetails() { COLLAPSE CONTAINER order.details; }

EXTEND FORM order PROPERTIES() linesSelected, selectLines, collapseDetails;

DESIGN order {
    NEW steps FIRST {
        custom = 'Steps';
        MOVE PROPERTY(linesSelected());
        MOVE PROPERTY(selectLines());
        MOVE PROPERTY(collapseDetails());
    }
}
```

```jsx
export function Steps({data, controller}) {
    return <>
        <button aria-pressed={data.linesSelected.value === true} onClick={() => controller.selectLines.exec()}>Строки</button>
        <button onClick={() => controller.collapseDetails.exec()}>Скрыть детали</button>
    </>;
}
```

### Своё меню {#app-menu}

Нужно рисовать меню навигатора своим компонентом, а для части элементов оставить кнопку платформы. Для решения используются [окно навигатора](../language/React_windows.md#navigator-window), [стандартная кнопка](../language/React_windows.md#standard-button) и [`activate`](../language/Form_and_navigator_controllers.md#navigator-controller) контроллера навигатора.

```lsf
WINDOW appMenu VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CUSTOM 'AppMenu';

NAVIGATOR {
    NEW FOLDER sales 'Продажи' WINDOW appMenu PARENT {
        NEW orders;
        NEW invoices;
    }
}
```

```jsx
const { Caption } = window.lsfusion;

export function AppMenu({ data, controller }) {
    function item(name) {
        const e = data.byName[name];
        return <li key={name}>
            <button className={e.selected ? 'on' : ''}
                    onClick={ev => controller.activate(name, ev)}><Caption value={e.caption}/></button>
            {e.children.length > 0 && <ul>{e.children.map(item)}</ul>}
        </li>;
    }
    return <nav><ul>{data.root.map(item)}</ul></nav>;
}
```

То же меню с кнопкой платформы для `Sale.sales`:

```jsx
const { Platform, Caption } = window.lsfusion;

export function AppMenu({ data, controller }) {
    function item(name) {
        const e = data.byName[name];
        return <li key={name}>
            {name === 'Sale.sales'
                ? <Platform name="Sale.sales" className="menu-main"/>
                : <button onClick={ev => controller.activate(name, ev)}><Caption value={e.caption}/></button>}
            {e.children.length > 0 && <ul>{e.children.map(item)}</ul>}
        </li>;
    }
    return <nav><ul>{data.root.map(item)}</ul></nav>;
}
```

```lsf
NAVIGATOR {
    Sale.sales NOCUSTOM;
}
```

Почти стандартное меню пишется так: по одному `<Platform>` на каждый элемент, который рисует платформа, и разметка только для тех, которые компонент рисует сам.

### Своя полоса форм {#forms-board}

Нужно рисовать окно, в котором открываются формы, по-своему: полосой, которая переключает и закрывает открытые формы, над текущей формой. Для решения используется [окно форм](../language/React_windows.md#forms-window).

```lsf
EXTEND WINDOW System.forms CUSTOM 'FormsBoard';
```

```jsx
const { Platform, Caption } = window.lsfusion;

export function FormsBoard({ data, controller }) {
    const current = data.open.find(name => data.byName[name].selected);
    return <div className="board">
        <div className="board-bar">{data.open.map(name => {
            const e = data.byName[name];
            return <span key={name} className={e.selected ? 'on' : ''}>
                <a onClick={() => controller.select(name)}><Caption value={e.caption || '...'}/></a>
                {!e.blocked && <a onClick={() => controller.close(name)}>x</a>}
            </span>;
        })}</div>
        {current && <Platform key={current} name={current} className="board-current"/>}
    </div>;
}
```

### Свой журнал сообщений {#message-log}

Нужно рисовать сообщения пользователю своим журналом — со временем каждого сообщения и кнопкой, которая переключает режим закрепления окна. Для решения используется [окно сообщений](../language/React_windows.md#log-window).

```lsf
EXTEND WINDOW System.log CUSTOM 'MessageLog';
```

```jsx
const { Platform, Caption } = window.lsfusion;

export function MessageLog({ data, controller }) {
    return <div className="log">
        <a className="log-pin" onClick={() => controller.togglePin()}>pin</a>
        {data.messages.map(name => {
            const m = data.byName[name];
            return <div key={name} className={m.failed ? 'log-failed' : 'log-ok'}>
                <div className="log-line">{new Date(m.time).toLocaleTimeString()} <Caption value={m.caption}/></div>
                <Platform name={name}/>
            </div>;
        })}
    </div>;
}
```

### Оболочка приложения {#app-layout}

Нужно самим расположить окна приложения: верхнюю полосу, колонку меню, которую пользователь может свернуть, область форм и журнал сообщений. Для решения используется [расположение окон](../language/React_windows.md#window-layout).

```lsf
NAVIGATOR CUSTOM 'AppLayout';
```

```jsx
const { Platform } = window.lsfusion;

export function AppLayout({ data }) {
    const [menu, setMenu] = React.useState(true);
    // колонка меню есть, пока пользователь её не свернул и платформе есть что в ней показать
    const showMenu = menu && !data.byName['System.toolbar'].hidden;
    return <div className={showMenu ? 'app' : 'app app-no-menu'}>
        <header className="app-top">
            <Platform name="System.logo"/>
            <Platform name="System.root" className="app-sections"/>
            <Platform name="System.system"/>
            <button onClick={() => setMenu(!menu)}>Меню</button>
        </header>
        {showMenu && <aside className="app-menu"><Platform name="System.toolbar"/></aside>}
        <main className="app-work"><Platform name="System.forms"/></main>
        {!data.byName['System.log'].hidden && <footer className="app-log"><Platform name="System.log"/></footer>}
    </div>;
}
```

```css
.app { display: grid; height: 100%; grid-template: "top top" auto "menu work" 1fr "log log" auto / 240px 1fr; }
.app.app-no-menu { grid-template: "top" auto "work" 1fr "log" auto / 1fr; }
.app-top { grid-area: top; display: flex; align-items: center; }
.app-menu { grid-area: menu; }
.app-work { grid-area: work; display: flex; flex-direction: column; min-height: 0; }
.app-log { grid-area: log; height: 120px; display: flex; }
.app-work > .lsf-platform, .app-log > .lsf-platform { flex: 1; }
```

### Шапка приложения {#application-header}

Нужна шапка над областью форм с полем поиска, которое находит покупателей и открывает того, по которому щёлкнули. Для решения используются [чтение проекции](../language/React_projection.md#reading) через `useData` и `useController` и [члены](../language/React_view_controller.md#members) контроллера.

Шапка с полем поиска, которое находит записи, - это форма: ей нужны проекция, обновляемая при изменении данных, сессия, в которой хранится запрос, сортировка и постраничность, и способ открыть найденное. Поэтому она строится как форма и открывается в собственное окно - [`WINDOW ... FORMS`](../language/WINDOW_statement.md) над областью форм - оператором [`SHOW ... WINDOW windowName`](../language/SHOW_operator.md).

Здесь запрос — свойство формы, потому что пустой запрос должен находить ноль записей. Поиск по списку, который просматривает пользователь, по умолчанию использует [фильтр свойства](../language/React_filters_and_toolbar.md#property-filters).

```lsf
// окно, в котором живёт шапка: одна форма, нарисованная одна над областью форм, не выше этой формы
WINDOW header 'Шапка' FORMS AUTOSIZE POSITION(20, 6, 80, 10) HIDETITLE;

// чем шапка ищет и что находит
query 'Поиск' = DATA LOCAL ISTRING[100] ();
found (Customer c) = isISubstring(name(c), query()) OR isISubstring(city(c), query());

// что делает найденная запись: открывается обычной закладкой в System.forms
openCustomer 'Открыть' (Customer c) { SHOW customer OBJECTS c = c NOWAIT; }

// шапка - это ФОРМА: запрос - её свойство, найденное - её группа объектов, сессия - её собственная
FORM appHeader 'Шапка'
    PROPERTIES() query
    OBJECTS c = Customer
    PROPERTIES(c) READONLY name, city
    PROPERTIES(c) openCustomer GRID
    FILTERS found(c)
    ORDERS name(c)
;

// рисует её компонент, по обычной проекции формы
DESIGN appHeader {
    // это шапка, а не документ: никаких «Сохранить», «Отменить», «ОК», «Закрыть»
    REMOVE TOOLBARBOX;
    NEW search {
        custom = 'Search';
        fill = 1;               // контейнер занимает окно, а не сжимается по содержимому
        MOVE PROPERTY(query());
        MOVE BOX(c);
    }
}

// открывается один раз, при старте клиента; перезагрузка страницы - это снова старт клиента
onWebClientStarted() + {
    SHOW appHeader WINDOW header NOWAIT;
}
```

```jsx
// здесь ничего не запрашивается, не кэшируется и не инвалидируется: запрос и найденное принадлежат форме
const { useData, useController } = window.lsfusion;

function Search() {
    const data = useData();
    const controller = useController();
    const query = data.query.value || '';
    const rows = data.c.list;
    const input = React.useRef(null);

    // что поле отправило последним, пока проекция этого не показала: до тех пор проекция отстаёт от поля
    const [sent, setSent] = React.useState(null);
    const send = value => {
        setSent(value);
        controller.query.change(value);
    };
    // поле следует за запросом только если его изменил кто-то другой, и никогда пока пользователь в нём, -
    // поэтому догоняет его, когда пользователь из поля уходит
    const follow = () => {
        if (input.current && sent === null && input.current.value !== query && document.activeElement !== input.current)
            input.current.value = query;
    };
    React.useEffect(() => {
        if (query === sent)
            setSent(null);
        follow();
    }, [query, sent]);

    // где рисуются результаты: под полем и следом за ним
    const [box, setBox] = React.useState(null);
    const place = React.useCallback(() => {
        if (!input.current) return;
        const r = input.current.getBoundingClientRect();
        setBox({ left: r.left, top: r.bottom + 2, width: r.width });
    }, []);
    React.useEffect(place, [query, rows.length, place]);
    React.useEffect(() => {
        const onKey = e => {
            if (e.key !== 'Escape') return;
            if (input.current) input.current.value = ''; // пользователь в поле, поэтому follow() его не трогает
            send('');
        };
        window.addEventListener('resize', place);
        window.addEventListener('scroll', place, true);
        document.addEventListener('keydown', onKey);
        return () => {
            window.removeEventListener('resize', place);
            window.removeEventListener('scroll', place, true);
            document.removeEventListener('keydown', onKey);
        };
    }, [place, controller]);

    return <div className="search">
        <input className="search-input" maxLength={100} placeholder="Поиск клиентов…" ref={input}
               onChange={e => send(e.target.value)} onBlur={follow}/>
        {query && box && ReactDOM.createPortal(
            <div className="search-results" style={{ position: 'fixed', left: box.left, top: box.top, width: box.width, zIndex: 1000 }}>
                {rows.length === 0 && <div className="search-empty">Ничего не найдено</div>}
                {rows.map(row =>
                    <div key={row.key} className="search-row" onClick={() => controller.c.openCustomer.exec(row)}>
                        <b>{row.name.value}</b> <span className="search-city">{row.city.value}</span>
                    </div>)}
            </div>, document.body)}
    </div>;
}

window.lsfusion.custom.register('Search', Search, 'reactView');
```

Всё, что показывает компонент, приходит из проекции, которую он читает через `useData()`: запрос - это `data.query.value`, найденное - `data.c.list`, и платформа держит его актуальным по мере изменения запроса: `controller.query.change(…)` - обычное редактирование, и `FILTERS` формы применяются заново.

Поле ввода намеренно **неуправляемое** - ему не передаётся `value={query}` - и это первое, что представление такого рода обязано сделать правильно.

Значение, которое отправляет поле, проекция показывает сразу, так же, как платформа показывает правку до ответа сервера, и держит последнее из отправленных, пока приходят ответы на предыдущие: ответ сервера заменяет его, только если сервер сохранил иное или отклонил изменение. Но проекция может измениться и пока пользователь печатает - запрос запишет кто-то другой или поправит сервер, - и управляемое React поле перерисовывалось бы из неё во время ввода.

Поэтому поле хранит набранный текст и принимает значение из проекции, только когда запрос изменил кто-то другой и фокус находится вне поля; изменение, сделанное, пока поле в фокусе, поле принимает после потери фокуса. `sent` хранит значение, которое поле отправило последним, пока проекция его не покажет, чтобы поле не приняло своё изменение за чужое. Это рассчитано на то, что запрос сохраняется так, как набран: это свойство `DATA`, а `maxLength` не даёт ввести больше, чем вмещает его `ISTRING[100]`.

Клик выполняет действие, нарисованное на группе, для кликнутой строки - `controller.c.openCustomer.exec(row)` - и запись открывается там, где форма открывается по умолчанию, - закладкой в `System.forms`. Шапка остаётся: окно `FORMS` рисует одну форму за раз, поэтому форма, которую приложение позже откроет `WINDOW header`, заменит её, а `WINDOW` без окна её не тронет.

Двумя вещами шапка перестаёт выглядеть документом:

- `REMOVE TOOLBARBOX` убирает собственную панель формы - сохранять, отменять и закрывать здесь нечего, а кнопки рисует именно этот контейнер.
- Результаты рисуются **порталом в тело страницы**, а не внутри контейнера, и это второе, что представление такого рода обязано сделать правильно.

Окно по замыслу невысокое и обрезает то, что рисует его форма: список, оставленный внутри полосы шапки, срезается после первой строки. Большой `z-index` тоже не спасает: элемент самого окна - это контекст наложения (`position: relative; z-index: 0`), поэтому слой внутри него нельзя отобразить поверх соседних окон. Портал размещает список вне обрезающего его элемента и вне контекста наложения окна.

После этого слой позиционируется по `getBoundingClientRect()` поля и должен менять положение при изменении размера окна и прокрутке, а по Escape - скрываться.

Именно `AUTOSIZE` у окна не даёт полосе стать высокой и пустой: шапка ровно такой высоты, как её форма, - здесь это одно поле ввода, - и при этом растягивается поперёк, а контейнер с `fill = 1` внутри неё занимает эту высоту, а не сжимается по своему содержимому. Размер окна задаёт раскладка, а не форма в нём, поэтому без этой опции полоса занимает ту долю высоты, которую запрашивает `POSITION`, пуста она или полна.

Учтите и то, что клиент запоминает размер, заданный пользователем перетаскиванием мышью, и восстанавливает его вместо объявленного: вернуть объявленные размеры можно действием `Service.resetWindowsLayout`.

Про `POSITION` и навигатор полезно знать две вещи, прежде чем шапка покажется неправильной:

- Числа - это не прямоугольник, в котором рисуется окно, а способ, которым раскладка делит пространство: шапка с `POSITION(20, 6, 80, 10)` становится полосой справа от тулбара и над `System.forms`.
- Пока навигатор не закреплён полностью, его панель - это оверлей, отображаемый поверх соседних окон: щелчок по перекрытой части шапки активирует пункт меню в этом месте.

Форма получает изменения, зафиксированные через другое соединение, только при повторном чтении данных, поэтому шапка, которая должна это показывать, добавляет форме [`EVENTS ON SCHEDULE PERIOD n formRefresh()`](../paradigm/Interactive_view.md). И шапку рисует только десктопный веб-клиент: мобильный веб-клиент и десктоп-клиент рисуют одно окно `System.forms` и открывают шапку там, закладкой, поэтому приложение, которое их обслуживает, оставляет дизайн шапки пригодным и как обычной формы.

### Дерево каталога {#tree-catalogue}

Нужно самим рисовать дерево из категорий и их товаров — с узлами, которые раскрываются и сворачиваются, и текущей строкой, выделенной жирным. Для решения используются поля строк дерева, члены его групп `toggle` и `change` и собственный узел дерева с `current` и `expandCurrent()` ([Деревья](../language/React_trees_sortings_selection.md#trees)).

```lsf
FORM catalogue
    TREE t c = Category PARENT parent(c), i = Item
    PROPERTIES(c) name
    PROPERTIES(i) name
    FILTERS category(i) == c
;

DESIGN catalogue {
    GRID(TREE t) { custom = 'Tree'; }
}
```

```jsx
// a row of the tree, the member of the group it belongs to, and what hangs under it - the tree's current row in bold
function Node({row, member, rows, current}) {
    return (
        <li>
            {row.hasChildren && <button onClick={() => member.toggle(row)}>{row.expanded ? '−' : '+'}</button>}
            <span style={{fontWeight: row.key === current ? 'bold' : 'normal'}} onClick={() => member.change(row)}>
                {row.name.value}
            </span>
            {row.expanded && <ul>{rows.filter(r => r.row.parent === row.key).map(r =>
                <Node key={r.row.key} {...r} rows={rows} current={current}/>)}</ul>}
        </li>
    );
}

export function Tree({data, controller}) {
    const rows = [...data.c.list.map(row => ({row, member: controller.c})),
                  ...data.i.list.map(row => ({row, member: controller.i}))];
    return <div>
        <button onClick={() => controller.t.expandCurrent()}>Раскрыть текущий узел</button>
        <ul>{rows.filter(r => r.row.parent === null).map(r =>
            <Node key={r.row.key} {...r} rows={rows} current={data.t.current}/>)}</ul>
    </div>;
}
```

### Сортируемые заголовки колонок {#sortable-headers}

Нужны заголовки колонок, которые сортируют строки по щелчку и показывают направление сортировки. Для решения используются `orders` группы и его `change` ([Сортировки](../language/React_trees_sortings_selection.md#orders)). Форма — `orders` из раздела [Выбор компонента](../language/React_views.md#selecting-the-component).

```jsx
export function OrderBoard(props) {
    const group = props.data.o;
    const sort = property => {                     // одна сортировка: остальные сохраняют место и приоритет
        const current = group.orders.find(o => o.property === property);
        props.controller.o.orders.change({ property, desc: current ? !current.desc : false });
    };
    const arrow = property => {                    // по чему группа отсортирована, читается обратно из проекции
        const order = group.orders.find(o => o.property === property);
        return order ? (order.desc ? ' ↓' : ' ↑') : '';
    };
    return (
        <table>
            <thead><tr>
                <th onClick={() => sort('number')}>Номер {arrow('number')}</th>
                <th onClick={() => sort('sum')}>Сумма {arrow('sum')}</th>
            </tr></thead>
            <tbody>{group.list.map(row => <tr key={row.key}><td>{row.number.value}</td><td>{row.sum.value}</td></tr>)}</tbody>
        </table>
    );
}
```

### Выбор механизма отбора {#choosing-a-filter-mechanism}

Нужно выбрать, как представление отбирает и упорядочивает строки, которые показывает: текущим объектом группы, [пользовательскими фильтрами](../language/React_filters_and_toolbar.md#filters), фильтром по свойству, группой фильтров, фильтрами представления, сортировкой или свойством формы.

Строки отбираются и сортируются на сервере. Выбирайте механизм по ситуации:

| Ситуация | Механизм |
| --- | --- |
| Запись или категория, выбранная из списка | Текущий объект группы, `controller.<g>.change(row)` |
| Поиск, который вводит пользователь, по одному или нескольким свойствам | [Фильтр по свойству](../language/React_filters_and_toolbar.md#property-filters), `controller.<g>.filter.<p>.change(...)`. Для нескольких свойств — объединяющее свойство или соседние фильтры с `or`, заданные вместе |
| Условия, которые собирает редактор фильтра в представлении | Пользовательские фильтры группы, `controller.<g>.filters.change(...)` |
| Сортировка, которую выбирает пользователь | [Сортировки](../language/React_trees_sortings_selection.md#orders) группы, `controller.<g>.orders.change(...)` |
| Фиксированный набор именованных отборов | [Группа фильтров](../language/React_filters_and_toolbar.md#filter-groups), `FILTERGROUP` |
| Условие, которое задаёт представление и которое пользователь не должен видеть или сбрасывать | [Фильтры представления](../language/React_filters_and_toolbar.md#view-filters) группы, `controller.<g>.viewFilters.change(...)` |
| Значение, которое читает другая логика формы, пустое значение, которое должно отбирать ноль строк, или предикат, который пользовательские фильтры не выражают | Свойство формы (обычно `DATA LOCAL`) и `FILTERS` / `ORDERS` по нему |
| Какая область или закладка показана | Состояние React (`useState`, [`<Platform>`](../language/React_platform_components.md#placing), [`useShown`](../language/React_platform_components.md#placing)) |

### Редактор условий {#condition-editor}

Нужен свой редактор условий фильтра вместо условий, которые рисует платформа: пользователь выбирает свойство, вводит значение и добавляет условие. Для решения используются [пользовательская панель фильтров](../language/React_filters_and_toolbar.md#custom-filter-panel) и [пользовательские фильтры](../language/React_filters_and_toolbar.md#filters) группы с их `filterProperties`.

```lsf
DESIGN items {
    FILTERS(i) { custom = 'FilterPanel'; }
}
```

Представление, которое рисует только `FILTERS(<group>)`, строит по `filterProperties` редактор условия, хотя ни одного свойства группы в нём нет:

```jsx
export function FilterPanel({data, controller}) {
    const typed = data.i.filterProperties.filter(p => p.type === 'string' || p.type === 'number');
    const [choice, setChoice] = React.useState(typed[0]);
    const [text, setText] = React.useState('');
    const value = choice.type === 'number' ? (text === '' ? null : Number(text)) : text;
    return <div>
        <select onChange={e => setChoice(typed[e.target.selectedIndex])}>
            {typed.map(p => <option key={p.property}>{p.caption || p.property}</option>)}
        </select>
        <input type={choice.type === 'number' ? 'number' : 'text'} onChange={e => setText(e.target.value)}/>
        <button onClick={() => controller.i.filters.change({property: choice.property, compare: choice.defaultCompare, value})}>
            Добавить
        </button>
    </div>;
}
```

`value` — того типа, который называет `type`, и преобразуется как любое значение свойства: этот редактор читает строку
или число, поэтому предлагает только свойства `string` и `number`.

### Поиск по одному свойству {#property-search}

Нужно поле поиска по одному свойству списка. Для решения используется [фильтр по свойству](../language/React_filters_and_toolbar.md#property-filters).

```lsf
FORM items
    OBJECTS i = Item
    PROPERTIES(i) name FILTER, price
;

DESIGN items {
    NEW search FIRST {
        custom = 'ItemSearch';
        MOVE FILTER(name(i));
    }
}
```

```jsx
export function ItemSearch({data, controller}) {
    if (data['FILTER(name(i))'].hidden) return null;
    const filter = data.i.filter.name;
    return <label>{filter.caption}
        <input value={filter.value ?? ''} onChange={e => controller.i.filter.name.change(e.target.value)}/>
    </label>;
}
```

### Поиск по нескольким свойствам {#several-properties-search}

Нужно одно поле поиска по нескольким свойствам. Для решения используются [фильтры по свойствам](../language/React_filters_and_toolbar.md#property-filters) и [пакет](../language/Form_and_navigator_controllers.md#batch).

Одно поле ввода может искать по нескольким свойствам без поискового свойства `DATA LOCAL`. Один вариант — объединяющее свойство, объявленное с `FILTER` (здесь `name` и `description` — строковые свойства `Item`):

```lsf
searchText 'Поиск' (Item i) = CONCAT ' ', name(i), description(i);

FORM items
    OBJECTS i = Item
    PROPERTIES(i) searchText FILTER
;

DESIGN items {
    NEW search FIRST {
        custom = 'CombinedItemSearch';
        MOVE FILTER(searchText(i));
    }
}
```

```jsx
export function CombinedItemSearch({data, controller}) {
    const filter = data.i.filter.searchText;
    return <input value={filter.value ?? ''}
                  onChange={e => controller.i.filter.searchText.change(e.target.value)}/>;
}
```

[`CONCAT`](../language/CONCAT_operator.md) пропускает операнды `NULL` и ставит пробел между остальными. Поиск может найти совпадение на стыке полей, поэтому он отличается от «name содержит текст ИЛИ description содержит текст». Объединяющее свойство должно быть в `PROPERTIES`, но показывать его не обязательно: компонент `FILTER(searchText(i))` должен оставаться в дизайне.

Для отдельного совпадения в любом из свойств объявите их фильтры рядом и задавайте оба в одном синхронном пакете `batch`:

```lsf
FORM items
    OBJECTS i = Item
    PROPERTIES(i) name FILTER, description FILTER
;

DESIGN items {
    NEW search FIRST {
        custom = 'NameOrDescriptionSearch';
        MOVE FILTER(name(i));
        MOVE FILTER(description(i));
    }
}
```

```jsx
export function NameOrDescriptionSearch({data, controller}) {
    const change = text => controller.batch(() => {
        controller.i.filter.name.change(text, {compare: '_', negation: false, or: true});
        controller.i.filter.description.change(text, {compare: '_', negation: false, or: false});
    });
    return <input value={data.i.filter.name.value ?? ''} onChange={e => change(e.target.value)}/>;
}
```

При открытии формы пустые фильтры свойств занимают начало списка условий в порядке объявления, поэтому эти два условия соседние. Всегда меняйте и очищайте оба вместе: `or` связывает условие со следующим **активным**, поэтому очистка только второго может связать первое с посторонним условием.

Замена всего списка через `filters.change([...])` переносит пустые фильтры свойств в конец списка условий и не гарантирует, что эта пара останется соседней. Используйте парный вариант, только пока два условия соседние. Иначе используйте объединяющее свойство или явно задавайте весь список условий, когда представление управляет им целиком.

### Только фильтры или вся группа {#filters-or-whole-group}

Нужно решить, заменяет ли компонент только фильтры группы ([Пользовательская панель фильтров](../language/React_filters_and_toolbar.md#custom-filter-panel)) или всю группу.

То, что компонент получает состояние группы, не делает его единственным, кто это состояние рисует. Что рисуется,
решают дизайн и размещение через `<Platform>`, а не содержимое `data`: состояние принадлежит группе, кто бы его ни рисовал.
Отсюда два рецепта, и различаются они в дизайне, а не в проекции:

- **заменить фильтры** — `custom` на `FILTERS(o)`: компонент *и есть* область фильтров;
- **заменить всю группу** — `custom` на `BOX(o)`: больше не рисуется ничего, и фильтры принадлежат компоненту
  вместе со строками.

### Шапка над штатным гридом {#grid-header}

Нужна своя шапка над штатным гридом группы: количество записей, выгрузка в Excel, режим ручного обновления и вид представления. Для решения используется [тулбар](../language/React_filters_and_toolbar.md#toolbar) группы, который рисует представление.

```lsf
DESIGN items {
    NEW header FIRST {
        custom = 'ItemHeader';
        MOVE TOOLBARSYSTEM(i); // грид остаётся платформенным, тулбар — у представления
    }
}
```

```jsx
export function ItemHeader({data, controller}) {
    const items = data.i, buttons = controller.i;
    const [count, setCount] = React.useState(null);
    return <div>
        <button onClick={async () => setCount(await buttons.getCount())}>Сколько</button>
        {count != null && <span>Записей: {count}</span>}
        <button onClick={() => buttons.export()}>В Excel</button>
        <label>
            <input type="checkbox" checked={items.manual}
                   onChange={e => buttons.manual.change(e.target.checked)}/> ручное обновление
        </label>
        {items.manual && <button disabled={!items.updatePending} onClick={() => buttons.update()}>Обновить</button>}
        <select value={items.listView} onChange={e => buttons.listView.change(e.target.value)}>
            {items.listViews.map(view => <option key={view}>{view}</option>)}
        </select>
    </div>;
}
```

### Шапка дерева {#tree-header}

Нужна своя шапка над деревом, которое рисует платформа: раскрытие текущего узла и всего дерева, выгрузка в Excel и показ управляющих элементов фильтров. Для решения используются [тулбар и управляющие элементы фильтров дерева](../language/React_filters_and_toolbar.md#tree-toolbar).

```lsf
DESIGN catalogue {
    NEW header FIRST {
        custom = 'CatalogueHeader';
        MOVE TOOLBARSYSTEM(TREE t); // дерево остаётся платформенным; его тулбар — представления
    }
}
```

```jsx
export function CatalogueHeader({data, controller}) {
    const buttons = controller.t; // t — имя дерева
    return <div>
        <button onClick={() => buttons.expandCurrent()}>Раскрыть текущий узел</button>
        <button onClick={() => buttons.expandTree()}>Раскрыть дерево</button>
        <button onClick={() => buttons.export()}>Excel</button>
        <label>
            <input type="checkbox" checked={data.t.filterControlsVisible}
                   onChange={e => buttons.filterControlsVisible.change(e.target.checked)}/> фильтры
        </label>
    </div>;
}
```

### Карточки товаров в `CUSTOM` {#item-cards}

Нужно показывать строки группы карточками — одним из видов представления, между которыми переключает тулбар. Для решения используется [компонент React в `CUSTOM`](../language/React_component_views.md#form-custom) группы.

```lsf
FORM itemCards
    OBJECTS i = Item CUSTOM 'ItemCards'
    PROPERTIES(i) READONLY name
    PROPERTIES(i) quantity
;
```

```jsx
function ItemCards({data, controller}) {
    return data.list.map(row =>
        <button key={row.key} disabled={row.current}
                onClick={() => controller.change(row)}>
            {row.name.value}: {row.quantity.value}
        </button>
    );
}

window.lsfusion.custom.register('ItemCards', ItemCards, 'reactView');
```

### Контекст темы {#theme-context}

Нужно, чтобы контекст React, который задаёт представление, — тема — доходил до компонентов в ячейках таблицы платформы, которую представление размещает. Для решения используется [компонент React внутри представления React](../language/React_component_views.md#embedded). `Quantity` здесь — компонент свойства в [`CUSTOM`](../language/React_component_views.md#form-custom).

```lsf
FORM items
    OBJECTS i = Item
    PROPERTIES(i) READONLY name
    PROPERTIES(i) quantity CUSTOM 'Quantity'
;

DESIGN items {
    NEW board FIRST {
        custom = 'Board';
        MOVE GRID(i) { custom = NULL; }
    }
}
```

```jsx
const { Platform } = window.lsfusion;
const Theme = React.createContext('light');

export function Board() {
    return <Theme.Provider value="dark">
        <Platform name="GRID(i)"/> {/* платформенная таблица, количество в которой рисует Quantity */}
    </Theme.Provider>;
}

function Quantity({data}) {
    const theme = React.useContext(Theme); // 'dark' в каждой ячейке таблицы, которую размещает Board
    return <span className={theme}>{data.value}</span>;
}

window.lsfusion.custom.register('Quantity', Quantity, 'reactView');
```

### Сохранение состояния компонентов {#keeping-state}

Нужно, чтобы компоненты размещённого дочернего компонента сохраняли состояние, пока представление его не показывает, — как во вкладках ([Компонент React внутри представления React](../language/React_component_views.md#embedded)).

Чтобы компоненты в дочернем компоненте сохраняли состояние, пока он не показывается, оставьте его место отрендеренным и скройте его, вместо того чтобы рендерить место только показываемого дочернего компонента: `<div hidden={name !== active}><Platform name={name}/></div>`. Скрытый так дочерний компонент остаётся размещённым: скрытие его места не прекращает чтение его сервером.

Если сам узел размещения нужно удалить, состояние вместо этого сохраняет `ownRoot`.

### Компонент React или HTML-шаблон {#react-or-template}

Нужно выбрать, рисовать контейнер компонентом React или HTML-шаблоном ([Компонент React и HTML-шаблон](../language/React_views.md#choosing-between-a-react-component-and-an-html-template)).

Своя разметка, которой нужен вычисляемый приложением заголовок — как собственный текст или как заголовок контейнера, — не реализуется одним шаблоном.

Компонент React оправдан тогда, когда расположение вычисляется — дочерние компоненты размещаются по условию, сетка выводится из данных, прочитанных через `useData`, или разметка берётся из библиотеки компонентов. Компонент нужен и тогда, когда разметка должна показать то, что содержит проекция, — а для контейнера и потомка, которого рисует платформа, это его `caption` и `image`.
