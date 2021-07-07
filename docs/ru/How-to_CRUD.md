---
title: 'How-to: CRUD'
---

## Пример 1

### Условие

Есть множество предопределенных типов книг.

```lsf
CLASS Type 'Тип' {
    novel 'Роман',
    thriller 'Триллер',
    fiction 'Фантастика'
}
name 'Наименование' (Type g) = staticCaption(g) IF g IS Type;
```

Необходимо создать форму для выбора типа из списка.

### Решение

```lsf
FORM types 'Список типов'
    OBJECTS g = Type
    PROPERTIES(g) READONLY name

    LIST Type OBJECT g
;
```

Конструкция `LIST` обозначает, что данная форма будет использоваться при необходимости выбора списка типов (например, при попытке изменения типа для книги).

## Пример 2

### Условие

Есть набор книг с заданными наименованиями.

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[30] (Book) IN id;
```


:::info
Следует отметить, что все свойства `name` рекомендуется добавлять в группу `id`. Значения этого свойства будут идентифицировать объект в случае нарушения ограничения. Также оно будет добавлено на автоматические формы, если для класса не будут заданы формы редактирования и выбора.
:::

  

Необходимо создать форму со списком книг, с возможностью их добавления, редактирования и удаления.

### Решение

```lsf
FORM book 'Книга' // форма для отображения "карточки" книги
    OBJECTS b = Book PANEL
    PROPERTIES(b) name

    EDIT Book OBJECT b
;

FORM books 'Книги'
    OBJECTS b = Book
    PROPERTIES(b) READONLY name
    PROPERTIES(b) NEWSESSION NEW, EDIT, DELETE

    LIST Book OBJECT b
;

NAVIGATOR {
    NEW books;
}
```

## Пример 3

### Условие

Есть набор жанров книг с заданными наименованиями.

```lsf
CLASS Genre 'Жанр';
name 'Наименование' = DATA ISTRING[30] (Genre);
```

Необходимо создать форму со списком жанров, с возможностью их добавления, редактирования и удаления, и отдельную форму для выбора без такой возможности.

### Решение

```lsf
FORM genre 'Жанр'
    OBJECTS g = Genre PANEL
    PROPERTIES(g) name

    EDIT Genre OBJECT g
;

FORM genres 'Жанры'
    OBJECTS g = Genre
    PROPERTIES(g) READONLY name
    PROPERTIES(g) NEWSESSION NEW, EDIT, DELETE
;

FORM dialogGenre 'Жанры'
    OBJECTS g = Genre
    PROPERTIES(g) READONLY name

    LIST Genre OBJECT g
;

NAVIGATOR {
    NEW genres;
}
```

Такая схема (с тремя формами, вместо двух) наиболее подходит для того, чтобы отключить возможность редактирования жанров при их выборе, с целью минимизировать вероятность случайного исправления пользователем информации о жанрах. В таком случае, изменять жанры можно только на специальной форме.
