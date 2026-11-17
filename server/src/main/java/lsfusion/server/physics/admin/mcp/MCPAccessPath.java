package lsfusion.server.physics.admin.mcp;

import java.util.Locale;

// The path an agent reaches the central lsFusion MCP server (ai.lsfusion.org) by through this server: the application's
// MCP server (app), or the application's page (webapp) - its WebMCP, whose requests come to the same /mcp. The center
// gives each path its own guidance, so every request to it names the path: `via=<via()>` in its query
public enum MCPAccessPath {
    APP, WEBAPP;

    public String via() {
        return name().toLowerCase(Locale.ROOT);
    }
}
