---
slug: "/CONCAT_operator"
title: 'Оператор CONCAT'
---

Оператор `CONCAT` - создание [свойства](../paradigm/Properties.md), реализующего [объединение](../paradigm/String_operators_plus_CONCAT_SUBSTRING.md) строк.

### Синтаксис

```
CONCAT separatorExpr, concatExpr1, ..., concatExprN
```

### Описание

Оператор `CONCAT` создает свойство, которое соединяет значения `concatExpr1, ..., concatExprN` в порядке их записи, вставляя между ними разделитель `separatorExpr`. Обработка разделителя и пустых операндов соответствует [объединению `CONCAT`](../paradigm/String_operators_plus_CONCAT_SUBSTRING.md).

### Параметры

- `separatorExpr`

    [Выражение](Expression.md), значение которого используется как разделитель. Чаще всего это строковый литерал, но может быть любым строковым выражением.

- `concatExpr1, ..., concatExprN`

    Выражения, значения которых соединяются. Должен быть задан хотя бы один операнд.

### Примеры

```lsf
CLASS Person;
firstName = DATA STRING[100] (Person);
middleName = DATA STRING[100] (Person);
lastName = DATA STRING[100] (Person);

// если какая-то часть имени не задана, то эта часть будет пропущена вместе с пробелом
fullName(Person p) = CONCAT ' ', firstName(p), middleName(p), lastName(p);     
```
