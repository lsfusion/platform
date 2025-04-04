---
title: 'Docker installation'
---

:::info
To work with Docker containers, you need to install [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/).
:::

### Launching the lsFusion platform using Docker Compose {#docker-platform}

- Download the `compose.yaml` file from [central server](https://download.lsfusion.org/docker/) to a folder of your choice (we'll call it `$FUSION_DIR$`). This file contains the settings for running the three containers:
    - PostgreSQL
    - Application Server
    - Web client

- The `compose.yaml` setting (optional):
    - If you want to change the startup settings (e.g., use a different container version or customize environment variables), edit the `compose.yaml` file according to the [Docker documentation](https://docs.docker.com/get-started/overview/).
    - Application server startup parameters can also be set using the container's environment variables - in the environment attribute. For example, to change the server locale to Russian, write:
      ```yml
      environment:
        - USER_SETLANGUAGE=ru
        - USER_SETCOUNTRY=RU
      ```
      When searching for startup parameters in environment variables, Spring automatically converts them to uppercase and replaces dots with underscores. In the example above, the values of the environment variables will be substituted into the appropriate parameters: `user.setLanguage` and `user.setCountry`.
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

### Creating and running a Docker image of your project

If your project inherits the Maven module of the lsFusion platform `logics`, you can use the built-in tools to create a Docker image and generate a `compose.yaml` file.

#### Creating a Docker image

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

#### Generating and using `compose.yaml`

- Automatic generation:
    - When you build a project with one of the above commands with the `docker` profile, Maven automatically generates a `compose.yaml` file.
    - The file is generated with the substituted lsFusion platform version and the name of your project's Docker image.
    - The file is saved in the `target` folder or in the path specified in Maven's `docker.compose.outputDirectory` property. Also, the contents of the file are printed to the console after building.

- Running the generated `compose.yaml`:

  Startup and configuration are similar to the steps described in [Launching the lsFusion platform](#docker-platform) with some specifics:
    - The Docker Compose project name defaults to `artifactId` tag value. To generate a different project name, override the Maven property `docker.compose.projectName`.
    - If you do not use the `embedded-server` profile, the `docker-server` folder is not created when you start the application server container. The data will be stored in a Docker volume managed by the Docker Engine.