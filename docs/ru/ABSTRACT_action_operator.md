---
title: 'Оператор ABSTRACT'
---

Оператор `ABSTRACT` создает [абстрактное действие](Action_extension.md).

### Синтаксис

```
ABSTRACT [type [exclusionType] [order]] [FULL] [(argClassName1, ..., argClassNameN)] [returnClassName [(returnArgClassName1, ..., returnArgClassNameM)]]
```

### Описание

Оператор `ABSTRACT` создает абстрактное действие. Его реализации позже добавляются инструкциями [`ACTION+`](ACTION+_statement.md). В зависимости от выбранного типа платформа формирует из них поведение [оператора ветвления](Branching_CASE_IF_MULTI.md) или [оператора последовательности](Sequence.md).

Оператор `ABSTRACT` является [контекстно-независимым оператором-действием](Action_operators.md#contextindependent), поэтому его можно использовать только в [инструкции `ACTION`](ACTION_statement.md).

### Параметры

- `type`

    Опция. Возможные значения:

    - `CASE` - явная условная форма абстрактного действия. Условие выбора каждой реализации задается в соответствующей инструкции [`ACTION+`](ACTION+_statement.md) с помощью блока `WHEN`.
    - `MULTI` - [полиморфная форма](Branching_CASE_IF_MULTI.md#poly) абстрактного действия. Условием выбора реализации является совместимость текущих аргументов с ее [сигнатурой](ISCLASS_operator.md).
    - `LIST` - последовательная форма абстрактного действия. В этой форме все реализации выполняются по очереди.

    Если опция не указана, по умолчанию используется `MULTI`.

- `exclusionType`

    Опция. Задает [тип взаимоисключения](Branching_CASE_IF_MULTI.md#exclusive). Возможные значения:

    - `EXCLUSIVE` - взаимоисключающий режим для форм `CASE` и `MULTI`. В этом режиме для каждого набора аргументов должна существовать не более чем одна подходящая реализация.
    - `OVERRIDE` - режим для форм `CASE` и `MULTI`, в котором одновременно могут подходить несколько реализаций.

    Используется только вместе с `CASE` или `MULTI`. Для `CASE` по умолчанию используется `OVERRIDE`, для `MULTI` - `EXCLUSIVE`.

- `order`

    Опция. Возможные значения:

    - `FIRST` - для форм `CASE` и `MULTI` с `OVERRIDE` новые реализации добавляются в начало списка, поэтому из подходящих реализаций будет выполнена добавленная последней. Для формы `LIST` задает порядок выполнения, обратный порядку добавления. Если после `OVERRIDE` это значение не указано, оно используется по умолчанию.
    - `LAST` - для форм `CASE` и `MULTI` с `OVERRIDE` новые реализации добавляются в конец списка, поэтому из подходящих реализаций будет выполнена добавленная первой. Для формы `LIST` задает порядок выполнения, совпадающий с порядком добавления, и используется по умолчанию.

    Используется либо после `OVERRIDE` для форм `CASE` и `MULTI`, либо после `LIST`.

- `FULL`

    Ключевое слово. Если задано, платформа автоматически проверит [полноту набора реализаций](Action_extension.md#full): для всех потомков классов аргументов должна существовать хотя бы одна подходящая реализация (или ровно одна, если условия взаимоисключающие).

- `argClassName1, ..., argClassNameN`

    Список имен классов аргументов действия. Каждое имя задается [идентификатором класса](IDs.md#classid). Список может быть пустым. Если список не указан, используются классы параметров действия из [инструкции `ACTION`](ACTION_statement.md), в которой применяется оператор `ABSTRACT`.

- `returnClassName`

    Имя класса значения, которое возвращает действие. Задается [идентификатором класса](IDs.md#classid). Если этот параметр указан, абстрактное действие объявляется как действие с результатом.

- `returnArgClassName1, ..., returnArgClassNameM`

    Список имен классов дополнительных параметров, от которых зависит возвращаемое значение. Используется, если действие возвращает не одно значение, а набор значений по этим параметрам. Каждое имя задается [идентификатором класса](IDs.md#classid). Список может быть пустым.

### Примеры

```lsf
exportXls 'Выгрузить в Excel' ABSTRACT CASE OVERRIDE LAST (Order);
exportXls (Order o) + WHEN name(currency(o)) == 'USD' THEN {
    MESSAGE 'Export USD not implemented';
}

CLASS ABSTRACT Task;
run 'Выполнить' ABSTRACT MULTI EXCLUSIVE FULL (Task);

CLASS Task1 : Task;
name = DATA STRING[100] (Task);
run (Task1 t) + {
    MESSAGE 'Run Task1 ' + name(t);
}

onStarted ABSTRACT LIST ();
onStarted () + {
    MESSAGE 'Подготовка данных';
}
onStarted () + {
    MESSAGE 'Запуск обработчиков';
}

CLASS Issue;
CLASS Language;
localizedTitle = DATA STRING[100] (Issue, Language);

getLocalizedTitle(Issue issue) ABSTRACT STRING[100] (Language);
getLocalizedTitle (Issue issue) + {
    FOR Language l IS Language DO
        RETURN localizedTitle(issue, l);
}
```
