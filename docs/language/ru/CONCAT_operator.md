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

Первый операнд — всегда разделитель, даже если он задуман как префикс: `CONCAT 'Итого: ', 'a', 'b'` даёт `'aИтого: b'`, а не `'Итого: ab'`. Текст в начале или в конце результата записывают обычным операндом при пустом разделителе (`CONCAT '', 'Итого: ', 'a', 'b'`) либо добавляют [оператором `+`](Arithmetic_operators.md) (`'Итого: ' + CONCAT ', ', 'a', 'b'`).

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

// заголовок — отдельный операнд при пустом разделителе: первый операнд CONCAT всегда разделитель,
// и CONCAT 'Person: ', firstName(p), lastName(p) дал бы 'JohnPerson: Smith'
caption(Person p) = CONCAT '', 'Person: ', fullName(p);
```
