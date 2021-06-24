---
title: 'Оператор NEWTHREAD'
---

Оператор `NEWTHREAD` - создание [действия](Actions.md), которое выполняет другое действие в [новом потоке](New_threads_NEWTHREAD_NEWEXECUTOR.md).

### Синтаксис

    NEWTHREAD action [CONNECTION connectionExpr]
    NEWTHREAD action SCHEDULE [PERIOD periodExpr] [DELAY delayExpr]

### Описание

Оператор `NEWTHREAD` создает действие, которое выполняет другое действие в новом потоке. При указании ключевого слова `CONNECTION` можно указать соединение, которое будет использовано при выполнении действия. Также есть вторая форма оператора `NEWTHREAD` для запуска действия с помощью планировщика. Использование этой формы определяется наличием ключевого слова `SCHEDULE`.  

### Параметры

- `action`

    [Контекстно-зависимый оператор](Action_operators.md#contextdependent), описывающий действие, которое должно быть выполнено в новом потоке.

- `connectionExpr`

    [Выражение](Expression.md), значением которого является [свойство](Properties.md), возвращающее объект класса `SystemEvents.Connection`. Определяет соединение, для которого будет выполнено данное действие.  

- `periodExpr`

    Выражение, значением которого является свойство, возвращающее продолжительность периода повторения выполнения действия в миллисекундах. Если не указано, то действие.будет выполнено один раз.

- `delayExpr`

    Выражение, значением которого является свойство, возвращающее задержку первого выполнения действия в миллисекундах. Если не указано, то действие.будет выполнено без задержки.


### Примеры

```lsf
testNewThread ()  {
    //Показ всем сообщения 'Сообщение'
    FOR user(Connection conn) AND connectionStatus(conn) == ConnectionStatus.connectedConnection AND conn != currentConnection() DO {
        NEWTHREAD MESSAGE 'Сообщение'; CONNECTION conn;
    }

    //Выполнение действия action с периодичностью в 10 секунд и задержкой 5 секунд
    NEWTHREAD MESSAGE 'Hello World'; SCHEDULE PERIOD 10000 DELAY 5000;
}
```
