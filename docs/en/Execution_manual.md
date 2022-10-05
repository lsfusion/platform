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

-   Download the file `lsfusion-server-<version>.jar` of the required version (for example `lsfusion-server-5.0.jar`) from [the central server](https://download.lsfusion.org/java) to some folder (we will call this folder `$FUSION_DIR$`).

-   If the database server is located on another computer, and if authorization is enabled on the database server (for example, for Postgres, using the md5 method and if the password postgres is not empty), set the [parameters for connecting to the database server](Launch_parameters.md#connectdb) (e.g. by creating a startup [settings file](Launch_parameters.md#filesettings) in the `$FUSION_DIR$` folder)

-   Place [modules](Modules.md) developed in the lsFusion language as files with the extension lsf to the `$FUSION_DIR$` folder (or any subfolder). In addition, the rest of the resource files if any must also be placed there (e.g. report files, compiled Java files, pictures, etc.).

<a className="lsdoc-anchor" id="command"/>

-   Create a service in the operating system (for example, using [Apache Commons Daemon](http://commons.apache.org/daemon/)). In this case you must use the `$FUSION_DIR$` folder as the working directory and the following line as the start command: 
    - Linux
        ```shell script title="bash"   
        java -cp ".:lsfusion-server-5.0.jar" lsfusion.server.logics.BusinessLogicsBootstrap
        ```
      <details><summary>Sample script to start a service on CentOS</summary>
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
            Environment="CLASSPATH=.:lsfusion-server-5.0.jar"
            
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
        java -cp ".;lsfusion-server-5.0.jar" lsfusion.server.logics.BusinessLogicsBootstrap
        ```
      
### Installing the web server (web and desktop client) as a service {#appservice}


:::info
To install the web server, Apache Tomcat version 8 or higher must be installed on the computer.
:::

-   Download the file `lsfusion-client-<version>.war` of the required version from [the central server](https://download.lsfusion.org/java). For example, `lsfusion-client-5.0.war`. 
-   If the application server is located on another computer, as well as if [access parameters to the application server](Launch_parameters.md#accessapp) are different from the standard, set [connection parameters to the application server](Launch_parameters.md#connectapp) (for example by creating / editing the Tomcat [settings file](Launch_parameters.md#filewebsettings)) 
-   Deploy the application on Tomcat. The easiest way is to copy Tomcat to the webapps folder. In this case, the file can be renamed first (for example, to `lsfusion.war`), since the file name will correspond to the context path where the application will be available. If Tomcat uses port `8080`, then the web client will be available at: `http://localhost:8080/<filename of the war file>`. For example, `http://localhost:8080/lsfusion`. An empty context name in Tomcat corresponds to the name `ROOT`, that is, if the file name is `ROOT.war`, the web client will be available at `http://localhost:8080/`. You can download the desktop client from the authorization page at `Run Desktop Client` (via Java Web Start).

### Installing only the desktop client (on the client's computer)

-   Download the file `lsfusion-client-<version>.jar` of the required version from [the central server](https://download.lsfusion.org/). For example, `lsfusion-client-5.0.jar`

-   Create a shortcut on the desktop. In this case, you need to use as the working directory the directory which contains the downloaded client jar-file. Use the following line as the launch command:

    - bash
        ```shell script
        java -jar lsfusion-client-5.0.jar
        ```

:::info
You can also use the method of installing the desktop client for development. To do this, just download the file `lsfusion-client-<version>.jnlp` of the required version from the central server, and then run it locally on the client. This method is faster and more convenient, but less flexible.
:::


:::info
The latest versions that are currently under development (snapshots) can be downloaded directly from the maven repository [https://repo.lsfusion.org](https://repo.lsfusion.org/). For example, for the server, the full path is as follows: <https://repo.lsfusion.org/nexus/service/rest/repository/browse/public/lsfusion/platform/server/> (for server and desktop client you need to download jar files with the `-assembly` postfix)
:::

  
