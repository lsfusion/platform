---
title: 'Установка при помощи Docker'
---

:::info
Для работы с Docker-контейнерами необходимо установить [Docker](https://docs.docker.com/get-docker/) и [Docker Compose](https://docs.docker.com/compose/).
:::

### Запуск платформы lsFusion с помощью Docker Compose {#docker-platform}

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

### Создание и запуск Docker-образа вашего проекта

Если ваш проект наследует Maven-модуль платформы lsFusion `logics`, вы можете использовать встроенные инструменты для создания Docker-образа и генерации файла `compose.yaml`.

#### Создание Docker-образа

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

#### Генерация и использование `compose.yaml`

- Автоматическая генерация:
    - При сборке проекта одной из приведённых выше команд с профилем `docker` Maven автоматически создаёт файл `compose.yaml`.
    - Файл генерируется с подставленными версией платформы lsFusion и именем Docker-образа вашего проекта.
    - Файл сохраняется в папке `target` или по пути, указанному в свойстве Maven `docker.compose.outputDirectory`. Также содержимое файла выводится в консоль после сборки.

- Запуск сгенерированного `compose.yaml`:

  Запуск и настройка аналогичны шагам, описанным в разделе [Запуск платформы lsFusion](#docker-platform) с некоторыми особенностями:
  - Имя проекта Docker Compose по умолчанию равно значению тэга `artifactId`. Чтобы генерировалось другое имя проекта, переопределите свойство Maven `docker.compose.projectName`.
  - Если вы не используете профиль `embed-server`, папка `docker-server` при запуске контейнера сервера приложений не создаётся. Данные будут храниться в Docker volume, управляемом Docker Engine.
