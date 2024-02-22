---
title: 'For production'
---

## Installation

In addition to installing **lsFusion**, these installers/scripts also install **OpenJDK**, **PostgreSQL**, and **Tomcat**. Tomcat is embedded into the lsFusion Client installation, and OpenJDK and PostgreSQL are installed separately (in particular, in separate folders).

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

Executable exe files:
**lsFusion 5.1** (OpenJDK **11.0.16**, PostgreSQL **15.1**, Tomcat **9.0.67**, IntelliJ IDEA Community Edition **2022.3.1**)

- [x64](https://download.lsfusion.org/exe/lsfusion-5.1-x64.exe)
- <details>
  <summary>Older versions</summary>

    - lsFusion 5.0 Server & Client
        - [x64](https://download.lsfusion.org/exe/lsfusion-5.0-x64.exe)
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

Subsequently, `$INSTALL_DIR$` refers to the folder selected during the installation of lsFusion (by default, `Program Files/lsFusion <version>`). It is also assumed that all parameters (ports, web context name) are left equal to default values.

<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
<TabItem value="linux">

Bash scripts using yum / apt (the latest stable releases are used as minor versions):

lsFusion **5** Server & Client (+ OpenJDK **11**, PostgreSQL **15**, Tomcat **9.0.67**):

| OS                               | Command / Script                                                        |
|----------------------------------|-------------------------------------------------------------------------|
| RHEL 8+ / CentOS 8+ / Fedora 35+ | `source <(curl -s https://download.lsfusion.org/dnf/install-lsfusion5)` |
| RHEL 7 / CentOS 7                | `source <(curl -s https://download.lsfusion.org/yum/install-lsfusion5)` |
| Ubuntu 18+ / Debian 9+           | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion5)` |

</TabItem>
</Tabs>

## After Installation

### Ports

After the installation is completed, the following will by default be locally installed on the computer and launched as services:

- DB server (PostgreSQL) on port `5432`
- application server (Server) on port `7652`
- web server (Client) on port `8080`

### Installing / updating an application

In order to upload the developed logic to the installed application server (Server), you must:

Place [modules](Modules.md) developed in the lsFusion language as files with an lsf extension in a folder located in the server's [classpath](Launch_parameters.md#appjava) (default value for automatic installation, see below). In addition, the rest of the resource files if any must also be placed there (e.g. report files, compiled Java files, images, etc.). These files may be placed in subfolders of the classpath, as well as inside jar files (zip archives with the jar extension). After all the files have been copied, you need to [restart](#restart) the server.

:::info
It is often convenient to place all project files inside a single jar file. To generate such a file automatically, you can use [Maven](Development_manual.md#maven) (with assemble and noserver profiles) or the build tools built into the [IDE](IDE.md#build).
:::

By default, the server's classpath is equal to `$APP_DIR$;$APP_DIR$/*;server.jar`, i.e. the `$APP_DIR$` folder and all its subfolders, all jar files in the `$APP_DIR$` folder (but not its subfolders), and also the jar file of the application server itself.
 
<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

`$APP_DIR$` is equal to `$INSTALL_DIR$/lib` 
</TabItem>
<TabItem value="linux">

`$APP_DIR$` is equal to `/var/lib/lsfusion` 

The application server is installed and started under the automatically created non-privileged user `lsfusion` so files in the folder should be accessible for this user to read. 
</TabItem>
</Tabs>

### Installing / updating clients

To give users access to the installed system, you must:

Send users a link to `http://<web address of the web server (Client)>:8080`. When users open this link, they will be redirected by default to the login page, where, if necessary, they can install the desktop client via Java Web Start (requires Java (JDK) installed, for example, by following [this](https://developers.redhat.com/products/openjdk/download) link with registration or this one [without](https://github.com/ojdkbuild/ojdkbuild)). Web and desktop clients are updated automatically with [updates to the web server](#update) (Client)

:::info
Under Windows, you can also use desktop client [installers](https://download.lsfusion.org/exe/) (`lsfusion-desktop-*` files with the correct OS version and bit width). However, unlike installing with Java Web Start, a desktop client installed in this way will not be automatically updated. Therefore, you will need to update it manually by downloading the file of the new version of the desktop client (`lsfusion-client-5.<new version>.jar`) from [the central server](https://download.lsfusion.org/java) and replacing the `$INSTALL_DIR$/client.jar` file with it.
:::

:::caution
All paths and commands are given below for the major version 5 of the platform (for other versions just replace 5 with the required number, for example `lsfusion5-server` → `lsfusion11-server`)


<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

All paths by default
</TabItem>
<TabItem value="linux">

Paths changed (in particular with symlinks) in accordance with Linux ideology
</TabItem>
</Tabs>
:::

### Updating {#update}

Programs installed separately (OpenJDK, PostgreSQL) are also updated separately (for more details about this process, see the documentation for these programs)

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

Platform components are also updated separately from each other. To do this, you must download the file of the new version of the component from [the central server](https://download.lsfusion.org/java) and replace the following file with it:

|Component|Files|
|-|-|
|Application Server (Server)|File on the central server: `lsfusion-server-5.<new version>.jar`<br/>File to replace: `$INSTALL_DIR$/Server/server.jar`|
|Web server (Client)|File on the central server: `lsfusion-server-5.<new version>.jar`<br/>File to replace: `$INSTALL_DIR$/Client/webapps/ROOT.war`<br/>To update Tomcat, you need to download the archive with the new version of Tomcat and unzip it to the `$INSTALL_DIR$/Client` folder without the `webapps` directory and the [startup parameters](#settings) file|
</TabItem>
<TabItem value="linux">

Platform components are also updated separately from each other. To do this, you must run the commands:

#### Application Server (Server)

| OS                               | Command                       |
|----------------------------------|-------------------------------|
| RHEL 8+ / CentOS 8+ / Fedora 35+ | `dnf update lsfusion5-server` |
| RHEL 7 / CentOS 7                | `yum update lsfusion5-server` |
| Ubuntu 18+ / Debian 9+           | `apt update lsfusion5-server` |

#### Web server (Client)

| OS                               | Command                       |
|----------------------------------|-------------------------------|
| RHEL 8+ / CentOS 8+ / Fedora 35+ | `dnf update lsfusion5-client` |
| RHEL 7 / CentOS 7                | `yum update lsfusion5-client` |
| Ubuntu 18+ / Debian 9+           | `apt update lsfusion5-client` |
<!--- comment to prevent multiple error messages in IDEA --->

#### Unstable versions
Upgrade platform to a SNAPSHOT version : `source <(curl -s https://download.lsfusion.org/apt/update-lsfusion5) <platform version>`.

For example, `source <(curl -s https://download.lsfusion.org/apt/update-lsfusion5) 6.0-SNAPSHOT`.

</TabItem>
</Tabs>

## Custom installation

If any of the programs listed in the installation (platform components) do not need to be installed / are already installed on your computer:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

These programs can be excluded during installation using the corresponding graphical interface.
</TabItem>
<TabItem value="linux">

The following are scripts for installing specific platform components:

Database Server - PostgreSQL **14**:

| OS                            | Command / Script                                                          |
|-------------------------------|---------------------------------------------------------------------------|
| RHEL 7 / CentOS 7 / Fedora 29 | `source <(curl -s https://download.lsfusion.org/yum/install-lsfusion5-db)`|
| Ubuntu 18+ / Debian 9+        | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion5-db)`|

Application Server - lsFusion 5 Server (+ OpenJDK **11**):


| OS                            | Command / Script                                                               |
|-------------------------------|--------------------------------------------------------------------------------|
| RHEL 7 / CentOS 7 / Fedora 29 | `source <(curl -s https://download.lsfusion.org/yum/install-lsfusion5-server)` |
| Ubuntu 18 / Debian 9          | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion5-server)` |
 
Web server - lsFusion 5 Client (+ Tomcat 9.0.67): 

| OS                            | Command / Script                                                               |
| ------------------------------|--------------------------------------------------------------------------------|
| RHEL 7 / CentOS 7 / Fedora 29 | `source <(curl -s https://download.lsfusion.org/yum/install-lsfusion5-client)` |
| Ubuntu 18 / Debian 9          | `source <(curl -s https://download.lsfusion.org/apt/install-lsfusion5-client)` |
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
</Tabs>

When installing platform components on different computers, it is also necessary to [configure the parameters](#settings) to connect them to each other

| Components on different computers                   | Connection parameters                                                      | Configurable file                                                |
| --------------------------------------------------- | -------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| DB server and application server (Server)           | [Application server to DB server](Launch_parameters.md)                    | [File](#settings) lsFusion application server startup parameters |
| Application server (Server) and web server (Client) | [Web server to application server](Launch_parameters.md#connectapp) | [File](#settings) lsFusion web server startup parameters         |

:::info
When installing under Windows, the above parameters are requested during the installation process and the parameter files are configured automatically.
:::

## Manual setup (file paths, service names)
   
### [Startup parameters](Launch_parameters.md) {#settings}

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

|Component|java|lsfusion|
|-|-|-|
|Application server (Server)|Java tab in the graphical interface `$INSTALL_DIR/Server/bin/lsfusion5_serverw.exe`<br/>[`classpath`](Launch_parameters.md#appjava) - the Classpath parameter in the same tab|`$INSTALL_DIR/Server/conf/settings.properties` file|
|Web server (Client)|Java tab in the graphical interface `$INSTALL_DIR/Client/bin/lsfusion5_serverw.exe`|`$INSTALL_DIR/Client/conf/catalina/localhost/ROOT.xml` file|
|Desktop client|Java parameters are set inside the `j2se` tag in the jnlp file.||
</TabItem>

<TabItem value="linux">

|Component|java|lsfusion|
|-|-|-|
|Application server (Server)|The `FUSION_OPTS` parameter in the file `/etc/lsfusion5-server/lsfusion.conf`<br/>[`classpath`](Launch_parameters.md#appjava) - the `CLASSPATH` parameter in the same file|`/etc/lsfusion5-server/settings.properties` file|
|Web server (Client)|The `CATALINA_OPTS` parameter in the file `/etc/lsfusion5-client/lsfusion.conf`|`/etc/lsfusion5-client/catalina/localhost/ROOT.xml` file|
|Desktop client|Java parameters are set inside the `j2se` tag in the jnlp file.||
</TabItem>
</Tabs>

### Restart {#restart}

Any changes made to the startup parameters, as well as changes to lsFusion modules, require a server restart (when changing lsFusion modules only the application server (Server)). This can be done with:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

#### Application server (Server)
```shell script title="GUI"
Control Panel > Admin > Services > lsFusion 5 Server
```

```shell script title="Command line" 
# Stop server
$INSTALL_DIR/Server/bin/lsfusion5_server.exe //SS//lsfusion5_server
 
# Start server
$INSTALL_DIR/Server/bin/lsfusion5_server.exe //ES//lsfusion5_server
```

#### Web server (Client)
```shell script title="GUI"
Control Panel > Admin > Services > lsFusion 5 Client
```

```shell script title="Command line"
# Stop server
$INSTALL_DIR/Client/bin/lsfusion5_client.exe //SS//lsfusion5_client
 
# Start server
$INSTALL_DIR/Client/bin/lsfusion5_client.exe //ES//lsfusion5_client
```
</TabItem>
<TabItem value="linux">

#### Application server (Server)
```shell script title="Command line" 
# Stop server
systemctl stop lsfusion5-server
 
# Start server
systemctl start lsfusion5-server
```

#### Web server (Client)
```shell script title="Command line"
# Stop client
systemctl stop lsfusion5-client
 
# Start client
systemctl start lsfusion5-client
```

</TabItem>
</Tabs>

### [Logs](Journals_and_logs.md) {#logs}

Platform logs are written to the following folders:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

| Component                   | Folder                          |
| --------------------------- | ------------------------------- |
| Application server (Server) | `$INSTALL_DIR$/Server/logs`     |
| Web server (Client)         | `$INSTALL_DIR$/Client/logs`     |
| Desktop client              | `Users/<username>/.fusion/logs` |
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
<TabItem value="linux">


| Component                   | Folder                          |
| --------------------------- |---------------------------------|
| Application server (Server) | `/var/log/lsfusion5-server`     |
| Web server (Client)         | `/var/log/lsfusion5-client`     |
| Desktop client              | `/home/<username>/.fusion/logs` |
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
</Tabs>

The main logs (including the process of stopping and starting the server) are located in:

- Application server (Server) - `stdout`
- Web server (Client) - `catalina.out` (since the web server runs on Tomcat).

### [Locale](Internationalization.md)

The locale used by the platform is determined based on the locale installed in the operating system. If necessary, it can be changed with:

<Tabs groupId="operating-systems" defaultValue="win" values={[{label: 'Windows', value: 'win'}, {label: 'Linux', value: 'linux'}]}>
<TabItem value="win">

```shell script title="GUI"
Control Panel > Language and Regional Standards
```
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
<TabItem value="linux">

```shell script title="Command line"
localectl set-locale LANG=en_US.utf8
```
<!--- comment to prevent multiple error messages in IDEA --->
</TabItem>
</Tabs>
