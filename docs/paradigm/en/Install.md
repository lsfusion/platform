---
slug: "/Install"
title: 'Install'
sidebar_label: Overview
---

### Install automatically

If you want to install the lsFusion platform and all of the additional applications it requires in one click, you can use special installers that will automate the process.

-   [for development](Development_auto.md)  - contains instructions on installing and configuring the development environment.
-   [for production](Execution_auto.md) - contains instructions on installing and configuring the environment needed to put the developed system into production.

### Install manually 

If you prefer more customized configuration, already have IntelliJ IDEA and PostgreSQL installed, want to use other versions of additional applications or use Linux for development, you can use the lsFusion manual installation guide.

-   [for development](Development_manual.md)  - contains instructions on installing and configuring the development environment.
-   [for production](Execution_manual.md) - contains instructions on installing and configuring the environment needed to put the developed system into production.

### Install with Docker

If you prefer to run the platform and its database in containers without installing them on the host, you can use the Docker-based installation. It runs PostgreSQL, the application server, and the web client as separate containers described by a single `compose.yaml` file, and also lets a project that inherits the platform Maven module build its own image and generate its own `compose.yaml`.

-   [Docker installation](Docker.md) - contains instructions on launching the platform with Docker Compose and on building a Docker image of your project.

### Additional setup

-   [MCP server setup](MCP_server.md) — how to enable the MCP endpoint on the production application for external AI clients (e.g., `claude.ai`).
