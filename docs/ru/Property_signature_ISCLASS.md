---
id: Property_signature_CLASS
title: 'Принадлежность сигнатуре (ISCLASS)'
---

Оператор принадлежности сигнатуре создает [свойство](Properties.md), которое определяет, может ли, с точки зрения [классов](Classes.md), указанное в операторе свойство иметь не `NULL` значение для переданных аргументов или нет. Фактически данный оператор выводит возможные классы указанного свойства из его семантики, после чего при помощи [логических](Logical_operators_AND_OR_NOT_XOR.md) операторов и оператора [классификации](Classification_IS_AS.md) создает требуемое свойство.

### Язык

Для реализации этого оператора используется [оператор `ISCLASS`](ISCLASS_operator.md).

### Пример

```lsf
CLASS Person;
name = ABSTRACT CASE STRING[100] (Person);

CLASS Student : Person;
studentName = DATA STRING[100] (Student);

name(s) += WHEN ISCLASS(studentName(s)) THEN studentName(s); // равносильно WHEN s IS Student THEN studentName(s)
```
