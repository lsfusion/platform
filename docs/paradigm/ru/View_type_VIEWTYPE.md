---
slug: "/View_type_VIEWTYPE"
title: 'Вид отображения (VIEWTYPE)'
---

Оператор вида отображения создаёт [свойство](Properties.md), значением которого является текущий *вид отображения*, в котором [группа объектов](Form_structure.md#objects) показывается пользователю: в виде таблицы, сводной таблицы, карты, календаря или пользовательского представления.

### Язык

Для объявления свойства, возвращающего текущий вид отображения группы объектов, используется [оператор `VIEWTYPE`](../language/Object_group_operator.md).

### Примеры

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
    PROPERTIES(s) name
;
isPivot 'Склады в виде сводной таблицы' () = [ VIEWTYPE stores.s]() == ListViewType.pivot;
```
