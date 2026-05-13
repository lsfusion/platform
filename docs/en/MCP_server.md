---
title: 'MCP server setup'
---

The lsFusion platform can act as an MCP server ([Model Context Protocol](https://modelcontextprotocol.io/)) for external AI clients (for example, `claude.ai`). The MCP server runs on top of the standard [HTTP API of the platform](Access_from_an_external_system.md), so no separate service has to be deployed — it is enough to configure two parameters on the application server: `enableAPI` and HTTPS.

### 1. The `enableAPI` parameter {#enableapi}

This parameter controls access to the program interface through which the MCP client talks to the server (`eval/action`, `exec`, etc.). For the full description, see [Working parameters](Working_parameters.md).

| Value | Behavior |
|---|---|
| `0` | API is fully disabled |
| `1` | Only authenticated access is allowed |
| `2` | Anonymous access is allowed — acceptable only on dev / sandbox installations |

The value `2` must not be used for the production application: it opens the API without authorization.

#### Recommended approach: enable per role

The recommended way to configure `enableAPI` in production is to keep it `0` globally and set it to `1` only for the roles that actually need to work through the MCP client (typically `Administrator`, or a dedicated role created specifically for AI access). Then only users assigned to those roles can use the MCP server.

This is done at runtime through the form `Administration > System > Settings > Parameters` — the `enableAPI` parameter with the value `1` is bound to the required role (the per-role settings mechanism is described in [Working parameters](Working_parameters.md)).

#### Alternative ways to set the value

If per-role configuration is not an option for some reason, `enableAPI` can be set in one of the standard ways (in order of increasing priority — see [Working parameters](Working_parameters.md)):

- In the project's `lsfusion.properties` — `settings.enableAPI=1`.
- In `conf/settings.properties` of a specific installation — `settings.enableAPI=1`.
- In the [Java startup parameters](Launch_parameters.md#appjava) of the application server — `-Dsettings.enableAPI=1`.
- Globally in the `Administration > System > Settings > Parameters` form (without binding to a role).

The highest priority is given to the value set in the database (the `Parameters` form), so in case of a mismatch with the value in `*.properties` files, the database value wins.

### 2. HTTPS is mandatory {#https}

:::warning
Connecting the MCP server to `claude.ai` (or to any other external MCP client) is allowed **only over HTTPS**. Publishing the MCP endpoint over `http://` is not permitted.
:::

HTTPS deployment is done according to the scheme described at the end of the [Automatic installation](Execution_auto.md) page. The MCP server URL configured in the MCP client must start with `https://` and resolve to a public DNS address with a valid certificate — self-signed certificates do not work, the client will refuse to establish a connection.

### URL example {#example-url}

The MCP server endpoint corresponds to the `/mcp` path on the web server (Client) of the production application. The URL configured in the MCP client is built as:

```
https://<web server url>/mcp
```

For example, if the application is deployed at `https://erp.example.com`, the URL to register in the MCP client will be:

```
https://erp.example.com/mcp
```
