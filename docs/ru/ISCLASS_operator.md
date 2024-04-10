---
id: CLASS_operator
title: 'Оператор ISCLASS'
---

Оператор `ISCLASS` - создание свойства, реализующего [оператор принадлежности сигнатуре](Property_signature_ISCLASS.md).

### Синтаксис

```
ISCLASS(expr) 
```

### Описание

Оператор `ISCLASS` создает свойство, которое определяет, может ли, с точки зрения классов, указанное в операторе выражение иметь не `NULL` значение для переданных аргументов или нет.

### Параметры

- `expr`

    [Выражение](Expression.md), описывающее и создающее свойство, для которого выводится набор классов параметров - сигнатура. Принадлежность к этой сигнатуре и будет проверяться. 

### Пример

```lsf
CLASS Person;
name = ABSTRACT CASE STRING[100] (Person);

CLASS Student : Person;
studentName = DATA STRING[100] (Student);

name(s) += WHEN ISCLASS(studentName(s)) THEN studentName(s); // равносильно WHEN s IS Student THEN studentName(s)
```
