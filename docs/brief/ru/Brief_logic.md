---
slug: "/Brief_logic"
title: 'Brief: domain logic'
---

- [Классы](#классы)
- [Свойства](#свойства)
- [Действия](#действия)
- [События](#события)
- [Ограничения](#ограничения)
- [Сессии изменений](#сессии-изменений)

## Классы

### Что такое класс

[Класс](../paradigm/Classes.md) — это множество объектов и базовый элемент,
которым типизируется всё остальное: сигнатура свойства или действия — это
классы его параметров, а у каждого объекта формы есть свой класс. Классы
могут наследоваться, в том числе от нескольких родителей сразу.

```
CLASS [ABSTRACT] name [caption] [: parent1, ..., parentN];
CLASS [NATIVE] name [caption] [{ objectName1 [objectCaption1], ... }] [: parent1, ..., parentN];
```

Вторая форма объявляет класс со СТАТИЧЕСКИМИ объектами — фиксированным
именованным набором, заданным в коде, а не создаваемым во время работы.

**Аналогия**: классы ООП, только диспетчеризация множественная — по классам
всех параметров, а не одного получателя.

### Класс — это не таблица

Это самое дорогое непонимание модели, поэтому стоит сказать прямо. Таблица
хранит не объекты класса, а значения свойств. Её ключевые поля хранят
идентификаторы объектов, а типизируются эти поля классами параметров тех
свойств. Свойство, объявленное без явного `TABLE`, попадает в таблицу, чьи
классы ключей ему подходят. То есть соответствие классов таблицам — следствие
того, как объявлены свойства, а не решение объявления класса. См.
[Brief: физическая модель](Brief_physical.md#execution).

### Полиморфизм

Поведение специализируется по классу через `ABSTRACT`-свойства и действия с
реализациями `+=`, а также через `MULTI`. См.
[абстрактные свойства](#properties) и
[расширение классов](Brief_physical.md#extensions).

## Свойства

### Выражения и композиция

[Свойство](../paradigm/Properties.md) принимает набор объектов-параметров и возвращает ровно одно значение — как чистая функция, но вычисляемая сразу на всей базе, как колонка SQL-запроса. Значение либо хранится ([первичное свойство](../paradigm/Data_properties_DATA.md), [оператор `DATA`](../language/DATA_operator.md)), либо вычисляется выражением: арифметика, логика (`AND`, `OR`, `NOT`), сравнения, операции со строками, проверка и приведение класса (`IS`, `AS`). Функции строк, чисел и дат (`lpad`, `substr`, `mod`, `currentDate`) — не операторы, а свойства модулей `Utils` и `Time`.

Подстановка свойства в выражение другого свойства — [композиция](../paradigm/Composition_JOIN.md); [оператор `JOIN`](../language/JOIN_operator.md) записывает ее явно. Список создающих свойства операторов — [Операторы](../paradigm/Property_operators_paradigm.md).

```lsf
price = DATA NUMERIC[14,2] (Item);
vat = DATA NUMERIC[6,2] (Item);
priceWithVAT (Item i) = price(i) * (1 + vat(i) / 100);
```

### Группировка (GROUP)

[Оператор `GROUP`](../language/GROUP_operator.md) разбивает все наборы объектов на группы и вычисляет для каждой одну агрегирующую функцию — `SUM`, `MAX`, `MIN`, `CONCAT`, `LAST`, `EQUAL`, `AGGR` / `NAGGR`, `CUSTOM` (агрегат СУБД):

```
GROUP
type [expr1, ..., exprN]
[orderClause]
[TOP topExpr] [OFFSET offsetExpr]
[WHERE whereExpr]
[BY groupExpr1, ..., groupExprM]
```

Отдельной функции подсчета нет: количество считается как `GROUP SUM 1`. **Аналогия**: `GROUP BY` в SQL, только результат — самостоятельное свойство, а не часть запроса. Механизм — какие наборы объектов попадают в группу, какими получаются параметры создаваемого свойства и где важен порядок — описан в [группировке](../paradigm/Grouping_GROUP.md).

```lsf
sold (Sku s) = GROUP SUM quantity(OrderDetail d) BY sku(d);
```

### Разбиение и сортировка (PARTITION)

[Оператор `PARTITION`](../language/PARTITION_operator.md) тоже разбивает наборы объектов на группы блоком `BY`, но возвращает результат не на группу, а на каждый набор объектов — по окну внутри его группы, заданному порядком `ORDER`. Агрегирующие функции здесь — `SUM`, `PREV`, `LAST`, `CUSTOM`: отсюда места и ранги, сквозная нумерация, накопительные итоги, значение предыдущего или последнего набора в окне. **Аналогия**: оконные функции SQL (`OVER (PARTITION BY ... ORDER BY ...)`). Что именно попадает в окно, описано в [разбиении / упорядочивании](../paradigm/Partitioning_sorting_PARTITION_..._ORDER.md).

Форма `PARTITION UNGROUP` решает обратную задачу — [распределяет](../paradigm/Distribution_UNGROUP.md) значение свойства-источника по наборам объектов группы: пропорционально заданному выражению (`PROPORTION`) или по порядку (`LIMIT`).

```lsf
place (Team t) = PARTITION SUM 1 ORDER DESC points(t), t BY conference(t);
```

### Агрегация объектов (GROUP AGGR, AGGR)

Эти операторы работают не со значениями, а с объектами.

`GROUP AGGR` — форма [оператора `GROUP`](../language/GROUP_operator.md), возвращающая сам объект группы. Получается отображение, обратное перечисленным в `BY` свойствам, — например, поиск объекта по коду; платформа добавляет ограничение, что в группе такой объект не более одного.

[Оператор `AGGR`](../language/AGGR_operator.md) идет дальше: он сам создает объект, когда агрегируемое выражение становится не `NULL`, и удаляет, когда оно снова `NULL`, заполняя свойства-отображения на параметры. Механизм — [агрегации](../paradigm/Aggregations.md).

```lsf
countryName = GROUP AGGR Country c BY name(c);
shipment (Invoice i) = AGGR ShipmentInvoice WHERE createShipment(i);
```

### Выбор и переопределение (CASE, IF, OVERRIDE)

[Оператор выбора](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) проверяет условия по порядку и возвращает результат первого выполненного; условие выполнено, если его значение не `NULL`.

- [`CASE`](../language/CASE_operator.md) — явные пары `WHEN ... THEN ...` и необязательный `ELSE`.
- [`IF`](../language/IF_operator.md) — постфиксная одиночная форма `result IF condition`; [`IF ... THEN`](../language/IF_..._THEN_operator.md) добавляет блок `ELSE`.
- [`OVERRIDE`](../language/OVERRIDE_operator.md) — первый операнд, не равный `NULL`; так же подставляется значение по умолчанию вместо `NULL`.
- [`EXCLUSIVE`](../language/EXCLUSIVE_operator.md) — то же плюс декларация, что не `NULL` не более одного операнда.
- [`MULTI`](../language/MULTI_operator.md) — операнд выбирается по совместимости классов аргументов с его сигнатурой.

```lsf
signedQuantity (Ledger l) = MULTI quantity[InLedger](l), quantity[OutLedger](l);
price (Item i) = OVERRIDE salePrice(i), basePrice(i), 0;
```

### Рекурсия (RECURSION)

[Оператор `RECURSION`](../language/RECURSION_operator.md) создает свойство, вычисляемое итерациями; к нему обращаются на деревьях, графах и транзитивных замыканиях — уровень узла, все предки объекта, достижимость по цепочке ссылок. Его части — `STEP`, префикс `$` перед параметром и опция `CYCLES` — и то, как считаются итерации, описаны в [рекурсии](../paradigm/Recursion_RECURSION.md).

```lsf
level (Group child, Group parent) = RECURSION 1 IF child IS Group AND parent = child
                                              STEP 1 IF parent = parent($parent);
```

### Абстрактные свойства (ABSTRACT)

[Оператор `ABSTRACT`](../language/ABSTRACT_operator.md) объявляет свойство без реализации: базовый модуль задает класс значения и классы параметров, а другие модули добавляют реализации [инструкцией `+=`](../language/plus_equals_statement.md). Из них платформа собирает оператор выбора — это [расширение свойств](../paradigm/Property_extension.md), способ снять зависимость между модулями и получить полиморфизм свойств.

Какая реализация выбирается и что требуется от реализаций, задается опциями — `MULTI`, `CASE`, `VALUE`, `EXCLUSIVE`, `OVERRIDE`, `FULL` — и описано в [расширении свойств](../paradigm/Property_extension.md).

```lsf
name 'Наименование' = ABSTRACT ISTRING[250] (Document);      // базовый модуль
name (Shipment s) += ISTRING[250]('Поставка ' + number(s));  // модуль поставок
```

## Действия

### Изменение состояния (`<-`, NEW, DELETE)

[Оператор `CHANGE`](../language/CHANGE_operator.md) пишет значение в изменяемое свойство, [оператор `NEW`](../language/NEW_operator.md) добавляет [объект](../paradigm/New_object_NEW.md) конкретного класса, операторы [`DELETE`](../language/DELETE_operator.md) и [`CHANGECLASS`](../language/CHANGECLASS_operator.md) удаляют объект или переводят его в другой класс:

```
[CHANGE] propertyId(expr1, ..., exprN) <- valueExpr [WHERE whereExpr]
NEW className WHERE whereExpr [TO propertyId(prm1, ..., prmN)]
NEW [alias =] className [AUTOSET] action
DELETE expr [WHERE whereExpr]
CHANGECLASS expr TO className [WHERE whereExpr]
```

[Изменение](../paradigm/Property_change_CHANGE.md) пишется сразу по всем наборам аргументов, удовлетворяющим условию, одной операцией над множеством. **Аналогия**: `UPDATE ... SET ... WHERE`, а не присваивание переменной. При [смене класса или удалении](../paradigm/Class_change_CHANGECLASS_DELETE.md) платформа обнуляет хранимые значения первичных свойств, в которых объект стал недопустим.

```lsf
setDiscount () {
    discount(Customer c) <- 15 WHERE totalOrders(c) > 100;
    NEW o = Order { date(o) <- currentDate(); }
}
```

### Вызов и последовательность

[Действие](../paradigm/Actions.md) двойственно свойству: свойство говорит, чему значение равно, действие — как оно меняется. Объявляется [инструкцией `ACTION`](../language/ACTION_statement.md):

```
name [caption] [(param1, ..., paramN)] { actionBody } [options]
```

Тело в фигурных скобках — [последовательность](../paradigm/Sequence.md): вложенные действия выполняются в порядке записи; внутри блока можно объявить свойства `LOCAL`, живущие только во время его выполнения. [Вызов действия](../paradigm/Call_EXEC.md) записывается именем с аргументами, `[EXEC] actionId(expression1, ..., expressionN) [TO toProperty]`, либо подставляется прямо как значение. **Аналогия**: вызов процедуры.

### Циклы (FOR, WHILE)

[Оператор `FOR`](../language/FOR_operator.md) выполняет тело по разу на каждый набор объектов, для которого условие не `NULL`; [оператор `WHILE`](../language/WHILE_operator.md) пересчитывает условие на каждом шаге, поэтому изменения из тела учитываются:

```
FOR expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[TOP topExpr] [OFFSET offsetExpr]
[NEW [alias =] className]
DO action
[ELSE alternativeAction]

WHILE expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[NEW [alias =] className]
DO action
```

К циклу обращаются, когда тело действительно построчное — диалог, сообщение, внешний вызов. Механизмы — [цикл](../paradigm/Loop_FOR.md) и [рекурсивный цикл](../paradigm/Recursive_loop_WHILE.md).

```lsf
createDetails (Order o) {
    FOR in(Sku s) NEW d = OrderDetail DO {
        order(d) <- o;
        sku(d) <- s;
    }
}
```

### Ветвление (CASE, IF)

[Ветвление](../paradigm/Branching_CASE_IF_MULTI.md) вызывает действие, соответствующее выполнившемуся условию; условие выполнено, если его значение не `NULL`. В операторах [`IF ... THEN`](../language/IF_..._THEN_action_operator.md) и [`CASE`](../language/CASE_action_operator.md) условие записывается явно, в [операторе `MULTI`](../language/MULTI_action_operator.md) условие — соответствие аргументов вызова сигнатуре действия, то есть диспетчеризация по классу аргумента:

```
IF condition
THEN action
[ELSE alternativeAction]

CASE [exclusionType]
    WHEN condition1 THEN action1
    ...
    WHEN conditionN THEN actionN
    [ELSE elseAction]

MULTI [exclusionType] action1, ..., actionN
```

```lsf
message (Shape s) { MULTI { message[Square](s); }, { message[Circle](s); } }
```

Отложенный вариант — абстрактное действие [`ABSTRACT`](../language/ABSTRACT_action_operator.md): базовый модуль объявляет точку расширения, реализации к ней добавляют другие модули ([расширение действий](../paradigm/Action_extension.md)).

### Управление потоком

- [`BREAK`](../paradigm/Interruption_BREAK.md) выходит из ближайшего цикла, [`CONTINUE`](../paradigm/Next_iteration_CONTINUE.md) переходит к следующей итерации, [`RETURN [выражение]`](../paradigm/Exit_RETURN.md) выходит из ближайшего вызова действия с указанным значением как результатом.
- [`TRY action [CATCH catchAction] [FINALLY finallyAction]`](../language/TRY_operator.md) — `CATCH` поглощает [ошибку](../paradigm/Exception_handling_TRY.md), давая доступ к ней через `messageCaughtException[]` и `lsfStackTraceCaughtException[]`; `FINALLY` выполняется в любом случае. **Аналогия**: `try` / `catch` / `finally`.
- [`NEWTHREAD action [dispatchClause]`](../language/NEWTHREAD_operator.md) — выполнение в [отдельном потоке](../paradigm/New_threads_NEWTHREAD_NEWEXECUTOR.md), сразу или по расписанию (`SCHEDULE`: задержка и период). [`NEWEXECUTOR`](../language/NEWEXECUTOR_operator.md) выбирает, куда отправить поток: в серверный пул, где тело работает в [сессии изменений](../paradigm/Change_sessions.md) вызывающего кода, или в клиентское соединение (`CLIENT`), где оно получает собственную новую сессию в навигаторе этого соединения.

### Действия на форме

- [`SHOW`](../language/SHOW_operator.md) — [открытие формы](../paradigm/In_an_interactive_view_SHOW_DIALOG.md) в интерактивном представлении; переданные объекты становятся текущими.
- [`DIALOG`](../language/DIALOG_operator.md) — то же открытие как диалог ввода значения: каждый объект, помеченный `INPUT` или `CHANGE`, возвращает в блок `DO` свое последнее текущее значение.
- [`ACTIVATE`](../language/ACTIVATE_operator.md) — [активация](../paradigm/Activation_ACTIVATE.md) формы, вкладки, свойства или набора объектов в группе объектов.
- [`EXPAND`](../language/EXPAND_operator.md) / `COLLAPSE` — разворачивание и сворачивание [контейнера формы](../paradigm/Container_visibility_EXPAND_COLLAPSE.md) и узлов [дерева объектов](../paradigm/Object_tree_visibility_EXPAND_COLLAPSE.md).
- [`MESSAGE`](../paradigm/Show_message_MESSAGE_ASK.md) и [`INPUT`](../paradigm/Value_input.md) — сообщение и ввод значения без отдельной формы.

```
ACTIVATE FORM formName
ACTIVATE TAB formName.componentSelector
ACTIVATE PROPERTY formPropertyId

ACTIVATE [seekDirection] formObjectId = expr
ACTIVATE [seekDirection] formGroupObjectId [OBJECTS formObject1 = expr1, ..., formObjectK = exprK]
```

## События

### События данных (WHEN)

[Событие](../paradigm/Events.md) выполняет заданное действие — свою *обработку* — при изменении данных. [Блок описания события](../language/Event_description_block.md) задает, глобальное событие или локальное — для всей базы или в пределах [сессии изменений](../paradigm/Change_sessions.md) — ключевыми словами `GLOBAL` и `LOCAL`, ограничивает событие заданными формами через `FORMS` и упорядочивает его обработку относительно других через `AFTER`.

Событие на изменение данных создают три инструкции:

- [`WHEN`](../language/WHEN_statement.md) — [простое событие](../paradigm/Simple_event.md): обработка выполняется на каждый набор объектов, на котором условие не `NULL`;
- [`<- WHEN`](../language/lt-_WHEN_statement.md) — [вычисляемое событие](../paradigm/Calculated_events.md): вместо обработки задается изменение первичного свойства;
- [`ON`](../language/ON_statement.md) — событие общего вида: обработка выполняется один раз на все изменения.

```
WHEN eventClause eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;
propertyId(param1, ..., paramN) <- valueExpr WHEN eventExpr;
ON eventClause eventAction;
```

**Аналогия**: триггер базы данных, но условием служит произвольное свойство над всей базой.

```lsf
sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));
```

### События формы (ON)

[События формы](../paradigm/Form_events.md) возникают на открытой форме: в точке её жизни (`INIT`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`), на действие пользователя либо по таймеру через `SCHEDULE`. Обработка подключается опцией `ON` — в блоках [событий](../language/Event_block.md), [свойств и действий](../language/Properties_and_actions_block.md) и [объектов](../language/Object_blocks.md#objects) инструкции `FORM` либо в [опциях свойства](../language/Property_options.md).

| Что событие обслуживает | События |
| ----------------------- | ------- |
| форму целиком | `INIT`, `QUERYCLOSE`, `QUERYOK`, `OK`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`, `SCHEDULE` |
| объект формы | `CHANGE` |
| группу объектов | `FILTER`, `ORDER`, `SELECT`, `FILTERS`, `ORDERS` |
| группу фильтров | `FILTERGROUPS` |
| свойство или действие | `CHANGE`, `CHANGEWYS`, `GROUPCHANGE`, `EDIT`, `CONTEXTMENU`, `KEYPRESS`, `FILTERS PROPERTY`, `SELECT PROPERTY` |
| контейнер | `EXPAND`, `COLLAPSE`, `TAB` |

Постфиксы `BEFORE` и `AFTER` дают моменты до и после операции. Для событий изменения свойства есть стандартные обработки `READONLY`, `READONLYIF` и `SELECTOR`.

```lsf
FORM sku 'Товар'
    OBJECTS s = Sku
    PROPERTIES(s) price ON CHANGE changePrice(s)
    EVENTS ON INIT initSku()
;
```

### Порядок выполнения

Обработки локальных событий выполняются не в момент изменения данных, а в определенные моменты жизни сессии — см. [выполнение локальных событий](../paradigm/Events.md#local). Обработки синхронных глобальных событий выполняются внутри транзакции [применения изменений](../paradigm/Apply_changes_APPLY.md), вместе с проверками [ограничений](../paradigm/Constraints.md).

Порядок между обработками, реагирующими на одно и то же изменение, определяют зависимости по данным; явно он задается ключевым словом `AFTER` (синоним `GOAFTER`) в [блоке описания события](../language/Event_description_block.md).

## Ограничения

### Ограничения (CONSTRAINT)

[Ограничение](../paradigm/Constraints.md) — свойство, значение которого всегда должно быть `NULL`. Проверяется оно на событии, заданном [блоком описания события](../language/Event_description_block.md) инструкции, а по умолчанию — на глобальном событии `APPLY`; если к этому моменту оно стало не `NULL` хотя бы на одном наборе объектов, платформа показывает сообщение с этими наборами и [отменяет](../paradigm/Cancel_changes_CANCEL.md) изменения. Создается [инструкцией `CONSTRAINT`](../language/CONSTRAINT_statement.md):

```
CONSTRAINT [eventClause] constraintExpr [CHECKED [BY propertyId1, ..., propertyIdN]] MESSAGE messageExpr
    [PROPERTIES outExpr1, ..., outExprM];
```

Опция `CHECKED BY` заставляет диалог изменения перечисленных свойств отфильтровывать значения, нарушающие ограничение.

[Простые ограничения](../paradigm/Simple_constraints.md) задаются видом связи между свойствами, а не выражением, Реализованы два вида: следствие — [инструкция `=>`](../language/=gt_statement.md) — и определенность — [опция `NONULL`](../language/Property_options.md). Следствие умеет разрешать нарушение само: его клауза `RESOLVE [LEFT] [RIGHT]` указывает платформе, какую сторону исправлять. У `NONULL` такой клаузы нет — у него есть `[DELETE]`, удаляющий нарушившие объекты.

**Аналогия**: `CHECK` и `NOT NULL` в SQL, но условием служит произвольное свойство над всей базой.

```lsf
CONSTRAINT balance(Sku s, Stock st) < 0 MESSAGE 'Остаток не может быть отрицательным';
```

### Агрегируемые объекты и ограничения

[Агрегация](../paradigm/Aggregations.md) создает *агрегируемый* объект, когда агрегируемое свойство становится не `NULL` на наборе значений параметров, и удаляет этот объект, когда свойство снова становится `NULL`. Создает и удаляет объект только [оператор `AGGR`](../language/AGGR_operator.md); [`GROUP AGGR`](../language/GROUP_operator.md) строит обратное свойство — от значений параметров к объекту. Оба описаны в [Brief: свойства](Brief_logic.md#свойства).

Для целостности это еще один инвариант, который платформа поддерживает сама: на набор значений параметров приходится не более одного агрегируемого объекта. `GROUP AGGR` добавляет его как ограничение на свою группу, а `AGGR` держит в его рамках соответствие между объектами и значениями параметров. Как это ограничение поддерживается, описано в [агрегациях](../paradigm/Aggregations.md) и [простых ограничениях](../paradigm/Simple_constraints.md).

```lsf
// поставка по инвойсу создается, когда у инвойса ставится признак создания поставки, и удаляется, когда он снимается
shipment (Invoice i) = AGGR ShipmentInvoice WHERE createShipment(i);
```

## Сессии изменений

### Что такое сессия

[Сессия изменений](../paradigm/Change_sessions.md) — место, где изменения накапливаются локально, а не пишутся в базу сразу. В нее попадают изменения [первичных свойств](../paradigm/Data_properties_DATA.md), в том числе локальных, и изменения классов объектов — созданные и удаленные объекты, `CHANGECLASS`. Пока изменения не применены, они остаются в этой сессии; что из них увидит другая сессия, решают вместе открывающий ее оператор — `NESTEDSESSION` показывает все изменения верхней сессии, `NEWSESSION` читает из базы, перенося локальные свойства, названные его `NESTED` — списком в скобках либо все сразу через `NESTED LOCAL`, — изменения классов, если задан `CLASSES`, и, перечислено оно или нет, всё объявленное `DATA LOCAL NESTED`, — и объявления самих свойств, ведь объявленное `DATA LOCAL NESTED` переносится само; текущую сессию действию задает контекст выполнения — сессия формы, сессия вызывающего действия либо сессия, переданная платформой.

Значение свойства на начало сессии возвращает [оператор `PREV`](../paradigm/Previous_value_PREV.md), а производные от него [операторы изменений](../paradigm/Change_operators_SET_CHANGED_etc.md) `SET`, `DROPPED`, `CHANGED`, `SETCHANGED`, `DROPCHANGED`, `SETDROPPED` отвечают, что именно изменилось в сессии.

**Аналогия**: незафиксированная транзакция базы данных, живущая все время работы пользователя с формой.

### NEWSESSION и NESTEDSESSION

[Оператор `NEWSESSION`](../language/NEWSESSION_operator.md) выполняет вложенное действие в отдельной [сессии](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md), изолированной от текущей; [оператор `NESTEDSESSION`](../language/NESTEDSESSION_operator.md) — во вложенной, которая копирует в себя изменения текущей сессии, а применение внутри нее возвращает изменения обратно. Если любой из этих операторов выполняется во время [транзакции применения](../paradigm/Apply_changes_APPLY.md) текущей сессии, в этот момент сессия не создается — вложенное действие откладывается и выполняется в текущей сессии внутри той же транзакции.

```
NEWSESSION [NEWSQL] [FORMS formId1, ..., formIdM] [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] action
NESTEDSESSION [SINGLE] action
```

Опция `NESTED` перечисляет переносимые локальные свойства, `NEWSQL` открывает сессию на отдельном SQL-соединении. Они взаимоисключающи: написанные вместе, они разберутся, но победит `NEWSQL`, а вся клауза `NESTED` будет проигнорирована.

К новой сессии обращаются, когда действие — отдельная единица работы: диалог, импорт, обработка файла, фоновая запись.

```lsf
logError (STRING message) {
    NEWSESSION NEWSQL {
        NEW e = LogEntry { text(e) <- message; }
        APPLY;
    }
}
```

### APPLY и CANCEL

[Оператор `APPLY`](../language/APPLY_operator.md) [применяет изменения](../paradigm/Apply_changes_APPLY.md) — записывает накопленное в базу, выполняя по дороге обработки глобальных [событий](../paradigm/Events.md) и проверки [ограничений](../paradigm/Constraints.md). Во вложенной сессии он не пишет в базу ничего: изменения копируются обратно в сессию, в которую вложена эта. [Оператор `CANCEL`](../language/CANCEL_operator.md) [отменяет изменения](../paradigm/Cancel_changes_CANCEL.md) — очищает сессию, кроме случая внутри транзакции применения, где он вместо этого отменяет выполняющийся `APPLY`.

```
APPLY [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] [SERIALIZABLE] [action]
CANCEL [NESTED [nestedPropertySelector] [CLASSES]]
```

Итог применения читается из свойств `System.canceled[]` и `System.applyMessage[]`.

**Аналогия**: `COMMIT` и `ROLLBACK`.

### Видимость изменений между сессиями

Вне транзакции применения то, что видит новая сессия, задают вместе создавший ее оператор и объявления самих свойств, между двумя краями: сессия, наследующая непримененные изменения верхней, и сессия, которая их не видит и читает зафиксированное состояние базы. Это не переключатель — `NESTED (...)` переносит перечисленные локальные свойства, а `CLASSES` независимо переносит изменения классов, так что унаследовать можно и часть состояния.

Оператор — не единственное место, где это решается. Свойство, объявленное `DATA LOCAL NESTED`, переносится само, без перечисления в операторе, а `MANAGESESSION` / `NOMANAGESESSION` в этом объявлении сужают перенос до `APPLY` / `CANCEL` или до `NEWSESSION` соответственно ([`DATA`](../language/DATA_operator.md), [`NEWSESSION`](../language/NEWSESSION_operator.md)).

`NESTEDSESSION` — один край: видны все изменения верхней сессии. `NEWSESSION` — другой: свойства читаются из базы, кроме объявленных `DATA LOCAL NESTED` — те переносятся сами, — а опции `NESTED` и `CLASSES` расширяют перенос. `NEWSQL`, наоборот, сужает его: на собственном соединении не переносится ничего, объявлено `NESTED` или нет.

Что дает каждое сочетание, описано в [создании сессий](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md) и в [операторе `NEWSESSION`](../language/NEWSESSION_operator.md).
