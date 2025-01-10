---
title: 'Оператор NEWCONNECTION'
---

Оператор `NEWCONNECTION` - создание [действия](Actions.md), выполняющего другое действие с сохранением подключений SQL, TCP, DBF.

### Синтаксис

```
NEWCONNECTION action
```

### Описание

Оператор `NEWCONNECTION` создает действие, которое сохраняет подключения SQL, TCP, DBF и позволяет выполнять `EXTERNAL SQL`, `EXTERNAL TCP`, `EXTERNAL DBF` без необходимости каждый раз создавать новое подключение. Все подключения закрываются в конце выполнения оператора NEWCONNECTION. 

### Параметры

- `action`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий действие, которое будет выполнено.

### Примеры

```lsf
test  {
    NEWCONNECTION {
        EXTERNAL SQL 'jdbc:postgresql://connection/string' EXEC 'first query'; //первый EXTERNAL создаёт подключение и не закрывает его
        EXTERNAL SQL 'jdbc:postgresql://connection/string' EXEC 'second query'; //второй EXTERNAL использует уже созданное подключение
    }
	//Все созданные подключения закрываются в конце выполнения NEWCONNECTION вне зависимости от того, были ли ошибки при выполнении
}
```
