---
title: 'For development'
---


:::info
To install the application server, IDE, and client, Java version 8 or later must first be installed on the computer.
:::


:::info
For the application server to work, it must have access to the PostgreSQL database management server of at least version 9.6. The PostgreSQL server must accept connections using password authentication by the md5 or trust method. You can configure authorization by editing the `pg_hba.conf` file, as described in the PostgreSQL [documentation](http://www.postgresql.org/docs/9.2/static/auth-pg-hba-conf.html).
:::

### Installing the IDE Plugin


:::info
To install the plug-in, Intellij IDEA version 2022.2 or higher must be installed on the computer.
:::

<iframe width="245px" height="48px" src="https://plugins.jetbrains.com/embeddable/install/7601"></iframe>

### Installing an application server via IDE

-   When [creating a new lsFusion project](IDE.md#newproject) click the `Download` button opposed to the `lsFusion library`: IDEA automatically downloads the JAR file of the latest (non-beta) version of the lsFusion server from the [central server](https://download.lsfusion.org/java/) and installs this file as a dependency of this project (or rather, as its only module: `File > Project Structure > Modules > project name > Dependencies tab`). Also, if necessary, you can download another version of the server (different from the latest) or select a previously downloaded server JAR file on the local disk.  


:::info
Note that IDEA remembers the downloaded/specified application server file in its settings and automatically sets it when creating other lsFusion projects. 
:::

### Install desktop client

-   After the server starts, in the start log one of the last lines will be a line with a link to the JNLP file (for example, https://download.lsfusion.org/java/lsfusion-client-6.0.jnlp), which when run will automatically install the client using Java Web Start technology.

### Install Web Client

- Install [Apache Tomcat](https://tomcat.apache.org/download-90.cgi) version 9.
- Add `--add-opens=java.base/java.util=ALL-UNNAMED` to the Apache Tomcat startup parameters if Java version higher than 11 is used.
- Download [web-client](https://download.lsfusion.org/java/lsfusion-client-6.0.war) of lsFusion platform.
- Place the war file in Apache Tomcat webapps folder, for example, with the name lsfusion.war.
- The web client will be available at http://localhost:8080/lsfusion.

## Installation in existing Java projects

### Installing an application server via IDE {#existingide}

-   Download the `lsfusion-server-<version>.jar` file of the required version (for example, `lsfusion-server-6.0.jar`) from the [central server](https://download.lsfusion.org/java/) to the folder of the required project module (we will call this folder `$FUSION_DIR$`).
-   If the database server is located on another computer, and if authorization is enabled on the database server (for example, for Postgres, using the md5 method and if the postgres password is not empty), set the [database server connection parameters](Launch_parameters.md#connectdb) (e.g., by creating a startup [settings file](Launch_parameters.md#filesettings) in the project folder)
-   Add the downloaded file as a dependency of the required project module (`File > Project Structure > Modules > module name > Dependencies tab > +`) 
-   Create a [startup configuration](IDE.md#configuration) (when creating a new lsFusion project, this and the upper two sections are done automatically). If the platform is loaded as a library, instead of creating a configuration you can use a Spring bean with the `logicsInstance` ID from the `lsfusion.xml` configuration file, and its `start()` and `stop()` methods, responsible for starting and stopping the application server, respectively.


:::info
For an existing maven project, server installation and loading can (and should) be done differently (see below). 
:::

### Installing the application server via Maven (only for Maven projects) {#maven}

-   Register in `pom.xml` or as parent `lsfusion.platform.build:logics`, or as dependency `lsfusion.platform:server` (at present these artifacts are not in the central repository, and so the path to the lsFusion repository must be specified additionally). For example:
    ```xml
    <repositories>
        <repository>
            <id>lsfusion</id>
            <name>lsFusion Public Repository</name>
            <url>https://repo.lsfusion.org</url>
        </repository>
    </repositories>

    <parent>
        <groupId>lsfusion.platform.build</groupId>
        <artifactId>logics</artifactId>
        <version>6.0</version>
    </parent>
    ```
    The first option (with parent) is good in that:

    -   Maven will automatically configure an uber-jar assembly (i.e., a single file containing all the project files). You can start this assembly using maven profile `assemble` - when this profile is activated, a JAR file with an `assembly` postfix is additionally generated in the package phase, containing not only the project files, but also the files of all the project dependencies. The generated jar file will not contain only the application server files. This is useful in cases where the application server is installed separately from the application itself (for example, using [automatic installation](Execution_auto.md)), and it is undesirable to include the application server in the resulting uber-jar. In addition to `assemble`, the platform also supports the maven profile `embed-server`, which, when activated (along with `assemble`), will also include application server files when generating a jar file with the postfix `assembly` (example command: `mvn package -P assemble,embed-server`).


    -   Maven and IDE will automatically determine the source and resources directories (for example, `src/main/lsfusion` is the default)

    -   Maven will automatically configure weaving of the application server aspects. However, this is only rarely necessary - if the project has application RMI servers (that is, objects inheriting `lsfusion.interop.server.RmiServerInterface`, which are accessed remotely via RMI), or various system annotations of the application server are used (for example, `lsfusion.server.base.caches.IdentityLazy` for caching execution results).

    In the second case, all of the above must be manually configured directly by the developer.

    As for other projects not created using the operation for creating a new lsFusion project, for a maven project you must manually create a [settings file](Launch_parameters.md#filesettings) and a [startup configuration](IDE.md#configuration) (or, if the platform needs to be loaded as a library, use a [special Spring bean](#existingide))


:::info
If working with a large project, and [metacodes](Metaprogramming.md) are actively being used in it (such as [ERP](https://github.com/lsfusion-solutions/erp)), it is recommended that in the IDEA startup parameters (`idea.exe.vmoptions` or `idea64.exe.vmoptions`) the `Xmx` be increased to at least 2gb.
:::
