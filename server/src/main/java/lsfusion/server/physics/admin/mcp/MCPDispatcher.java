package lsfusion.server.physics.admin.mcp;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.NamedFileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.logics.remote.MCPResult;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.server.logics.controller.remote.RemoteLogics;
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
 * <p>Tool surface:
 * <ul>
 *   <li>Classpath browsing — {@code lsfusion_files_list}, {@code lsfusion_files_search},
 *       {@code lsfusion_files_read} (via {@link MCPFileTools}).</li>
 *   <li>Remote AI helpers — {@code lsfusion_retrieve_docs|howtos|community},
 *       {@code lsfusion_validate_syntax}, {@code lsfusion_get_guidance} (proxied via
 *       {@link MCPRemoteClient} to https://ai.lsfusion.org/mcp; mirrors the IDEA plugin's
 *       {@code McpToolset}).</li>
 *   <li>Script execution — {@code lsfusion_eval} (delegates to {@link MCPEvalTool}, which
 *       runs an lsFusion {@code run(...)} action under the caller's auth context).</li>
 * </ul>
 */
public class MCPDispatcher {

    private static final String PROTOCOL_VERSION = "2024-11-05";

    static final String TOOL_FILES_LIST = "lsfusion_files_list";
    static final String TOOL_FILES_SEARCH = "lsfusion_files_search";
    static final String TOOL_FILES_READ = "lsfusion_files_read";

    static final String TOOL_RETRIEVE_DOCS = "lsfusion_retrieve_docs";
    static final String TOOL_RETRIEVE_HOWTOS = "lsfusion_retrieve_howtos";
    static final String TOOL_RETRIEVE_COMMUNITY = "lsfusion_retrieve_community";
    static final String TOOL_VALIDATE_SYNTAX = "lsfusion_validate_syntax";
    static final String TOOL_GET_GUIDANCE = "lsfusion_get_guidance";

    static final String TOOL_EVAL = "lsfusion_eval";

    private static final Set<String> REMOTE_TOOLS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            TOOL_RETRIEVE_DOCS, TOOL_RETRIEVE_HOWTOS, TOOL_RETRIEVE_COMMUNITY,
            TOOL_VALIDATE_SYNTAX, TOOL_GET_GUIDANCE)));

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
                    remoteLogics.checkMCPAccess(token);
                    return rpcResult(jsonrpc, id, structuredResult(MCPFileTools.list(args)));
                case TOOL_FILES_SEARCH:
                    remoteLogics.checkMCPAccess(token);
                    return rpcResult(jsonrpc, id, structuredResult(MCPFileTools.search(args)));
                case TOOL_FILES_READ: {
                    remoteLogics.checkMCPAccess(token);
                    JSONObject payload = MCPFileTools.read(args);
                    // Single-copy invariant: large text content + binary blobs leave the
                    // structured / text views and live exactly once in a resource entry. Only
                    // small text content stays inline in the structured payload.
                    MCPBinaryContent.SlimResult slim = MCPBinaryContent.slimFileRead(payload);
                    JSONObject result = structuredResult(slim.payload);
                    appendAll(result.getJSONArray("content"), slim.resources);
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
        tools.put(validateSyntaxDescriptor());
        tools.put(getGuidanceDescriptor());
        tools.put(evalDescriptor());
        return tools;
    }

    private static JSONObject filesListDescriptor() {
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("pathPattern", strProp("Gitignore-style glob (NOT a regex). Examples: `**/*.lsf`, `lsfusion/**/Order*.lsf`, `**/*.{md,xml}`. Empty / omitted ⇒ everything. Glob grammar reference: `*` `**` `?` `[abc]` `[!abc]` `{a,b}` `\\X`."))
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
                        .put("pathPattern", strProp("Gitignore-style glob (NOT a regex) restricting which files to scan. Same grammar as `" + TOOL_FILES_LIST + ".pathPattern`. Default `" + MCPFileTools.DEFAULT_SEARCH_PATH_GLOB + "`."))
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

    private static JSONObject validateSyntaxDescriptor() {
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("text", strProp("lsFusion DSL source — top-level statements (CLASS, property/action decls, EXTEND blocks, …). No module wrapping needed.")))
                .put("required", new JSONArray().put("text"))
                .put("additionalProperties", false);
        return new JSONObject()
                .put("name", TOOL_VALIDATE_SYNTAX)
                .put("description", "Syntax-check lsFusion statements. Prefer IDE-based checking when available.")
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
        JSONObject input = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("script", strProp("lsFusion DSL containing a top-level `run(<names>) { … }` action — that's what gets invoked. Param names are local to the script. Example: `run(customer, tpl) { PRINT XLSX FROM tpl; MESSAGE customer; }`."))
                        .put("params", new JSONObject()
                                .put("type", "array")
                                .put("items", new JSONObject()
                                        .put("oneOf", new JSONArray()
                                                .put(new JSONObject().put("type", "string"))
                                                .put(fileEntrySchema())))
                                .put("description", "Values bound positionally to the script's `run(...)` interfaces. Each element is a string or a file `{base64|text, fileName?, extension?}`. Mix freely. Whole call body capped at 16 MiB."))
                        .put("returnNames", new JSONObject()
                                .put("type", "array")
                                .put("items", new JSONObject().put("type", "string"))
                                .put("description", "Optional list of lsFusion property names (compound, e.g. `Module.someProp`) to read after `run` finishes; the engine looks each one up via `findPropertyByCompoundName` and surfaces its current value in the response's `results[]`. Arbitrary / non-property labels will fail. Omit to get the action's external return automatically — that's the first non-null `export*` slot (`exportFile` / `exportString` / `exportObject` / …), populated by `EXPORT FROM …` (with or without `TO`), `PRINT … TO`, or direct assignment to those props.")))
                .put("required", new JSONArray().put("script"))
                .put("additionalProperties", false);
        return new JSONObject()
                .put("name", TOOL_EVAL)
                .put("description", "Run an lsFusion `run(<names>) { … }` action under the caller's auth context. Param names are local to the script. The inbound /mcp HTTP request's metadata is exposed to the script through the same lsFusion properties /eval populates (request headers, cookies, body, URL components, etc.). Returns `{status, results:[…]}`. Without `returnNames`, `results[]` carries the action's external return — the first non-null `export*` slot, populated by `EXPORT FROM …` (with or without `TO`), `PRINT … TO`, or direct assignment to those props. `EXPORT FROM …` without an explicit format clause defaults to JSON (`extension:\"json\"`, `mimeType:\"application/json\"`); a missing slot yields `results: []`. Each entry may carry `name` for multi-result responses; file-typed entries additionally carry `fileName` / `extension` / `mimeType` / `size`; the payload field depends on the type:\n" +
                        "  • Small text (< " + largeTextKiB + " KiB) — inline `value` (UTF-8 string), counts against the per-call inline budget.\n" +
                        "  • Large text (≥ " + largeTextKiB + " KiB) — moved to a sibling `resource` content entry; the slot carries a `resourceUri` pointer (the bytes are already in this response's `content[*].resource.text`, not a separately fetchable URL).\n" +
                        "  • Small binary (< " + inlineBinKiB + " KiB) — inline `valueBase64` while the per-call inline budget allows. Falls back to the URL path below if the budget is exhausted.\n" +
                        "  • Large binary OR small binary with budget exhausted — `url` field with a single-use download URL (path-absolute, `…/file/temp/mcp/<24-char-nonce>/<fileName>.<ext>?dumb=0`). The URL is its own credential — the nonce has ~124 bits of entropy and the `/file/temp/mcp/**` chain is configured `security=\"none\"`, so a sandboxed MCP client can GET it without forwarding any auth header. Delete-after-read (5 s lag) plus a 5-min orphan TTL — re-run the eval if you need the bytes again. URL-delivered binaries don't count against the inline budget and aren't subject to inline-cap truncation regardless of size.\n" +
                        "  • Truncated text — `truncated:true` + `omittedReason` (`perFileCap` for a single text value over " + (MCPEvalTool.MAX_INLINE_FILE_BYTES / (1024 * 1024)) + " MiB, `totalCap` for a text value that doesn't fit the remaining " + (MCPEvalTool.MAX_INLINE_TOTAL_BYTES / (1024 * 1024)) + " MiB inline budget shared by the whole call) + `omittedSize` only — the bytes are NOT in the response. Re-run with a chunked / smaller-output script. Truncation applies to text only; binary always has either inline or URL delivery available.\n" +
                        "  • Null slot — `{isNull:true}`.")
                .put("inputSchema", input);
    }

    /**
     * JSON-schema fragment for one file payload — used as an alternative to a plain string
     * inside {@code params}. The {@code oneOf} clause encodes the runtime invariant
     * (exactly one of {@code base64} / {@code text}) so a strict MCP client can reject the
     * shape before the request leaves it; {@code additionalProperties:false} keeps stray
     * fields from being silently dropped server-side.
     */
    private static JSONObject fileEntrySchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("base64", new JSONObject().put("type", "string").put("description", "Binary content, base64-encoded (xlsx/pdf/zip/png/…). Mutually exclusive with `text`."))
                        .put("text", new JSONObject().put("type", "string").put("description", "Text content as UTF-8 (csv/json/xml/…). Mutually exclusive with `base64`."))
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
