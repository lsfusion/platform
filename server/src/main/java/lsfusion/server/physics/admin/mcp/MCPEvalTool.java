package lsfusion.server.physics.admin.mcp;

import lsfusion.base.BaseUtils;
import lsfusion.base.MIMETypeUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.ResultExternalResponse;
import lsfusion.server.logics.controller.remote.RemoteLogics;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * MCP eval tool — runs an lsFusion script under the caller's auth context and renders the
 * result as MCP-friendly text. The script must contain a top-level
 * {@code run(<names>) { … }} action declaration; the {@code params} array is bound
 * positionally to that {@code run}'s interfaces.
 *
 * <p>HTTP metadata flow-through: the {@code envelope} {@link ExternalRequest} captured by
 * the web handler from the inbound {@code /mcp} request (headers, cookies, scheme, host,
 * contextPath, sessionId, body, …) is propagated into the request the eval action runs
 * against, so scripts that read e.g. {@code headers[<name>]} or {@code cookies[<name>]}
 * see the same values they'd see from a regular {@code /eval} call. The HTTP envelope
 * inherits verbatim; {@code params} is replaced with the tool args (positional array);
 * {@code returnNames} is intentionally left empty.
 */
public class MCPEvalTool {

    private static final String UTF8 = StandardCharsets.UTF_8.name();

    /**
     * Hard cap on bytes inlined in a <em>single</em> eval text result (UTF-8 byte length of
     * {@code value}). Above this the text is omitted and the slot is marked
     * {@code truncated:true} with {@code omittedReason:"perFileCap"}. Binary results are
     * <em>not</em> subject to this cap — they inline as {@code valueBase64} only when small
     * (under {@link MCPBinaryContent#MAX_INLINE_BINARY_BYTES} AND inline budget allows),
     * otherwise they flow through the {@code /file/temp/mcp/...} URL path regardless of size.
     */
    public static final int MAX_INLINE_FILE_BYTES = 10 * 1024 * 1024;

    /**
     * Hard cap on the <em>total</em> bytes inlined across all eval result slots in one call —
     * counts text {@code value} and small-binary {@code valueBase64} together. Applied as a
     * best-fit budget: a text payload that doesn't fit in the remaining budget gets dropped
     * with {@code truncated:true} + {@code omittedReason:"totalCap"}; a small-binary payload
     * that doesn't fit instead falls through to the URL path (URL bytes don't count against
     * this budget). A {@code totalCap} truncation always means a text result.
     */
    public static final int MAX_INLINE_TOTAL_BYTES = 30 * 1024 * 1024;

    /** Marker put on truncated/omitted result entries — see {@link #MAX_INLINE_FILE_BYTES}. */
    private static final String REASON_PER_FILE = "perFileCap";
    /** Marker put on truncated/omitted result entries — see {@link #MAX_INLINE_TOTAL_BYTES}. */
    private static final String REASON_TOTAL = "totalCap";

    /** Hard cap on a <em>single</em> entry in the {@code messages} array (UTF-8 byte length).
     *  Anything longer gets truncated with a {@code "... [truncated]"} marker so a single
     *  oversize MESSAGE can't blow the JSON-RPC envelope on its own. */
    public static final int MAX_MESSAGE_BYTES = 8 * 1024;
    /** Hard cap on the <em>total</em> bytes consumed by all messages in one call. */
    public static final int MAX_MESSAGES_TOTAL_BYTES = 256 * 1024;
    /** Hard cap on number of message entries — keeps a runaway loop emitting MESSAGE in a
     *  tight FOR from producing a 10 000-entry array. */
    public static final int MAX_MESSAGES_COUNT = 100;

    public static JSONObject eval(JSONObject args, RemoteLogics remoteLogics,
                                  AuthenticationToken token, ConnectionInfo connectionInfo,
                                  ExternalRequest envelope,
                                  LinkedHashMap<String, NamedFileData> files, String placeholderId) throws Exception {
        String script = MCPArgs.getString(args, "script");
        if (script == null || script.isEmpty()) {
            throw new IllegalArgumentException("'script' is required (non-empty string)");
        }
        ExternalRequest.Param scriptParam = ExternalRequest.getUrlParam(script, UTF8, "script");
        ExternalRequest request = buildRequest(args, envelope);

        ExternalResponse response = remoteLogics.eval(token, connectionInfo, /*action=*/ false, scriptParam, request);
        return toJson(response, files, placeholderId);
    }

    /**
     * Build the {@link ExternalRequest} the script's run-action sees. HTTP metadata
     * (headers / cookies / scheme / host / contextPath / sessionId / body / …) inherits
     * verbatim from the {@code envelope} the web handler captured for the inbound /mcp
     * call, so the script can consult those attributes the same way a /eval-driven script
     * would. {@code params} is replaced with the tool's args (positional array).
     * {@code returnNames} is intentionally empty — letting MCP clients pick which
     * properties to read by compound name would be implicit scope creep, so the
     * result-read path falls through to lsFusion's standard `RETURN`-or-`export*`
     * selection (the script controls what comes back).
     */
    private static ExternalRequest buildRequest(JSONObject args, ExternalRequest envelope) {
        List<ExternalRequest.Param> paramList = new ArrayList<>();
        Object params = args.opt("params");
        if (params instanceof JSONArray) {
            JSONArray arr = (JSONArray) params;
            for (int i = 0; i < arr.length(); i++) {
                Object value = arr.opt(i);
                if (value == null || JSONObject.NULL.equals(value)) {
                    // Silently skipping would shift positional order — the third entry of
                    // [a, null, b] would land at $2 instead of $3. Reject explicitly.
                    throw new IllegalArgumentException(
                            "params[" + i + "] is null; positional order would shift. "
                                    + "Pass an explicit value (or restructure the script if the slot is unused).");
                }
                paramList.add(toImplicitParam(value, "params[" + i + "]"));
            }
        } else if (params != null && !JSONObject.NULL.equals(params)) {
            throw new IllegalArgumentException("`params` must be an array; got "
                    + params.getClass().getSimpleName());
        }

        // No `returnNames` here on purpose — the script controls what comes back via
        // `RETURN` (idiomatic) or, when a serialized export is needed, EXPORT / PRINT TO /
        // direct `exportFile() <- ...`. Letting the tool read arbitrary properties by
        // compound name would be implicit scope creep (anything visible in the schema), and
        // a script-side `RETURN expr;` / `EXPORT FROM Module.someProp;` is more explicit.
        // Inherit HTTP metadata from the envelope so the script sees the same shape /eval
        // provides. MESSAGE / PRINT MESSAGE output is captured server-side unconditionally
        // (RemoteConnection always pushes a log processor on the non-interactive path) and
        // surfaced to MCP clients via ResultExternalResponse.logMessages.
        return new ExternalRequest(/*returnNames=*/ new String[0],
                paramList.toArray(new ExternalRequest.Param[0]),
                envelope.headerNames, envelope.headerValues, envelope.cookieNames, envelope.cookieValues,
                envelope.appHost, envelope.appPort, envelope.exportName,
                envelope.scheme, envelope.method, envelope.webHost, envelope.webPort,
                envelope.contextPath, envelope.servletPath, envelope.pathInfo, envelope.query,
                envelope.contentType, envelope.sessionId, envelope.body,
                envelope.signature, envelope.returnMultiType,
                envelope.needNotificationId, envelope.isInteractiveClient);
    }

    /**
     * Turn one user-supplied value (string or file-object) into an implicit
     * {@link ExternalRequest.Param} bound STRICTLY positionally — never by name. We pick
     * {@code url=false} + empty name {@code ""} so:
     * <ul>
     *   <li>The Param classifies as implicit per {@link ExternalRequest.Param#isImplicitParam()}
     *       (because {@code !url}), so the eval engine consumes it positionally to fill the
     *       next free interface slot in the script's {@code run(...)} declaration.</li>
     *   <li>{@code name=""} can never accidentally match a script-side interface name (those
     *       must be valid lsFusion identifiers, never empty), so the strict
     *       "param names live INSIDE the script" contract holds even if the user picks
     *       awkward run-interface names like {@code run(p, x)}.</li>
     * </ul>
     */
    private static ExternalRequest.Param toImplicitParam(Object value, String origin) {
        if (value instanceof String) {
            return new ExternalRequest.Param((String) value, /*url=*/ false, UTF8, /*name=*/ "");
        }
        if (value instanceof JSONObject) {
            return buildFileParam((JSONObject) value, origin);
        }
        throw new IllegalArgumentException(origin + " must be a string or a file object, got "
                + value.getClass().getSimpleName());
    }

    /**
     * Build an implicit body {@link ExternalRequest.Param} from one file-object entry.
     * The body param's url flag is false ⇒ implicit per
     * {@link ExternalRequest.Param#isImplicitParam()}, so it lands positionally in the
     * script's {@code run(...)} interfaces alongside string params from the same array.
     *
     * <p>Validation is fail-fast (mirrors what an MCP client expects from a strict tool):
     * <ul>
     *   <li>Exactly one of {@code base64} / {@code text} must be present.</li>
     *   <li>Malformed base64 surfaces as a clear {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * <p>Extension is resolved as: explicit {@code extension} field → extracted from
     * {@code fileName} (if it has a dot) → fallback {@code "file"}. {@code fileName} is
     * normalized through {@link BaseUtils#getFileName(String)}, so any directory prefix is
     * stripped before reaching the script.
     */
    private static ExternalRequest.Param buildFileParam(JSONObject fp, String origin) {
        for (String key : fp.keySet()) {
            if (!FILE_PARAM_KEYS.contains(key)) {
                throw new IllegalArgumentException(origin + " has unknown field `" + key
                        + "`; allowed: " + FILE_PARAM_KEYS);
            }
        }
        String b64 = MCPArgs.getStringAt(fp, "base64", origin + ".base64");
        String text = MCPArgs.getStringAt(fp, "text", origin + ".text");
        if (b64 != null && text != null) {
            throw new IllegalArgumentException(origin + " has both `base64` and `text`; pick one (mutually exclusive)");
        }
        byte[] bytes;
        if (b64 != null) {
            try {
                bytes = Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(origin + ".base64 is not valid base64: " + e.getMessage());
            }
        } else if (text != null) {
            bytes = text.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException(origin + " needs either `base64` or `text`");
        }

        // Resolve fileName + extension: explicit `extension` wins, else extracted from
        // `fileName` (if it has a dot), else fallback "file"; fileName is normalized to its
        // base name only (no directory part, no extension fragment).
        String rawFileName = nullIfEmpty(MCPArgs.getStringAt(fp, "fileName", origin + ".fileName"));
        String explicitExt = normalizeExtension(MCPArgs.getStringAt(fp, "extension", origin + ".extension"), origin);
        String extension = explicitExt;
        if (extension == null && rawFileName != null) {
            String fromName = BaseUtils.getFileExtension(rawFileName);
            if (!fromName.isEmpty()) extension = fromName;
        }
        if (extension == null) extension = "file";
        String fileName = rawFileName == null ? null : BaseUtils.getFileName(rawFileName);

        // url=false ⇒ implicit; fills the next free run() interface slot in array order.
        // Empty `name` keeps the param strictly positional — it can't accidentally match a
        // script-side interface name (lsFusion identifiers are never empty).
        FileData fileData = new FileData(new RawFileData(bytes), extension);
        return new ExternalRequest.Param(fileData, /*url=*/ false, UTF8, /*name=*/ "", fileName);
    }

    private static final Set<String> FILE_PARAM_KEYS = new LinkedHashSet<>(
            Arrays.asList("base64", "text", "fileName", "extension"));

    private static String nullIfEmpty(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    /**
     * Normalize a user-supplied {@code extension} field — descriptor calls for "without dot",
     * but agents commonly hand in {@code ".xlsx"} / {@code "  PDF  "} / etc. Strips
     * surrounding whitespace and any leading dots, then rejects path separators (a slash in
     * the extension is almost certainly a misuse: descriptor wanted just the suffix). An
     * empty result collapses to {@code null} so the fileName-derived fallback can take
     * over. Case is preserved on purpose — downstream {@code FileData.extension} feeds
     * lsFusion FORMAT lookups that may be case-sensitive in user code.
     */
    private static String normalizeExtension(String raw, String origin) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        int firstNonDot = 0;
        while (firstNonDot < trimmed.length() && trimmed.charAt(firstNonDot) == '.') firstNonDot++;
        trimmed = trimmed.substring(firstNonDot);
        if (trimmed.isEmpty()) return null;
        if (trimmed.indexOf('/') >= 0 || trimmed.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(origin + ".extension must not contain path separators; got `" + raw + "`");
        }
        return trimmed;
    }

    private static JSONObject toJson(ExternalResponse response,
                                     LinkedHashMap<String, NamedFileData> files, String placeholderId) {
        JSONObject out = new JSONObject().put("status", response.getStatusHttp());
        if (response instanceof ResultExternalResponse) {
            ResultExternalResponse r = (ResultExternalResponse) response;
            JSONArray results = new JSONArray();
            // Per-call running total — best-fit budget for INLINE payloads only (text values +
            // small binary `valueBase64`). URL-delivered binaries don't count against this,
            // since their bytes flow to disk via FileUtils.saveMCPFile, not through the
            // JSON-RPC envelope. Keeps multi-slot scripts from pushing hundreds of MiB of
            // base64 into one response while leaving the URL path unbounded by JSON budget.
            int[] inlineRemaining = { MAX_INLINE_TOTAL_BYTES };
            if (r.results != null) {
                for (int i = 0; i < r.results.length; i++) {
                    JSONObject item = renderResult(r.results[i], i, inlineRemaining, files, placeholderId);
                    if (item != null) results.put(item);
                }
            }
            out.put("results", results);
            // Server-collected MESSAGE / PRINT MESSAGE text. Plain MESSAGE entries are raw
            // text; explicit non-default forms get a "TYPE: ..." prefix. Only emitted when
            // the array is non-empty.
            putCappedMessages(out, r.logMessages);
        } else {
            out.put("results", new JSONArray());
        }
        return out;
    }

    private static JSONObject renderResult(ExternalRequest.Result result, int index, int[] inlineRemaining,
                                           LinkedHashMap<String, NamedFileData> files, String placeholderId) {
        JSONObject item = new JSONObject();
        if (result.name != null) item.put("name", result.name);
        if (result.fileName != null) item.put("fileName", result.fileName);
        Object value = result.value;
        if (value instanceof String) {
            inlineString(item, (String) value, inlineRemaining);
        } else if (value instanceof FileData) {
            FileData fd = (FileData) value;
            // The engine surfaces a NULL slot via FileData.NULL (extension="null", empty bytes).
            // Drop null slots — named or not — from the array entirely (we return null and
            // the caller filters). The "named null vs absent" distinction can't be acted on
            // by an AI-agent consumer anyway: for static EXPORT FROM x=a, y=b they already
            // know what they asked for from the script; for parameterized RETURN they have
            // no separate ground-truth list to compare against. Either way an absent entry
            // already means "no value", so the explicit marker would be pure noise.
            if (fd.isNull()) {
                return null;
            }
            byte[] bytes = fd.getRawFile().getBytes();
            String extension = fd.getExtension();
            // Same case-insensitivity rule as MCPFileTools: lowercase before MIME lookup,
            // octet-stream fallback when the extension isn't in the catalog.
            String extKey = extension == null ? null : extension.toLowerCase(Locale.ROOT);
            String mimeType = "application/octet-stream";
            if (extKey != null && MIMETypeUtils.isFileExtensionMIMEType(extKey)) {
                mimeType = MIMETypeUtils.MIMETypeForFileExtension(extKey);
            }
            item.put("type", "file");
            item.put("extension", extension);
            item.put("mimeType", mimeType);
            item.put("size", bytes != null ? bytes.length : 0);
            if (bytes == null) return item;
            if (MCPBinaryContent.isLikelyText(extension, bytes, 0, bytes.length)) {
                inlineFileText(item, bytes, inlineRemaining);
            } else {
                dispatchBinary(item, fd, result.fileName, index, inlineRemaining, files, placeholderId);
            }
        } else if (value != null) {
            inlineString(item, value.toString(), inlineRemaining);
        }
        return item;
    }

    /**
     * Decide where a binary result goes: small ({@code <} {@link MCPBinaryContent#MAX_INLINE_BINARY_BYTES})
     * AND fits inline budget → inline {@code valueBase64} (clients that can't reuse the MCP
     * connector's auth context for a secondary GET still see the bytes). Otherwise — too big to
     * inline OR inline budget exhausted — register the {@link NamedFileData} under a per-call
     * placeholder; web tier resolves it to a {@code /file/temp/mcp/...} URL via
     * {@link lsfusion.gwt.server.FileUtils#saveMCPFile}. URL-delivered files do NOT count against
     * the inline budget — their bytes flow to disk, not through the JSON-RPC envelope, so the
     * envelope budget is irrelevant. This unblocks any size of binary that previously got
     * truncated because base64-inlining was the only path.
     */
    private static void dispatchBinary(JSONObject item, FileData fd, String fileName, int index,
                                       int[] inlineRemaining,
                                       LinkedHashMap<String, NamedFileData> files, String placeholderId) {
        byte[] bytes = fd.getRawFile().getBytes();
        if (bytes.length < MCPBinaryContent.MAX_INLINE_BINARY_BYTES && bytes.length <= inlineRemaining[0]) {
            item.put("valueBase64", Base64.getEncoder().encodeToString(bytes));
            inlineRemaining[0] -= bytes.length;
            return;
        }
        String placeholder = "__MCP_FILE_" + placeholderId + "_" + index + "__";
        String name = (fileName != null && !fileName.isEmpty()) ? fileName : "result-" + index;
        files.put(placeholder, new NamedFileData(fd, name));
        item.put("url", placeholder);
    }

    /**
     * Put a plain text value on {@code item}, charging UTF-8 byte length against the running
     * inline budget. Crossing either the per-result cap or the per-call total cap drops the
     * payload and tags the reason so callers can choose how to recover.
     */
    private static void inlineString(JSONObject item, String text, int[] inlineRemaining) {
        int byteLen = MCPBinaryContent.utf8Length(text);
        if (byteLen > MAX_INLINE_FILE_BYTES) {
            markTruncated(item, REASON_PER_FILE, byteLen);
            return;
        }
        if (byteLen > inlineRemaining[0]) {
            markTruncated(item, REASON_TOTAL, byteLen);
            return;
        }
        item.put("value", text);
        inlineRemaining[0] -= byteLen;
    }

    /**
     * Inline a text-classified file as {@code value} (UTF-8 string), with the same dual cap
     * as {@link #inlineString} but charging the raw byte length we already have.
     */
    private static void inlineFileText(JSONObject item, byte[] bytes, int[] inlineRemaining) {
        if (bytes.length > MAX_INLINE_FILE_BYTES) {
            markTruncated(item, REASON_PER_FILE, bytes.length);
            return;
        }
        if (bytes.length > inlineRemaining[0]) {
            markTruncated(item, REASON_TOTAL, bytes.length);
            return;
        }
        item.put("value", new String(bytes, StandardCharsets.UTF_8));
        inlineRemaining[0] -= bytes.length;
    }

    /**
     * Add the action's {@code logMessages} to a JSON object under the {@code "messages"} key,
     * applying the per-message / total / count caps so a runaway MESSAGE loop or one
     * gigantic MESSAGE can't blow out the JSON-RPC envelope. No-op when the array is
     * {@code null} or empty (silent script ⇒ no {@code messages} key in the response).
     *
     * <p>Truncation rules:
     * <ul>
     *   <li>An entry longer than {@link #MAX_MESSAGE_BYTES} is cut at the byte boundary plus a
     *       {@code "... [truncated]"} suffix.</li>
     *   <li>Once the running total reaches {@link #MAX_MESSAGES_TOTAL_BYTES} or the count
     *       reaches {@link #MAX_MESSAGES_COUNT}, remaining entries are dropped and a final
     *       summary entry like {@code "... N more messages omitted (per-call cap)"} is
     *       appended.</li>
     * </ul>
     *
     * <p>Package-private so {@code MCPDispatcher}'s error path can apply the same caps when
     * it pulls messages out of {@code MessagesException}.
     */
    static void putCappedMessages(JSONObject out, String[] messages) {
        if (messages == null || messages.length == 0) return;
        JSONArray arr = new JSONArray();
        int totalBytes = 0;
        int included = 0;
        int omitted = 0;
        for (int i = 0; i < messages.length; i++) {
            String m = messages[i] != null ? messages[i] : "";
            if (included >= MAX_MESSAGES_COUNT) {
                omitted = messages.length - i;
                break;
            }
            String capped = m;
            int byteLen = MCPBinaryContent.utf8Length(m);
            if (byteLen > MAX_MESSAGE_BYTES) {
                // Char-based substring is approximate (multi-byte UTF-8 chars expand) but
                // the trailing "... [truncated]" marker makes the caller aware of the cut,
                // so a slight over-cap is acceptable.
                capped = m.substring(0, Math.min(m.length(), MAX_MESSAGE_BYTES)) + " ... [truncated]";
                byteLen = MCPBinaryContent.utf8Length(capped);
            }
            if (totalBytes + byteLen > MAX_MESSAGES_TOTAL_BYTES) {
                omitted = messages.length - i;
                break;
            }
            arr.put(capped);
            totalBytes += byteLen;
            included++;
        }
        if (omitted > 0) {
            arr.put("... " + omitted + " more messages omitted (per-call cap)");
        }
        out.put("messages", arr);
    }

    private static void markTruncated(JSONObject item, String reason, int byteLen) {
        item.put("truncated", true);
        item.put("omittedReason", reason);
        item.put("omittedSize", byteLen);
        item.put("inlineLimit", MAX_INLINE_FILE_BYTES);
        item.put("totalLimit", MAX_INLINE_TOTAL_BYTES);
    }

}
