---
slug: "/Brief_modularity"
title: 'Brief: модули'
---

## Модули и порядок

*Модуль* — функционально законченная часть проекта: объявления классов, свойств, действий, форм, событий, ограничений ([модули](../paradigm/Modules.md)). Один модуль — один файл `.lsf`, начинающийся с [заголовка модуля](../language/Module_header.md): `MODULE`, `REQUIRE`, `PRIORITY`, `NAMESPACE`.

`REQUIRE` перечисляет модули, от которых текущий [зависит](../paradigm/Modules.md#depends). Зависимость транзитивна, циклы не допускаются, и по ней строится порядок инициализации: модуль инициализируется после всех своих зависимостей. От модуля `System` зависит любой модуль. Зависимость определяет и видимость: элемент можно найти по имени только в модуле-зависимости, поэтому расширять чужой функционал позволяет техника [расширений](../paradigm/Extensions.md), см. [Brief: расширения](Brief_extensions.md).

[Проект](../paradigm/Projects.md) — совокупность модулей и сопутствующих файлов; по умолчанию модулями считаются все файлы `.lsf` в classpath сервера приложений, а параметры запуска `logics.includePaths`, `logics.topModule`, `logics.orderDependencies` сужают этот набор и переопределяют порядок.

**Аналогия**: пакет или сборка.

```lsf
MODULE Sale;
REQUIRE System, Utils, Item;
NAMESPACE Sale;
```

## Системные модули

*Системные модули* поставляются вместе с платформой — это стандартная библиотека, которую проект подключает через `REQUIRE` и не переопределяет ([системные модули](../paradigm/System_modules.md)). Двенадцать из них платформа загружает сама — `System`, `Service`, `Reflection`, `Authentication`, `Security`, `SystemEvents`, `Email`, `Icon`, `Scheduler`, `Time`, `Utils`, `UserEvents`, — но загруженность не означает зависимость: неявной зависимостью каждого модуля является только `System`, а объявление из любого другого требует `REQUIRE`.

| Модуль | Для чего |
| --- | --- |
| [`System`](../paradigm/System_System.md) | корневые типы, базовые классы, инфраструктура |
| [`Utils`](../paradigm/System_Utils.md) | вспомогательные свойства и действия общего назначения |
| [`Time`](../paradigm/System_Time.md) | свойства и операции над датой и временем |
| [`Authentication`](../paradigm/System_Authentication.md) | пользователи, контакты, вход в систему |
| [`Security`](../paradigm/System_Security.md) | роли и политики доступа |
| [`Service`](../paradigm/System_Service.md) | сервисные действия и настройки сервера |
| [`SystemEvents`](../paradigm/System_SystemEvents.md) | события жизненного цикла сервера |
| [`UserEvents`](../paradigm/System_UserEvents.md) | программный доступ к фильтрам и сортировкам формы |
| [`Reflection`](../paradigm/System_Reflection.md) | метаданные о навигаторе, формах, свойствах, таблицах |
| [`Scheduler`](../paradigm/Scheduler.md) | запуск действий по расписанию |
| [`Email`](../paradigm/System_Email.md) | отправка и приём почты |
| [`Icon`](../paradigm/System_Icon.md) | каталог иконок интерфейса |

Прикладные дополнения: `Backup`, `Chat`, [`Eval`](../paradigm/Eval_EVAL.md), `Excel`, `Document` / `Word`, `Image` / `OpenCV`, `I18n`, `Integration`, `MasterData`, [`Numerator`](../paradigm/Utils_Numerator.md), `Hierarchy`, `Historizable`, `Geo`, `Printer` / `QZTray` / `Sound` / `Com`, `ProcessMonitor` / `Profiler`, `RabbitMQ` / `WebSocket`, `Messenger` со своими `Telegram` / `Slack` / `Viber` / `Whatsapp` / `Skype`, `SQLUtils`, `DefaultData`, `Schedule`.

Всё это — обычные объявления в `.lsf`, а не примитивы языка: `lpad`, `currentDate`, `currentUser` — свойства, искать их следует в ветке `paradigm`.
