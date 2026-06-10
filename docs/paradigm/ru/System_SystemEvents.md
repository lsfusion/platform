---
slug: "/System_SystemEvents"
title: 'SystemEvents'
---

`SystemEvents` — [системный модуль](System_modules.md), объявляющий обработчики событий запуска платформы, свойства версий платформы, настройки оформления по среде, а также системные журналы исключений, запусков сервера, подключений клиентов, сессий изменений и пингов компьютеров. Он же собирает действия push-уведомлений и взаимодействия с клиентом и пункты навигатора для логотипа, диалога оформления и форм журналов. Подключается через `REQUIRE SystemEvents` (`System`, `Reflection` и `Time` тянутся автоматически).

### События запуска

Эти абстрактные списки действий — обработчики [событий запуска](Launch_events.md): разработчик встраивает в них логику инициализации, а платформа запускает нужный список в соответствующий момент запуска сервера или клиента. `onStarted[]` — обработчик запуска сервера приложений; полный набор обработчиков и моменты их срабатывания описаны в статье [`События запуска`](Launch_events.md).

| Обработчик                       | Когда выполняется                                                                              |
|----------------------------------|------------------------------------------------------------------------------------------------|
| `onInit[]`                       | ранняя инициализация сервера до основного старта; запускает синхронизацию версий               |
| `onStarted[]`                    | запуск сервера приложений, до приёма подключений клиентов                                       |
| `onFirstStarted[]`               | только самый первый запуск сервера приложений (когда выполняется `firstStart[]`)                |
| `onFinallyStarted[]`             | после `onStarted[]`, в конце последовательности запуска                                        |
| `onClientStarted[]`              | подключение клиента; диспетчеризует на десктоп- или веб-обработчик по типу клиента             |
| `onDesktopClientStarted[]`       | подключение десктоп-клиента                                                                    |
| `onWebClientStarted[]`           | подключение веб-клиента                                                                        |
| `onWebClientInit[STRING]`        | список по ресурсу с ключом-путём к CSS / JS-ресурсу, выполняется при инициализации веб-клиента  |
| `onLoginInit[STRING]`            | список по ресурсу, выполняется на странице входа                                               |

`notFirstStart[]` — хранимый признак того, что сервер уже запускался; `firstStart[]` — его отрицание, выбирающее первый запуск. У каждого обработчика есть обёртка `…Apply` (`onInitApply[]`, `onStartedApply[]`, `onFinallyStartedApply[]`, `onClientStartedApply[]`), которая выполняет список и фиксирует изменения через `APPLY`; `onStartedApply[]` дополнительно запускает `onFirstStarted[]` на первом запуске и устанавливает `notFirstStart[]`.

### Версии

| Свойство                  | Что хранит                                                                    |
|---------------------------|-------------------------------------------------------------------------------|
| `platformVersion[]`       | версия платформы как `TEXT`                                                    |
| `apiVersion[]`            | версия API как `INTEGER`                                                       |
| `revisionVersion[]`       | ревизия исходников как `INTEGER`                                               |
| `synchronizeVersions[]`   | читает текущие версии платформы / API / ревизии в свойства выше; запускается из `onInit[]` |

### Оформление

Настройки оформления разрешаются по `DesignEnv` — объекту среды оформления, представляющему выбранный одним клиентом внешний вид. Выбор задаётся тремя перечислимыми [классами](User_classes.md):

| Класс    | Значения                                                                        |
|----------|----------------------------------------------------------------------------------|
| `Theme`  | `excel`, `classic`, `flatly`, `lumen`, `quartz`, `simplex`, `sketchy`, `yeti`     |
| `Size`   | `normal`, `mini`, `tiny`                                                          |
| `Navbar` | `horizontal`, `vertical`                                                          |

У каждой настройки есть значение по среде (`designEnv…`), общесерверное значение по умолчанию (`server…`) и разрешённое значение. `theme[DesignEnv]`, `size[DesignEnv]` и `navbar[DesignEnv]` разрешаются через `OVERRIDE`: сначала значение по среде, затем серверное значение по умолчанию, затем встроенный запасной вариант (`Theme.classic` для темы, `heuristicSize[]` для размера, `Navbar.horizontal` для панели). Формы для текущей среды `size[]`, `navbar[]` читают разрешённое значение для `currentDesignEnv[]`.

| Свойство                          | Что задаёт                                                                      |
|-----------------------------------|----------------------------------------------------------------------------------|
| `theme[DesignEnv]` / `serverTheme[]` | цветовая тема и оформление виджетов; `nameTheme[]` — её имя как `STRING`       |
| `useBootstrap[DesignEnv]` / `useBootstrap[]` | используется ли оформление на основе Bootstrap (истинно для всех тем, кроме `Theme.excel`) |
| `size[DesignEnv]` / `serverSize[]` | размер виджетов; `isMini[]` / `isTiny[]` отмечают компактные размеры            |
| `navbar[DesignEnv]` / `serverNavbar[]` | ориентация панели навигации; `verticalNavbar[]` отмечает вертикальную         |
| `navigatorPinMode[DesignEnv]` / `navigatorPinMode[]` | режим закрепления навигатора; берётся из значения по среде, когда выполняется `useClientNavigatorPinMode[DesignEnv]`, иначе из серверного значения `serverNavigatorPinMode[]` |
| `mobileMode[DesignEnv]` / `mobileMode[]` | принудительно включает или выключает мобильную раскладку                    |
| `suppressOnFocusChange[DesignEnv]` | подавляет применение при смене фокуса                                           |
| `contentWordWrap[DesignEnv]` / `contentWordWrap[]` | перенос текста содержимого                                        |
| `highlightDuplicateValue[DesignEnv]` / `highlightDuplicateValue[]` | подсветка повторяющихся значений ячеек             |
| `userFiltersManualApplyMode[DesignEnv]` / `userFiltersManualApplyMode[]` | применение пользовательских фильтров вручную, а не по мере ввода |
| `dontShowCloseButtonOnInactiveTab[DesignEnv]` / `dontShowCloseButtonOnInactiveTab[]` | скрывает кнопку закрытия на неактивных вкладках     |

`ColorTheme` (светлая / тёмная / авто) переключается действием `toggleColorTheme[]`, которое для текущей среды переходит светлая → тёмная → авто, применяет и обновляет форму. Режим закрепления навигатора переключается действием `toggleNavigatorPinMode[]`.

Форма `design` редактирует оформление текущей среды (`captionTheme[DesignEnv]`, `captionSize[DesignEnv]`, `captionNavbar[DesignEnv]` и переключатели выше) и перезагружает клиент при применении, если изменилась настройка, требующая перезагрузки; `showDesign[]` открывает её плавающим окном. В окне `system` навигатора модуль добавляет пункты `showDesign`, `toggleNavigatorPinMode` и `toggleColorTheme`.

При инициализации веб-клиента `onWebClientInit[STRING]` регистрирует CSS- и JS-ресурсы клиента: набор Bootstrap и зависящие от размера таблицы отступов / шрифтов, когда выполняется `useBootstrap[]`, либо обычные табличные стили в противном случае, а затем общие скрипты и стили виджетов. Ресурсы, лежащие под `/onStarted/`, подхватываются автоматически.

### Исключения

Иерархия классов `Exception` фиксирует ошибки сервера и клиента. `Exception` — абстрактный корень, делящийся на `ServerException` и `ClientException`; клиентская ветвь уточняется дальше:

| Класс                      | Место в иерархии                                                       |
|----------------------------|-------------------------------------------------------------------------|
| `Exception`                | абстрактный корень всех журналируемых исключений                        |
| `ServerException`          | ошибка на сервере                                                       |
| `ClientException`          | ошибка, сообщённая клиентом                                             |
| `WebClientException`       | ошибка веб-клиента (`: ClientException`)                                |
| `RemoteServerException`    | ошибка удалённого сервера, сообщённая клиенту (`: ClientException`)     |
| `RemoteClientException`    | абстрактная база для исключений, поднятых на удалённом клиенте (`: ClientException`) |
| `UnhandledException`       | необработанное исключение удалённого клиента (`: RemoteClientException`) |
| `HandledException`         | абстрактная база для обработанных исключений удалённого клиента (`: RemoteClientException`) |
| `FatalHandledException`    | фатальное обработанное исключение (`: HandledException`)                |
| `NonFatalHandledException` | нефатальное обработанное исключение (`: HandledException`)              |

| Свойство                         | Что хранит                                                             |
|----------------------------------|------------------------------------------------------------------------|
| `message[Exception]`             | текст ошибки                                                            |
| `date[Exception]` / `fromDate[Exception]` | момент возникновения как `DATETIME` / его `DATE`              |
| `erTrace[Exception]`             | Java-стек                                                              |
| `lsfStackTrace[Exception]`       | lsFusion-стек                                                          |
| `asyncStackTrace[Exception]`     | асинхронный стек                                                       |
| `type[Exception]`                | имя типа исключения                                                    |
| `javaStackTrace[Exception]`      | сообщение и Java-стек, объединённые вместе                             |
| `client[ClientException]` / `login[ClientException]` | компьютер клиента и логин, поднявшие ошибку          |
| `count[NonFatalHandledException]` | сколько раз свёрнуто повторяющееся нефатальное исключение            |
| `abandoned[NonFatalHandledException]` | было ли повторяющееся исключение заброшено                       |

Исключения журналируются через `@defineLog` и показываются на форме `exceptions`, где цвета текста и фона выделяют нефатальные и разные клиентские / серверные виды.

### Запуски

Класс `Launch` фиксирует каждый запуск сервера приложений.

| Свойство              | Что хранит                                                    |
|-----------------------|----------------------------------------------------------------|
| `computer[Launch]`    | компьютер сервера; `hostname[Launch]` — его имя хоста          |
| `time[Launch]`        | момент запуска как `DATETIME`; `date[Launch]` — его `DATE`     |
| `revision[Launch]`    | версия платформы, версия API и ревизия запуска                 |

`currentLaunch[]` хранит объект запуска работающего сервера. `onStarted[]` создаёт новый `Launch`, заполненный текущими компьютером, временем и версией, и устанавливает `currentLaunch[]`. Запуски журналируются через `@defineLog` и показываются на форме `launches`.

### Подключения

Класс `Connection` фиксирует каждое подключение клиента. `currentConnection[]` — подключение текущего запроса.

| Свойство                                                        | Что хранит                                                        |
|------------------------------------------------------------------|-------------------------------------------------------------------|
| `computer[Connection]`                                          | компьютер клиента; `hostnameComputer[Connection]` — его имя хоста |
| `remoteAddress[Connection]`                                     | удалённый IP-адрес клиента                                        |
| `headers[Connection, TEXT]` / `userAgent[Connection]`           | заголовки запроса и заголовок `User-Agent`                        |
| `cookies[Connection, TEXT]` / `sessionId[Connection]`           | cookie и cookie `JSESSIONID`                                      |
| `params[Connection, TEXT, INTEGER]` / `params[Connection, TEXT]` | параметры запроса, индексированные и объединённые                |
| `user[Connection]` / `userLogin[Connection]`                    | подключённый пользователь и логин                                 |
| `osVersion[Connection]` / `processor[Connection]` / `architecture[Connection]` / `cores[Connection]` | ОС и оборудование клиента          |
| `physicalMemory[Connection]` / `totalMemory[Connection]` / `maximumMemory[Connection]` / `freeMemory[Connection]` | характеристики памяти клиента |
| `javaVersion[Connection]` / `is64Java[Connection]`              | версия Java клиента и признак 64-битности                         |
| `screenWidth[Connection]` / `screenHeight[Connection]` / `screenSize[Connection]` / `scale[Connection]` | геометрия экрана и масштаб       |
| `clientType[Connection]` / `nameClientType[Connection]`         | тип клиента (`ClientType`) и его название                         |
| `connectionStatus[Connection]` / `nameConnectionStatus[Connection]` | статус подключения (`ConnectionStatus`) и его название        |
| `connectTime[Connection]` / `connectDate[Connection]` / `disconnectTime[Connection]` | моменты подключения и отключения                 |
| `lastActivity[Connection]` / `lastActivity[CustomUser]`         | момент последней активности подключения / пользователя            |

`ClientType` различает четыре вида клиента: `nativeDesktop`, `nativeMobile`, `webDesktop`, `webMobile`. Признаки по виду `isNativeDesktop[Connection]`, `isNativeMobile[Connection]`, `isWebDesktop[Connection]`, `isWebMobile[Connection]` проверяют тип, а производные `isDesktop[Connection]`, `isMobile[Connection]`, `isNative[Connection]`, `isWeb[Connection]` их объединяют (десктоп = нативный или веб-десктоп, нативный = нативный десктоп или мобильный, и т. д.). У каждого признака есть и форма для текущего подключения (`isNativeDesktop[]`, `isWeb[]`, …) над `currentConnection[]`.

`ConnectionStatus` отслеживает жизненный цикл: `connectedConnection`, `disconnectingConnection`, `disconnectedConnection`. `shutdown[Connection]` помечает подключение как отключаемое и просит его клиент завершиться; `shutdown[CustomUser]` делает то же для всех подключений пользователя; `reconnect[CustomUser]` просит каждый подключённый клиент пользователя переподключиться. Подключения журналируются через `@defineLog` и показываются на форме `connections`, где также перечислены формы, сессии, заголовки, cookie и параметры подключения.

### URL подключения

| Свойство                        | Что строит                                                                    |
|---------------------------------|--------------------------------------------------------------------------------|
| `origin[Connection]`            | `scheme://webHost:webPort` подключения                                         |
| `webPath[Connection]`           | `origin` плюс путь контекста                                                    |
| `currentOrigin[]` / `currentWebPath[]` | origin / веб-путь `currentConnection[]` либо собственные значения запроса, когда подключения нет |
| `currentOriginUrl[STRING]` / `currentOriginUrl[LINK]` | URL относительно текущего origin                               |
| `currentContextUrl[STRING]`     | URL относительно текущего веб-пути, с дописанным query подключения              |

### Журнал сессий изменений

Класс `Session` фиксирует одну зафиксированную сессию изменений.

| Свойство                                                            | Что хранит                                                   |
|----------------------------------------------------------------------|--------------------------------------------------------------|
| `user[Session]` / `nameUser[Session]` / `nameContact[Session]`       | пользователь, выполнивший сессию, и имена                    |
| `dateTime[Session]`                                                  | момент фиксации, устанавливаемый при создании сессии         |
| `form[Session]` / `captionForm[Session]`                            | форма, с которой пришли изменения, и её название             |
| `connection[Session]` / `hostnameComputerConnection[Session]` / `userLoginConnection[Session]` | исходное подключение, его хост и логин |
| `quantityAddedClasses[Session]` / `quantityRemovedClasses[Session]` / `quantityChangedClasses[Session]` | сколько объектов добавлено, удалено, изменено |
| `changes[Session]`                                                  | текстовая детализация изменений                              |

Сессии журналируются через `@defineLog` и показываются на форме `changes`, отфильтрованной по диапазону даты-времени. Расширения `clearApplicationLog[]` удаляют сессии старше `countDaysClearSession[]` и очищают детализацию изменений старше `countDaysClearSessionDetail[]`.

### Пинги

Запись пинга хранит замеры памяти по компьютеру за интервал, с ключом `(Computer, DATETIME from, DATETIME to)`.

| Свойство                                                                 | Что хранит                                              |
|--------------------------------------------------------------------------|----------------------------------------------------------|
| `pingFromTo[Computer, DATETIME, DATETIME]`                               | длительность пинга за интервал                           |
| `minTotalMemoryFromTo[…]` / `maxTotalMemoryFromTo[…]`                     | минимальная / максимальная общая память за интервал      |
| `minUsedMemoryFromTo[…]` / `maxUsedMemoryFromTo[…]`                       | минимальная / максимальная использованная память за интервал |

`limitPing[]`, `limitMaxTotalMemory[]` и `limitMaxUsedMemory[]` задают пороги предупреждений; свойства `…Sum` суммируют время, проведённое выше каждого порога, а свойства `average…DateFrom` дают взвешенные по времени средние за диапазон. Форма `pings` показывает замеры по компьютеру, оборудование компьютера, взятое из последнего подключения, а также пороги и средние. `countDaysClearPings[]` задаёт, сколько дней записей пинга хранить; расширение `clearApplicationLog[]` удаляет более старые.

### Push-уведомления и взаимодействие с клиентом

| Свойство / действие                                     | Что делает                                                                  |
|---------------------------------------------------------|------------------------------------------------------------------------------|
| `pushPublicKey[]` / `pushPrivateKey[]`                  | пара ключей VAPID для web push, заполняемая на первом запуске                 |
| `subscription[Connection]`                              | подписка подключения на web push                                             |
| `notify[JSON, JSON]`                                    | показывает уведомление на клиенте текущего подключения                        |
| `push[Connection, JSON, JSON]`                          | проталкивает уведомление на клиент подключения                               |
| `pushNotify[Connection, JSON, JSON]`                    | проталкивает и показывает уведомление сразу                                  |
| `notification[STRING, JSON]` / `notification[STRING]`   | строит `JSON` уведомления из заголовка и опций                               |
| `action[INTEGER]` / `action[STRING]`                    | строит `JSON` действия из идентификатора уведомления или URL                  |
| `share[STRING, STRING, STRING]` / `shareAction[STRING]` | делится URL через диалог клиента, с откатом на всплывающее копирование ссылки |
| `evalServer[TEXT]`                                       | выполняет переданный код на сервере                                          |
| `evalInAllCurrentConnections[TEXT, TEXT]`               | выполняет переданный код на каждом подключённом клиенте                      |

`customize[STRING, STRING]` открывает диалог `customizeForm` для настройки формы: базовый код и код `EXTEND FORM`, хранимый в `dataExtendCode[Form]` (для всех пользователей) и `dataExtendCode[Form, User]` (для текущего пользователя). `formCustomizeBackground[]` и `formCustomizeShowIf[]` подкрашивают и показывают пункт настройки.

### Логотип

`logo[]` — изображение логотипа навигатора; `logoAction[]` — пункт навигатора с логотипом, показывающий текущую версию и пользователя. Модуль размещает `logoAction` в окне `logo` навигатора.

### Язык

- [Заголовок модуля](../language/Module_header.md) — синтаксис `MODULE` / `REQUIRE`, подключающий модуль.
- [Оператор `ABSTRACT`](../language/ABSTRACT_action_operator.md) — абстрактные списки действий, которыми объявлены обработчики событий запуска.
- [Оператор `WHEN`](../language/WHEN_statement.md) — обработчики событий, реагирующие на изменения оформления и запускающие перезагрузку или изменение размера клиента.

### Связано

- [`System modules`](System_modules.md) — общий список модулей платформы.
- [`События запуска`](Launch_events.md) — концепция событий запуска; объявленные этим модулем обработчики жизненного цикла (`onStarted`, `onWebClientStarted`, …) живут там.
- [`Журналы и логи`](Journals_and_logs.md) — системные журналы, которые наполняет этот модуль (исключения, запуски, подключения, сессии, пинги).
- [`Монитор процессов`](Process_monitor.md) — мониторинг работающих подключений и активности сервера.
- [`Навигатор`](Navigator.md) — пункты навигатора, которые добавляет этот модуль (логотип, оформление, журналы).
- [`Service`](System_Service.md) — служебные действия и мониторинг базы данных.
- [`Reflection`](System_Reflection.md) — метаданные о навигаторе, формах и свойствах.
- [`Authentication`](System_Authentication.md) — пользователи, контакты и вход.
