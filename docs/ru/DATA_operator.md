---
title: 'Оператор DATA'
---

Оператор `DATA` - создание [первичного свойства](Data_properties_DATA.md).

### Синтаксис

```lsf
DATA [LOCAL [NESTED [MANAGESESSION | NOMANAGESESSION]]] returnClass [(argumentClass1, ..., argumentClassN)]
```

### Описание

Оператор `DATA` создает первичное свойство. Этот [оператор-свойство](Property_operators_paradigm.md) не может использоваться внутри [выражений](Expression.md). Первичное свойство может быть создано локальным, за это отвечает ключевое слово `LOCAL`. 

Для локального свойства можно дополнительно указать `NESTED`. Тогда свойство становится [вложенным](Session_management.md#nested), и его значения сохраняются при операциях управления сессиями. Если после `NESTED` не указан дополнительный модификатор, вложенность действует и для создания новой сессии, и для операций управления текущей сессией. Модификатор `MANAGESESSION` оставляет вложенность только для `APPLY` / `CANCEL`, а `NOMANAGESESSION` - только для `NEWSESSION`.

Этот оператор нельзя использовать в [операторе `JOIN`](JOIN_operator.md) (внутри `[ ]`), так как для первичного свойства обязательно должно быть задано имя.

### Параметры

- `LOCAL`

    Ключевое слово, при указании которого создается [локальное первичное свойство](Data_properties_DATA.md#local). 

- `NESTED`

    Ключевое слово, которое можно использовать только после `LOCAL`. Помечает локальное свойство как [вложенное](Session_management.md#nested). Без дополнительных модификаторов это означает, что свойство будет считаться вложенным и при [создании новой сессии](NEWSESSION_operator.md), и при `APPLY` / `CANCEL`.

- `MANAGESESSION` | `NOMANAGESESSION`

    Ключевые слова, которые можно использовать только после `NESTED`.

    - `MANAGESESSION` - свойство считается вложенным только для операций, управляющих текущей сессией (`APPLY`, `CANCEL`).
    - `NOMANAGESESSION` - свойство считается вложенным только при переходе в `NEWSESSION` и обратно.

- `returnClass`

    [Идентификатор класса](IDs.md#classid) возвращаемого значения свойства. 

- `argumentClass1, ..., argumentClassN`

    Список идентификаторов классов аргументов свойства. Может быть пустым; в этом случае обычно используется `()`.

### Примеры

```lsf
CLASS Item;
quantity = DATA LOCAL INTEGER (Item);

sessionOwners = DATA LOCAL NESTED MANAGESESSION INTEGER ();

CLASS Order;
selected = DATA LOCAL NESTED NOMANAGESESSION BOOLEAN (Order);

CLASS Country;
isDayOff = DATA BOOLEAN (Country, DATE);
```
