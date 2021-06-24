---
title: 'How-to: Метапрограммирование'
---

Часто возникает потребность в написании "похожего" кода для определенных случаев. Для этой цели существует [инструкция `META`](META_statement.md), которая позволяет создавать некий шаблон кода, называемый *метакодом*. В нем можно использовать параметры, которые затем будут заменяться на определенные значения при использовании этого метакода. Такой подход называется [метапрограммирование](Metaprogramming.md).

Рассмотрим задачу создания простого справочника, как описано в статье [How-to: CRUD](How-to_CRUD.md).

```lsf
CLASS Book 'Книга';
name 'Наименование' = DATA ISTRING[30] (Book) IN id;
```

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

На основе этого программного кода можно создать следующий метакод:

```lsf
META defineObject(class, id, shortId, caption, multiCaption)
    CLASS class caption;
    TABLE id(class);

    name 'Наименование' = DATA ISTRING[100] (class);

    FORM id caption
        OBJECTS shortId = class PANEL
        PROPERTIES(shortId) name

        EDIT class OBJECT shortId
    ;

    FORM id##s multiCaption
        OBJECTS shortId = class
        PROPERTIES(shortId) READONLY name
        PROPERTIES(shortId) NEWSESSION NEW, EDIT, DELETE

        LIST class OBJECT shortId
    ;

    NAVIGATOR {
        NEW id##s;
    }
END

META defineObject(id, shortId, caption, multiCaption)
    @defineObject(###id, id, shortId, caption, multiCaption);
END
```

Важно отметить, что один метакод может внутри вызывать другой.

Использование метакода осуществляется следующим образом:

```lsf
@defineObject(book, b, 'Книга', 'Книги');
@defineObject(magazine, m, 'Журнал', 'Журналы');
```

В первом случае, при генерации результирующего кода система заменит все лексемы `id` на `book`, `shortId` на `b`, `caption` на `'Книга'`, а `multiCaption` на `'Книги'`. При этом при использовании склейки `##` замена будет произведена без изменений, а при использовании `###` первая буква значения будет заменена на заглавную. Сгенерированный код будет выглядеть следующим образом:

```lsf
CLASS Book 'Книга';
TABLE book(Book);

name 'Наименование' = DATA ISTRING[100] (Book);

FORM book 'Книга'
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

CLASS Magazine 'Журнал';
TABLE magazine(Magazine);

name 'Наименование' = DATA ISTRING[100] (Magazine);

FORM magazine 'Журнал'
    OBJECTS m = Magazine PANEL
    PROPERTIES(m) name

    EDIT Magazine OBJECT m
;

FORM magazines 'Журналы'
    OBJECTS m = Magazine
    PROPERTIES(m) READONLY name
    PROPERTIES(m) NEWSESSION NEW, EDIT, DELETE

    LIST Magazine OBJECT m
;

NAVIGATOR {
    NEW magazines;
}
```

Для того, чтобы IDE "видела" код сгенерированный метакодами, нужно включить соответствующий режим через пункт меню.

![](images/How-to_Metaprogramming_enable.png)

При включенном режиме работы с метакодами сгенерированный код будет автоматом подставляться в исходниках при его использовании.

![](images/How-to_Metaprogramming_metaMode.png)

Любые изменения в нем невозможны, так как будут автоматически затираться IDE. Однако, при коммите изменений в программе в систему контроля версий рекомендуется выключать этот режим, чтобы избежать ненужной истории изменений.

Объекты, созданные при помощи метакода, можно в дальнейшем расширять используя стандартные [механизмы](How-to_Extensions.md).

```lsf
genre 'Жанр' = DATA ISTRING[20] (Book);
EXTEND FORM book PROPERTIES(b) genre;
EXTEND FORM books PROPERTIES(b) genre;
```
