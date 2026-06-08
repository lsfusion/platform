---
slug: "/Event_block"
title: 'Блок событий'
---

Блоки событий [инструкции `FORM`](FORM_statement.md) - набор конструкций, управляющих [событиями](../paradigm/Form_events.md) в интерактивном представлении формы.

### Синтаксис

```
EVENTS formEventDecl1, ..., formEventDeclN
```

Где каждый `formEventDecli` имеет следующий синтаксис:

```
ON eventType [replaceMode] eventActionId(param1, ..., paramK) | { eventActionOperator }
```

### Описание

Блок событий позволяет задать обработчики [событий формы](../paradigm/Form_events.md), которые возникают в результате определенных действий пользователя. В одном блоке можно через запятую указать произвольное количество обработчиков событий. Если для события указывается несколько обработчиков, то они гарантированно будут выполняться в порядке их задания. 

### Параметры 

- `eventType`

    Тип события формы. Задается одним из следующих ключевых слов:

    - `INIT` 
    - `OK`
    - `OK BEFORE`
    - `OK AFTER`
    - `APPLY`
    - `APPLY BEFORE` 
    - `APPLY AFTER` 
    - `CANCEL`
    - `CLOSE`
    - `DROP`
    - `CHANGE objName`, `[CHANGE] OBJECT objName` - указывают, что действие должно быть выполнено при изменении текущего значения объекта с именем `objName`.
    - `[CHANGE] FILTER groupObjectName` - указывает, что действие должно быть выполнено при изменении фильтра, применённого к группе объектов `groupObjectName`.
    - `[CHANGE] ORDER groupObjectName` - указывает, что действие должно быть выполнено при изменении сортировки, применённой к группе объектов `groupObjectName`.
    - `[CHANGE] FILTERS groupObjectName` - указывает, что действие должно быть выполнено, когда пользователь изменяет пользовательские фильтры группы объектов `groupObjectName`.
    - `[CHANGE] ORDERS groupObjectName` - указывает, что действие должно быть выполнено, когда пользователь изменяет пользовательские сортировки группы объектов `groupObjectName`.
    - `[CHANGE] FILTERGROUPS filterGroupName` - указывает, что действие должно быть выполнено, когда пользователь изменяет фильтр, выбранный в группе фильтров `filterGroupName`.
    - `[CHANGE] FILTERS PROPERTY formPropertyName` - указывает, что действие должно быть выполнено, когда пользователь изменяет значение фильтра по свойству `formPropertyName`.
    - `[CHANGE] PROPERTY formPropertyName` - задаёт обработчик события `CHANGE` для свойства `formPropertyName` на данной форме, замещая ранее заданный обработчик.
    - `[CHANGE] PROPERTY BEFORE formPropertyName`, `[CHANGE] PROPERTY AFTER formPropertyName` - указывают, что действие должно быть выполнено непосредственно до или после изменения значения свойства `formPropertyName` на форме.
    - `QUERYOK`
    - `QUERYCLOSE`
    - `EXPAND componentSelector` - указывает, что действие должно быть выполнено после разворачивания контейнера `componentSelector`. 
    - `COLLAPSE componentSelector` - указывает, что действие должно быть выполнено после сворачивания контейнера `componentSelector`.
    - `TAB componentSelector` - указывает, что действие должно быть выполнено после того, как закладка `componentSelector` стала активна. 
    - `SCHEDULE PERIOD intPeriod [FIXED]` - создаёт планировщик, выполняющий действие каждые `intPeriod` секунд. Ключевое слово `FIXED` указывает на то, что период до следующего действия отсчитывается от старта текущего действия. По умолчанию период отсчитывается от окончания текущего действия.

- `replaceMode`

    Управляет тем, замещает ли обработчик ранее заданные обработчики этого события или добавляется к ним. `REPLACE` замещает все ранее заданные для события обработчики; `NOREPLACE` добавляет обработчик к ним. Если не указано, по умолчанию используется `REPLACE` для `QUERYOK` и `QUERYCLOSE` и `NOREPLACE` для всех остальных событий. К форме `[CHANGE] PROPERTY formPropertyName` (без `BEFORE` / `AFTER`) `replaceMode` не применяется — она всегда замещает свой единственный обработчик.

- `eventActionId`

    [Идентификатор действия](IDs.md#propertyid), которое будет являться обработчиком события.

- `param1, ..., paramK`

    Список параметров действия. Каждый параметр задается именем объекта формы. Имя объекта, в свою очередь, задается [простым идентификатором](IDs.md#id).

- `actionOperator`

    [Контекстно-зависимый оператор-действие](Action_operators.md). В качестве параметров этого оператора можно использовать имена уже объявленных объектов на форме.


### Примеры

```lsf
showImpossibleMessage()  { MESSAGE 'It\'s impossible'; };

posted = DATA BOOLEAN (Invoice);

FORM invoice 'Инвойс' // создаем форму по редактированию инвойса
    OBJECTS i = Invoice PANEL // создаем объект класса инвойс

    //    ...  задаем остальное поведение формы

    EVENTS
        // указываем, что при нажатии пользователем OK должно выполняться действия, 
        // которое выполнит действия по "проведению" данного инвойса
        ON OK { posted(i) <- TRUE; }, 

        // по нажатию кнопки formDrop выдаем сообщение, что такого не может быть, так как эта кнопка 
        // по умолчанию будет показываться только в форме по выбору инвойса, а эта форма по сути 
        // является формой редактирования инвойса
        ON DROP showImpossibleMessage() 
;

CLASS Shift;
currentShift = DATA Shift();

CLASS Cashier;
currentCashier = DATA Cashier();

CLASS Receipt;
shift = DATA Shift (Receipt);
cashier = DATA Cashier (Receipt);

FORM POS 'POS' // объявляем форму для продажи товара покупателю в торговом зале

    OBJECTS r = Receipt PANEL // добавляем объект, в котором будет храниться текущий чек

    //    ... объявляем поведение формы
;

createReceipt ()  {
    NEW r = Receipt {
        shift(r) <- currentShift();
        cashier(r) <- currentCashier();

        ACTIVATE POS.r = r;
    }
}

// расширяем форму обработчиком ON INIT, чтобы при её открытии createReceipt создавал новый чек
// и сразу делал его текущим объектом на форме
EXTEND FORM POS 
    EVENTS
        // при открытии формы выполняем действие по созданию нового чека, которое заполняет смену,
        // кассира и прочую информацию
        ON INIT createReceipt() 
        // применять каждые 60 секунд
        ON SCHEDULE PERIOD 60 FIXED apply(); 
;
```
