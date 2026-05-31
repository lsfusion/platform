---
slug: "/System_System"
title: 'System'
---

`System` — корневой [системный модуль](System_modules.md) платформы. Подключается автоматически: любой другой модуль зависит от него без явного `REQUIRE`. Модуль объявляет корневые [классы](User_classes.md), стандартные [группы](Groups_of_properties_and_actions.md) свойств и действий, поверхность управления [сессией изменений](Change_sessions.md), контекст входящего HTTP-запроса, действия жизненного цикла формы, свойства идентичности приложения и локальные буферы, в которые пишут операторы экспорта, импорта и запроса значений.

### Корневые классы

| Класс                 | Назначение                                                                              |
|-----------------------|------------------------------------------------------------------------------------------|
| `Object`              | базовый класс всех пользовательских объектов; родитель остальных пользовательских классов |
| `StaticObject`        | базовый класс для статических объектов (значений, объявленных в коде модуля)             |
| `CustomObjectClass`   | объект-метакласс: каждый пользовательский класс представлен здесь одним статическим объектом, к которому привязаны статистика и каркасные свойства |

### Стандартные группы

`root` / `public` / `private` — корни иерархии групп. `base` (внутри `public`), `id` (внутри `base`), `uid` (внутри `id`) — стандартные подгруппы, в которые принято класть ключевые свойства. `drillDown` и `propertyPolicy` — отдельные корни под детализирующие свойства и правила доступа. `objects` — группа, обычно используемая для собирающих JSON-структуры свойств в интерактивных формах.

### Метаданные объектов

`class[Object]` — класс объекта (через `CustomObjectClass`). `className[Object]` — имя класса как `STRING`. `prevClassName[Object]` — то же, но для значения класса на начало сессии. Для статических объектов: `name[StaticObject]` (имя), `caption[StaticObject]` (отображаемое название), `order[StaticObject]` (порядок), `image[StaticObject]` (картинка). Для класса в целом: `stat[CustomObjectClass]` — оценка числа объектов.

### Управление сессией изменений

| Свойство / действие                                           | Что делает                                                                                  |
|---------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `apply[]` / `cancel[]`                                        | обёртки над операторами `APPLY` и `CANCEL`                                                   |
| `canceled[]`                                                  | признак, что последний `APPLY` отменён из-за ограничения                                    |
| `applyMessage[]`                                              | текст ошибки последнего `APPLY` (заполняется самой платформой)                              |
| `ApplyFilter`                                                 | класс с шорткатами фильтра: `onlyCalc`, `onlyCheck`, `onlyData`, `session`, `withoutRecalc` |
| `applyFilter[]` / `applyOnlyCalc[]` / `applyOnlyCheck[]` / `applyOnlyData[]` / `applySession[]` / `applyOnlyWithoutRecalc[]` / `applyAll[]` | задают / снимают фильтр на следующий `APPLY` |
| `check[]`                                                     | прогон `APPLY` под фильтром `onlyCheck` с гарантированным восстановлением фильтра          |
| `sessionOwners[]` / `manageSession[]`                         | счётчик владельцев сессии и признак её самостоятельности                                    |
| `throwException[TEXT]`                                        | поднимает исключение с указанным сообщением                                                 |
| `setNoCancelInTransaction[]` / `dropNoCancelInTransaction[]`  | запрет / разрешение отката внутри транзакции                                                |
| `setNoEventsInTransaction[]` / `dropNoEventsInTransaction[]`  | запрет / разрешение событий внутри транзакции                                               |
| `executeLocalEvents[TEXT]` / `executeLocalEvents[]`           | принудительный запуск локальных событий (опционально по фильтру)                            |
| `beforeCanceled[]` / `requestCanceled[]` / `requestPushed[]`  | служебные флаги жизненного цикла запроса                                                    |
| `logMessage[]`                                                | сообщение, выводимое в системный лог при определённых событиях                              |
| `empty[]` / `empty[Object]`                                   | пустые действия-заглушки                                                                    |

### Контекст HTTP-запроса

Платформа наполняет следующие локальные `NESTED`-свойства при входящем HTTP-запросе и читает их в исходящем ответе.

**Входящие**: `headers[TEXT]`, `cookies[TEXT]`, `params[TEXT, INTEGER]` (и сокращение `params[TEXT]`), `fileParams[TEXT, INTEGER]` / `fileParams[TEXT]`, `actionPathInfo[]`, `contentType[]`, `body[]`, `appHost[]`, `appPort[]`, `exportName[]`, `method[]`, `scheme[]`, `webHost[]`, `webPort[]`, `contextPath[]`, `servletPath[]`, `pathInfo[]`, `query[]`, `insecureSSL[]`, `timeoutHttp[]`.

**Исходящие**: `headersTo[TEXT]`, `cookiesTo[TEXT]`, `statusHttp[]`, `failedHttp[]` (вычисляется как `statusHttp() < 200 OR statusHttp() >= 300`), `statusHttpTo[]`.

**Производные URL**: `origin[]` (`scheme://webHost:webPort`), `webPath[]` (`origin + contextPath`), `url[]` (`webPath + servletPath + pathInfo`), `apiOriginUrl[STRING]` / `apiOriginUrl[LINK]` (URL к API относительно `origin`), `apiContextUrl[STRING]` (URL к API относительно `webPath`).

**TCP**: `responseTcp[]`, `timeoutTcp[]`.

### Состояние пойманного исключения

Внутри блока `CATCH` доступны `messageCaughtException[]`, `javaStackTraceCaughtException[]`, `lsfStackTraceCaughtException[]` — соответственно текст сообщения, Java-стек и lsFusion-стек последнего пойманного исключения.

### Открытие файлов и ссылок

`open[STRING]` / `open[FILE]` / `open[NAMEDFILE]` / `open[RAWFILE]` / `open[LINK]` / `open[RAWLINK]` (с короткими и расширенными перегрузками для имени файла и флага `noWait`) — открывают переданное значение на стороне клиента в ассоциированном приложении или браузере.

`htmlLinkInTab[HTMLLINK]` — открыть HTML-ссылку отдельной вкладкой; на форме `htmlLinkInTab` лежит сам HTML-просмотрщик.

### Жизненный цикл формы

| Свойство / действие                               | Что делает                                                  |
|---------------------------------------------------|--------------------------------------------------------------|
| `formApply[]`                                     | применить (сохранить) изменения формы (Alt+Enter)            |
| `formCancel[]`                                    | отменить изменения формы (Shift+Esc)                         |
| `formRefresh[]`                                   | перечитать данные формы (F5)                                 |
| `formOk[]`                                        | подтвердить и закрыть диалог (Ctrl+Enter)                    |
| `formClose[]`                                     | закрыть форму (Esc)                                          |
| `formDrop[]`                                      | удалить текущий объект формы (Alt+Delete)                    |
| `formEditReport[]`                                | редактировать шаблон отчёта формы (Ctrl+E)                   |
| `formShare[]` / `formCustomize[]`                 | поделиться текущим состоянием / открыть настройку формы      |
| `formCustomizeBackground[]` / `formCustomizeShowIf[]` | абстрактные свойства (`ABSTRACT COLOR()` / `ABSTRACT BOOLEAN()`) для кастомизации диалога настройки формы |
| `formApplied[]`                                   | абстрактный список действий, вызываемых после успешного `APPLY` на форме (по умолчанию показывает уведомление об успехе) |
| `navigatorRefresh[]`                              | обновить навигатор                                          |
| `forceUpdate[STRING]`                             | принудительно перечитать данные группы объектов              |
| `seek[Object]`                                    | найти и активировать объект на текущей форме                 |
| `sleep[LONG]`                                     | пауза в миллисекундах                                       |

### Признаки контекста формы

`isActiveForm[]`, `isDocked[]`, `isEditing[]`, `isAdd[]`, `isManageSession[]`, `isExternal[]`, `showOk[]`, `showDrop[]`, `isDataChanged[]` — состояния текущей формы и её взаимодействия с сессией. `isEditable[]` / `isReadonly[]` — признак редактируемости формы.

### Полиморфные edit и delete

`edit[Object]` и `delete[Object]` объявлены как абстрактные действия с реализацией по умолчанию (`SHOW EDIT` и `DELETE`) и доступны для расширения под конкретные пользовательские классы. `formEdit[Object]` — прямой проброс к `edit[Object]`. `formEditObject[Object]` оборачивает `formEdit[Object]` в `NEWSESSION` только если объект уже существует в БД (`PREV(o IS Object)`), и вызывает `formEdit[Object]` напрямую в противном случае. `formDelete[Object]` вызывает `delete[Object]` напрямую, когда `sessionOwners[]` непустой; иначе показывает подтверждение и при согласии выполняет `delete[Object]` с последующим `APPLY`.

### Локальные буферы

Платформа предоставляет три параллельных набора локальных свойств для разных стадий обмена:

- **`export…`** — куда `EXPORT` пишет промежуточный результат. По одному свойству на каждый встроенный класс: `exportObject[]`, `exportInteger[]`, `exportLong[]`, `exportDouble[]`, `exportNumeric[]`, `exportString[]`, `exportText[]`, `exportRichText[]`, `exportHTMLText[]`, `exportDate[]`, `exportTime[]`, `exportDateTime[]`, `exportZDateTime[]`, `exportYear[]`, `exportBoolean[]`, `exportTBoolean[]`, `exportInterval<Type>[]` для всех интервалов, `exportColor[]`, `exportJSON[]` / `exportJSONText[]`, `exportXML[]`, `exportHTML[]`, и аналогичные `export<Class>File[]` / `export<Class>Link[]` для всех файловых и ссылочных классов.
- **`requested…`** — те же буферы, объявленные как `NESTED` для пакетного запроса значений у пользователя (`INPUT` / `REQUEST`).
- **Импорт**: `importFile[]`, `imported[INTEGER]` и `importedString[STRING[10]]` — признак того, что соответствующая «строка» дошла из плоского файла. `inputList[INTEGER]` / `displayInputList[INTEGER]` — буфер списка значений и подписей. `readFile[]`, `readDialogPath[]`, `showResult[]` — результаты чтения файла, выбора пути и вывода сообщения.

### Идентичность приложения

`logicsName[]` (от `dataLogicsName`), `logicsCaption[]`, `topModule[]`, `displayName[]` (вычисляется как `dataDisplayName ?? logicsCaption ?? topModule`) — имя, заголовок и отображаемое имя приложения. `hashModules[]` — хеш набора модулей, под которым сервер видит инициализированную логику.

Графика: `logicsLogo[]` (изображение логотипа), `logicsIcon[]` (иконка приложения), `PWAIcon[]` (иконка для PWA, ожидается 512×512). Для каждого — тройка действий `load<Name>[]` / `open<Name>[]` / `reset<Name>[]`.

### Оформление по умолчанию

| Свойство                                       | Назначение                                                              |
|-------------------------------------------------|--------------------------------------------------------------------------|
| `defaultBackgroundColor[]` / `defaultOverrideBackgroundColor[]` | базовый цвет фона; OVERRIDE-вариант возвращает жёлтый (`RGB(255,255,0)`) при пустом значении |
| `defaultForegroundColor[]` / `defaultOverrideForegroundColor[]` | базовый цвет текста; OVERRIDE-вариант — красный                       |
| `selectedRowBackgroundColor[]` / `selectedCellBackgroundColor[]` / `focusedCellBackgroundColor[]` / `focusedCellBorderColor[]` / `tableGridColor[]` | пользовательские цвета строк, ячеек и сетки |
| `customReportCharWidth[]` / `reportCharWidth[]` | ширина символа в отчётах (по умолчанию `8`)                              |
| `customReportRowHeight[]` / `reportRowHeight[]` | высота строки в отчётах (по умолчанию `18`)                              |
| `reportNotToStretch[]` / `reportToStretch[]`    | управление растяжкой содержимого в отчёте                                |

### Среда исполнения и проверки

`checkIsServer[]` / `isServer[]` — проверка и признак, что текущее выполнение идёт на сервере. `random[]` — `DOUBLE` от `0` до `1`, `randomUUID[]` — `md5` от пары случайных значений, `randInt[INTEGER]` — целое в диапазоне `[1; max]`. `notEmpty[STRING]` — конвертирует пустую строку в `NULL`. `upper[STRING]` / `lower[STRING]` — регистр.

`requestCanceled[]` — родное свойство, признак отмены текущего запроса. `noSystemToolbarCaptions[]` — абстрактный флаг скрытия подписей системной панели. `isHTMLSupported[]` — родное свойство, поддерживает ли клиент HTML.

### Работа с файлами

`file[RAWFILE, STRING]` — собрать `FILE` из «сырых» байтов и расширения. `file[FILE, STRING]` — клонировать `FILE`, заменив расширение. `namedFile[…]` — аналогично для `NAMEDFILE` (с именем). `name[NAMEDFILE]`, `extension[FILE]`, `extension[NAMEDFILE]` — компоненты файла. `md5[FILE]` — MD5-хеш. `resourceImage[STRING]` — путь ресурса как файл-картинка для UI.

### Клиентская загрузка

`loadLibrary[STRING]` / `loadFont[STRING]` — загрузить на клиенте JS-библиотеку или шрифт. `reload[]` — перезагрузить клиент.

### Окна и навигатор

Модуль фиксирует основные [окна](Form_views.md): `logo`, `root`, `system` (верхняя горизонтальная панель), `toolbar` (левая вертикальная), `forms` (центральная область), `log` (правая полоса для уведомлений). Под каждое окно есть абстрактный CSS-класс (`logoWindowClass[]`, …) для кастомизации.

В навигаторе создаётся системная папка `Administration` с подпапками `Application` (опции / интеграция / миграция), `System` (производительность, нотификации, планировщик, логи).

### Язык

- [Заголовок модуля](../language/Module_header.md) — синтаксис `MODULE` / `REQUIRE`; модуль `System` подключается автоматически.
- [Оператор `APPLY`](../language/APPLY_operator.md) — лежит в основе действий управления сессией.
- [Оператор `CANCEL`](../language/CANCEL_operator.md) — отмена изменений сессии.
- [Оператор `EXPORT`](../language/EXPORT_operator.md) — пишет в локальные буферы `export…`.
- [Оператор `IMPORT`](../language/IMPORT_operator.md) — наполняет `imported`, `importedString` и связанные локальные свойства.

### Связано

- [`System modules`](System_modules.md) — общий перечень модулей платформы.
- [`Time`](System_Time.md) — отдельный модуль времени, подключающийся через `REQUIRE Time`.
- [`Utils`](System_Utils.md) — сборник вспомогательных свойств.
- [`Сессия изменений`](Change_sessions.md) — концепция сессии и её фиксации.
- [`Формы`](Forms.md) — что такое форма и как её жизненный цикл связан с системными действиями.
