---
title: 'Параметры запуска'
---

## Сервер приложений (Server)

### Java {#appjava}

Java параметры запуска сервера приложений задаются в его команде запуска (например для [ручной](Execution_manual.md#command) или [автоматической](Execution_auto.md#settings) установки):

||Название|Тип|Описание|По умолчанию|
|---|---|---|---|---|
|Системные (начинаются на `X`)|[Стандартные](https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)||Стандартные Java параметры. Прежде всего важно обратить внимание на:<ul><li>`cp` - classpath, пути в которых java ищет class файлы и другие ресурсы (в том числе lsFusion модули). По умолчанию равен `.` - текущая папка (при [автоматической установке отличается](Execution_auto.md)).</li><li>`Xmx` - максимальный размер памяти. Значение по умолчанию определяется в зависимости от конфигурации компьютера, на котором запускается сервер приложений. Для сложных логик рекомендуется устанавливать не менее 4ГБ. </li></ul>||
||`-XX:CMSInitiatingOccupancyFraction`|`int`|Вообще это стандартный параметр, отвечающий за порог, после которого включается CMS сборщик мусора. В то же время платформа использует этот параметр для таргетирования объема используемой памяти при помощи LRU кэшей (устанавливая более агрессивные параметры их очистки, если эта цель превышена, и менее агрессивные - в обратном случае). Для высоконагруженных серверов рекомендуется устанавливать в диапазоне от `40` до `60`.|`70`|
|Пользовательские (начинаются на `D`)|`-Dlsfusion.server.lightstart`|`boolean`|Режим "облегченного" запуска (как правило используется при разработке). В этом режиме сервер не выполняет операции синхронизации метаданных, создания форм настройки [политики безопасности](Security_policy.md) и т.п., соответственно уменьшается время запуска и объем потребляемой памяти при запуске.<br/>В [IDE](IDE.md) регулируется галочкой в [lsFusion server конфигурации](IDE.md#configuration) (по умолчанию включена).|`false`|
||<a className="lsdoc-anchor" id="devmode"/>`-Dlsfusion.server.devmode`|`boolean`|Режим разработки. В этом режиме:<ul><li>Не запускаются системные задачи (чтобы не мешать отладчику)</li><li>Включается возможность редактирования [дизайна отчетов](Report_design.md) в [интерактивном печатном](In_a_print_view_PRINT.md#interactive) представлении</li><li>Включается анонимный доступ к API и UI ([системные параметры](Working_parameters.md) `enableAPI`, `enableUI`). Кроме того в этом режиме анонимный доступ идет под админом, а не анонимным пользователем</li><li>Клиент автоматически переподключается при потере связи</li><li>Выключается кэш чтения отчетов из ресурсов</li></ul>В [IDE](IDE.md) автоматически включается при запуске [lsFusion server конфигурации](IDE.md#configuration).|`false`|
||`-Dlsfusion.server.testmode`|`boolean`|Включает некоторые экспериментальные возможности<br/>Автоматически включается, если включены assertion'ы (опция `-ea`)|`false`|

### lsFusion {#applsfusion}

lsFusion параметры запуска сервера приложений могут задаваться одним из следующих способов (в порядке их приоритетов, снизу более приоритетные) :

-   В ресурсах в xml-файле `lsfusion.xml` в местах использования этих параметров, после: (актуально для форков платформы)
-   В `lsfusion.properties` (обычно являются частью проекта, а значит действует по умолчанию для всех инсталляций)
-   В `conf/settings.properties` (для конкретных инсталляций)
-   В [Java параметрах запуска](#appjava) (начиная с `D`, например `-Dlogics.topModule=FFF`)

|Название|Тип|Описание|По умолчанию|
|---|---|---|---|
|<a className="lsdoc-anchor" id="connectdb"/>`db.server`, `db.name`, `db.user`, `db.password`, `db.connectTimeout`|`string`, `string`, `string`, `string`, `int`|Параметры подключения к серверу БД (базы данных):<ul><li>`db.server` - адрес сервера БД (плюс при необходимости порт через:, например `localhost:6532`)</li><li>`db.name` - имя БД</li><li>`db.user` - имя пользователя для подключения к серверу БД</li><li>`db.password` - пароль пользователя для подключения к серверу БД</li><li>`db.connectTimeout` - таймаут подключения к СУБД</li></ul>|`localhost`, `lsfusion`, `postgres`, , `1000`|
|<a className="lsdoc-anchor" id="accessapp"/>`rmi.port`, `rmi.exportName`, `http.port`|`int`, `string`, `int`|Параметры доступа к серверу приложений:<ul><li>`rmi.port` - порт сервера приложений (экспортируемых им RMI регистра / объектов)</li><li>`rmi.exportName` - имя сервера приложений (экспортируемого им корневого RMI объекта). Имеет смысл использовать, если на одном порту необходимо экспортировать несколько логик</li><li>`http.port` - порт веб-сервера встроенного в сервер приложений (используется для [обращения из внешних систем](Access_from_an_external_system.md))</li></ul>|`7652`, `default`, `7651`|
|<a className="lsdoc-anchor" id="project"/>`logics.includePaths`, `logics.excludePaths`, `logics.topModule`, `logics.orderDependencies`|`string`, `string`, `string`, `string`|Параметры [проекта](Projects.md) (какие модули загружать и в каком порядке, подробное описание по ссылке)|`logics.includePaths` равен `*`, остальные - пустые|
|<a className="lsdoc-anchor" id="locale"/>`user.country`, `user.language`, `user.timezone`, `user.twoDigitYearStart` (`user.setCountry`, `user.setLanguage`, `user.setTimezone`)|`string`, `string`, `string`, `int`|Стандартные Java параметры, определяющие параметры [локали](Internationalization.md#locale) (региональные настройки - язык, страна и т.п., подробное описание по ссылке)<br/>Из-за особенностей Java Spring (а именно, что параметры локали считаются Java Spring заданными, даже если они явно не заданы в команде запуска, то есть настройки этих параметров в `.properties` файлах игнорируются), в платформе поддерживаются "клоны" этих параметров начинающиеся на set, которые, в случае если заданы (как в `.properties` файлах так и в строке запуска), "перегружают" родные параметры. То есть приоритет такой ОС, `-Duser.*`, `user.set*` в `.properties` файлах, `-Duser.set*` (все вышесказанное не касается `user.twoDigitYearStart`, так как он не является стандартным Java параметром)|Первые три определяются из настроек операционной системы, Текущий год минус `80`|
|<a className="lsdoc-anchor" id="namingpolicy"/>`db.namingPolicy`, `db.maxIdLength`|`string`, `int`|Параметры [политики именования](Tables.md#name) таблиц и полей:`db.namingPolicy` - имя java-класса политики (полное, с package'м), в конструкторе должен принимать один параметр типа `int` - максимальный размер имени.Имена классов встроенных политик:<ul><li>Полное с сигнатурой - `lsfusion.server.physics.dev.id.name.FullDBNamingPolicy`</li><li>Полное без сигнатуры - `lsfusion.server.physics.dev.id.name.NamespaceDBNamingPolicy`</li><li>Краткое - `lsfusion.server.physics.dev.id.name.ShortDBNamingPolicy`</li></ul>`db.maxIdLength` - максимальный размер имени таблицы или поля. Передается первым параметром в конструктор java-класса политики именования таблиц и полей.|Полное с сигнатурой, `63`|
|`db.denyDropModules`, `db.denyDropTables`|`boolean`, `boolean`|Запреты на удаления при запуске:<ul><li>`db.denyDropModules` - модулей</li><li>`db.denyDropTables` - таблиц</li></ul>|`false`, `false`|
|`logics.initialAdminPassword`|`string`|Пароль администратора по умолчанию||

### Пример файла conf/settings.properties ([3-й пункт](#applsfusion)): {#filesettings}

**$FUSION\_DIR$/conf/settings.properties**

    db.server=localhost
    db.name=lsfusion
    db.user=postgres
    db.password=pswrd

    rmi.port=7652


:::info
По умолчанию предполагается, что файлы параметров запуска `conf/settings.properties` и `lsfusion.properties` находятся в папке запуска сервера приложений. Впрочем при [автоматической установке](Execution_auto.md) под Linux для этих файлов (как и для папок [логов](Journals_and_logs.md#logs))  автоматически создаются symlink'и на [другие файлы](Execution_auto.md#settings), расположение которых лучше соответствует идеологии Linux.
:::

## Веб-сервер (Client)

### Java {#webjava}

Java параметры запуска веб-сервера задаются в команде запуска Tomcat, на котором, в свою очередь, запускается этот веб-сервер (например для [автоматической](Execution_auto.md#settings) установки). 

||Название|Тип|Описание|
|---|---|---|---|
|Системные (начинаются на `X`)|[Стандартные](https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)||Стандартные Java параметры. Прежде всего важно обратить внимание на:<ul><li>`Xmx` - максимальный размер памяти. Для сложных логик рекомендуется устанавливать не менее 2ГБ.</li></ul>|

### lsFusion {#weblsfusion}

lsFusion параметры запуска веб-сервера могут задаваться одним из следующих способов (в порядке их приоритетов, снизу более приоритетные):

-   В параметрах [контекста](http://tomcat.apache.org/tomcat-7.0-doc/config/context.html#Defining_a_context) веб-приложения:
    -   в веб-приложении в файле `/WEB-INF/web.xml`, тег `context-param` (актуально для форков платформы)
    -   в веб-приложении в файле `/META-INF/context.xml`, тег `Context`, тег `Parameter` (актуально для форков платформы)
    -   в Tomcat в файле `$CATALINA_BASE/conf/[enginename]/[hostname]/[contextpath].xml`, тег `Context`, тег `Parameter`, где:
        -   `$CATALINA_BASE$` - папка, в которую установлен Tomcat (например, в [автоматической](Execution_auto.md#settings) установке, эта папка равна `$INSTALL_DIR/Client`)
        -   `[contextpath]` - контекстный путь веб-приложения (например, в [автоматической](Execution_auto.md#settings) установке, по умолчанию это имя пустое, что в Tomcat'е эквивалентно имени `ROOT`, в [ручной](Execution_manual.md#appservice) - зависит от имени war-файла), 
        -   `[enginename]` и `[hostname]` - имена механизма реализации tomcat и компьютера веб-сервера (например в [автоматической](Execution_auto.md#settings) установке, эти имена равны `catalina` и `localhost` соответственно)
    -   в Tomcat в файле `$CATALINA_BASE/conf/server.xml`, тег `Context`, тег `Parameter` (не рекомендуется)
-   В параметрах URL'а (например `http://tryonline.lsfusion.org?host=3.3.3.3&port=4444`)

|Название|Тип|Описание|По умолчанию|
|---|---|---|---|
|<a className="lsdoc-anchor" id="connectapp"/>`host`, `port`, `exportName`|`string`, `int`, `string`|Параметры подключения к серверу приложений. Должны соответствовать [параметрам доступа](#accessapp) к серверу приложений.<ul><li>`host` - адрес сервера приложений</li><li>`port` - порт сервера приложений. Должен соответствовать параметру - `rmi.port`</li><li>`exportName` - имя сервера приложений. Должен соответствовать параметру - `rmi.exportName`</li></ul>|`localhost`, `7652`, `default`|

### Пример файла настройки Tomcat ([3-й пункт](#weblsfusion) в параметрах контекста): {#filewebsettings}

**$CATALINA\_BASE/conf/\[enginename\]/\[hostname\]/ROOT.xml**
```xml
<?xml version='1.0' encoding='utf-8'?>
<Context>
    <Parameter name="host" value="localhost" override="false"/>
    <Parameter name="port" value="7652" override="false"/>
</Context>
```

:::info
Помимо параметров запуска, в платформе также существуют [системные параметры](Working_parameters.md), которые задаются немного по другому и актуальны преимущественно для процессов работы различных компонент платформы (то есть процессов, происходящих после их запуска).
:::
