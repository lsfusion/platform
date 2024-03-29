---
title: 'Оператор CLASS'
---

Оператор `CLASS` - создание свойства, реализующего [оператор принадлежности сигнатуре](Property_signature_CLASS.md).

### Синтаксис

```
CLASS(expr) 
```

### Описание

Оператор `CLASS` создает свойство, которое определяет может ли, с точки зрения классов, заданное свойство иметь не `NULL` значение для переданных аргументов или нет.

### Параметры

- `expr`

    [Выражение](Expression.md), результатом которого является свойство. Для этого свойства выводится набор классов параметров, принадлежность к которым будет проверять результирующее свойство. 

### Примеры

```lsf
CLASS A;
a = ABSTRACT CASE STRING[100] (A);

CLASS B : A;
b = DATA STRING[100] (B);

a(B b) += WHEN CLASS(b(b)) THEN b(b); // равносильно WHEN b IS B THEN b(b)
```
