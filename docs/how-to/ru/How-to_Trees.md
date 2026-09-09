---
slug: "/How-to_Trees"
title: 'How-to: Деревья'
---

## Пример 1

### Условие

Есть список книг, привязанных к определенным категориям.

```lsf
CLASS Category 'Категория';
name 'Наименование' = DATA ISTRING[50] (Category);

CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[50] (Book);
category 'Категория' = DATA Category (Book);
```

Нужно построить форму с деревом, в котором на верхнем уровне будет категория, а под ним - товар.

### Решение

```lsf
FORM books 'Книги'
    TREE cb c = Category, b = Book
    PROPERTIES name(c), name(b)
    FILTERS category(b) == c
;
```

## Пример 2

### Условие

Аналогичен [**Примеру 1**](#пример-1), но для категории задана иерархия путем указания родителя каждой категории.

```lsf
parent 'Родитель' = DATA Category (Category);
```

Нужно построить форму с деревом, в котором будут отображаться категории в виде дерева.

### Решение

```lsf
FORM categories 'Категории'
    TREE categories c = Category PARENT parent(c)
    PROPERTIES(c) name
;
```

## Пример 3

### Условие

Аналогичен [**Примеру 2**](#пример-2).

Нужно сделать форму с деревом категорий, справа от которого показать книги, которые относятся к текущей категории и всем ее потомкам.

### Решение

```lsf
isParent 'Является родителем' (Category child, Category parent) = RECURSION 1 IF child IS Category AND parent == child
                                                                            STEP 1 IF parent == parent($parent) MATERIALIZED;

FORM categoryBooks 'Книги по категориям'
    TREE categories c = Category PARENT parent(c)
    PROPERTIES(c) name

    OBJECTS b = Book
    PROPERTIES(b) name
    FILTERS isParent(category(b), c)
;

DESIGN categoryBooks {
    NEW pane FIRST {
        fill = 1;
        horizontal = TRUE;
        MOVE BOX(TREE categories);
        MOVE BOX(b);
    }
}
```

Свойство `isParent[Category, Category]` не равно `NULL`, если второй аргумент — предок первого или та же самая категория (его числовое значение — количество путей между ними, в дереве всегда `1`), поэтому фильтр отбирает книги текущей категории и всех её потомков. Тот же набор свойств для иерархии по `parent[Category]` — `isParent[Category, Category]`, `level[Category]`, `canonicalName[Category]` и другие — даёт метакод `@defineHierarchy` системного модуля [`Hierarchy`](../paradigm/Utils_Hierarchy.md); здесь, где свойство `parent[Category]` уже объявлено, его подключает `@defineHierarchyCustom(category, Category)`, и объявлять `isParent[Category, Category]` вручную тогда не нужно — метакод создаёт его с тем же смыслом и значением `TRUE`.

## Пример 4

### Условие

Аналогичен [**Примеру 2**](#пример-2), но иерархия подключается системным модулем [`Hierarchy`](../paradigm/Utils_Hierarchy.md), а не объявлением `parent[Category]` вручную.

Нужно дать пользователю строить дерево категорий на форме: добавлять категорию на верхний уровень или под выбранную, а также переносить категорию к другому родителю.

### Решение

```lsf
@defineHierarchy(category); // parent, nameParent, level, isParent, canonicalName, ...

FORM category 'Категория'
    OBJECTS c = Category PANEL
    PROPERTIES(c) name, nameParent
    EDIT Category OBJECT c
;

addCategory 'Добавить подкатегорию' (Category parent) {
    NEWSESSION {
        NEW c = Category {
            parent(c) <- parent;
            SHOW category OBJECTS c = c DOCKED;
        }
    }
} TOOLBAR;

addRootCategory 'Добавить категорию' () {
    NEWSESSION {
        NEW c = Category {
            SHOW category OBJECTS c = c DOCKED;
        }
    }
} TOOLBAR;

FORM categories 'Категории'
    TREE categories c = Category PARENT parent(c)
    PROPERTIES(c) READONLY name, canonicalName
    PROPERTIES() addRootCategory
    PROPERTIES(c) addCategory
    PROPERTIES(c) NEWSESSION EDIT, DELETE
;
```

Действие `addCategory[Category]` привязано к объекту дерева и создает категорию под текущей; действие с параметром класса `Category` доступно, только пока категория выбрана, поэтому корневую категорию создает действие без параметров `addRootCategory[]`. Родитель существующей категории меняется правкой `nameParent[Category]` на ее форме редактирования — открывается диалог выбора нового родителя. Для подкатегории подошло бы и стандартное действие `NEW`: объявленное метакодом свойство `parent[Category]` при создании объекта автоматически получает текущую категорию.
