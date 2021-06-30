---
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
level 'Уровень' (Category child, Category parent) = RECURSION 1l IF child IS Category AND parent == child
                                                                 STEP 2l IF parent == parent($parent) MATERIALIZED;

FORM categoryBooks 'Книги по категориям'
    TREE categories c = Category PARENT parent(c)
    PROPERTIES(c) name

    OBJECTS b = Book
    PROPERTIES(b) name
    FILTERS level(category(b), c)
;

DESIGN categoryBooks {
    NEW pane FIRST {
        fill = 1;
        type = SPLITH;
        MOVE BOX(TREE categories);
        MOVE BOX(b);
    }
}
```
