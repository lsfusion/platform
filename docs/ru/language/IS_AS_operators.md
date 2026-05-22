---
slug: "/IS_AS_operators"
title: 'Операторы IS, AS'
---

Операторы `IS`, `AS` - создание [свойства](../paradigm/Properties.md), реализующего [классификацию](../paradigm/Classification_IS_AS.md).

### Синтаксис

```
expression IS className
expression AS className
```

### Описание

Оператор `IS` создает свойство, которое возвращает `TRUE`, если значение [выражения](Expression.md) принадлежит указанному классу.

Оператор `AS` создает свойство, которое возвращает значение выражения, если это значение принадлежит указанному классу.

### Параметры

- `expression`

    Выражение, значение которого проверяется на принадлежность классу.

- `className`

    Имя класса. [Идентификатор класса](IDs.md#classid).

### Примеры 

```lsf
asOrder(object) = object AS Order;

person = DATA Human (Order);
isMale (Order o) = person(o) IS Male;
```
