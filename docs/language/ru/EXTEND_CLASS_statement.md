---
slug: "/EXTEND_CLASS_statement"
title: 'Инструкция EXTEND CLASS'
---

Инструкция `EXTEND CLASS` - [расширение](../paradigm/Class_extension.md) существующего класса.

### Синтаксис

```
EXTEND CLASS name 
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

 Инструкция `EXTEND CLASS` расширяет существующий [пользовательский класс](../paradigm/User_classes.md) дополнительными родительскими классами и новыми [статическими объектами](../paradigm/Static_objects.md). Расширять можно, в том числе и [абстрактные классы](../paradigm/User_classes.md#abstract), добавляя им родительские классы.

### Параметры

- `name`

    Имя класса. [Составной идентификатор](IDs.md#cid). 

- `objectName1, ..., objectNameM`

    Имена новых статических объектов указанного класса. Каждое имя задается [простым идентификатором](IDs.md#id). Имя каждого статического объекта доступно через свойство `name[StaticObject]`.

- `objectCaption1, ..., objectCaptionM`

    Заголовки новых статических объектов указанного класса. Каждый заголовок является [строковым литералом](Literals.md#strliteral). Если заголовок не задан, то заголовком статического объекта будет являться его имя. Заголовок каждого статического объекта доступен через свойство `caption[StaticObject]`.

- `imageSetting`

    Настройка иконки статического объекта. Один из вариантов:

    - `IMAGE`

        [Указание иконки](../paradigm/Icons.md#manual) вручную, после которого может следовать `imageLiteral` — [строковый литерал](Literals.md#strliteral), значение которого определяет иконку. Если `imageLiteral` не указан, происходит переключение в режим [автоматической установки](../paradigm/Icons.md#auto) иконки.

    - `NOIMAGE`

        У статического объекта не должно быть иконки.

- `parent1, ..., parentN`

    Список имен новых родительских классов. Каждое имя задается составным идентификатором. 

### Примеры

```lsf
CLASS ABSTRACT Shape;
CLASS Box : Shape;

CLASS Quadrilateral;
EXTEND CLASS Box : Quadrilateral; // Добавляем наследование

CLASS ShapeType {
	point 'Точка',
	segment 'Отрезок'
}

EXTEND CLASS ShapeType { // Добавляем статический объект
	circle 'Окружность'
}
```
