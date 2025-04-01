---
title: 'Для промышленной эксплуатации'
---


:::info
Для установки сервера приложений, веб-сервера и клиента на компьютере должна быть предварительно установлена Java версии не ниже 8.
:::


:::info
Для работы сервера приложений должен быть открыт доступ к серверу управления базами данных PostgreSQL версии не ниже 9.6. PostgreSQL сервер должен принимать подключения, используя авторизацию по паролю методом md5 или trust. Настроить авторизацию можно, отредактировав файл `pg_hba.conf`, как это описано в [документации](http://www.postgresql.org/docs/9.2/static/auth-pg-hba-conf.html) PostgreSQL.
:::

### Установка сервера приложений в качестве сервиса

-   Скачать файл `lsfusion-server-<version>.jar` нужной версии (например `lsfusion-server-6.0-beta2.jar`) с [центрального сервера](https://download.lsfusion.org/java/) в некоторую папку (далее будем называть эту папку `$FUSION_DIR$`).

-   Если сервер БД находится на другом компьютере, а также если на сервере БД включена авторизация (например, для Postgres, по методу md5 и пароль postgres не пустой), задать [параметры подключения к серверу БД](Launch_parameters.md#connectdb) (например, создав [файл настроек](Launch_parameters.md#filesettings) запуска в папке `$FUSION_DIR$`)

-   Поместить разработанные на языке lsFusion [модули](Modules.md) в виде файлов с расширением lsf в папку `$FUSION_DIR$` (или в любую подпапку). Кроме того туда необходимо поместить остальные файлы ресурсов (если они есть, например, файлы отчетов, скомпилированные Java файлы, картинки и т.п.).

<a className="lsdoc-anchor" id="command"/>

-   Создать службу в операционной системе (например, при помощи [Apache Commons Daemon](http://commons.apache.org/daemon/)), при этом в качестве рабочего директория необходимо использовать папку `$FUSION_DIR$`, а в качестве команды запуска - следующую строку:

    - Linux
        ```shell script title="bash"   
        java -cp ".:lsfusion-server-6.0-beta2.jar" lsfusion.server.logics.BusinessLogicsBootstrap
        ```
      <details>
      <summary>Пример скрипта для запуска службы в CentOS</summary>

        ```
        [Unit]
        Description=lsFusion
        After=network.target
        
        [Service]
        Type=forking
        Environment="PID_FILE=/usr/lsfusion/jsvc-lsfusion.pid"
        Environment="JAVA_HOME=/usr/java/latest"
        Environment="LSFUSION_HOME=/usr/lsfusion"
        Environment="LSFUSION_OPTS=-Xms1g -Xmx4g"
        Environment="CLASSPATH=.:lsfusion-server-6.0-beta2.jar"
        
        ExecStart=/usr/bin/jsvc \
                -home $JAVA_HOME \
                -jvm server \
                -cwd $LSFUSION_HOME \
                -pidfile $PID_FILE \
                -outfile ${LSFUSION_HOME}/logs/stdout.log \
                -errfile ${LSFUSION_HOME}/logs/stderr.log \
                -cp ${LSFUSION_HOME}/${CLASSPATH} \
                $LSFUSION_OPTS \
                lsfusion.server.logics.BusinessLogicsBootstrap
        
        ExecStop=/usr/bin/jsvc \
                -home $JAVA_HOME \
                -stop \
                -pidfile $PID_FILE \
                lsfusion.server.logics.BusinessLogicsBootstrap
        
        [Install]
        WantedBy=multi-user.target
        ```
            
      </details>

    - Windows
        ```shell script title="cmd"
        java -cp ".;lsfusion-server-6.0-beta2.jar" lsfusion.server.logics.BusinessLogicsBootstrap
        ```

### Установка веб-сервера (веб и десктоп-клиента) в качестве сервиса {#appservice}


:::info
Для установки веб-сервера на компьютере должен быть установлен Apache Tomcat не ниже 8 версии.
:::

-   Добавить `--add-opens=java.base/java.util=ALL-UNNAMED` в параметры запуска Apache Tomcat, если используется версия Java выше 11.
-   Скачать файл `lsfusion-client-<version>.war` нужной версии с [центрального сервера](https://download.lsfusion.org/java/). Например, `lsfusion-client-6.0-beta2.war`. 
-   Если сервер приложений находится на другом компьютере, а также если [параметры доступа к серверу приложений](Launch_parameters.md#accessapp) отличается от стандартных, задать [параметры подключения к серверу приложений](Launch_parameters.md#connectapp) (например, создав / отредактировав [файл настроек](Launch_parameters.md#filewebsettings) Tomcat) 
-   Развернуть приложение на Tomcat. Наиболее простой способ - скопировать в папку webapps Tomcat. В этом случае файл можно сначала переименовать (например, в `lsfusion.war`), так как имя файла будет соответствовать контекстному пути, по которому будет доступно приложение. Если Tomcat использует порт `8080`, то веб-клиент будет доступен по адресу: `http://localhost:8080/<имя war-файла>`. Например, `http://localhost:8080/lsfusion`. Пустое имя контекста в Tomcat соответствует имени `ROOT`, то есть если имя файла - `ROOT.war`, то веб-клиент будет доступен по ссылке `http://localhost:8080/`. Десктоп-клиента можно скачать на странице авторизации по ссылке `Run Desktop Client` (через Java Web Start).

### Установка только десктоп-клиента (на компьютере клиента)

-   Скачать файл `lsfusion-client-<version>.jar` нужной версии с [центрального сервера](https://download.lsfusion.org/). Например, `lsfusion-client-6.0-beta2.jar`

-   Создать ярлык на рабочем столе, при этом в качестве рабочего директория необходимо использовать директорий, в котором находится скачанный jar-файл клиента, а в качестве команды запуска - следующую строку:

    - bash
        ```shell script
        java -jar lsfusion-client-6.0-beta2.jar
        ```


:::info
Для установки десктоп-клиента можно также использовать метод установки десктоп-клиента для разработки. Для этого достаточно скачать файл `lsfusion-client-<version>.jnlp` нужной версии с центрального сервера, после чего запустить его локально на клиенте. Такой способ является более быстрым и удобным, но менее гибким.
:::


:::info
Последние версии, которые сейчас находятся в разработке (snapshot), можно скачать непосредственно из maven репозитария [https://repo.lsfusion.org](https://repo.lsfusion.org/). Например для сервера полный путь выглядит следующим образом: https://repo.lsfusion.org/nexus/service/rest/repository/browse/public/lsfusion/platform/server/ (для сервера и десктоп-клиента нужно скачивать jar файлы с постфиксом `-assembly`)
:::

### Использование платформы lsFusion с Docker и Docker Compose

:::info
Для работы с Docker-контейнерами необходимо установить [Docker](https://docs.docker.com/get-docker/) и [Docker Compose](https://docs.docker.com/compose/).
:::

#### Запуск платформы lsFusion с помощью Docker Compose {#docker-platform}

- Скачайте файл `compose.yaml` с [центрального сервера](https://download.lsfusion.org/docker/) в выбранную папку (будем называть её `$FUSION_DIR$`). Этот файл содержит настройки для запуска трёх контейнеров:
    - PostgreSQL
    - Сервер приложений
    - Веб-клиент

- Настройка `compose.yaml` (опционально):
    - Если требуется изменить параметры запуска (например, использовать другую версию контейнера или настроить переменные окружения), отредактируйте файл `compose.yaml` в соответствии с [документацией Docker](https://docs.docker.com/get-started/overview/).
    - Параметры запуска сервера приложений также можно задавать при помощи переменных среды контейнера - в атрибуте environment. К примеру, чтобы изменить локаль сервера на русскую, напишите:
      ```yml
      environment:
        - USER_SETLANGUAGE=ru
        - USER_SETCOUNTRY=RU
      ```    
      При поиске параметров запуска в переменных среды Spring автоматически преобразует их к верхнему регистру и заменяет точки на символы подчёркивания. В примере выше значения переменных среды подставятся в соответствующие параметры: `user.setLanguage` и `user.setCountry`. 
    - Доступные образы контейнеров lsFusion:
        - [Сервер](https://hub.docker.com/r/lsfusion/server/tags)
        - [Клиент](https://hub.docker.com/r/lsfusion/client/tags)

- Запуск контейнеров:
    
    Перейдите в папку `$FUSION_DIR$` и выполните команду:
      ```bash
      docker-compose up
      ```
    После завершения запуска веб-клиент будет доступен по адресу: `http://localhost:8080/`.

- Работа с файлами проекта:
    - После первого запуска в папке `$FUSION_DIR$` будут созданы подпапки:
        - `docker-client-conf` — конфигурация клиента.
        - `docker-db` — данные базы данных.
        - `docker-server` — файлы сервера.
    - В папку `docker-server` поместите модули на языке lsFusion (файлы `.lsf` или папки с ними), а также дополнительные ресурсы (отчёты, Java-файлы, изображения, CSS, JS и т.д.). В этой же папке находятся серверные логи и файл `settings.properties`.

---

#### Создание и запуск Docker-образа вашего проекта

Если ваш проект наследует Maven-модуль платформы lsFusion `logics`, вы можете использовать встроенные инструменты для создания Docker-образа и генерации файла `compose.yaml`.

##### Создание Docker-образа

- Сборка образа:
  
  Сборка Docker-образа привязана к фазам Maven и активируется профилем `docker`.
        - На фазе `install` образ собирается и загружается в локальный реестр.
        - На фазе `deploy` образ загружается в публичный реестр (например, Docker Hub).
  
  Для сборки образа выполните в папке проекта команду:
      ```bash
      mvn install -P assemble,docker
      ```
  Если вы хотите собрать образ на основе uber-jar с включенным в него сервером lsFusion, добавьте профиль `embed-server`:
      ```bash
      mvn install -P assemble,embed-server,docker
      ```

- Загрузка образа в публичный реестр:
  
  Чтобы собрать образ и загрузить его в Docker Hub, выполните:
      ```bash
      mvn deploy -P assemble,docker
      ```
  или (для uber-jar с сервером):
      ```bash
      mvn deploy -P assemble,embed-server,docker
      ```

- Настройка имени образа:

  По умолчанию имя образа имеет вид: `local/<artifactId>:<version>` (artifactId, version - значения соответствующих тэгов в файле pom.xml модуля проекта). Вы можете переопределить часть имени или имя целиком через свойства Maven в `pom.xml`:
  ```xml
  <properties>
      <docker.image.namespace>foo</docker.image.namespace>
      <docker.image.repository>bar</docker.image.repository>
      <docker.image.tag>1.0</docker.image.tag>
      <!-- или -->
      <docker.image.fullName>foo/bar:1.0</docker.image.fullName>
  </properties>
  ```

##### Генерация и использование `compose.yaml`

- Автоматическая генерация:
    - При сборке проекта одной из приведённых выше команд с профилем `docker` Maven автоматически создаёт файл `compose.yaml`.
    - Файл генерируется с подставленными версией платформы lsFusion и именем Docker-образа вашего проекта.
    - Файл сохраняется в папке `target` или по пути, указанному в свойстве Maven `docker.compose.outputDirectory`. Также содержимое файла выводится в консоль после сборки.

- Запуск сгенерированного `compose.yaml`:
  
  Запуск и настройка аналогичны шагам, описанным в разделе [Запуск платформы lsFusion](#docker-platform) с некоторыми особенностями:
        - Имя проекта Docker Compose по умолчанию равно значению тэга `artifactId`. Чтобы генерировалось другое имя проекта, переопределите свойство Maven `docker.compose.projectName`.
        - Если вы не используете профиль `embed-server`, папка `docker-server` при запуске контейнера сервера приложений не создаётся. Данные будут храниться в Docker volume, управляемом Docker Engine.
