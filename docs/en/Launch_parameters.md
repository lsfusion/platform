---
title: 'Launch parameters'
sidebar_label: Overview
---

## Application server (Server)

### Java {#appjava}

Java application server startup parameters are set in the launch command (for example, for [manual](Execution_manual.md#command) or [automatic](Execution_auto.md#settings) installation):

||Name|Type|Description|Default|
|---|---|---|---|---|
|System (starting with `X`)|[Standard](https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)||Standard Java parameters. It is important above all to pay attention to:<ul><li>`cp` - classpath, the paths in which java looks for class files and other resources (including lsFusion modules). The default is `.` - current folder (different for [automatic installation](Execution_auto.md)).</li><li>`Xmx` - maximum memory size. The default value is determined depending on the configuration of the computer on which the application server is running. For complex logics, it is recommended that you allocate at least 4GB. </li></ul>||
||`-XX:CMSInitiatingOccupancyFraction`|`int`|In general, this is the standard parameter responsible for the threshold after which the CMS garbage collector is turned on. At the same time, the platform uses this parameter to target the memory usage amount using LRU caches (setting more aggressive parameters for cleaning them if this goal is exceeded, and less aggressive in the opposite case). For heavily loaded servers, it is recommended that you set it in the range from `40` to `60`.|`70`|
|Custom (starting with `D`)|`-Dlsfusion.server.lightstart`|`boolean`|"Light" start mode (usually used during development). In this mode, the server does not perform metadata synchronization operations or create [security policy](Security_policy.md) settings forms, etc., and the startup time and the amount of memory consumed at startup are therefore reduced.<br/>In the [IDE](IDE.md) it is set with a checkmark in [lsFusion server configuration](IDE.md#configuration) (enabled by default).|`false`|
||<a className="lsdoc-anchor" id="devmode"/>`-Dlsfusion.server.devmode`|`boolean`|Development mode. In this mode:<ul><li>System tasks are not launched (so as not to interfere with the debugger)</li><li>You can edit [report design](Report_design.md) in [interactive print](In_a_print_view_PRINT.md#interactive) view</li><li>Anonymous access to the API and UI is enabled ([system parameters](Working_parameters.md) enableAPI, enableUI). In addition, anonymous access in this mode is as an admin and not an anonymous user</li><li>Client is automatically reconnected when connection is lost</li><li>The cache for reading reports from resources is turned off</li></ul>In the [IDE](IDE.md), automatically enabled when running [lsFusion server configuration](IDE.md#configuration).|`false`|
||`-Dlsfusion.server.testmode`|`boolean`|Enables some experimental features<br/>Automatically enabled if assertions are enabled (`-ea` option)|`false`|

### lsFusion {#applsfusion}

lsFusion startup parameters for server applications can be set in one of the following ways (in the order of their priorities, lower priority at the bottom):

-   In the resources in the `lsfusion.xml` file in the places where these parameters are used, after: (relevant for platform forks)
-   In `lsfusion.properties` (usually part of a project, which means it acts by default for all installations)
-   In `conf/settings.properties` (for specific installations)
-   In the [Java startup options](#appjava) (starting with `D`, e.g. `-Dlogics.topModule=FFF`)

|Name|Type|Description|Default|
|---|---|---|---|
|<a className="lsdoc-anchor" id="connectdb"/>`db.server`, `db.name`, `db.user`, `db.password`, `db.connectTimeout`|`string`, `string`, `string`, `string`, `int`|Database server connection parameters:<ul><li>`db.server` - the address of the database server (plus, if necessary, the port after a colon, for example `localhost:6532`)</li><li>`db.name` - database name</li><li>`db.user` - username to connect to the database server</li><li>`db.password` - user password to connect to the database server</li><li>`db.connectTimeout` - DBMS connection timeout</li></ul>|`localhost`, `lsfusion`, `postgres`, , `1000`|
|<a className="lsdoc-anchor" id="accessapp"/>`rmi.port`, `rmi.exportName`, `http.port`|`int`, `string`, `int`|Access settings for the application server:<ul><li>`rmi.port` - port for the application server (RMI register / objects exported)</li><li>`rmi.exportName` - name of the application server (the root RMI object exported by it). It makes sense to use it if you need to export several logics on one port</li><li>`http.port` - port for the web server embedded in the application server (used for [access from external systems](Access_from_an_external_system.md))</li></ul>|`7652`, `default`, `7651`|
|<a className="lsdoc-anchor" id="project"/>`logics.includePaths`, `logics.excludePaths`, `logics.topModule`, `logics.orderDependencies`|`string`, `string`, `string`, `string`|Parameters of the [project](Projects.md) (which modules to load and in what order, detailed description here)|`logics.includePaths` equals `*`, others blank|
|<a className="lsdoc-anchor" id="locale"/>`user.country`, `user.language`, `user.timezone`, `user.twoDigitYearStart` (`user.setCountry`, `user.setLanguage`, `user.setTimezone`)|`string`, `string`, `string`, `int`|Standard Java parameters defining [locale](Internationalization.md#locale) parameters (regional settings - language, country, etc., detailed description here)<br/>Due to the peculiarities of Java Spring (namely, locale parameters are considered by Java Spring to be set even if they are not explicitly specified in the start command, that is, settings of these parameters in `.properties` files are ignored), the platform supports "clones" of these parameters that start as set: if they are specified (either in .properties files or in the launch string), they "overload" the native parameters. That is, the priority is OS, `-Duser.*`, `User.set*` in `.properties` files and `-Duser.set*` (none of the above applies to `user.twoDigitYearStart`, since it is not a standard Java parameter)|The first three are determined from the operating system settings, current year minus `80`|
|<a className="lsdoc-anchor" id="namingpolicy"/>`db.namingPolicy`, `db.maxIdLength`|`string`, `int`|Parameters of the [naming policy](Tables.md#name) for tables and fields:`db.namingPolicy` - the name of the java class of the property (full name, with package); in the constructor, it must accept one parameter of type `int` - the maximum size of the name.Builtin policy class names:<ul><li>Complete with signature - `lsfusion.server.physics.dev.id.name.FullDBNamingPolicy`</li><li>Complete without signature - `lsfusion.server.physics.dev.id.name.NamespaceDBNamingPolicy`</li><li>Brief - `lsfusion.server.physics.dev.id.name.ShortDBNamingPolicy`</li></ul>`db.maxIdLength` - maximum size of a table or field name. Passed as the first parameter to the constructor of the java class of the naming policy for tables and fields.|Complete with signature, `63`|
|`db.denyDropModules`, `db.denyDropTables`|`boolean`, `boolean`|Ban on deletion at startup:<ul><li>`db.denyDropModules` - modules</li><li>`db.denyDropTables` - tables</li></ul>|`false`, `false`|
|`logics.initialAdminPassword`|`string`|Default admin password||

### Example conf/settings.properties file ([section 3](#applsfusion)): {#filesettings}

**$FUSION\_DIR$/conf/settings.properties**

    db.server=localhost
    db.name=lsfusion
    db.user=postgres
    db.password=pswrd

    rmi.port=7652


:::info
By default, it is assumed that the startup parameter files `conf/settings.properties` and `lsfusion.properties` are located in the application server's startup folder. However, with [automatic installation](Execution_auto.md) under GNU Linux symlinks for these files (as well as for [log](Journals_and_logs.md#logs) folders)  are automatically created to [other files](Execution_auto.md#settings) whose layout is better aligned with Linux ideology.
:::

## Web server (Client)

### Java {#webjava}

Java web server startup parameters are set in the Tomcat launch command, which, in turn, launches this web server (for example, for [automatic](Execution_auto.md#settings) installation). 

||Name|Type|Description|
|---|---|---|---|
|System (starting with `X`)|[Standard](https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)||Standard Java parameters. It is important above all to pay attention to:<ul><li>`Xmx` - maximum memory size. For complex logics, it is recommended that you allocate at least 2GB. </li></ul>|

### lsFusion {#weblsfusion}

lsFusion startup parameters for the web server can be set in one of the following ways (in the order of their priorities, lower priority at the bottom):

-   In web applications' [context](http://tomcat.apache.org/tomcat-7.0-doc/config/context.html#Defining_a_context) parameters:
    -   in a web application in the file `/WEB-INF/web.xml`, the `context-param` tag (relevant for platform forks)
    -   in a web application in the file `/META-INF/context.xml`, `Context` tag, `Parameter` tag (relevant for platform forks)
    -   in Tomcat, in the file `$CATALINA_BASE/conf/[enginename]/[hostname]/[contextpath].xml`, tag `Context`, tag `Parameter`, where:
        -   `$CATALINA_BASE$` is the folder where Tomcat is installed (for example, with [automatic](Execution_auto.md#settings) installation, this folder is `$INSTALL_DIR/Client`)
        -   `[contextpath]` - contextual path of the web application (for example, with [automatic](Execution_auto.md#settings) installation this name is empty by default, which in Tomcat is equivalent to the name `ROOT`; with [manual](Execution_manual.md#appservice) installation it depends on the name of the war file), 
        -   `[enginename]` and `[hostname]` are the names of the tomcat implementation mechanism and the web server computer (for example, with [automatic](Execution_auto.md#settings) installation these names are `catalina` and `localhost` respectively)
    -   in Tomcat, in the file `$CATALINA_BASE/conf/server.xml`, `Context` tag, `Parameter` tag (not recommended)
-   In URL parameters (e.g. `http://tryonline.lsfusion.org?host=3.3.3.3&port=4444`)

|Name|Type|Description|Default|
|---|---|---|---|
|<a className="lsdoc-anchor" id="connectapp"/>`host`, `port`, `exportName`|`string`, `int`, `string`|Connection settings for the application server. Must match the [access parameters](#accessapp) for the application server.<ul><li>`host` - application server address</li><li>`port` - port of the application server. Must match the parameter `rmi.port`</li><li>`exportName` - name of the application server. Must match the parameter `rmi.exportName`</li></ul>|`localhost`, `7652`, `default`|

### Example Tomcat configuration file ([section 3](#weblsfusion) in context parameters): {#filewebsettings}

**$CATALINA\_BASE/conf/\[enginename\]/\[hostname\]/ROOT.xml**
```xml
<?xml version='1.0' encoding='utf-8'?>
<Context>
    <Parameter name="host" value="localhost" override="false"/>
    <Parameter name="port" value="7652" override="false"/>
</Context>
```

:::info
In addition to the launch parameters, the platform also has [system parameters](Working_parameters.md) which are set a little differently and are relevant mainly for processes of various components of the platform (that is, processes that occur after they are launched).
:::
