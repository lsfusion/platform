---
slug: "/Composition_JOIN"
title: 'Композиция (JOIN)'
---

Оператор *композиции* создаёт [свойство](Properties.md), значением которого является другое свойство — *главное* — применённое к значениям, возвращаемым другими свойствами, переданными как его *аргументы*.

Класс результата — это класс значения главного свойства.

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
