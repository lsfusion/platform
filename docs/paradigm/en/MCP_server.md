---
slug: "/MCP_server"
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

The metadata document itself is correct once reached; only the strict discovery URL is outside the web application's context. The protected-resource metadata URL emitted by the platform's `WWW-Authenticate` header is already in-context, so that hop works without a rewrite.

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
