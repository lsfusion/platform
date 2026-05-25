---
slug: "/TRY_operator"
title: 'Оператор TRY'
---

Оператор `TRY` создает [действие](../paradigm/Actions.md), которое выполняет некоторое действие с [обработкой исключений](../paradigm/Exception_handling_TRY.md).

### Синтаксис

```
TRY action [CATCH catchAction] [FINALLY finallyAction]
```

### Описание

Оператор `TRY` создает действие, которое выполняет другое действие и занимается обработкой исключений в нем. Поведение при обработке исключений зависит от наличия ключевого слова `FINALLY`.

Без ключевого слова `FINALLY` ошибки, возникающие в основном действии, перехватываются и никуда не передаются.

С ключевым словом `FINALLY` `finallyAction` выполняется после основного действия вне зависимости от того, возникла ли ошибка. Если ошибка возникла и блок `CATCH` отсутствует, ошибка повторно передается окружающему действию после выполнения `finallyAction`.

### Параметры

- `action`

    [Контекстно-зависимый оператор](Action_operators.md#contextdependent), описывающий действие, которое будет выполнено с обработкой исключений.

- `catchAction`

    Контекстно-зависимый оператор, описывающий действие, которое будет выполнено в случае возникновения ошибки во время выполнения действия. При этом сообщение ошибки будет записано в свойство `messageCaughtException[]`, java-стек ошибки будет записан в `javaStackTraceCaughtException[]`, а LSF стек - в `lsfStackTraceCaughtException[]`.

- `finallyAction`

    Контекстно-зависимый оператор, описывающий действие, которое будет выполнено после выполняемого действия вне зависимости от того, возникла ошибка или нет.

### Примеры

```lsf
tryToImport(FILE f)  {
    TRY {
        LOCAL a = BPSTRING[10] (INTEGER);

        IMPORT XLS FROM f TO a = A;
    }
}

CLASS MyLock {
    lock 'Блокировка'
}

singleDo ()  {
    NEWSESSION {
        lock(MyLock.lock);
        IF lockResult() THEN
        TRY {
            MESSAGE 'Lock Obtained';
        } CATCH {
            MESSAGE messageCaughtException();
        } FINALLY unlock(MyLock.lock);
    }
}
```
