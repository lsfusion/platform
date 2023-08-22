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

-   Скачать файл `lsfusion-server-<version>.jar` нужной версии (например `lsfusion-server-5.1.jar`) с [центрального сервера](https://download.lsfusion.org/java) в некоторую папку (далее будем называть эту папку `$FUSION_DIR$`).

-   Если сервер БД находится на другом компьютере, а также если на сервере БД включена авторизация (например, для Postgres, по методу md5 и пароль postgres не пустой), задать [параметры подключения к серверу БД](Launch_parameters.md#connectdb) (например, создав [файл настроек](Launch_parameters.md#filesettings) запуска в папке `$FUSION_DIR$`)

-   Поместить разработанные на языке lsFusion [модули](Modules.md) в виде файлов с расширением lsf в папку `$FUSION_DIR$` (или в любую подпапку). Кроме того туда необходимо поместить остальные файлы ресурсов (если они есть, например, файлы отчетов, скомпилированные Java файлы, картинки и т.п.).

<a className="lsdoc-anchor" id="command"/>

-   Создать службу в операционной системе (например, при помощи [Apache Commons Daemon](http://commons.apache.org/daemon/)), при этом в качестве рабочего директория необходимо использовать папку `$FUSION_DIR$`, а в качестве команды запуска - следующую строку:

    - Linux
        ```shell script title="bash"   
        java -cp ".:lsfusion-server-5.1.jar" lsfusion.server.logics.BusinessLogicsBootstrap
        ```
      <details><summary>Пример скрипта для запуска службы в CentOS</summary>
      <br/>
      
            [Unit]
            Description=lsFusion
            After=network.target
            
            [Service]
            Type=forking
            Environment="PID_FILE=/usr/lsfusion/jsvc-lsfusion.pid"
            Environment="JAVA_HOME=/usr/java/latest"
            Environment="LSFUSION_HOME=/usr/lsfusion"
            Environment="LSFUSION_OPTS=-Xms1g -Xmx4g"
            Environment="CLASSPATH=.:lsfusion-server-5.1.jar"
            
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
      
      </details>

    - Windows
        ```shell script title="cmd"
        java -cp ".;lsfusion-server-5.1.jar" lsfusion.server.logics.BusinessLogicsBootstrap
        ```

### Установка веб-сервера (веб и десктоп-клиента) в качестве сервиса {#appservice}


:::info
Для установки веб-сервера на компьютере должен быть установлен Apache Tomcat не ниже 8 версии.
:::

-   Скачать файл `lsfusion-client-<version>.war` нужной версии с [центрального сервера](https://download.lsfusion.org/java). Например, `lsfusion-client-5.1.war`. 
-   Если сервер приложений находится на другом компьютере, а также если [параметры доступа к серверу приложений](Launch_parameters.md#accessapp) отличается от стандартных, задать [параметры подключения к серверу приложений](Launch_parameters.md#connectapp) (например, создав / отредактировав [файл настроек](Launch_parameters.md#filewebsettings) Tomcat) 
-   Развернуть приложение на Tomcat. Наиболее простой способ - скопировать в папку webapps Tomcat. В этом случае файл можно сначала переименовать (например, в `lsfusion.war`), так как имя файла будет соответствовать контекстному пути, по которому будет доступно приложение. Если Tomcat использует порт `8080`, то веб-клиент будет доступен по адресу: `http://localhost:8080/<имя war-файла>`. Например, `http://localhost:8080/lsfusion`. Пустое имя контекста в Tomcat соответствует имени `ROOT`, то есть если имя файла - `ROOT.war`, то веб-клиент будет доступен по ссылке `http://localhost:8080/`. Десктоп-клиента можно скачать на странице авторизации по ссылке `Run Desktop Client` (через Java Web Start).

### Установка только десктоп-клиента (на компьютере клиента)

-   Скачать файл `lsfusion-client-<version>.jar` нужной версии с [центрального сервера](https://download.lsfusion.org/). Например, `lsfusion-client-5.1.jar`

-   Создать ярлык на рабочем столе, при этом в качестве рабочего директория необходимо использовать директорий, в котором находится скачанный jar-файл клиента, а в качестве команды запуска - следующую строку:

    - bash
        ```shell script
        java -jar lsfusion-client-5.1.jar
        ```


:::info
Для установки десктоп-клиента можно также использовать метод установки десктоп-клиента для разработки. Для этого достаточно скачать файл `lsfusion-client-<version>.jnlp` нужной версии с центрального сервера, после чего запустить его локально на клиенте. Такой способ является более быстрым и удобным, но менее гибким.
:::


:::info
Последние версии, которые сейчас находятся в разработке (snapshot), можно скачать непосредственно из maven репозитария [https://repo.lsfusion.org](https://repo.lsfusion.org/). Например для сервера полный путь выглядит следующим образом: <https://repo.lsfusion.org/nexus/service/rest/repository/browse/public/lsfusion/platform/server/> (для сервера и десктоп-клиента нужно скачивать jar файлы с постфиксом `-assembly`)
:::

### Установка и запуск сервера приложений и веб-клиента с помощью Docker-контейнеров

:::info
Для работы с docker-контейнерами необходимо, чтобы в операционной системы был установлен [Docker](https://docs.docker.com/get-docker/) и [Docker-compose](https://docs.docker.com/compose/)
:::

-   Скачать файл `docker-compose.yml` с [центрального сервера](https://download.lsfusion.org/docker/) в некоторую папку (далее будем называть эту папку `$FUSION_DIR$`).
    Этот файл содержит в себе настройки для запуска трех контейнеров:
    - Контейнер postgres
    - Контейнер сервера приложений
    - Контейнер веб-клиента
-   Если есть необходимость внести какие-то изменения в процесс запуска (например в операционной системе уже установлен сервер postgres, либо требуется использовать версии контейнеров отличные от тех, что заданы по умолчанию, либо необходимо использовать какие-то особенные переменные окружения), то необходимо редактировать файл docker-compose.yml под ваши требования согласно документации [Docker](https://docs.docker.com/get-started/overview/)
-   Находясь в папке `$FUSION_DIR$` из консоли выполнить команду `docker-compose up`.
-   По оканчанию загрузки и запуска контейнеров веб-клиент будет доступен из браузера по адресу `http://localhost:8080/`
-   После первого запуска Docker создаст подпапки в папке `$FUSION_DIR$`:
    - `docker-client-conf`
    - `docker-db`
    - `docker-server`
-   В папку `docker-server` необходимо поместить разработанные на языке lsFusion модули в виде файлов с расширением .lsf либо папки содержащие такие файлы. Так же туда необходимо поместить остальные файлы ресурсов (если они есть, например, файлы отчетов, скомпилированные Java файлы, картинки, .css, .js файлы и т.п.). Так же в этой папке находятся серверные логи и файл `settings.properties`.