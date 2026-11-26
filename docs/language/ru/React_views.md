---
slug: "/React_views"
title: 'Представления React'
---

*Представление React* — [пользовательское представление](../paradigm/Custom_views.md), которое компонент React рисует в веб-клиенте вместо платформы как часть [дизайна формы](../paradigm/Form_design.md) или [дизайна навигатора](../paradigm/Navigator_design.md). Статьи этого раздела описывают, что такой компонент получает и что вызывает.

Контейнер [`DESIGN`](DESIGN_statement.md) может отрисовываться компонентом React вместо стандартной раскладки. Компонент получает проекцию состояния формы и сам рисует всё поддерево контейнера. Таблицу, тулбар, управляющие элементы фильтров, группу фильтров и фильтр по свойству так же может рисовать собственный компонент React вместо их представления платформы ([Собственное представление компонента](React_component_views.md#component-views)). Вне формы компонент React может рисовать [окно навигатора](React_windows.md#navigator-window), [окно форм](React_windows.md#forms-window), [окно сообщений](React_windows.md#log-window) и [расположение окон](React_windows.md#window-layout).

Это возможность только веб-клиента. Десктоп-клиент десериализует контейнер и отрисовывает его обычное (не React) поддерево, а компонент с собственным компонентом React рисует представлением платформы, поэтому дизайн остаётся рабочим в обоих клиентах.

### Виды представлений React {#kinds}

Представления React бывают следующих видов:

- контейнер дизайна формы: имя компонента задаёт его `custom` в `DESIGN`, и компонент рисует всё поддерево контейнера ([Выбор компонента](#selecting-the-component));
- [собственное представление компонента](React_component_views.md#component-views): имя компонента задаёт `custom` таблицы дерева, тулбара, управляющих элементов фильтров, группы фильтров или фильтра по свойству, и компонент рисует на их месте то, что они показывают из своей группы объектов или дерева;
- [компонент React в `CUSTOM`](React_component_views.md#form-custom): имя компонента задаёт опция `CUSTOM` свойства или группы объектов, и компонент рисует одну ячейку свойства или строки группы;
- [окно или расположение окон](React_windows.md): имя компонента задаёт опция `CUSTOM` инструкции `WINDOW`, а для расположения — инструкции `NAVIGATOR`, и компонент рисует окно навигатора, окно форм или окно сообщений либо располагает окна.

Представление React на форме читает [проекцию формы](React_projection.md) и вызывает члены своего [контроллера](React_view_controller.md).

Остальное описано в других статьях:

- компоненты и хуки платформы для отрисовки строк, изображений и заголовков — в статье [Отрисовка в React](React_rendering.md);
- размещение того, что рисует платформа, — в статье [Компоненты, которые рисует платформа](React_platform_components.md);
- фокус, загрузка формы, клавиши и привязки мыши, обновление по расписанию и скрытое представление — в статье [Поведение формы в React](React_form_behavior.md);
- части группы объектов, которые рисует представление, — в статьях [Деревья, сортировки и выделение в React](React_trees_sortings_selection.md) и [Фильтры и тулбар в React](React_filters_and_toolbar.md);
- контроллеры формы и навигатора, которые получает любое представление на JavaScript, — в статье [Контроллеры формы и навигатора](Form_and_navigator_controllers.md).

### Выбор компонента {#selecting-the-component}

Компонент задаётся [атрибутом `custom`](DESIGN_statement.md#containerprops) контейнера:

```lsf
FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) READONLY number, date, sum
;

DESIGN orders {
    BOX(o) {
        custom = 'OrderBoard';
    }
}
```

Здесь объект `o` отрисовывается компонентом React `OrderBoard` вместо стандартной таблицы. Для компонента, не являющегося контейнером, см. [Собственное представление компонента](React_component_views.md#component-views).

### Размер окна и базовый размер {#base-size}

[Размер окна](../paradigm/Form_design.md#windowSize) и [базовый размер](../paradigm/Form_design.md#components) контейнера задаются через [атрибуты `size`, `width` и `height`](DESIGN_statement.md#component-properties). Например, `size = (900, 600)` задаёт базовый размер контейнеру React, а `size = (-1, -1)` у главного контейнера формы делает окно динамическим по обоим направлениям, `height = -1` — только по высоте. Атрибуты `fill` и `overflowVert` управляют расширением и переполнением по вертикали. Динамическое окно подходит для формы без таблиц с содержимым умеренной высоты.

### Компонент {#the-component}

`OrderBoard` — именованный экспорт из [модуля](Client_JavaScript_modules.md) `.jsx` в каталоге `src/main/web`. Примеры статей о React используют JSX.

Для проекта [с файлами без сборки](Client_JavaScript_modules.md#without-the-build) поместите тот же компонент в файл `.jsx` — он преобразуется на сервере при отдаче. `import` и `export` там недоступны, поэтому ключевое слово `export` опускается, а компонент работает с предоставляемым платформой `window.React` — либо напишите его через `React.createElement` в обычном `.js`. Любой из этих файлов кладётся в `src/main/resources/web/init` (автозагрузка) или в `src/main/resources/web` с регистрацией через `onWebClientInit`.

Компонент — это обычная функция, получающая `props.data` и `props.controller`:

```jsx
export function OrderBoard(props) {
    const orders = props.data.o.list;
    return <div className="order-board">{orders.length} orders</div>;
}
```

Впервые компонент отрисовывается с первой проекцией своего контейнера, когда пришли первые данные формы, и никогда раньше: `props.data` имеет форму, описанную в статье [Проекция формы](React_projection.md), с самой первой отрисовки, поэтому ничего в нём не нужно проверять на ещё не пришедшую проекцию.

Компонент, который рисует одну строку, получает её как `props.row`, когда его отрисовывает [`List`](React_rendering.md#rendering-rows):

```jsx
function Row(props) {
    const r = props.row;
    return (
        <div className={r.current ? "order order-current" : "order"}>
            <span>{r.number.value}</span>
            <span>{r.sum.value}</span>
        </div>
    );
}
```

### Глобальные переменные времени выполнения {#globals}

Компоненты и хуки платформы — глобальные переменные времени выполнения в `window.lsfusion`, как и `custom.register`, который регистрирует компонент для `CUSTOM`. Каждая из них описана в своём разделе:

| Имя | Раздел |
| --- | --- |
| `useData`, `useController` | [Чтение проекции](React_projection.md#reading) |
| `List` | [Отрисовка строк](React_rendering.md#rendering-rows) |
| `BucketScope`, `useBucket`, `Buckets` | [Раскладка строк по ячейкам](React_rendering.md#bucketing) |
| `useSeekOnScroll` | [Отслеживание прокрутки](React_rendering.md#seek-on-scroll) |
| `Image` | [Отрисовка изображения](React_rendering.md#image) |
| `Caption` | [Отрисовка заголовка](React_rendering.md#caption) |
| `Platform`, `usePlatform`, `useShown` | [Размещение дочернего компонента](React_platform_components.md#placing) |
| `useVisible` | [Скрытое представление](React_form_behavior.md#hidden-view) |
| `custom.register` | [Компонент React в `CUSTOM`](React_component_views.md#form-custom) |

### Компонент React и HTML-шаблон {#choosing-between-a-react-component-and-an-html-template}

Для компонента, который только размещает потомков и ничего не читает из `props.data`, см. также [места HTML-шаблона](HTML_templates.md#template-places). Если платформа рисует все дочерние компоненты контейнера, в `props.data` нет ни значений групп, ни значений свойств, поскольку такой компонент не проецируется, — только записи `{ caption, image, hidden }` дочерних компонентов в `props.data`.

Компонент React тоже называет свои дочерние компоненты: чтобы добавить дочерний компонент, нужно изменить JSX, одного `DESIGN` недостаточно.