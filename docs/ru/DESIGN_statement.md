---
title: 'Инструкция DESIGN'
---

Инструкция `DESIGN` - изменение [дизайна формы](Form_design.md).

## Синтаксис

Синтаксис представляет собой вложенные друг в друга блоки *инструкций дизайна*. Внешний блок, начинающийся с ключевого слова `DESIGN`, определяет [форму](Forms.md), дизайн которой будет изменяться: 

```
DESIGN formName [caption] [CUSTOM] {
    designStatement1
    ...
    designStatementN
}
```

Каждый `designStatement` описывает одну инструкцию дизайна. Инструкции дизайна бывают следующих типов: 

```
NEW name [insertPos] [{...}];
MOVE selector [insertPos] [{...}];  
selector [{...}];   
REMOVE selector;
propertyName = value;
```

Первые три инструкции: *создание* (`NEW`), *перемещение* (`MOVE`) и *редактирование* могут в свою очередь содержать вложенные блоки инструкций дизайна. Инструкции дизайна *удаление* (`REMOVE`) и *изменение значения свойства* (`=`) являются простыми одиночными инструкциями. Каждая инструкция дизайна должна завершаться точкой с запятой, если в ней не содержится вложенный блок инструкций.

<a className="lsdoc-anchor" id="selector"/>

Каждый `selector` может быть одного из следующих типов:

```
componentName
PROPERTY(formPropertyName)
FILTERGROUP(filterGroupName)
PARENT(selector)
GROUP([propertyGroupSelector][,groupObjectTreeSelector])
noGroupObjectTreeContainerType
groupObjectTreeContainerType(groupObjectTreeSelector)
```

В свою очередь, `groupObjectTreeSelector` может быть одного из двух видов:

```
groupObjectSelector
TREE treeSelector
```

## Описание

При помощи инструкции `DESIGN` разработчик может управлять [дизайном](Form_design.md) [интерактивного представления](Interactive_view.md) формы путем создания, перемещения и удаления контейнеров и компонент, а также задания им определенных свойств. По умолчанию для каждой формы создается [дизайн по умолчанию](Form_design.md#defaultDesign) вместе с соответствующими контейнерами. При необходимости можно пересоздать дизайн без созданных по умолчанию контейнеров и выполненных ранее настроек. Осуществляется это с помощью ключевого слова `CUSTOM`.  

Каждый блок инструкций дизайна, заключенный в фигурные скобки, позволяет изменять некоторый компонент и его потомков, будем называть этот компонент *текущим компонентом* либо *текущим контейнером*, если нам известно, что компонент в данной ситуации должен являться контейнером. Во внешнем блоке, идущем после ключевого слова `DESIGN`, текущим компонентом является контейнер `main`. Существует следующие виды инструкций дизайна:

- *Инструкция создания* (`NEW`) позволяет создать новый контейнер, делая его потомком текущего контейнера. Текущим компонентом в блоке инструкций дизайна, содержащемся в этой инструкции, будет являться созданный контейнер.
- *Инструкция перемещения* (`MOVE`) позволяет сделать некоторый существующий компонент непосредственным потомком текущего контейнера. Предварительно этот компонент удаляется из предыдущего родительского контейнера. Текущим компонентом в блоке инструкций дизайна, содержащемся в этой инструкции, будет являться перемещаемый компонент. 
- *Инструкция редактирования* позволяет изменить указанный компонент, который должен являться потомком (не обязательно непосредственным) текущего контейнера. Текущим компонентом в блоке инструкций дизайна, содержащемся в этой инструкции, будет являться указанный элемент.
- *Инструкция удаления* (`REMOVE`) позволяет удалить указанный компонент из иерархии компонентов. Удаляемый компонент должен являться потомком текущего контейнера. 
- *Инструкция изменения значения свойства* (`=`) позволяет изменить значение указанного свойства текущего компонента.

Иерархия компонент, описываемая в рамках одной инструкции, может иметь произвольный уровень вложенности и описывать любое количество компонентов и их свойств на каждом из уровней.

Для обращения к компонентам дизайна можно использовать их имя, а также обращаться к компонентам свойств на форме (`PROPERTY`), родительскому компоненту (`PARENT`), контейнерам групп свойств (`GROUP`) и другим базовым компонентам / компонентам дизайна по умолчанию.

## Параметры

### Общие параметры

- `formName`

    Имя изменяемой формы. [Составной идентификатор](IDs.md#cid).

- `caption`

    Новый заголовок формы в интерактивном режиме отображения. [Строковый литерал](Literals.md#strliteral). В [навигаторе](Navigator.md) заголовок формы при этом не изменяется.

- `name`

    Имя создаваемого контейнера. [Простой идентификатор](IDs.md#id).

- `insertPos`

    Указание позиции вставки или перемещения компонента. Может задаваться одним из следующих способов:

    - `BEFORE selector`
    - `AFTER selector` 
    
        Указание того, что компонент должен быть добавлен или перенесен непосредственно перед (`BEFORE`) или после (`AFTER`) указанного компонента. Указанный компонент должен быть непосредственным потомком текущего контейнера. 
    
    - `FIRST`
    
        Ключевое слово, указывающее на то, что компонент должен быть добавлен или перенесен в начало списка непосредственных потомков текущего контейнера.

    - `LAST`

        Ключевое слово, указывающее на то, что компонент должен быть добавлен или перенесен в конец списка непосредственных потомков текущего контейнера. В отличие от добавления по умолчанию, компоненты, вставленные с помощью `LAST`, всегда будут располагаться после всех компонентов, добавленных в порядке объявления.

    - `DEFAULT`

        Ключевое слово, указывающее на то, что компонент добавляется в список непосредственных потомков текущего контейнера в порядке объявления. Является значением по умолчанию.

- `propertyName`

    Имя свойства компонента. Список существующих свойств перечислен в таблицах ниже.

- `value`

    Значение, присваиваемое соответствующему свойству контейнера. Допустимый тип значения можно посмотреть в таблицах ниже.

### Свойства компонентов

|Имя свойства|Описание|Вид значения|Значение по умолчанию|Примеры|
|---|---|---|---|---|
|`span`|todo|[Целочисленный литерал](Literals.md#intliteral)|`1`|`2`|
|`defaultComponent`|Указание того, что на данный компонент должен быть выставлен фокус при инициализации формы. Может быть установлен только для одного компонента на всей форме|Дополненный [логический литерал](Literals.md#booleanliteral)|`FALSE`|`TRUE`<br/>`FALSE`|
|`activated`|todo|[Логический литерал](Literals.md#booleanliteral)|`FALSE`|`TRUE`<br/>`FALSE`|
|`fill`|Аналогично свойству `flex`, но кроме того, если устанавливается нулевое значение, то свойство `align` устанавливается в `START`, иначе `align` устанавливается в `STRETCH`|Литерал типа `NUMERIC`|`0`|`1.5`|
|`size`|Базовый размер компонента в пикселях (значение `-1` означает, что размер не установлен)|Пара [целочисленных литералов](Literals.md#intliteral) (ширина, высота)|`(-1, -1)`|`(100, 20)`|
|`height`|Базовый размер компонента по высоте в пикселях.|Целочисленный литерал|`-1`|`50`|
|`width`|Базовый размер компонента по ширине в пикселях.|Целочисленный литерал|`-1`|`20`|
|`flex`|Коэффициент расширения. Значение свойства, аналогичного свойству [CSS flex-grow](http://www.w3schools.com/cssref/css3_pr_flex-grow.asp). Задает насколько компонент должен расти в размерах относительно других компонентов|[Литерал типа `NUMERIC`](Literals.md#numericliteral)|`0`|`0.25`|
|`shrink`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`alignShrink`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`align`<br/>`alignment`|Способ выравнивания компонента внутри контейнера. Допустимые значения: `START` (В начале), `CENTER` (В центре), `END` (В конце), `STRETCH` (Растянуть).|Тип выравнивания|`START`|`STRETCH`|
|`overflowHorz`|todo|Логический литерал|`auto`|`clip`<br/>`visible`<br/>`auto`|
|`overflowVert`|todo|Логический литерал|`auto`|`clip`<br/>`visible`<br/>`auto`|
|`marginTop`|Отступ сверху|Целочисленный литерал|`0`|`3`|
|`marginRight`|Отступ справа|Целочисленный литерал|`0`|`1`|
|`marginBottom`|Отступ снизу|Целочисленный литерал|`0`|`4`|
|`marginLeft`|Отступ слева|Целочисленный литерал|`0`|`1`|
|`margin`|Отступ. Устанавливает одинаковое значение свойствам `marginTop`, `marginRight`, `marginBottom`, `marginLeft`|Целочисленный литерал|`0`|`5`|
|`captionFont`|Шрифт, который будет использоваться для отображения заголовка компонента|[Строковый литерал](Literals.md#strliteral)|зависит от компонента|`'Tahoma bold 16'`<br/>`'Times 12'`|
|`font`|Шрифт, который будет использоваться для отображения текста компонента, например, значения свойства, заголовка действия, текста в таблице|Строковый литерал|зависит от компонента|`'Tahoma bold 16'`<br/>`'Times 12'`|
|`class`|todo|Строковый литерал|NULL|todo|
|`fontSize`|Размер шрифта, который будет использоваться для отображения текста компонента|Числовой литерал|зависит от компонента|`10`|
|`fontStyle`|Стиль шрифта, который будет использоваться для отображения текста компонента. Может содержать слова `'bold'` и/или `'italic'`, либо пустую строку|Строковый литерал|`''`|`'bold'`<br/>`'bold italic'`|
|`background`|Цвет, который будет использоваться для отображения фона компонента|[Литерал класса `COLOR`](Literals.md#colorliteral)|`#FFFFFF`|`#FFFFCC`<br/>`RGB(255, 0, 0)`|
|`foreground`|Цвет, который будет использоваться для отображения текста компонента|Цвет |`NULL`|`#FFFFCC`<br/>`RGB(255, 0, 0)`|
|`showIf`|Указание условия, при котором контейнер будет отображаться|[Выражение](Expression.md)|`NULL`|`isLeapYear(date)`<br/>`hasComplexity(a, b)`|

### Свойства контейнеров

|Имя свойства|Описание|Вид значения|Значение по умолчанию|Примеры|
|---|---|---|---|---|
|`caption`|Заголовок контейнера|Строковый литерал|`NULL`|`'Заголовок'`|
|`image`|todo|Строковый литерал|`NULL`|todo|
|`collapsible`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`border`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`collapsed`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`horizontal`|Горизонтальный контейнер|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`tabbed`|Контейнер с табами|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`childrenAlignment`|Способ выравнивания дочерних компонентов внутри контейнера. Допустимые значения: `START`, `CENTER`, `END`|Тип выравнивания|`START`|`CENTER`|
|`alignCaptions`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`grid`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`wrap`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`resizeOverflow`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`custom`|todo|String literal|NULL|todo|
|`lines`|Количество линий (рядов или колонок) в контейнере|Целочисленный литерал|`1`|`3`|
|`lineSize`|todo|Целочисленный литерал|NULL|todo|
|`captionLineSize`|todo|Целочисленный литерал|NULL|todo|

### Свойства свойств и действий на форме

|Имя свойства|Описание|Вид значения|Значение по умолчанию|Примеры|
|---|---|---|---|---|
|`autoSize`|Автоматическое определение размера для компонента. Применяется только для текстовых компонентов|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`boxed`|Рисование рамки вокруг компонента|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`panelCaptionVertical`|Указание того, что в панели необходимо рисовать заголовок компонента свойства или действия сверху от значения|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCaptionLast`|Указание того, что в панели необходимо рисовать сначала значение, а потом заголовок свойства|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCaptionAlignment`|Способ выравнивания заголовка компонента. Допустимые значения: `START` (В начале), `CENTER` (В центре), `END` (В конце), `STRETCH` (Растянуть).|Тип выравнивания|`START`|`STRETCH`|
|`changeOnSingleClick`|Указание того, что при однократном нажатии мышкой на компонент свойства, необходимо начинать редактирование|Дополненный логический литерал|зависит от свойства|`TRUE`<br/>`FALSE`|
|`focusable`|Указание того, что компонент свойства (действия) или колонка в таблице могут владеть фокусом|Дополненный логический литерал|changeKey = `NULL`|`TRUE`<br/>`FALSE`|
|`hide`|Указание того, что компонент свойства (действия) должен быть всегда спрятан|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`regexp`|Регулярное выражение, которому должно соответствовать значение свойства при редактировании|Строковый литерал|`NULL`|`'^((8\|\\+7)[\\- ]?)?(\\(?\\d\{3\}\\)?[\\- ]?)?[\\d\\- ]\{7,10\}$'`|
|`regexpMessage`|Сообщение, которое будет выдано пользователю, если он введет значение не соответствующее регулярному выражению|Строковый литерал|сообщение по умолчанию|`'Неправильный формат телефона'`|
|`pattern`|Шаблон форматирования значения свойства. Синтаксис задания шаблона аналогичен синтаксису [DecimalFormat](https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html) либо [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) в зависимости от типа значения|Строковый литерал|`NULL`|`'#,##0.00'`|
|`maxValue`|Максимальное числовое значение, которое позволяет ввести компонент свойства|Целочисленный литерал|`NULL`|`1000000`<br/>`5000000000L`|
|`echoSymbols`|Указание того, что вместо значения свойства будет показываться набор символов `*`. Используется, например, для паролей|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`noSort`|Запрет сортировки|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`defaultCompare`|Фильтр по умолчанию. Допустимые значения: `EQUALS`, `GREATER`, `LESS`, `GREATER_EQUALS`, `LESS_EQUALS`, `NOT_EQUALS`, `CONTAINS`, `LIKE`.|Строковый литерал|`CONTAINS`|`GREATER`|
|`valueSize`|Ширина и высота ячейки значения свойства в пикселях|Пара целочисленных литералов (ширина, высота)|`(-1, -1)`|`(100, 100)`|
|`valueHeight`|Высота ячейки значения свойства в пикселях|Целочисленный литерал|зависит от свойства|`100`|
|`valueWidth`|Ширина ячейки значения свойства в пикселях|Целочисленный литерал|зависит от свойства|`100`|
|`captionHeight`|Высота заголовка свойства в пикселях|Целочисленный литерал|`-1`|`100`|
|`captionWidth`|Ширина заголовка свойства в пикселях|Целочисленный литерал|`-1`|`100`|
|`charHeight`|Высота ячейки значения свойства в символах (рядах).|Целочисленный литерал|зависит от свойства|`2`|
|`charWidth`|Ширина ячейки значения свойства в символах|Целочисленный литерал|зависит от свойства|`10`|
|`valueFlex`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`changeKey`|Клавиша, при нажатии которой будет начато редактирование свойства. Принцип задания аналогичен заданию параметра в [Keystroke.getKeystroke(String)](https://docs.oracle.com/javase/8/docs/api/javax/swing/KeyStroke.html#getKeyStroke-java.lang.String-)|Строковый литерал|`NULL`|`'ctrl F6'`<br/>`'BACK_SPACE'`<br/>`'alt shift X'`|
|`changeKeyPriority`|**deprecated since version 6, используйте параметр `priority` в `changeKey`**|Целочисленный литерал|`NULL`|`1000`|
|`changeMouse`|todo|Строковый литерал|`NULL`|`'DBLCLK'`|
|`changeMousePriority`|**deprecated since version 6, используйте параметр `priority` в `changeMouse`**|Целочисленный литерал|`NULL`|`1000`|
|`showChangeKey`|Указание того, что в заголовок свойства будет включено название сочетания клавиш, по которому будет начато редактирование|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`focusable`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelColumnVertical`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`valueClass`|todo|Строковый литерал|NULL|todo|
|`captionClass`|todo|Строковый литерал|NULL|todo|
|`caption`|Заголовок свойства или действия|Строковый литерал|заголовок свойства или действия|`'Заголовок'`|
|`tag`|todo|Строковый литерал|NULL|todo|
|`imagePath`<br/>`image`|Путь к файлу с картинкой, которая будет отображаться в качестве иконки действия. Путь указывается относительно каталога `images`|Строковый литерал|`NULL`|`'image.png'`|
|`comment`|Комментарий свойства или действия|Строковый литерал|Комментарий свойства или действия|`'Комментарий'`|
|`commentClass`|Класс комментария свойства или действия|Строковый литерал|Класс комментария свойства или действия|`'comment-class'`|
|`panelCommentVertical`|Указание того, что в панели необходимо рисовать комментарий свойства или действия сверху или снизу от значения|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCommentFirst`|Указание того, что в панели необходимо рисовать сначала комментарий, а потом значение свойства|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCommentAlignment`|Способ выравнивания компонента комментария внутри контейнера. Допустимые значения: `START` (В начале), `CENTER` (В центре), `END` (В конце), `STRETCH` (Растянуть).|Тип выравнивания|`START`|`STRETCH`|
|`placeholder`|Placeholder свойства|Строковый литерал|placeholder свойства|`'Placeholder'`|
|`tooltip`|Подсказка, которая будет показываться при наведении пользователем мышки на заголовок свойства или действия|Строковый литерал|tooltip по умолчанию|`'Подсказка'`|
|`valueTooltip`|Подсказка, которая будет показываться при наведении пользователем мышки на значение свойства|Строковый литерал|tooltip по умолчанию|`'Подсказка'`|
|`valueAlignment`|Способ выравнивания значения компонента. Допустимые значения: `START` (В начале), `CENTER` (В центре), `END` (В конце), `STRETCH` (Растянуть).<br/>**deprecated since version 6, используйте `valueAlignmentHorz`**|Тип выравнивания|`START`|`STRETCH`|
|`valueAlignmentHorz`|Способ выравнивания по горизонтали значения компонента. Допустимые значения: `START` (В начале), `CENTER` (В центре), `END` (В конце), `STRETCH` (Растянуть).|Тип выравнивания|`START`|`STRETCH`|
|`valueAlignmentVert`|Способ выравнивания по вертикали значения компонента. Допустимые значения: `START` (В начале), `CENTER` (В центре), `END` (В конце), `STRETCH` (Растянуть).|Тип выравнивания|`START`|`STRETCH`|
|`valueOverflowHorz`|todo|Строковый литерал|`auto`|`auto`<br/>`clip`<br/>`visible`|
|`valueOverflowVert`|todo|Строковый литерал|`auto`|`auto`<br/>`clip`<br/>`visible`|
|`valueShrinkHorz`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`valueShrinkVert`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`clearText`|Указание того, что в начале редактирования свойства должен сбрасываться текущий текст|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`notSelectAll`|Указание того, что в начале редактирования весь текст не выделяется|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`askConfirm`|Указание того, что при попытке редактирования свойства (выполнении действия) будет выполнен запрос на подтверждение|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`askConfirmMessage`|Текст вопроса о подтверждении редактирования свойства (выполнении редактирования)|Строковый литерал|сообщение по умолчанию|`'Вы действительно хотите изменить это свойство?'`|
|`toolbar`|todo|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`notNull`|Указание того, что в случае `NULL` значения свойства, компонент этого свойства должен быть подсвечен|Дополненный логический литерал|`notNull` свойства|`TRUE`<br/>`FALSE`|
|`select`|todo|Строковый литерал|NULL|todo|

### Свойства тулбара

|Имя свойства|Описание|Вид значения|Значение по умолчанию|Примеры|
|---|---|---|---|---|
|`visible`|Указание видимости компонента|Логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showViews`|Показывать кнопки переключения режимов отображения|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showGroup`|Показывать кнопки переключения режимов отображения<br/>**deprecated since version 6, используйте `showViews`**|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showFilters`|Показывать кнопку настройки фильтров|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showSettings`|Показывать кнопку настройки таблицы|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showCountQuantity`|Показывать кнопку подсчета количества рядов|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showCalculateSum`|Показывать кнопку подсчета суммы по колонке|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showPrintGroupXls`|Показывать кнопку экспорта таблицы в xls-формат|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|
|`showManualUpdate`|Показывать кнопку ручного обновления|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|

### Свойства таблицы

|Имя свойства|Описание|Вид значения|Значение по умолчанию|Примеры|
|---|---|---|---|---|
|`autosize`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`boxed`|Рисование рамки вокруг компонента|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`tabVertical`|Указание того, что переход фокуса между ячейками будет осуществляться сверху вниз, а не слева направо|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`quickSearch`|Указание того, что в таблице будет осуществляться быстрый поиск элемента|`FALSE`|`TRUE`<br/>`FALSE`|
|`headerHeight`|Высота заголовка в пикселях|Целочисленный литерал|NULL|`60`|
|`hierarchicalWidth`|Ширина первой колонки дерева|Целочисленный литерал|NULL|`100`|
|`hierarchicalCaption`|Заголовок первой колонки дерева|Строковый литерал|'Дерево'|`Заголовок дерева`|
|`resizeOverflow`|todo|Логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|
|`lineWidth`|todo|Целочисленный литерал|NULL|`60`|
|`lineHeight`|todo|Целочисленный литерал|NULL|`60`|
|`enableManualUpdate`|Включить режим ручного обновления по умолчанию|Дополненный логический литерал|`FALSE`|`TRUE`<br/>`FALSE`|

### Другие свойства

|Имя свойства|Действует для|Описание|Вид значения|Значение по умолчанию|Примеры|
|---|---|---|---|---|---|
|`visible`|пользовательский фильтр, дерево классов|Указание видимости компонента для задания пользовательских фильтров (дерева классов)|Дополненный логический литерал|`TRUE`|`TRUE`<br/>`FALSE`|

### Параметры `selector`

- `componentName`

    Имя компонента дизайна. [Простой идентификатор](IDs.md#id).

- `formPropertyName`

    [Имя свойства / действия на форме](Properties_and_actions_block.md#name).

- `filterGroupName`

    Имя [группы фильтров](Filters_and_sortings_block.md#filterName). [Простой идентификатор](IDs.md#id).

- `propertyGroupSelector`

    Имя [группы свойств / действий](Groups_of_properties_and_actions.md). [Простой идентификатор](IDs.md#id).

- `groupObjectSelector`

    Имя [группы объектов на форме](Object_blocks.md#groupName). [Простой идентификатор](IDs.md#id).

- `treeSelector`

    Имя [дерева объектов на форме](Object_blocks.md#treeName). [Простой идентификатор](IDs.md#id).

- `noGroupObjectTreeContainerType`

    Тип контейнера формы:

    - `BOX` - общий контейнер формы
    - `PANEL` - содержит компоненты свойств, которые отображаются в панель (`PANEL`), и для которых группа отображения не определена.
    - `TOOLBARBOX` - общий контейнер тулбара.
    - `TOOLBARLEFT `- левая часть тулбара
    - `TOOLBARRIGHT` - правая часть тулбара
    - `TOOLBAR` - содержит компоненты свойств, которые отображаются в тулбар (`TOOLBAR`), и для которых группа отображения не определена.

- `groupObjectTreeContainerType`

    Тип контейнера группы объектов / дерева:

    - Все типы контейнера формы `noGroupObjectTreeContainerType` (семантика аналогична).
    - `GRID` - компонент таблицы
    - `TOOLBARSYSTEM` - системный тулбар (количество записей, групповая корректировка и т.п.).
    - `FILTERGROUPS` - содержит компоненты групп фильтров
    - `FILTERS` - компонент, который отображает пользовательские фильтры

## Примеры

```lsf
DESIGN order { // настраиваем дизайн формы, начиная с дизайна по умолчанию
    // отмечаем, что все изменения иерархии будут происходит для самого верхнего контейнера
    // создаем новый контейнер самым первым перед системными кнопками, в который положим два контейнера -
    // шапка и спецификации
    NEW orderPane FIRST { 
        fill = 1; // указываем, что контейнер должен занимать все доступное ему место
        MOVE BOX(o) { // переносим в новый контейнер все, что касается объекта o
            PANEL(o) { // настроим как отображаются свойства в панели объекта o
                horizontal = FALSE; // делаем, чтобы все потомки шли сверху вниз
                NEW headerRow1 { // создаем контейнер - первый ряд
                    horizontal = TRUE;
                    MOVE PROPERTY(date(o)) { // переносим свойство даты заказа
                        // "перегружаем" заголовок свойства в дизайне формы (вместо стандартного)
                        caption = 'Дата редактируемого заказа'; 
                        // задаем подсказку для свойства дата заказа    
                        tooltip = 'Введите сюда дату, когда был сделан заказ'; 
                        background = #00FFFF; // делаем фон красным
                    }
                    MOVE PROPERTY(time(o)) { // переносим свойство времени заказа
                        foreground = #FF00FF; // делаем цвет зеленым
                    }
                    MOVE PROPERTY(number(o)) { // переносим свойство номер заказа
                        // ставим, что пользователю желательно должно показываться 5 символов
                        charWidth = 5; 
                    }
                    MOVE PROPERTY(series(o)); // переносим свойство серия заказа
                }
                NEW headerRow2 {
                    horizontal = FALSE; // потомки - сверху вниз
                }
                MOVE PROPERTY(note(o));
            }

            size = (400, 300); //указываем, что контейнер o.box должен иметь базовый размер 400x300 пикселей
        }
        // создаем контейнер, в котором будут хранится различные спецификации по заказу
        NEW detailPane { 
            // помечаем, что этот контейнер должен быть панелью закладок, где закладками являются его потомки
            tabbed = TRUE;
            // добавляем контейнер с строками заказа как одну из закладок верхней панели
            MOVE BOX(d) { 
                caption = 'Строки'; // задаем заголовок панели закладки
                // делаем, чтобы колонка с номером строки никогда не могла иметь фокус
                PROPERTY(index(d)) { focusable = FALSE; } 
                GRID(d) {
                    // делаем, чтобы по умолчанию фокус при открытии формы устанавливался на таблицу строк
                    defaultComponent = TRUE; 
                }
            }
            MOVE BOX(s) { // добавляем контейнер с итогами по sku как одну из закладок detailPane
                caption = 'Подбор';
            }
        }
    }
}

// разбиваем определение формы на две инструкции (вторую инструкцию можно перенести в другой модуль)
DESIGN order {
    // убираем из иерархии контейнер с кнопками печати и экспорта в xls, тем самым делая их невидимыми
    REMOVE TOOLBARLEFT; 
}
```

На выходе получаем следующую форму:

![](images/DESIGN_instruction.png)
