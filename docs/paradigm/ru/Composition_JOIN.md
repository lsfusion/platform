---
slug: "/Composition_JOIN"
title: 'Композиция (JOIN)'
---

Оператор *композиции* создаёт [свойство](Properties.md), значением которого является другое свойство — *главное* — применённое к значениям, возвращаемым другими свойствами, переданными как его *аргументы*.

Класс результата — это класс значения главного свойства.

### Аргументы `NULL` {#null}

Если значение аргумента равно `NULL`, то соответствующий параметр главного свойства равен `NULL`, и композиция возвращает значение, которое главное свойство имеет в этом случае. Например:

| Главное свойство | Значение, когда параметр равен `NULL` |
|---|---|
| [первичное свойство](Data_properties_DATA.md) | `NULL` |
| свойство, созданное оператором [группировки](Grouping_GROUP.md) | `NULL`, если параметр входит в ключ группы |
| свойство, созданное оператором [выбора](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) | не обязательно `NULL`: например, если условием служит сам параметр, это условие не выполняется, но может выполняться другое условие или может быть задан альтернативный результат |

### Язык

Для реализации композиции используется [оператор `JOIN`](../language/JOIN_operator.md).

### Примеры


```lsf
f = DATA INTEGER (INTEGER, INTEGER, INTEGER);
g = DATA INTEGER (INTEGER, INTEGER);
h = DATA INTEGER (INTEGER, INTEGER);
c(a, b) = f(g(a, b), h(b, 3), a);

count = DATA BPSTRING[255] (INTEGER);
name = DATA BPSTRING[255] (INTEGER);
formatted(INTEGER a, INTEGER b) = [FORMULA BPSTRING[255] ' CAST($1 AS TEXT) || \' / \' || CAST($2 AS TEXT)'](count(a), name(b));
```


```lsf
CLASS Triangle;
cathetus1 = DATA DOUBLE(Triangle);
cathetus2 = DATA DOUBLE(Triangle);

hypotenuseSq(triangle) = cathetus1(triangle)*cathetus1(triangle) + cathetus2(triangle)*cathetus2(triangle);

// аналогичное свойство, заданное с помощью композиции
hypotenuseSq2(triangle) = [ x*x + y*y](cathetus1(triangle), cathetus2(triangle)); 
```


```lsf
CLASS Item;
CLASS Batch;
item = DATA Item (Batch);
date = DATA DATE (Batch);
quantity = DATA INTEGER (Batch);
countDate = DATA DATE (Item); // NULL, пока товар ни разу не пересчитан

// выбор: при дате NULL первое условие не выполняется, а второй результат от даты не зависит
dateOrDefault (DATE d) = OVERRIDE d, 2000_01_01;
// для товара, который не пересчитывался, аргумент равен NULL, а значение - 2000_01_01
countDateOrDefault (Item i) = dateOrDefault(countDate(i));
// то же значение возвращается, когда аргумент - константа NULL
defaultDate () = dateOrDefault(NULL);

// группировка: параметр d входит в ключ группы, поэтому для даты NULL значения нет
receivedSince (Item i, DATE d) = GROUP SUM quantity(Batch b) IF item(b) = i AND date(b) >= d;
// для товара, который не пересчитывался, второй аргумент равен NULL, и значение тоже равно NULL.
// С (NOT d OR date(b) >= d) вместо date(b) >= d значение всё равно было бы равно NULL
receivedSinceCount (Item i) = receivedSince(i, countDate(i));
```
