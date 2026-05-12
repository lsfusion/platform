---
title: 'Оператор SCREENSHOT'
---

Оператор `SCREENSHOT` создаёт [действие](Actions.md), которое [делает снимок](Capture_SCREENSHOT.md) текущего отрисованного пользовательского интерфейса и записывает результат в свойство либо, если целевое свойство не задано, отправляет файл на клиент.

### Синтаксис

```
SCREENSHOT [HTML] [captureTarget] [TO propertyId]
```

Где `captureTarget` — одна из следующих конструкций:

```
FORM
containerId
```

### Описание

Оператор `SCREENSHOT` создаёт действие, которое захватывает текущее отрисованное состояние пользовательского интерфейса в [веб-клиенте](Capture_SCREENSHOT.md#client). Область захвата определяется выбранной [целью снимка](Capture_SCREENSHOT.md#target); выходной [формат](Capture_SCREENSHOT.md#format) выбирается ключевым словом `HTML`; [адресат](Capture_SCREENSHOT.md#destination) задаётся выражением `TO`.

### Параметры

- `HTML`

    Ключевое слово. Если указано, захватывается внутренняя HTML-разметка цели. Целевое свойство должно принимать `HTMLFILE` или один из обобщённых файловых типов `FILE`, `NAMEDFILE`. Если не указано, цель захватывается как PNG-изображение; целевое свойство должно тогда принимать `IMAGEFILE` или один из обобщённых файловых типов `FILE`, `NAMEDFILE`.

- `captureTarget`

    Цель снимка. Задаётся одним из следующих способов:

    - `FORM`

        Ключевое слово. Если указано, захватывается главный контейнер формы, в контексте которой выполняется действие. Действие должно выполняться в контексте формы.

    - `containerId`

        [Простой идентификатор](IDs.md#id) контейнера в [дизайне](Form_design.md) формы, в контексте которой выполняется действие. Действие должно выполняться в контексте формы, и форма должна содержать контейнер с указанным SID.

    Если не указаны ни `FORM`, ни `containerId`, захватывается всё тело страницы веб-клиента.

- `propertyId`

    [Идентификатор свойства](IDs.md#propertyid), в которое записывается захваченное содержимое. Свойство должно быть без параметров и должно принимать либо файловый тип, соответствующий выбранному формату (`IMAGEFILE` для изображения, `HTMLFILE` для HTML), либо один из обобщённых файловых типов `FILE`, `NAMEDFILE`. Если не указан, сгенерированный файл отправляется на клиент и открывается средствами операционной системы.

### Примеры

```lsf
CLASS Report;
name = DATA ISTRING[100] (Report);
image = DATA IMAGEFILE (Report);
html = DATA HTMLFILE (Report);

FORM dashboard
    OBJECTS r = Report
    PROPERTIES(r) name, image, html
;

DESIGN dashboard {
    NEW chartBox {
        caption = 'Chart';
        MOVE PROPERTY(image(r));
    }
}

captureToClient ()  {
    SCREENSHOT; // отправляется на клиент и открывается средствами ОС
}

captureViewport (Report r)  {
    LOCAL img = IMAGEFILE ();
    SCREENSHOT TO img;
    image(r) <- img();
}

captureForm (Report r)  {
    LOCAL img = IMAGEFILE ();
    SCREENSHOT FORM TO img;
    image(r) <- img();
}

captureChart (Report r)  {
    LOCAL img = IMAGEFILE ();
    SCREENSHOT chartBox TO img;
    image(r) <- img();
}

captureFormHtml (Report r)  {
    LOCAL page = HTMLFILE ();
    SCREENSHOT HTML FORM TO page;
    html(r) <- page();
}
```
