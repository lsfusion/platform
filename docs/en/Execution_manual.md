---
title: 'For production'
---


:::info
To install the application server, web server and client, Java version 8 or later must first be installed on the computer.
:::


:::info
For the application server to work, it must have access to the PostgreSQL database management server of at least version 9.6. The PostgreSQL server must accept connections using password authentication by the md5 or trust method. You can configure authorization by editing the `pg_hba.conf` file, as described in the [PostgreSQL](http://www.postgresql.org/docs/9.2/static/auth-pg-hba-conf.html) documentation.
:::

### Installing the application server as a service

-   Download the file `lsfusion-server-<version>.jar` of the required version (for example `lsfusion-server-6.0-beta2.jar`) from [the central server](https://download.lsfusion.org/java/) to some folder (we will call this folder `$FUSION_DIR$`).

-   If the database server is located on another computer, and if authorization is enabled on the database server (for example, for Postgres, using the md5 method and if the password postgres is not empty), set the [parameters for connecting to the database server](Launch_parameters.md#connectdb) (e.g. by creating a startup [settings file](Launch_parameters.md#filesettings) in the `$FUSION_DIR$` folder)

-   Place [modules](Modules.md) developed in the lsFusion language as files with the extension lsf to the `$FUSION_DIR$` folder (or any subfolder). In addition, the rest of the resource files if any must also be placed there (e.g. report files, compiled Java files, pictures, etc.).

<a className="lsdoc-anchor" id="command"/>

-   Create a service in the operating system (for example, using [Apache Commons Daemon](http://commons.apache.org/daemon/)). In this case you must use the `$FUSION_DIR$` folder as the working directory and the following line as the start command: 
    - Linux
        ```shell script title="bash"   
        java -cp ".:lsfusion-server-6.0-beta2.jar" lsfusion.server.logics.BusinessLogicsBootstrap
        ```
      <details>
      <summary>Sample script to start a service on CentOS</summary>

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
      
### Installing the web server (web and desktop client) as a service {#appservice}


:::info
To install the web server, Apache Tomcat version 8 or higher must be installed on the computer.
:::

-   Add `--add-opens=java.base/java.util=ALL-UNNAMED` to the Apache Tomcat startup parameters if Java version higher than 11 is used.    
-   Download the file `lsfusion-client-<version>.war` of the required version from [the central server](https://download.lsfusion.org/java/). For example, `lsfusion-client-6.0-beta2.war`. 
-   If the application server is located on another computer, as well as if [access parameters to the application server](Launch_parameters.md#accessapp) are different from the standard, set [connection parameters to the application server](Launch_parameters.md#connectapp) (for example by creating / editing the Tomcat [settings file](Launch_parameters.md#filewebsettings)) 
-   Deploy the application on Tomcat. The easiest way is to copy Tomcat to the webapps folder. In this case, the file can be renamed first (for example, to `lsfusion.war`), since the file name will correspond to the context path where the application will be available. If Tomcat uses port `8080`, then the web client will be available at: `http://localhost:8080/<filename of the war file>`. For example, `http://localhost:8080/lsfusion`. An empty context name in Tomcat corresponds to the name `ROOT`, that is, if the file name is `ROOT.war`, the web client will be available at `http://localhost:8080/`. You can download the desktop client from the authorization page at `Run Desktop Client` (via Java Web Start).

### Installing only the desktop client (on the client's computer)

-   Download the file `lsfusion-client-<version>.jar` of the required version from [the central server](https://download.lsfusion.org/). For example, `lsfusion-client-6.0-beta2.jar`

-   Create a shortcut on the desktop. In this case, you need to use as the working directory the directory which contains the downloaded client jar-file. Use the following line as the launch command:

    - bash
        ```shell script
        java -jar lsfusion-client-6.0-beta2.jar
        ```

:::info
You can also use the method of installing the desktop client for development. To do this, just download the file `lsfusion-client-<version>.jnlp` of the required version from the central server, and then run it locally on the client. This method is faster and more convenient, but less flexible.
:::


:::info
The latest versions that are currently under development (snapshots) can be downloaded directly from the maven repository [https://repo.lsfusion.org](https://repo.lsfusion.org/). For example, for the server, the full path is as follows: https://repo.lsfusion.org/nexus/service/rest/repository/browse/public/lsfusion/platform/server/ (for server and desktop client you need to download jar files with the `-assembly` postfix)
:::

### Using Docker and Docker Compose with the lsFusion platform

:::info
To work with Docker containers, you need to install [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/).
:::

#### Launching the lsFusion platform using Docker Compose {#docker-platform}

- Download the `compose.yaml` file from [central server](https://download.lsfusion.org/docker/) to a folder of your choice (we'll call it `$FUSION_DIR$`). This file contains the settings for running the three containers:
    - PostgreSQL
    - Application Server
    - Web client

- The `compose.yaml` setting (optional):
    - If you want to change the startup settings (e.g., use a different container version or customize environment variables), edit the `compose.yaml` file according to the [Docker documentation](https://docs.docker.com/get-started/overview/).
    - Available lsFusion container images:
        - [Server](https://hub.docker.com/r/lsfusion/server/tags)
        - [Client](https://hub.docker.com/r/lsfusion/client/tags)

- Starting the containers:
    - Navigate to the `$FUSION_DIR$` folder and run the command:
      ```bash
      docker-compose up
      ```
    - Once the launch is complete, the web client will be available at ``http://localhost:8080/``.

- Working with project files:
    - After the first run, subfolders will be created in the `$FUSION_DIR$` folder:
        - `docker-client-conf` - client configuration.
        - `docker-db` - database data.
        - `docker-server` - server files.
    - In the `docker-server` folder put the lsFusion language modules (`.lsf` files or folders with them), as well as additional resources (reports, Java files, images, CSS, JS, etc.). The server logs and the `settings.properties` file are also in the same folder.

---

### Using lsFusion platform with Docker and Docker Compose

:::info
To work with Docker containers, you need to install [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/).
:::

#### Launching the lsFusion platform using Docker Compose {#docker-platform}

- Download the `compose.yaml` file from [central server](https://download.lsfusion.org/docker/) to a folder of your choice (we'll call it `$FUSION_DIR$`). This file contains the settings for running the three containers:
    - PostgreSQL
    - Application Server
    - Web client

- The `compose.yaml` setting (optional):
    - If you want to change the startup settings (e.g., use a different container version or customize environment variables), edit the `compose.yaml` file according to the [Docker documentation](https://docs.docker.com/get-started/overview/).
    - Available lsFusion container images:
        - [Server](https://hub.docker.com/r/lsfusion/server/tags)
        - [Client](https://hub.docker.com/r/lsfusion/client/tags)

- Starting the containers:

  Navigate to the `$FUSION_DIR$` folder and run the command:
  ```bash
  docker-compose up
  ```
  Once the launch is complete, the web client will be available at ``http://localhost:8080/``.

- Working with project files:
    - After the first run, subfolders will be created in the `$FUSION_DIR$` folder:
        - `docker-client-conf` - client configuration.
        - `docker-db` - database data.
        - `docker-server` - server files.
    - In the `docker-server` folder put the lsFusion language modules (`.lsf` files or folders with them), as well as additional resources (reports, Java files, images, CSS, JS, etc.). The server logs and the `settings.properties` file are also in the same folder.

---

#### Creating and running a Docker image of your project

If your project inherits the Maven module of the lsFusion platform `logics`, you can use the built-in tools to create a Docker image and generate a `compose.yaml` file.

##### Creating a Docker image

- Building the image:
  
  Building a Docker image is tied to Maven phases and is activated by the `docker` profile.
        - In the `install` phase, the image is built and loaded into the local registry.
        - In the `deploy` phase, the image is uploaded to a public registry (e.g. Docker Hub).
  
  To build the image, run the command in the project folder:
      ```bash
      mvn install -P assemble,docker
      ```
  If you want to build an image based on uber-jar with the lsFusion server included, add the `embed-server` profile:
      ```bash
      mvn install -P assemble,embed-server,docker
      ```

- Uploading the image to the public registry:

  To assemble an image and upload it to Docker Hub execute:
  ```bash
  mvn deploy -P assemble,docker
  ```
  Or (for uber-jar with server):
  ```bash
  mvn deploy -P assemble,embed-server,docker
  ```

- Configuring the image name:

  The default image name is: `local/<artifactId>:<version>` (artifactId, version are the values of the corresponding tags in the pom.xml file of the project module). You can override part of the name or the whole name via Maven properties in ``pom.xml``:
  ```xml
  <properties>.
      <docker.image.namespace>foo</docker.image.namespace>
      <docker.image.repository>bar</docker.image.repository>
      <docker.image.tag>1.0</docker.image.tag>
      <!-- or -->
      <docker.image.fullName>foo/bar:1.0</docker.image.fullName>
  </properties>
  ```

##### Generating and using `compose.yaml`

- Automatic generation:
    - When you build a project with one of the above commands with the `docker` profile, Maven automatically generates a `compose.yaml` file.
    - The file is generated with the substituted lsFusion platform version and the name of your project's Docker image.
    - The file is saved in the `target` folder or in the path specified in Maven's `docker.compose.outputDirectory` property. Also, the contents of the file are printed to the console after building.

- Running the generated `compose.yaml`:

  Startup and configuration are similar to the steps described in [Starting the lsFusion platform](#docker-platform) with some specifics:
  - The Docker Compose project name defaults to `artifactId` tag value. To generate a different project name, override the Maven property `docker.compose.projectName`.
  - If you do not use the `embedded-server` profile, the `docker-server` folder is not created when you start the application server container. The data will be stored in a Docker volume managed by the Docker Engine.
