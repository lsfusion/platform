---
slug: "/OVERRIDE_operator"
title: 'Оператор OVERRIDE'
---

Оператор `OVERRIDE` - создание [свойства](../paradigm/Properties.md), реализующего [выбор](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#poly) одного из значений (полиморфная форма).

### Синтаксис

```
OVERRIDE expr1, ..., exprN
```

### Описание

Оператор `OVERRIDE` создает свойство, значением которого будет значение одного из указанных в операторе свойств. Выбор осуществляется среди свойств, значения которых не равны `NULL`. Если несколько свойств не равны `NULL`, то выбирается значение первого из этих свойств.

Скобки сразу после `OVERRIDE` группируют его первый операнд, а не ограничивают список операндов, поэтому запись `OVERRIDE(expr1, expr2)` не принимается.

### Параметры

- `expr1, ..., exprN`

    Список [выражений](Expression.md), значения которых будут определять значение свойства.

### Примеры

```lsf
CLASS Group;
markup = DATA NUMERIC[8,2] (Group);

markup = DATA NUMERIC[8,2] (Book);
group = DATA Group (Book);
overMarkup (Book b) = OVERRIDE markup(b), markup(group(b));
// наценка NULL сравнивается как 0
sameMarkup (Book b1, Book b2) = (OVERRIDE markup(b1), 0) = (OVERRIDE markup(b2), 0);

notNullDate (INTEGER i) = OVERRIDE date(i), 2010_01_01;
```
