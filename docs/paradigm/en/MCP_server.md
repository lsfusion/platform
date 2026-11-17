---
slug: "/MCP_server"
title: 'MCP server setup'
sidebar_label: 'MCP Server'
---

The lsFusion platform can act as an MCP server ([Model Context Protocol](https://modelcontextprotocol.io/)) for external AI clients (for example, `claude.ai`). The MCP server runs on top of the standard [HTTP API of the platform](Access_from_an_external_system.md), so no separate service has to be deployed — it is enough to configure two parameters on the application server: `enableAPI` and HTTPS. The `/mcp` endpoint itself is served by the web server (Client) of the application, not by the HTTP port of the application server — see [URL example](#example-url).

### 1. The `enableAPI` parameter {#enableapi}

This parameter controls access to the program interface through which the MCP client talks to the server (`eval/action`, `exec`, etc.). For the full description, see [Working parameters](Working_parameters.md).

| Value | Behavior |
|---|---|
| `0` | API is fully disabled (**default**) |
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

### Authorization discovery on a non-root deployment {#well-known}

When the API requires authentication (`enableAPI=1`), `/mcp` starts OAuth discovery by returning `401` with a `WWW-Authenticate: Bearer` challenge. The challenge includes `resource_metadata`, pointing to the protected-resource metadata document:

```
https://host/<context>/.well-known/oauth-protected-resource
```

The metadata URLs are built from the request's external base URL (`scheme://host[:port][/contextPath]`, including forwarded proto/host headers), so the JSON contents are correct for a deployment under a context path. For example, under `https://host/lsfusion`, the protected-resource metadata says that the resource is `https://host/lsfusion` and that its authorization server is also `https://host/lsfusion`.

The discovery chain is therefore:

```
POST /lsfusion/mcp
  -> 401 WWW-Authenticate: Bearer resource_metadata="https://host/lsfusion/.well-known/oauth-protected-resource"
GET /lsfusion/.well-known/oauth-protected-resource
  -> authorization_servers: ["https://host/lsfusion"]
GET /.well-known/oauth-authorization-server/lsfusion
  -> authorization-server metadata
```

The non-root deployment problem is the last step. For an issuer with a path, RFC 8414 §3.1 puts the well-known segment at the host root and appends the issuer path after it, so a strict client looks for:

```
https://host/.well-known/oauth-authorization-server/lsfusion
```

A Java web application deployed under `/lsfusion` only owns `/lsfusion/*`, so the platform naturally serves the metadata at the in-context URL instead:

```
https://host/lsfusion/.well-known/oauth-authorization-server
```

The metadata document itself is correct once reached. Only the strict discovery URL is outside the web application's context. The protected-resource metadata URL emitted by the platform's `WWW-Authenticate` header is already in-context, so that hop works without a rewrite.

A strict OAuth client does not fall back from the RFC 8414 host-root URL to the in-context URL on a `404`: it stops discovery, and the user sees a generic "couldn't reach the server" error with no useful server-side log. The `claude.ai` connector behaves this way.

Fix this at the HTTP layer in front of the application (a reverse proxy, ingress, or the servlet container) — not in application code — by rewriting the host-root authorization-server discovery URL back into the application context. For a Tomcat host-level `RewriteValve`:

```
RewriteRule ^/\.well-known/oauth-authorization-server/(.+)$ /$1/.well-known/oauth-authorization-server [L]
RewriteRule ^/\.well-known/openid-configuration/(.+)$        /$1/.well-known/oauth-authorization-server [L]
```

The rules are generic: the captured path is the context path, so they work for `/lsfusion`, `/mycompany`, and other non-root deployments. The same rewrite can be implemented in a reverse proxy or ingress. The second rule aliases OIDC discovery to the same OAuth authorization-server metadata document, because some clients try the OIDC well-known URL during discovery.

:::note
Root-context deployments, where the application is served directly at `https://host` (as in the `https://erp.example.com` example above), do not need these rewrites: the host-root and in-context URLs collapse to the same `/.well-known/oauth-authorization-server`.
:::

### Browser agent: WebMCP {#webmcp}

The AI agent built into the browser gets the same tools — through [WebMCP](https://webmachinelearning.github.io/webmcp/), in a browser that has `document.modelContext`. Nothing has to be turned on for that: as it opens, the page of the web client registers with the browser every tool the MCP server of the application lists, and the agent calls them on behalf of the user logged in to the application — in the user's web session, with no connection of its own and no OAuth. The agent's access to each tool is that of an external MCP client connected on behalf of this user (see [the `enableAPI` parameter](#enableapi)).

A tool is the server's: its description, its parameters and its result are the ones `/mcp` gives, and it runs on the server, as when called through `/mcp`. The page does one thing itself: it runs the script of `lsfusion_eval`, and the action of `lsfusion_exec`, where the user works. Where exactly, the `form` parameter of the tool says:

| `form` | Where the script runs | Change session |
|---|---|---|
| Not set | In the navigator, as an action the user started | A new one per call: the changes are discarded unless the script applies them itself |
| Set | In a form — the one the parameter addresses, and with the empty value `{}` the form the user is in —, as an action the user started | The form's session: the script sees its unapplied changes, and its own changes are shown in the form and applied together with the form's changes |

A form the script shows opens in the user's browser, and a message or a dialog is shown to the user, the call waiting for the answer (a message shown with `NOWAIT` is not waited for). So the script gives the agent a value as the result of the action, not in a message. In a form, an [`APPLY`](../language/APPLY_operator.md) or a [`CANCEL`](../language/CANCEL_operator.md) in the script acts on the form's session as a whole, the user's unsaved changes included.

The `form` parameter addresses one open form by any of its name (`name`), the label it was opened with (`label`) and the window it was opened into (`window`), as the [`ACTIVATE FORM` operator](../language/ACTIVATE_operator.md) does; the call fails when the address names no form, or several, and with `{}` when the user is in no form. The page shows the address in the attributes of the form's element: `lsfusion-form` (the name), `lsfusion-form-label` and `lsfusion-form-window`. In a form, `lsfusion_exec` looks the name of the action up first among what the form exposes to its page (the [`CUSTOMS` block](../language/FORM_statement.md)), and runs such an action whatever `enableAPI` is, as the form's own page does.

Through `/mcp` there is no page of a user: the script runs on the server, in a session of its own, a new one per call, there is nobody to show a message to — the messages are returned in the tool's result —, and a call with `form` is refused. The page does not run a script this way.

The parameters of the script and the result of the tool are the same wherever the script runs, and the result does not say where it ran. A text of 64 KiB and more is returned beside the result, as a resource, through `/mcp`, and in the result itself on the page, where the link of a file too big to be in the result works in the user's web session.

Beside the server's tools the page registers one of its own, `lsfusion_client`, which `/mcp` does not have. It calls a method of the client, where the user works: `method` is the method's name, and `args` gives the method's arguments by their names. Whose method it is, the `form` parameter says, as it says where a script runs: when it is set, a method of a form — the one the parameter addresses, and with `{}` the form the user is in —, and when it is not set, a method of the navigator. The navigator has one method: it activates an element of the navigator by its name, as a click on the element does, and the page shows that name in the `lsfusion-container` attribute of the element. The methods of a form do what the user does in a form, and the form shows the result as it shows the user's own actions: they change the sortings and the user filters of a table, select a filter of a filter group, make a row the current one, select rows and columns of a table, return the text of the selected cells and paste a text into them, change the value of a property in the current row or run its action, return the values the form suggests for a property, the number of a table's rows and the sum of a column, export a table, open and close the nodes of a tree, make the form the current one and close it. The tool's description lists the methods and their arguments. Instead of `method` and `args`, the `batch` parameter gives several calls of a form's methods, `[{method, args}]`: they are made in the form's batch, and their changes are sent to the server in one request, in the order of the calls. A call sees the changes of the previous calls as the page has them before the server's answer: a row is one that the form showed before, and a row that the server finds is not yet the current one. A batch carries only changes, so the methods that return something — the values the form suggests, the number of rows, the sum, the text of the cells — and the methods that export a table, paste a text, make the form the current one or close it are called separately. A call that the page refuses stops the calls after it, and the error says which call it is; the changes of the calls before it are sent. A failure on the server is a failure of the whole request. There is no rollback: what was done before a failure stays done. The page shows what a call names: the name of a property in the `lsfusion-property` attribute of its cells, of its column's header and of its field in a panel, and a row of a table or of a tree in the attributes of the row's element — `lsfusion-group`, the object group of the row, and `lsfusion-row`, the key of the row. A row is one of those the form shows; the rows a custom view draws have the attributes only if the view writes them. A call is made with the client's own requests, as the user's action is, so it does not depend on [the `enableAPI` parameter](#enableapi).
