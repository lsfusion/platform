---
slug: "/ACTIVATE_operator"
title: 'Оператор ACTIVATE'
---

Оператор `ACTIVATE` - создание [действия](../paradigm/Actions.md), которое [активирует](../paradigm/Activation_ACTIVATE.md) один из элементов формы: указанную [форму](../paradigm/Forms.md), закладку, свойство (или действие) на форме либо набор [объектов](../paradigm/Activation_ACTIVATE.md#search) в группе объектов формы.

### Синтаксис 

```
ACTIVATE FORM formName
ACTIVATE TAB formName.componentSelector
ACTIVATE PROPERTY formPropertyId

ACTIVATE [seekDirection] formObjectId = expr
ACTIVATE [seekDirection] formGroupObjectId [OBJECTS formObject1 = expr1, ..., formObjectK = exprK]
```

### Описание

Синтаксис оператора `ACTIVATE` зависит от вида активируемого элемента.

#### Активация формы, закладки или свойства

Формы `ACTIVATE FORM`, `ACTIVATE TAB` и `ACTIVATE PROPERTY` создают действие, которое активизирует форму, закладку или свойство (действие) на форме. Действие не имеет параметров и не использует [контекст](Action_operators.md#contextdependent). Поведение определяется ключевым словом:

- `FORM` — активирует указанную форму у пользователя, если она уже открыта (в виде запроса клиенту). Если форма была открыта несколько раз, активируется та, которая была открыта первой. Если форма не открыта, действие не имеет эффекта.
- `TAB` — делает указанную закладку активной в соответствующей панели закладок. Активация закладки выполняется только в том случае, если указанная форма в момент выполнения действия является активной (тот же вызов на неактивной форме не имеет эффекта). Закладка не активируется, если она является пустым контейнером (без дочерних элементов).
- `PROPERTY` — переводит фокус на указанное свойство или действие, отображаемое на текущей активной форме. Для успешного выполнения указанное свойство должно находиться на форме, выполняющей действие.

#### Активация объектов в группе

Формы `ACTIVATE ... formObjectId = expr` и `ACTIVATE ... formGroupObjectId [OBJECTS ...]` создают действие, выполняющее [активацию объектов в группе](../paradigm/Activation_ACTIVATE.md#search). В первом варианте указывается искомое значение одиночного объекта на форме (этот объект может находиться в составе некоторой группы объектов), во втором — конкретная группа объектов и искомые значения для некоторых её объектов (будем называть их *объектами поиска*).

### Параметры

- `formName`

    Имя формы. [Составной идентификатор](IDs.md#cid).

- `componentSelector`

    [Селектор](DESIGN_statement.md#selector) компонента дизайна. Компонент должен быть закладкой панели вкладок (то есть находиться внутри контейнера со свойством `tabbed = TRUE`).

- `formPropertyId`

    Глобальный [идентификатор свойства или действия на форме](IDs.md#formpropertyid), на которое должен перейти фокус.

- `seekDirection`

    Опция. Задаёт [направление поиска](../paradigm/Activation_ACTIVATE.md#direction). Возможные значения:

    - `FIRST` - для дополнительных объектов выбирается **первый** подходящий набор; для объектов поиска, если искомый набор не найден, выбирается **следующий** ближайший.
    - `LAST` - для дополнительных объектов выбирается **последний** подходящий набор; для объектов поиска, если искомый набор не найден, выбирается **предыдущий** ближайший.
    - `NULL` - текущие значения объектов указанной группы сбрасываются в `NULL`. Для формы с одиночным объектом и для формы с блоком `OBJECTS` сбрасываются все объекты группы, не указанные явно в операторе (в том числе *дополнительные*); явно указанным объектам присваиваются переданные выражения.

    Если опция не указана, используется [тип объектов по умолчанию](Object_blocks.md), заданный у группы объектов (значение `PREV` явно в операторе не указывается).

- `formObjectId`

    Глобальный [идентификатор объекта на форме](IDs.md#groupobjectid), для которого указывается искомое значение.

- `expr`

    [Выражение](Expression.md), значением которого является искомое значение объекта на форме.

- `formGroupObjectId`

    Глобальный [идентификатор группы объектов](IDs.md#groupobjectid), для объектов которой указываются искомые значения.

- `formObject1 ... formObjectK`

    Список имён объектов на форме. Может содержать только часть объектов указанной группы объектов. Имя объекта задаётся [простым идентификатором](IDs.md#id).

- `expr1 ... exprK`

    Список выражений, значения которых являются искомыми значениями соответствующих объектов в указанной группе объектов.

### Примеры

```lsf
//Форма с двумя закладками
FORM myForm 'Моя форма'
    OBJECTS u = CustomUser
    PROPERTIES(u) name

    OBJECTS c = Chat
    PROPERTIES(c) name
;

DESIGN myForm {
    NEW tabbedPane FIRST {
        tabbed = TRUE;
        NEW contacts {
            caption = 'Контакты';
            MOVE BOX(u);
        }
        NEW recent {
            caption = 'Последние';
            MOVE BOX(c);
        }
    }
}

testAction()  {
    ACTIVATE FORM myForm;
    ACTIVATE TAB myForm.recent;
}

CLASS ReceiptDetail;
barcode = DATA STRING[30] (ReceiptDetail);
quantity = DATA STRING[30] (ReceiptDetail);

FORM POS
    OBJECTS d = ReceiptDetail
    PROPERTIES(d) barcode, quantityGrid = quantity
;

createReceiptDetail 'Добавить строку продажи'(STRING[30] barcode)  {
    NEW d = ReceiptDetail {
        barcode(d) <- barcode;
        ACTIVATE PROPERTY POS.quantityGrid;
    }
}
```

```lsf
number = DATA INTEGER (Order);
FORM orders
    OBJECTS o = Order
    PROPERTIES(o) READONLY number, currency, customer
;
newOrder  {
    NEW new = Order {
        number(new) <- (GROUP MAX number(Order o)) (+) 1;
        ACTIVATE orders.o = new;
    }
}
activateFirst  { ACTIVATE FIRST orders.o; }
activateLast  { ACTIVATE LAST orders.o; }

EXTEND FORM orders
    PROPERTIES(o) newOrder, activateFirst, activateLast
;
```
