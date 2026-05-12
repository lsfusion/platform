package lsfusion.server.physics.admin.mcp;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.NamedFileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.logics.remote.MCPResult;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.server.logics.controller.remote.RemoteLogics;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Server-side MCP (Model Context Protocol) dispatcher.
 *
 * <p>Speaks JSON-RPC 2.0 — {@code initialize} / {@code tools/list} / {@code tools/call} —
 * as a pure string-in / string-out transformation. The web tier owns the HTTP transport
 * (see {@code lsfusion.http.controller.MCPRequestHandler} in web-client) and forwards the
 * raw JSON body through {@code RemoteLogicsInterface.mcp}, so the same dispatch code runs
 * regardless of where the request enters from.
 *
 * <p>Three dispatch strategies, picked per tool by {@code handleToolsCall}:
 * <ul>
 *   <li>Pure protocol / no app state — {@code tools/list}, {@code initialize},
 *       {@code lsfusion_retrieve_*}, {@code lsfusion_get_guidance}. No session, no gate.</li>
 *   <li>Classpath read + gated — {@code lsfusion_files_*} (via {@link MCPFileTools}).
 *       {@code RemoteLogics.access} runs the per-role {@code enableAPI} check, then the
 *       classpath scan runs without a session.</li>
 *   <li>Script execution — {@code lsfusion_eval} via {@link MCPEvalTool} → {@code RemoteLogics.eval}.
 *       Reuses the API-eval pipeline incl. {@code checkEnableApi} (so {@code @api} applies).</li>
 * </ul>
 *
 * <p>Authentication itself (bogus-bearer rejection, anonymous-vs-{@code enableAPI=2}
 * decision, JWT verification) happens earlier at {@code OAuthDispatcher.validateToken}
 * before any request reaches this dispatcher. {@code token} here is either a JWT-verified
 * user-token or — when {@code enableAPI=2} explicitly allows anonymous on the API surface —
 * an anonymous token; the action-level {@code checkAPIAccess} calls handle the latter case
 * by reading the resolved {@code enableAPI} (which may be a per-user / role-based override).
 */
public class MCPDispatcher {

    private static final String PROTOCOL_VERSION = "2024-11-05";

    static final String TOOL_FILES_LIST = "lsfusion_files_list";
    static final String TOOL_FILES_SEARCH = "lsfusion_files_search";
    static final String TOOL_FILES_READ = "lsfusion_files_read";

    static final String TOOL_RETRIEVE_DOCS = "lsfusion_retrieve_docs";
    static final String TOOL_RETRIEVE_HOWTOS = "lsfusion_retrieve_howtos";
    static final String TOOL_RETRIEVE_COMMUNITY = "lsfusion_retrieve_community";
    static final String TOOL_GET_GUIDANCE = "lsfusion_get_guidance";

    static final String TOOL_EVAL = "lsfusion_eval";

    private static final Set<String> REMOTE_TOOLS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            TOOL_RETRIEVE_DOCS, TOOL_RETRIEVE_HOWTOS, TOOL_RETRIEVE_COMMUNITY,
            TOOL_GET_GUIDANCE)));

    private final RemoteLogics remoteLogics;

    public MCPDispatcher(RemoteLogics remoteLogics) {
        this.remoteLogics = remoteLogics;
    }

    /**
     * Handle one JSON-RPC frame.
     *
     * <p>An empty / blank body is treated as a {@code tools/list} request — convenient for a
     * quick {@code curl -X POST .../mcp} discovery probe. JSON-RPC <em>notifications</em>
     * (requests without an {@code id} member) return {@code MCPResult} with {@code json=null}
     * per spec; the web tier acknowledges with HTTP 202.
     *
     * <p>Returned {@link MCPResult} pairs the JSON-RPC envelope with a side-map of binary
     * payloads referenced as placeholders inside the JSON. The web tier resolves those
     * placeholders to download URLs (via {@code FileUtils.saveMCPFile}) before writing the
     * HTTP response. The placeholder pattern carries {@code placeholderId} — a per-call
     * random salt — so user-returned strings can't accidentally match.
     */
    public MCPResult dispatch(AuthenticationToken token, ConnectionInfo connectionInfo, ExternalRequest request, String body) {
        String placeholderId = BaseUtils.randomString(20);
        LinkedHashMap<String, NamedFileData> files = new LinkedHashMap<>();
        JSONObject rpc;
        boolean syntheticRequest = false;
        try {
            String trimmed = body == null ? "" : body.trim();
            if (trimmed.isEmpty()) {
                // Empty body → synthetic `tools/list` for plain-curl discovery; pass an id so it
                // is not classified as a JSON-RPC notification (which would yield no response).
                rpc = new JSONObject().put("jsonrpc", "2.0").put("id", 0).put("method", "tools/list");
                syntheticRequest = true;
            } else {
                rpc = new JSONObject(trimmed);
            }
        } catch (Exception e) {
            return new MCPResult(rpcError("2.0", null, -32700, "Parse error: " + e.getMessage(), null).toString(), files);
        }
        boolean isNotification = !syntheticRequest && !rpc.has("id");
        JSONObject response = handleRpc(rpc, token, connectionInfo, request, files, placeholderId);
        // Notifications discard the response body per JSON-RPC spec — also drop the side-map
        // so we don't pay RMI bytes shipping FileData payloads the web tier would only throw
        // away (web-tier short-circuits to 202 and never calls resolveFiles for null json).
        if (isNotification) return new MCPResult(null, new LinkedHashMap<>());
        return new MCPResult(response.toString(), files);
    }

    private JSONObject handleRpc(JSONObject rpc, AuthenticationToken token, ConnectionInfo connectionInfo, ExternalRequest request,
                                 LinkedHashMap<String, NamedFileData> files, String placeholderId) {
        // Strict envelope: same contract we apply to tool args. A non-string `jsonrpc` /
        // `method`, an unsupported jsonrpc version, or a present-but-non-object `params`
        // is a malformed envelope and gets a clean -32600. Coercion (e.g. `method:123` →
        // dispatchable `"123"`, `"jsonrpc":"1.0"` silently echoed back) is exactly what
        // we're avoiding.
        Object id = rpc.has("id") ? rpc.opt("id") : null;
        String jsonrpc;
        String method;
        try {
            String j = MCPArgs.getString(rpc, "jsonrpc");
            if (j == null) {
                jsonrpc = "2.0";
            } else if (!"2.0".equals(j)) {
                return rpcError("2.0", id, -32600,
                        "Unsupported `jsonrpc` version: \"" + j + "\" (expected \"2.0\")", null);
            } else {
                jsonrpc = j;
            }
            method = MCPArgs.getString(rpc, "method");
        } catch (IllegalArgumentException e) {
            return rpcError("2.0", id, -32600, "Invalid request: " + e.getMessage(), null);
        }

        JSONObject params = null;
        if (rpc.has("params")) {
            Object paramsRaw = rpc.opt("params");
            if (paramsRaw != null && !JSONObject.NULL.equals(paramsRaw)) {
                if (!(paramsRaw instanceof JSONObject)) {
                    return rpcError(jsonrpc, id, -32600, "`params` must be a JSON object when present", null);
                }
                params = (JSONObject) paramsRaw;
            }
        }

        if (method == null || method.isEmpty()) {
            return rpcError(jsonrpc, id, -32600, "Missing or empty `method`", null);
        }

        try {
            switch (method) {
                case "initialize":  return handleInitialize(jsonrpc, id);
                case "tools/list":  return rpcResult(jsonrpc, id, new JSONObject().put("tools", buildToolsList()));
                case "tools/call":  return handleToolsCall(jsonrpc, id, params, token, connectionInfo, request, files, placeholderId);
                default:            return rpcError(jsonrpc, id, -32601, "Method not found: " + method, null);
            }
        } catch (Exception e) {
            ServerLoggers.systemLogger.warn("MCP " + method + " failed", e);
            JSONObject data = new JSONObject()
                    .put("exception", e.getClass().getName())
                    .put("message", String.valueOf(e.getMessage()));
            return rpcError(jsonrpc, id, -32000, "Internal MCP error", data);
        }
    }

    private JSONObject handleInitialize(String jsonrpc, Object id) {
        JSONObject result = new JSONObject()
                .put("protocolVersion", PROTOCOL_VERSION)
                .put("serverInfo", new JSONObject()
                        .put("name", "lsfusion-server")
                        .put("version", "1.0.0"))
                .put("capabilities", new JSONObject()
                        .put("tools", new JSONObject().put("listChanged", false)));
        return rpcResult(jsonrpc, id, result);
    }

    private JSONObject handleToolsCall(String jsonrpc, Object id, JSONObject params,
                                       AuthenticationToken token, ConnectionInfo connectionInfo,
                                       ExternalRequest request,
                                       LinkedHashMap<String, NamedFileData> files, String placeholderId) {
        if (params == null) {
            return rpcError(jsonrpc, id, -32602, "Missing params for tools/call", null);
        }
        Object nameRaw = params.opt("name");
        if (!(nameRaw instanceof String) || ((String) nameRaw).isEmpty()) {
            return rpcError(jsonrpc, id, -32602, "Missing or invalid tool `name` (must be a non-empty string)", null);
        }
        String name = (String) nameRaw;
        JSONObject args;
        if (params.has("arguments") && !JSONObject.NULL.equals(params.opt("arguments"))) {
            args = params.optJSONObject("arguments");
            if (args == null) {
                return rpcError(jsonrpc, id, -32602, "`arguments` must be a JSON object", null);
            }
        } else {
            args = new JSONObject();
        }

        try {
            switch (name) {
                case TOOL_FILES_LIST:
                case TOOL_FILES_SEARCH:
                case TOOL_FILES_READ: {
                    // Per-role enableAPI gate; releases the session before the classpath scan
                    // (files_search can run up to 30s — no point holding a pooled session).
                    remoteLogics.access(token, connectionInfo, request);
                    JSONObject payload;
                    switch (name) {
                        case TOOL_FILES_LIST: payload = MCPFileTools.list(args); break;
                        case TOOL_FILES_SEARCH: payload = MCPFileTools.search(args); break;
                        case TOOL_FILES_READ: payload = MCPFileTools.read(args); break;
                        default: throw new IllegalStateException("unexpected file tool: " + name);
                    }
                    JSONObject result;
                    if (TOOL_FILES_READ.equals(name)) {
                        // Single-copy invariant: large text content + binary blobs leave the
                        // structured / text views and live exactly once in a resource entry. Only
                        // small text content stays inline in the structured payload.
                        MCPBinaryContent.SlimResult slim = MCPBinaryContent.slimFileRead(payload);
                        result = structuredResult(slim.payload);
                        appendAll(result.getJSONArray("content"), slim.resources);
                    } else {
                        result = structuredResult(payload);
                    }
                    return rpcResult(jsonrpc, id, result);
                }
                case TOOL_EVAL: {
                    // MCPEvalTool decides binary inline-vs-URL itself (it sees raw bytes BEFORE
                    // base64 inflation, so the inline-cap can be applied to the right number)
                    // and populates the side-map directly. slimEval afterwards only handles
                    // text large/small.
                    //
                    // Use a LOCAL side-map and merge into the dispatcher's `files` only after
                    // the response is fully built — if any step in here throws, the catch
                    // returns errorResult and the local map is GC'd. Otherwise a partially
                    // populated dispatcher-level map would leak placeholder-less FileData
                    // entries, which the web tier would still write to disk during
                    // resolveFiles() despite the JSON-RPC response containing an error result.
                    LinkedHashMap<String, NamedFileData> localFiles = new LinkedHashMap<>();
                    JSONObject payload = MCPEvalTool.eval(args, remoteLogics, token, connectionInfo, request, localFiles, placeholderId);
                    MCPBinaryContent.SlimResult slim = MCPBinaryContent.slimEval(payload);
                    JSONObject result = structuredResult(slim.payload);
                    appendAll(result.getJSONArray("content"), slim.resources);
                    files.putAll(localFiles);
                    return rpcResult(jsonrpc, id, result);
                }
                default:
                    if (REMOTE_TOOLS.contains(name)) {
                        return rpcResult(jsonrpc, id, remoteToolResult(name, args));
                    }
                    return rpcError(jsonrpc, id, -32601, "Unknown tool: " + name, null);
            }
        } catch (IllegalArgumentException e) {
            return rpcResult(jsonrpc, id, errorResult(name, e.getMessage()));
        } catch (RemoteConnection.MessagesException e) {
            // Eval action threw with partial MESSAGE / PRINT MESSAGE output already emitted —
            // surface both the failure cause AND the captured messages. Without this branch
            // the messages from before the throw would be silently dropped, since the generic
            // catch below only carries the exception text.
            ServerLoggers.systemLogger.warn("MCP tool " + name + " failed (with partial messages)", e.getCause());
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            JSONObject err = errorResult(name, cause.getMessage());
            MCPEvalTool.putCappedMessages(err, e.logMessages);
            return rpcResult(jsonrpc, id, err);
        } catch (Exception e) {
            ServerLoggers.systemLogger.warn("MCP tool " + name + " failed", e);
            return rpcResult(jsonrpc, id, errorResult(name, e.getMessage()));
        }
    }

    private JSONObject remoteToolResult(String toolName, JSONObject args) {
        String payload = MCPRemoteClient.callRemoteTool(toolName, args);
        JSONObject result = new JSONObject()
                .put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", payload)))
                .put("isError", false);
        if (!TOOL_GET_GUIDANCE.equals(toolName)) {
            try {
                result.put("structuredContent", new JSONObject(payload));
            } catch (Exception e1) {
                try {
                    result.put("structuredContent", new JSONArray(payload));
                } catch (Exception ignored) {
                    // leave as plain text
                }
            }
        }
        return result;
    }

    private static void appendAll(JSONArray dst, JSONArray src) {
        for (int i = 0; i < src.length(); i++) dst.put(src.get(i));
    }

    private static JSONObject structuredResult(JSONObject payload) {
        return new JSONObject()
                .put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", payload.toString())))
                .put("structuredContent", payload)
                .put("isError", false);
    }

    private static JSONObject errorResult(String toolName, String message) {
        return new JSONObject()
                .put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", toolName + " error: " + message)))
                .put("isError", true);
    }

    private static JSONObject rpcResult(String jsonrpc, Object id, JSONObject result) {
        JSONObject resp = new JSONObject().put("jsonrpc", jsonrpc);
        if (id != null) resp.put("id", id);
        resp.put("result", result);
        return resp;
    }

    private static JSONObject rpcError(String jsonrpc, Object id, int code, String message, JSONObject data) {
        JSONObject error = new JSONObject().put("code", code).put("message", message);
        if (data != null) error.put("data", data);
        JSONObject resp = new JSONObject().put("jsonrpc", jsonrpc);
        if (id != null) resp.put("id", id);
        resp.put("error", error);
        return resp;
    }

    // ── descriptors ──────────────────────────────────────────────────────────

    private static JSONArray buildToolsList() {
        JSONArray tools = new JSONArray();
        tools.put(filesListDescriptor());
        tools.put(filesSearchDescriptor());
        tools.put(filesReadDescriptor());
        tools.put(retrieveDescriptor(TOOL_RETRIEVE_DOCS,
                "Official lsFusion docs and language reference."));
        tools.put(retrieveDescriptor(TOOL_RETRIEVE_HOWTOS,
                "Task examples and how-tos for combined scenarios."));
        tools.put(retrieveDescriptor(TOOL_RETRIEVE_COMMUNITY,
                "Tutorials, articles, and community discussions. Use only when docs/howtos didn't resolve the question."));
        tools.put(getGuidanceDescriptor());
        tools.put(evalDescriptor());
        return tools;
    }

    private static JSONObject filesListDescriptor() {
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("pathPattern", strProp("Gitignore-style glob (NOT a regex). Examples: `**/*.lsf`, `lsfusion/**/Order*.lsf`, `**/*.{md,xml}`. Empty / omitted ⇒ default source-glob `" + MCPFileTools.DEFAULT_SOURCE_PATH_GLOB + "` (lsf / java / properties / xml / sql / md / json / yaml — skips .class files, jar metadata, dep-resources). Pass `**` to list everything in the classpath. Glob grammar reference: `*` `**` `?` `[abc]` `[!abc]` `{a,b}` `\\X`."))
                        .put("limit", intProp("Max paths per call. Default " + MCPFileTools.DEFAULT_LIST_LIMIT + ", max " + MCPFileTools.MAX_LIST_LIMIT + ". Paginate via `offset`."))
                        .put("offset", intProp("Paths to skip; use to fetch next page after `truncated:true`. Default 0.")))
                .put("additionalProperties", false);
        return new JSONObject()
                .put("name", TOOL_FILES_LIST)
                .put("description", "List server-classpath resources matching a glob. Returns `files[]` (full paths with leading `/`) + `truncated` / `timedOut`.")
                .put("inputSchema", input);
    }

    private static JSONObject filesSearchDescriptor() {
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("regex", strProp("`java.util.regex` content matcher (this one IS a regex). Applied per line via `find()`. Examples: `\\bcustomer\\b`, `Customer.*name`, `^WHEN\\s`. Avoid catastrophic-backtracking shapes like `(a+)+b`."))
                        .put("pathPattern", strProp("Gitignore-style glob (NOT a regex) restricting which files to scan. Same grammar and same default as `" + TOOL_FILES_LIST + ".pathPattern` (`" + MCPFileTools.DEFAULT_SOURCE_PATH_GLOB + "`)."))
                        .put("limit", intProp("Max hits. Default " + MCPFileTools.DEFAULT_SEARCH_LIMIT + ", max " + MCPFileTools.MAX_SEARCH_LIMIT + "; sets `truncated:true` when hit."))
                        .put("contextChars", intProp("Excerpt length per hit. Default 120, max 500."))
                        .put("timeoutSeconds", intProp("Wall-clock cap. Default " + MCPFileTools.DEFAULT_SEARCH_TIMEOUT_SECS + "s, max " + MCPFileTools.MAX_SEARCH_TIMEOUT_SECS + "s; sets `timedOut:true`."))
                        .put("maxScannedFiles", intProp("Hard cap on files visited. Default " + MCPFileTools.DEFAULT_SEARCH_MAX_FILES + ", max " + MCPFileTools.MAX_SEARCH_MAX_FILES + "; sets `truncated:true`."))
                        .put("maxFileBytes", intProp("Per-file scan budget in bytes. Default " + MCPFileTools.DEFAULT_SEARCH_MAX_FILE_BYTES + " (" + (MCPFileTools.DEFAULT_SEARCH_MAX_FILE_BYTES / 1024) + " KiB), max " + MCPFileTools.MAX_SEARCH_MAX_FILE_BYTES + ".")))
                .put("required", new JSONArray().put("regex"))
                .put("additionalProperties", false);
        return new JSONObject()
                .put("name", TOOL_FILES_SEARCH)
                .put("description", "Grep server classpath: `regex` matches content per line, `pathPattern` (glob) selects which files to scan. Returns `hits[]` of `{path,line,excerpt}` + `scannedFiles` / `candidates` / `truncated` / `timedOut`.")
                .put("inputSchema", input);
    }

    private static JSONObject filesReadDescriptor() {
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", strProp("Full classpath path with leading `/`, exactly as returned by `" + TOOL_FILES_LIST + "` or `" + TOOL_FILES_SEARCH + "` (e.g. `/lsfusion/main/Sale.lsf`). NOT a glob, NOT a regex — a single literal resource path. Required."))
                        .put("offset", intProp("Byte offset to start reading from (default 0). Use after a previous truncated read to fetch the next chunk: pass `offset = previous.offset + previous.bytesRead`. For text files this works correctly across multi-byte UTF-8 boundaries — chunks are trimmed to end on a complete character, with `utf8BoundaryTrimmed:true` flagged when that happened."))
                        .put("maxBytes", intProp("Max bytes to return in this call. Default " + MCPFileTools.DEFAULT_READ_MAX_BYTES + " (" + (MCPFileTools.DEFAULT_READ_MAX_BYTES / 1024) + " KiB), capped at " + MCPFileTools.MAX_READ_MAX_BYTES + " (" + (MCPFileTools.MAX_READ_MAX_BYTES / (1024 * 1024)) + " MiB). When the underlying resource has more bytes past this window the response sets `truncated:true` and `eof:false`. Edge case: when `maxBytes < 4` and the chunk would otherwise land mid-UTF-8 character on a known-text file, up to 3 additional bytes may be appended so the response surfaces at least one complete character (forward-progress guarantee for tiny chunked reads).")))
                .put("required", new JSONArray().put("path"))
                .put("additionalProperties", false);
        int largeKiB = MCPBinaryContent.LARGE_TEXT_THRESHOLD_BYTES / 1024;
        return new JSONObject()
                .put("name", TOOL_FILES_READ)
                .put("description", "Read a resource from the server's classpath. Output:\n" +
                        "  • Small text (< " + largeKiB + " KiB) — inline `content` (UTF-8).\n" +
                        "  • Larger text / any binary — sibling `resource` content entry (text → `resource.text`, binary → `resource.blob`); the structured payload's `resourceUri` identifies that embedded entry (it is NOT a separately fetchable URL — the bytes are already in the same response).\n" +
                        "Always sets `mimeType` (`application/octet-stream` fallback) + `bytesRead` + `eof` + `truncated`. Partial reads encode their byte range as `?range=<offset>-<end>` in the URI.")
                .put("inputSchema", input);
    }

    private static JSONObject retrieveDescriptor(String name, String description) {
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("query", strProp("Short topical phrase. Semantic match, not exact. Returns `{docs:[{source,text,score}]}` ranked by descending score.")))
                .put("required", new JSONArray().put("query"))
                .put("additionalProperties", false);
        return new JSONObject()
                .put("name", name)
                .put("description", description)
                .put("inputSchema", input);
    }

    private static JSONObject getGuidanceDescriptor() {
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("additionalProperties", false);
        return new JSONObject()
                .put("name", TOOL_GET_GUIDANCE)
                .put("description", "Brief overview and rules for working with lsFusion. Call once at the start of an lsFusion task when the guidance isn't already known; follow the rules it returns.")
                .put("inputSchema", input);
    }

    private static JSONObject evalDescriptor() {
        int largeTextKiB = MCPBinaryContent.LARGE_TEXT_THRESHOLD_BYTES / 1024;
        int inlineBinKiB = MCPBinaryContent.MAX_INLINE_BINARY_BYTES / 1024;
        int textPerFileMiB = MCPEvalTool.MAX_INLINE_FILE_BYTES / (1024 * 1024);
        int textTotalMiB = MCPEvalTool.MAX_INLINE_TOTAL_BYTES / (1024 * 1024);
        int msgPerEntryKiB = MCPEvalTool.MAX_MESSAGE_BYTES / 1024;
        int msgTotalKiB = MCPEvalTool.MAX_MESSAGES_TOTAL_BYTES / 1024;
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("script", strProp("lsFusion DSL containing a top-level `run(...)` action. Example: `run(STRING x) { MESSAGE 'lookup ' + x; RETURN x; }`."))
                        .put("params", new JSONObject()
                                .put("type", "array")
                                .put("items", new JSONObject()
                                        .put("oneOf", new JSONArray()
                                                .put(new JSONObject().put("type", "string"))
                                                .put(fileEntrySchema())))
                                .put("description", "Values bound positionally to the script's `run(...)` interfaces (`params[0]` → first, `params[1]` → second, etc.). Each element is either a plain string or a file `{base64|text, fileName?, extension?}`. Whole tool-call body capped at 16 MiB ⇒ ~12 MiB max raw binary after base64 inflation.")))
                .put("required", new JSONArray().put("script"))
                .put("additionalProperties", false);
        return new JSONObject()
                .put("name", TOOL_EVAL)
                .put("description",
                        "Run an lsFusion `run(...)` action under the caller's auth context. The tool does NOT read arbitrary properties — to surface a property value, do `RETURN expr;` for plain values (the idiomatic path) or `EXPORT FROM Module.someProp;` when you specifically need a serialized export (XLSX/CSV/JSON/...) in the script.\n\n" +
                        "**Output algorithm (script-controlled):**\n" +
                        "  1. If the action declares a `RETURN` value, that result property is read first; `export*` writes are ignored even when the RETURN expression evaluates to null (the null is formatted by the RETURN value's type).\n" +
                        "  2. Otherwise, writes to lsFusion's `export*` slots — `EXPORT FROM ... [TO ...]`, `PRINT ... TO`, or direct (`exportFile() <- x;`). When multiple slots were written, the first non-null wins in this fixed order: files → strings → numbers → date/times → links → others.\n" +
                        "  3. Without `RETURN` and without any `export*` write, `results[]` is empty.\n\n" +
                        "Typically `results[]` has one entry; multi-entry only when the winning property is parameterized (e.g. `RETURN total(customer)` with `customer` left as a free interface). `EXPORT FROM` without an explicit format defaults to JSON. Null file slots are dropped from `results[]` entirely — an absent entry IS the null signal. See `outputSchema` for entry shape.\n\n" +
                        "**HTTP envelope passthrough.** The /mcp request's metadata (headers, cookies, body, URL components) reaches the script via the same lsFusion properties `/eval` populates — `headers[name]`, `cookies[name]`, etc.\n\n" +
                        "**`messages` capture.** MESSAGE / PRINT MESSAGE output is collected server-side and surfaced, when non-empty, as a `messages` array. Plain `MESSAGE` (the common case) appears as the raw text; explicit non-default forms keep a literal type prefix (`\"WARN: ...\"`, `\"ERROR: ...\"`, `\"INFO: ...\"`, `\"LOG: ...\"`, `\"SUCCESS: ...\"`). Capped at " + MCPEvalTool.MAX_MESSAGES_COUNT + " entries / " + msgTotalKiB + " KiB total / " + msgPerEntryKiB + " KiB per entry; overage replaced with a `\"... N more messages omitted\"` tail entry.\n\n" +
                        "**Error path (tool-specific deviation from MCP defaults):** when the action threw, `structuredContent` is absent and any captured `messages` is attached at the result top level (next to `content` / `isError`) instead of inside `structuredContent`."
                )
                .put("inputSchema", input)
                .put("outputSchema", evalOutputSchema(largeTextKiB, inlineBinKiB, textPerFileMiB, textTotalMiB));
    }

    /**
     * JSON-schema for {@code structuredContent} returned on the success path. Each
     * {@code results[]} entry has all common metadata fields at the top level (name, type,
     * fileName, extension, mimeType, size) plus exactly one payload-field group from
     * {@code oneOf} (value / valueBase64 / url / resourceUri / truncated). Null file slots
     * are dropped from the array entirely — an absent entry IS the null signal, and an
     * AI-agent consumer can't act on the distinction between "slot was null" and "slot
     * wasn't there" anyway. Field descriptions explain when each appears, so a client
     * validating the schema or rendering it as docs gets the same picture as the prose
     * description above.
     */
    private static JSONObject evalOutputSchema(int largeTextKiB, int inlineBinKiB, int textPerFileMiB, int textTotalMiB) {
        JSONObject resultProps = new JSONObject()
                .put("name", strProp("Optional identifier — set only on parameterized multi-key results, formatted as comma-joined key tuple. Absent for the typical case of a single value."))
                .put("type", new JSONObject().put("const", "file").put("description", "Present only for file-typed values (FileData). Absent for plain scalars (String / Number / Date / Boolean / etc.)."))
                .put("fileName", strProp("Original filename without directory part. May be absent."))
                .put("extension", strProp("File extension without the dot (e.g. `xlsx`, `pdf`, `json`)."))
                .put("mimeType", strProp("Resolved via the lsFusion MIME catalog or `application/octet-stream` fallback."))
                .put("size", intProp("Raw byte length of the file."))
                .put("value", strProp("UTF-8 text representation. Set for plain scalars (lsFusion String / Text / Number / Date / Boolean / ...) and for text-classified files (.json, .csv, .txt, .lsf, etc.) under " + largeTextKiB + " KiB."))
                .put("valueBase64", strProp("Base64-encoded raw bytes. Set for binary files under " + inlineBinKiB + " KiB that fit the per-call inline budget."))
                .put("url", strProp("Path-absolute single-use download URL (e.g. `.../file/temp/mcp/<24-char-nonce>/<fileName>.<ext>`). Set for binary files ≥ " + inlineBinKiB + " KiB or when the inline budget is exhausted. Fetch with plain GET, no auth header — the nonce IS the credential."))
                .put("resourceUri", strProp("URI of a sibling `resource` content entry on the same tool result. Set when text ≥ " + largeTextKiB + " KiB; the bytes are in `content[*].resource.text` of THIS response, NOT separately fetchable."))
                .put("truncated", new JSONObject().put("const", true).put("description", "Set when text exceeded the per-call inline cap; bytes are NOT in the response."))
                .put("omittedReason", new JSONObject()
                        .put("enum", new JSONArray().put("perFileCap").put("totalCap"))
                        .put("description", "`perFileCap`: a single text value > " + textPerFileMiB + " MiB. `totalCap`: text fits perFileCap but doesn't fit the remaining " + textTotalMiB + " MiB inline budget."))
                .put("omittedSize", intProp("UTF-8 byte length of the omitted text."))
                .put("inlineLimit", intProp("Per-text-value cap in bytes (currently " + textPerFileMiB + " MiB)."))
                .put("totalLimit", intProp("Per-call inline total budget in bytes (currently " + textTotalMiB + " MiB)."));

        JSONArray variants = new JSONArray()
                .put(new JSONObject().put("title", "Inline scalar or small text").put("required", new JSONArray().put("value")))
                .put(new JSONObject().put("title", "Large text → resource pointer").put("required", new JSONArray().put("resourceUri")))
                .put(new JSONObject().put("title", "Inline binary").put("required", new JSONArray().put("type").put("valueBase64")))
                .put(new JSONObject().put("title", "URL-delivered binary").put("required", new JSONArray().put("type").put("url")))
                .put(new JSONObject().put("title", "Truncated text").put("required", new JSONArray().put("truncated").put("omittedReason").put("omittedSize")));

        JSONObject resultItem = new JSONObject()
                .put("type", "object")
                .put("description", "One returned value from the `RETURN`-or-`export*` selection. Exactly one payload-field group is set per entry — see `oneOf` variants.")
                .put("properties", resultProps)
                .put("oneOf", variants);

        return new JSONObject()
                .put("type", "object")
                .put("required", new JSONArray().put("status").put("results"))
                .put("properties", new JSONObject()
                        .put("status", intProp("HTTP-style status code the action set (default 200)."))
                        .put("results", new JSONObject()
                                .put("type", "array")
                                .put("description", "Returned values. Per-entry shape in `items`; population algorithm in the tool description.")
                                .put("items", resultItem))
                        .put("messages", new JSONObject()
                                .put("type", "array")
                                .put("description", "Captured `MESSAGE` / `PRINT MESSAGE` output.")
                                .put("items", new JSONObject().put("type", "string"))));
    }

    /**
     * JSON-schema fragment for one file payload — used as an alternative to a plain string
     * inside {@code params}. The {@code oneOf} clause encodes the runtime invariant
     * (exactly one of {@code base64} / {@code text}) so a strict MCP client can reject the
     * shape before the request leaves it; {@code additionalProperties:false} matches the
     * server-side fail-fast validation in
     * {@link MCPEvalTool#buildFileParam(JSONObject, String)} (unknown fields throw
     * {@link IllegalArgumentException}).
     */
    private static JSONObject fileEntrySchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("base64", new JSONObject().put("type", "string").put("description", "Binary content, base64-encoded (xlsx/pdf/zip/png/…)."))
                        .put("text", new JSONObject().put("type", "string").put("description", "Text content as UTF-8 (csv/json/xml/…)."))
                        .put("fileName", new JSONObject().put("type", "string").put("description", "Original filename, e.g. \"report.xlsx\". Directory part and trailing extension are stripped before binding — script's `param.fileName` sees just the base name (\"report\"). Used to derive `extension` when omitted."))
                        .put("extension", new JSONObject().put("type", "string").put("description", "Extension without the dot (\"xlsx\"). Drives `IMPORT XLSX FROM …` etc. Resolution: explicit → from `fileName` → \"file\".")))
                .put("oneOf", new JSONArray()
                        .put(new JSONObject().put("required", new JSONArray().put("base64")))
                        .put(new JSONObject().put("required", new JSONArray().put("text"))))
                .put("additionalProperties", false);
    }

    private static JSONObject strProp(String description) {
        return new JSONObject().put("type", "string").put("description", description);
    }

    private static JSONObject intProp(String description) {
        return new JSONObject().put("type", "integer").put("description", description);
    }
}
