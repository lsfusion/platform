---
title: 'Оператор INTERNAL'
---

Оператор `INTERNAL` - создание [действия](Actions.md), выполняющего [внутренний вызов](Internal_call_INTERNAL.md).

### Синтаксис

Оператор имеет две формы:

    INTERNAL className [(classId1, ..., classIdN)] [NULL]
    INTERNAL <{anyTokens}> [NULL]

### Описание

Оператор `INTERNAL` создает действие, которое вызывает код, написанный на языке программирования Java. Первая форма оператора позволяет указать полное имя java-класса. Этот класс должен быть унаследован от java-класса `lsfusion.server.physics.dev.integration.internal.to.InternalAction` и в нем должен быть реализован метод `executeInternal`, который будет выполнен в момент вызова действия.

Вторая форма оператора позволяет внутри блока `<{...}>` написать некоторый код на языке `Java`, который будет являться кодом метода `executeInternal` в сгенерированном java-классе. В этом коде можно обращаться к единственному параметру метода `executeInternal` -  `context` класса `lsfusion.server.logics.action.controller.context.ExecutionContext`.

### Параметры

- `className`

    Полное имя java-класса (fully qualified name). [Строковый литерал](Literals.md#strliteral).

- `classId1, ..., classIdN`

    Список [идентификаторов классов](IDs.md#classid) аргументов действия. Если не указывается, то создаваемое действие будет иметь ноль параметров.

- `NULL`

    Ключевое слово, при указании которого в действие можно передавать параметры, равные `NULL`.

- `anyTokens`

    Исходный код, написанный на языке программирования Java. 

### Примеры

```lsf
showOnMap 'Показать на карте' 
    INTERNAL 'lsfusion.server.logics.classes.data.utils.geo.ShowOnMapAction' (DOUBLE, DOUBLE, MapProvider, BPSTRING[100]);

serviceDBMT 'Обслуживание БД (многопоточно, threadCount, timeout)' 
    INTERNAL 'lsfusion.server.physics.admin.service.action.ServiceDBMultiThreadAction' (INTEGER, INTEGER) NULL;

printlnAction 'Вывести текст в консоль' INTERNAL <{ System.out.println("action test"); }>;
// здесь context - это параметр метода executeInternal
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>; 
```
