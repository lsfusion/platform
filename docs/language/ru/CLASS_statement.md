---
slug: "/CLASS_statement"
title: 'Инструкция CLASS'
---

Инструкция `CLASS` - создание нового [пользовательского класса](../paradigm/User_classes.md).

### Синтаксис

```
CLASS ABSTRACT name [caption] [imageSetting] [: parent1, ..., parentN];
 
CLASS [NATIVE] name [caption] [imageSetting]
[{
    objectName1 [objectCaption1] [imageSetting],
    ...
    objectNameM [objectCaptionM] [imageSetting]
}] 
[: parent1, ..., parentN];
```

Где `imageSetting` принимает один из видов:

```
IMAGE [imageLiteral]
NOIMAGE
```

### Описание

Инструкция `CLASS` объявляет новый класс и добавляет его в текущий [модуль](../paradigm/Modules.md). 

Инструкция бывает двух видов: `CLASS ABSTRACT` для объявления [абстрактного класса](../paradigm/User_classes.md#abstract) и просто `CLASS` для объявления обычного класса. Во втором случае при объявлении класса можно объявить [статические объекты](../paradigm/Static_objects.md) этого класса, имена и заголовки которых указываются в ограниченном фигурными скобками блоке.   

Ключевое слово `NATIVE` используется в некоторых системных модулях. Оно предназначено для объявления отдельных системных классов, которые создаются до инициализации модулей.

### Параметры

- `name`

    Имя класса. [Простой идентификатор](IDs.md#id). Имя должно быть уникально в пределах текущего [пространства имен](../paradigm/Naming.md#namespace).

- `caption`

    Заголовок класса. [Строковый литерал](Literals.md#strliteral). Если заголовок не задан, то заголовком класса будет являться его имя.  

- `imageSetting`

    Настройка иконки класса (если указана после заголовка класса) или статического объекта (если указана после заголовка объекта). Один из вариантов:

    - `IMAGE`

        [Указание иконки](../paradigm/Icons.md#manual) вручную, после которого может следовать `imageLiteral` — [строковый литерал](Literals.md#strliteral), значение которого определяет иконку. Если `imageLiteral` не указан, происходит переключение в режим [автоматической установки](../paradigm/Icons.md#auto) иконки.

    - `NOIMAGE`

        У класса или статического объекта не должно быть иконки.

- `objectName1, ..., objectNameM`

    Имена статических объектов данного класса. Каждое имя задается простым идентификатором. Свойство `name[StaticObject]` возвращает это имя вместе с пространством имён и классом — [каноническое имя](../paradigm/Static_objects.md) объекта.

- `objectCaption1, ..., objectCaptionM`

    Заголовки статических объектов данного класса. Каждый заголовок является строковым литералом. Если заголовок не задан, то заголовком статического объекта будет являться его имя. Заголовок каждого статического объекта доступен через свойство `caption[StaticObject]`.

- `parent1, ..., parentN`

    Список имен родительских классов. Каждое имя задается [составным идентификатором](IDs.md#cid). Если список родительских классов не задан, то класс наследуется от класса `System.Object`.  

### Примеры

```lsf
CLASS ABSTRACT Document; // Заголовком этого класса будет 'Document'
CLASS IncomeDocument 'Приход' : Document;
CLASS OutcomeDocument 'Расход' : Document;

CLASS Person;
CLASS Worker;
CLASS Musician : Person, Worker; // множественное наследование

CLASS Barcode 'Штрих-код'; // Родительским классом Barcode будет класс System.Object

CLASS PrintOrientation {
    portrait 'Книжная',
    landscape 'Альбомная'
}

CLASS Currency 'Валюта' IMAGE 'currency.png'; // класс с иконкой
```
