---
title: 'Оператор CONCAT'
---

Оператор `CONCAT` - создание [свойства](Properties.md), реализующего [объединение](String_operators_+_CONCAT_SUBSTRING.md) строк.

### Синтаксис

```
CONCAT separatorExpr, concatExpr1, ..., concatExprN
```

### Описание

Оператор `CONCAT` создает свойство, которое выполняет соединение значений через разделитель `separatorExpr`. При этом пустые значения пропускаются и разделитель вставляется только между непустыми значениями.

### Параметры

- `separatorExpr`

    [Выражение](Expression.md), значение которого будет использовано как разделитель.

- `concatExpr1, ..., concatExprN`

    Выражения, значения которых будут соединены.

### Примеры

```lsf
CLASS Person;
firstName = DATA STRING[100] (Person);
middleName = DATA STRING[100] (Person);
lastName = DATA STRING[100] (Person);

// если какая-то часть имени не задана, то эта часть будет пропущена вместе с пробелом
fullName(Person p) = CONCAT ' ', firstName(p), middleName(p), lastName(p);     
```
