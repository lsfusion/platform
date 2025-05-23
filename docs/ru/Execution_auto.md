---
title: 'Для промышленной эксплуатации'
---

## Установка

Помимо установки lsFusion эти программы / скрипты установки также устанавливают **OpenJDK**, **PostgreSQL** и **Tomcat**. При этом Tomcat встраивается в установку lsFusion Client, а OpenJDK и PostgreSQL устанавливаются отдельно (в частности, в отдельные папки).

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

Исполняемые exe файлы:
**lsFusion 6.0** (OpenJDK **21.0.6**, PostgreSQL **17.4**, Tomcat **9.0.104**, IntelliJ IDEA Community Edition **2025.1**)

- [x64](https://download.lsfusion.org/exe/lsfusion-6.0-x64.exe)
- <details>
  <summary>Предыдущие версии</summary>

    - lsFusion 5.1 Server & Client
        - [x64](https://download.lsfusion.org/exe/lsfusion-5.1-x64.exe)
    - lsFusion 4.1 Server & Client
        - [x32](https://download.lsfusion.org/exe/lsfusion-4.1.exe)
        - [x64](https://download.lsfusion.org/exe/lsfusion-4.1-x64.exe)
    - lsFusion 3.1 Server & Client
        - [x32](https://download.lsfusion.org/exe/lsfusion-3.1.exe)
        - [x64](https://download.lsfusion.org/exe/lsfusion-3.1-x64.exe)
    - lsFusion 2.4 Server & Client
        - [x32](https://download.lsfusion.org/exe/lsfusion-2.4.exe)
        - [x64](https://download.lsfusion.org/exe/lsfusion-2.4-x64.exe)

  </details>

В дальнейшем `$INSTALL_DIR$` - папка, выбранная при установке lsFusion (по умолчанию `Program Files/lsFusion <версия>`). Также предполагается, что все параметры (порты, имя веб-контекста) оставлены равными по умолчанию.

<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
<TabItem value="linux">

Bash скрипты с использованием dnf/apt (в качестве минорных версий используются последние стабильные):

lsFusion **6** Server & Client (+ OpenJDK (**default-jdk / java-openjdk**), PostgreSQL **17**, Tomcat **9.0.104**):

| ОС                               | Команда / Скрипт                                                        |
|----------------------------------|-------------------------------------------------------------------------|
| RHEL 8+ / CentOS 8+ / Fedora 35+ | `source <(curl -s https://download.lsfusion.org/dnf/install-lsfusion6)` |
| Ubuntu 18+ / Debian 9+           | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion6)` |

</TabItem>
</Tabs>

## После установки

### Порты

После того как установка завершится, по умолчанию, на компьютере будут локально установлены и запущены в качестве служб:

- сервер БД (PostgreSQL) на порту `5432`
- сервер приложений (Server) на порту `7652`
- веб-сервер (Client) на порту `8080`

### Установка / обновление приложения

Для того чтобы загрузить разработанную логику на установленный сервер приложений (Server) необходимо: 

Поместить разработанные на языке lsFusion [модули](Modules.md) в виде файлов с расширением lsf в папку находящуюся в [classpath](Launch_parameters.md#appjava) сервера (значение по умолчанию при автоматической установке см. ниже). Кроме того туда необходимо поместить остальные файлы ресурсов (если они есть, например, файлы отчетов, скомпилированные Java файлы, картинки и т.п.). Допускается помещать эти файлы в подпапки classpath'а, а также внутри jar-файлов (zip-архивов с расширением jar). После того как все файлы скопированы, необходимо [перезапустить](#restart) сервер.

:::info
Часто бывает удобно поставлять все файлы проекта внутри одного jar-файла. Для того чтобы сформировать такой файл автоматически, можно использовать [Maven](Development_manual.md#maven) (с профилями assemble и noserver) или средства сборки, встроенные в [IDE](IDE.md#build).
:::

Classpath сервера по умолчанию устанавливается равным `$APP_DIR$;$APP_DIR$/*;server.jar`, то есть папка `$APP_DIR$` и все ее подпапки, все jar-файлы в папке `$APP_DIR$` (но не в подпапках), а также jar-файл самого сервера приложений.

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

`$APP_DIR$` при этом равен `$INSTALL_DIR$/lib` 
</TabItem>
<TabItem value="linux">

`$APP_DIR$` при этом равен `/var/lib/lsfusion` 

Сервер приложений инсталлируется и запускается под автоматически создаваемым непривилегированным пользователем `lsfusion`, соответственно, файлы в этой папке должны быть доступны этому пользователю на чтение. 
</TabItem>
</Tabs>

### Установка / обновление клиентов

Для того чтобы дать доступ пользователям к установленной системе необходимо:

Отправить пользователям ссылку `http://<сетевой адрес веб-сервера (Client)>:8080`. При открытии этой ссылки, пользователь, по умолчанию, будет перенаправлен на страницу логина, где он, в свою очередь, при необходимости, может установить себе десктоп-клиент через Java Web Start, предварительно установив себе Java (JDK) (например, по [этой](https://developers.redhat.com/products/openjdk/download) ссылке с регистрацией или по этой - [без](https://github.com/ojdkbuild/ojdkbuild)). Обновление веб и десктоп-клиентов происходит автоматически вместе с [обновлением веб-сервера](#update) (Client).

:::info
Под Windows также можно воспользоваться [программами установки](https://download.lsfusion.org/exe/) десктоп клиента (файлы `lsfusion-desktop-*` с нужной версией и разрядностью ОС). Однако в отличие от установки при помощи Java Web Start, установленный таким образом десктоп-клиент не будет автоматически обновляться. Соответственно для его ручного обновления необходимо скачать файл новой версии десктоп клиента (`lsfusion-client-6.<новая версия>.jar`) с [центрального сервера](https://download.lsfusion.org/java/) и заместить им файл `$INSTALL_DIR$/client.jar`.
:::

:::warning
Все пути и команды ниже приведены для мажорной версии платформы номер 6 (соответственно для других версий необходимо просто заменить 6 на нужное число, например `lsfusion6-server` → `lsfusion11-server`)

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

Все пути по умолчанию
</TabItem>
<TabItem value="linux">

Пути изменены (в частности при помощи symlink'ов) в соответствии с идеологией Linux
</TabItem>
</Tabs>
:::

### Обновление {#update}

Программы устанавливаемые отдельно (OpenJDK, PostgreSQL) обновляются также отдельно (более подробная об этом процессе в документации к соответствующим программам). 

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

Компоненты платформы также обновляются отдельно друг от друга. Чтобы сделать это, необходимо cкачать файл новой версии компоненты с [центрального сервера](https://download.lsfusion.org/java/) и заместить им следующий файл:

|Компонент|Файлы|
|-|-|
|Сервер приложений (Server)|Файл на центральном сервере: `lsfusion-server-6.<новая версия>.jar`<br/>Замещаемый файл: `$INSTALL_DIR$/Server/server.jar`|
|Веб-сервер (Client)|Файл на центральном сервере: `lsfusion-client-6.<новая версия>.war`<br/>Замещаемый файл: `$INSTALL_DIR$/Client/webapps/ROOT.war`<br/>Для обновления Tomcat, необходимо скачать архив с новой версией Tomcat и разархивировать его в папку `$INSTALL_DIR$/Client` без каталога `webapps` и файла [параметров запуска](#settings)
</TabItem>
<TabItem value="linux">

Компоненты платформы также обновляются отдельно друг от друга. Чтобы сделать это, необходимо выполнить команду:

#### Сервер приложений (Server)

| ОС                               | Команда                       |
|----------------------------------|-------------------------------|
| RHEL 8+ / CentOS 8+ / Fedora 35+ | `dnf update lsfusion6-server` |
| Ubuntu 18+ / Debian 9+           | `apt update lsfusion6-server` |

#### Веб-сервер (Client)

| ОС                               | Команда                       |
|----------------------------------|-------------------------------|
| RHEL 8+ / CentOS 8+ / Fedora 35+ | `dnf update lsfusion6-client` |
| Ubuntu 18+ / Debian 9+           | `apt update lsfusion6-client` |
<!--- comment to prevent multiple error messages in IDEA --->

#### Нестабильные версии
Обновление на конкретную SNAPSHOT-версию платформы : `source <(curl -s https://download.lsfusion.org/apt/update-lsfusion6) <platform version>`.

Например, `source <(curl -s https://download.lsfusion.org/apt/update-lsfusion6) 6.1-SNAPSHOT`.

</TabItem>
</Tabs>

## Выборочная установка

Если какие-то из перечисленных в установке программ (компонент платформы) не надо устанавливать / уже установлены на вашем компьютере:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

Эти программы можно исключить в процессе установки при помощи соответствующего графического интерфейса.
</TabItem>
<TabItem value="linux">

Ниже приведены скрипты для установки отдельных компонент платформы:

Сервер БД - PostgreSQL **17**:

| ОС                            | Команда / Скрипт                                                           |
|-------------------------------|----------------------------------------------------------------------------|
| Ubuntu 18+ / Debian 9+        | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion6-db)` |

Сервер приложений - lsFusion 6 Server (+ OpenJDK (**default-jdk**)):


| ОС                            | Команда / Скрипт                                                               |
|-------------------------------|--------------------------------------------------------------------------------|
| Ubuntu 18+ / Debian 9+        | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion6-server)` |
 
Веб-сервер - lsFusion 6 Client (+ Tomcat 9.0.104): 

| ОС                            | Команда / Скрипт                                                               |
|-------------------------------|--------------------------------------------------------------------------------|
| Ubuntu 18+ / Debian 9+        | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion6-client)` |
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
</Tabs>

При установке компонент платформы на разные компьютеры необходимо дополнительно [донастроить параметры](#settings) их подключения друг к другу:

| Компоненты на разных компьютерах                 | Параметры подключения                                               | Настраиваемый файл                                               |
| ------------------------------------------------ | ------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Сервер БД и сервер приложений (Server)           | [Сервера приложений к серверу БД](Launch_parameters.md)             | [Файл](#settings) lsFusion параметров запуска сервера приложений |
| Сервер приложений (Server) и веб-сервер (Client) | [Веб-сервера к серверу приложений](Launch_parameters.md#connectapp) | [Файл](#settings) lsFusion параметров запуска веб-сервера        |

:::info
При установке под Windows вышеописанные параметры запрашиваются в процессе установки и файлы параметров настраиваются автоматически.
:::

## Ручная донастройка (пути к файлам, имена служб) 

### [Параметры запуска](Launch_parameters.md) {#settings}

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

|Компонент|java|lsfusion|
|-|-|-|
|Сервер приложений (Server)|вкладка Java в графическом интерфейсе `$INSTALL_DIR/Server/bin/lsfusion6_serverw.exe`<br/>[`classpath`](Launch_parameters.md#appjava) - параметр Classpath в той же вкладке|файл `$INSTALL_DIR/Server/conf/settings.properties`|
|Веб-сервер (Client)|вкладка Java в графическом интерфейсе `$INSTALL_DIR/Client/bin/lsfusion6_serverw.exe`|файл `$INSTALL_DIR/Client/conf/catalina/localhost/ROOT.xml`|
|Десктоп-клиент|Java параметры задаются внутри тега `j2se` в jnlp файле.||
</TabItem>

<TabItem value="linux">

|Component|java|lsfusion|
|-|-|-|
|Сервер приложений (Server)|параметр `FUSION_OPTS` в файле `/etc/lsfusion6-server/lsfusion.conf`<br/>[`classpath`](Launch_parameters.md#appjava) - параметр `CLASSPATH` в том же файле|файл `/etc/lsfusion6-server/settings.properties`|
|Веб-сервер (Client)|параметр `CATALINA_OPTS` в файле `/etc/lsfusion6-client/lsfusion.conf`|файл `/etc/lsfusion6-client/catalina/localhost/ROOT.xml`|
|Десктоп-клиент|Java параметры задаются внутри тега `j2se` в jnlp файле.||
</TabItem>
</Tabs>

### Перезапуск {#restart}

Любые изменения, сделанные в параметрах запуска, а также изменение модулей lsFusion требуют перезапуска сервера (при изменении модулей lsFusion только сервера приложений (Server)). Это можно сделать при помощи:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

#### Сервер приложений (Server)
```shell script title="Графический интерфейс"
Панель управления > Администрирование > Службы > lsFusion 6 Server
```

```shell script title="Команда" 
# Остановить сервер
$INSTALL_DIR/Server/bin/lsfusion6_server.exe //SS//lsfusion6_server
 
# Запустить сервер
$INSTALL_DIR/Server/bin/lsfusion6_server.exe //ES//lsfusion6_server
```

#### Веб-сервер (Client)
```shell script title="GUI"
Панель управления > Администрирование > Службы > lsFusion 6 Client
```

```shell script title="Команда"
# Остановить клиент
$INSTALL_DIR/Client/bin/lsfusion6_client.exe //SS//lsfusion6_client
 
# Запустить клиент
$INSTALL_DIR/Client/bin/lsfusion6_client.exe //ES//lsfusion6_client
```
</TabItem>
<TabItem value="linux">

#### Сервер приложений (Server)
```shell script title="Команда" 
# Остановить сервер
systemctl stop lsfusion6-server
 
# Запустить сервер
systemctl start lsfusion6-server
```

#### Веб-сервер (Client)
```shell script title="Команда"
# Остановить клиент
systemctl stop lsfusion6-client
 
# Запустить клиент
systemctl start lsfusion6-client
```

</TabItem>
</Tabs>

### [Логи](Journals_and_logs.md) {#logs}

Логи платформы пишутся в следующие папки:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

| Компонент                   | Folder                                  |
| --------------------------- | --------------------------------------- |
| Сервер приложений (Server)  | `$INSTALL_DIR$/Server/logs`             |
| Веб-сервер (Client)         | `$INSTALL_DIR$/Client/logs`             |
| Десктоп-клиент              | `Users/<имя пользователя>/.fusion/logs` |
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
<TabItem value="linux">


| Компонент                   | Folder                                  |
| --------------------------- |-----------------------------------------|
| Сервер приложений (Server)  | `/var/log/lsfusion6-server`             |
| Веб-сервер (Client)         | `/var/log/lsfusion6-client`             |
| Десктоп-клиент              | `/home/<имя пользователя>/.fusion/logs` |
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
</Tabs>

Основные логи (в том числе процесс остановки и запуска сервера) находятся в:

- Сервер приложений (Server) - `stdout`
- Веб-сервера (Client) - `catalina.out` (так как веб-сервер запускается на базе Tomcat).

### [Локаль](Internationalization.md)

Локаль, используемая платформой, определяется на основе локали установленной в операционной системе. При необходимости ее можно изменить при помощи:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

```shell script title="Графический интерфейс"
Панель управления > Язык и региональные стандарты
```
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
<TabItem value="linux">

```shell script title="Команда"
localectl set-locale LANG=ru_RU.utf8
```
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
</Tabs>

