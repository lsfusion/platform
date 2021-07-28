---
title: 'Инструкция BEFORE'
---

Инструкция `BEFORE` - вызов [действия](Actions.md) перед вызовом другого действия. 

### Синтаксис

    BEFORE action(param1, ..., paramN) DO aspectAction;

Описание

Инструкция `BEFORE` задает действие (будем называть его *аспектом*), которое будет вызываться перед вызовом указанного действия.

### Параметры

- `action`

    [Идентификатор действия](IDs.md#propertyid), перед которым будет вызываться аспект.

- `param1, ..., paramN`

    Список имен параметров действия. Каждое имя задается [простым идентификатором](IDs.md#id). К этим параметрам можно обращаться при задании аспекта.

- `aspectAction`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий аспект.

### Примеры

```lsf
changeName(Sku s, STRING[100] name)  { name(s) <- name; }
// Сообщение будет показано перед каждым вызовом changeName
BEFORE changeName(Sku s, STRING[100] name) DO MESSAGE 'Changing user name'; 
```
