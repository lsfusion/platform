---
slug: "/Utils_Hierarchy"
title: 'Hierarchy'
---

Модуль `Hierarchy` даёт классу иерархию по родителю: объект ссылается свойством `parent[class]` на объект того же класса, а модуль достраивает по этой ссылке признак предка, уровень, число потомков, признак листа и полное имя от корня. Свойство `level[class, class]` вычисляется [рекурсией](Recursion_RECURSION.md); остальные свойства выводятся из него или прямо из `parent[class]`, где нужно — [группировкой](Grouping_GROUP.md) или [разбиением](Partitioning_sorting_PARTITION_..._ORDER.md); [материализованные](Materializations.md) свойства отмечены в таблице ниже. Подключается через `REQUIRE Hierarchy`.

### Подключение к классу

Иерархию добавляет один из трёх метакодов; различаются они тем, откуда берётся свойство `parent[class]`. У класса должно быть свойство `name[class]`: из него вычисляются `nameParent[class]` и `canonicalName[class]`.

| Метакод | Свойство `parent[class]` |
|---|---|
| `@defineHierarchy(object)`, `@defineHierarchy(object, class)` | Объявляется первичным свойством класса; при создании объекта с включённой автоматической установкой оно получает текущий объект того же класса, если такой есть |
| `@defineHierarchyAbstract(object)`, `@defineHierarchyAbstract(object, class)` | Объявляется абстрактным материализованным свойством класса; реализацию добавляет прикладной модуль |
| `@defineHierarchyCustom(object, class)` | Не объявляется: используется уже имеющееся в классе `parent[class]` |

В форме с одним аргументом класс получается из `object` заменой первой буквы на прописную: `@defineHierarchy(itemGroup)` — то же, что `@defineHierarchy(itemGroup, ItemGroup)`. Аргумент `object` становится также именем свойства «предок по уровню» и основой производных от него имён (см. таблицу ниже).

### Свойства иерархии

Все три метакода добавляют классу один и тот же набор свойств (`class` — класс объектов, `object` — первый аргумент метакода; у свойств с двумя параметрами первый — потомок, второй — предполагаемый предок).

| Свойство | Значение |
|---|---|
| `nameParent[class]` | `name[class]` родителя; помещается в группу `base` |
| `level[class, class]` | Не `NULL`, если второй объект — предок первого или он сам; значение (класса `LONG`) — `2` в степени расстояния между ними: `1` для самого объекта, `2` для родителя, `4` для родителя родителя и так далее. При политике `CYCLES NO`, действующей по умолчанию, расстояние не может превышать `30`: более длинная цепочка предков нарушает ограничение рекурсии, и сохранение отменяется. Материализовано |
| `isParent[class, class]` | `TRUE` там, где `level[class, class]` не `NULL`: признак «предок или сам объект» |
| `object[class, LONG]` | Предок первого аргумента, для которого `level[class, class]` равно второму аргументу (`1` — сам объект, `2` — родитель, `4` — родитель родителя); имя свойства — аргумент `object`, например `itemGroup[ItemGroup, LONG]` |
| `level[class]` | Уровень объекта — количество его предков вместе с ним самим (`1` для корня). Материализовано |
| `levelRoot[class, class]` | Номер предка в цепочке от корня к объекту: `1` для корня, `level[class]` объекта для него самого. Материализовано |
| `objectRoot[class, INTEGER]` | Предок первого аргумента, стоящий на позиции второго аргумента от корня (`1` — корень); имя — аргумент `object` с суффиксом `Root`, например `itemGroupRoot[ItemGroup, INTEGER]` |
| `childNumber[class]` | Количество непосредственных потомков; `NULL`, если их нет. Материализовано |
| `descendantNumber[class]` | Количество объектов, для которых данный — предок, включая его самого. Материализовано |
| `isLeaf[class]` | `TRUE`, если непосредственных потомков нет. Материализовано |
| `isParentLeaf[class, class]` | `isParent[class, class]` при условии, что первый объект — лист |
| `canonicalName[class]` | Полное имя: `name[class]` предков от корня до самого объекта через ` / ` (`ISTRING[255]`). Материализовано |

### Дополнительные метакоды

Дополнительные метакоды рассчитывают на уже добавленные свойства иерархии: `@defineHierarchyPlain` использует `objectRoot[class, INTEGER]`, `@defineHierarchyFilter` — `isParent[class, class]` и `name[class]`; их применяют после одного из трёх метакодов выше.

| Метакод | Что добавляет |
|---|---|
| `@defineHierarchyPlain(object)` | Материализованные свойства `object1[class]` … `object6[class]` — предки объекта на позициях от `1` до `6` от корня, то есть `objectRoot[class, INTEGER]` с фиксированной позицией; удобны для плоских отчётов и колонок вида «категория / направление / группа». Класс берётся из `object` заменой первой буквы на прописную |
| `@defineHierarchyFilter(object, class, property, caption)` | Локальное свойство `filter###property###object[]` класса `STRING[255]` с заголовком `caption` (например, `filterNameItemGroup[]` для `@defineHierarchyFilter(ItemGroup, ItemGroup, name, ...)`; аргумент `property` влияет только на это имя) и два счётчика по поддереву объекта — `inFilterName[class]` и `inIFilterName[class]`: количество объектов поддерева, включая его самого, у которых `name[class]` содержит введённую строку, с учётом и без учёта регистра (`NULL`, если таких нет); ими фильтруют дерево по подстроке наименования |

### Язык

- [Инструкция `META`](../language/META_statement.md) — объявление метакода и его применение инструкцией `@`, которой иерархия подключается к классу.
- [Заголовок модуля](../language/Module_header.md) — `REQUIRE Hierarchy`, подключающий модуль.

### Примеры

```lsf
CLASS Category 'Категория';
name 'Наименование' = DATA ISTRING[50] (Category);

@defineHierarchy(category); // parent, level, isParent, canonicalName, childNumber, isLeaf, ...

// книги категории и всех её подкатегорий
CLASS Book 'Книга';
category = DATA Category (Book);
inCategory (Book b, Category c) = isParent(category(b), c);

// корень ветки категории и её родитель
root (Category c) = categoryRoot(c, 1);
parentByLevel (Category c) = category(c, 2l);

FORM categories 'Категории'
    OBJECTS c = Category
    PROPERTIES(c) name, canonicalName, level, childNumber, isLeaf
;
```
