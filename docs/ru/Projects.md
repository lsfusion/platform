---
title: 'Проекты'
---

*Проект* - это совокупность [модулей](Modules.md) и дополнительной информации (картинок, файлов дизайнов отчетов и т.п.), которые полностью описывают функциональность создаваемой информационной системы.

Проекты, как и модули, могут зависеть друг от друга. Граф проектов при этом должен "включать" в себя граф модулей, то есть если модуль `A` [зависит](Modules.md#depends) от модуля `B`, то и проект модуля `A` должен зависеть от проекта модуля `B`.

Также, как правило, для проектов существуют возможности версионирования и автоматизации сборки (например, формирование одного исполняемого файла со всеми зависимостями).

### Язык

С технической точки зрения проект - это не более чем множество файлов, поэтому поддержка проектов не является непосредственно частью платформы. Предполагается, что для этого используются внешние инструменты: начиная от простых встроенных в IDE, и заканчивая сложными универсальными фреймворками (вроде [Maven](https://maven.apache.org/)).

По умолчанию при запуске платформа ищет все файлы с расширением **lsf** в [classpath](Launch_parameters.md#appjava) стартуемого сервера приложений и считает их подключаемыми модулями. Модули подключаются в порядке их [зависимостей](Modules.md#depends), так если `A` зависит от `B` и от `C`, то по умолчанию сначала инициализируется `B`, потом `C`, и только потом `A`.

Впрочем, вышеописанное поведение можно изменять при помощи соответствующих lsFusion параметров запуска сервера приложений:

-   [`logics.includePaths`, `logics.excludePaths`](Launch_parameters.md#project) - пути (относительно classpath), в которых платформа будет искать lsf-файлы. При задании этих параметров можно использовать как пути к конкретным файлам (например `A/B/C.lsf`), так и шаблоны путей (например `A/*` - все lsf-файлы в папке `A` и всех ее подпапках). Кроме того в этих параметрах можно указывать сразу несколько путей (шаблонов путей) через точку с запятой, например `A.lsf;dirB/*`. Имя jar-файла в пути не учитывается (то есть файл лежащий в `b.jar/C/x.lsf` считается имеет путь `C/x.lsf`). По умолчанию `includePaths` равен `*` (то есть все файлы), `excludedPaths` - пустой.
-   [logics.topModule](Launch_parameters.md#project) - имя верхнего модуля. Если этот параметр задан (не пустой), будут подключаться не все найденные lsf-файлы, а только заданный модуль и все его [зависимости](Modules.md#depends). По умолчанию этот параметр считается не заданным (пустым).
-   [logics.orderDependencies](Launch_parameters.md#project) - переопределение порядка зависимостей (задается как имена модулей через запятую). Так если `A` зависит от `B` и `C`, а этом параметре присутствует `B` и `C`, причем `C` идет раньше `B`, то `C` будет инициализирован до `B`. По умолчанию этот параметр считается не заданным (пустым), то есть используется порядок `REQUIRE` в самих lsf файлах.

Вне зависимости от описанных выше параметров, платформа всегда автоматически подключает следующие системные модули: [`System`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/System.lsf), [`Service`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Service.lsf), [`Reflection`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Reflection.lsf), [`Authentication`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Authentication.lsf), [`Security`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Security.lsf), [`SystemEvents`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/SystemEvents.lsf), [`Scheduler`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Scheduler.lsf), [`Email`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Email.lsf), [`Time`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Time.lsf) and [`Utils`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Utils.lsf).
