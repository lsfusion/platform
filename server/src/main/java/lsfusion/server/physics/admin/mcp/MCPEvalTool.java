package lsfusion.server.physics.admin.mcp;

import lsfusion.base.BaseUtils;
import lsfusion.base.MIMETypeUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.StringWithFiles;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.ResultExternalResponse;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.controller.remote.RemoteLogics;
import lsfusion.server.physics.admin.files.FileContentUtils;
import lsfusion.server.physics.admin.files.JsonToolArgs;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * {@code value}, or of a file's {@code text}). Above this the text is omitted and the slot is marked
     * {@code truncated:true} with {@code omittedReason:"perFileCap"}. Binary results are
     * <em>not</em> subject to this cap — they inline as {@code base64} only when small
     * (under {@link MCPBinaryContent#MAX_INLINE_BINARY_BYTES} AND inline budget allows),
     * otherwise they flow through the {@code /file/temp/mcp/...} URL path regardless of size.
     */
    public static final int MAX_INLINE_FILE_BYTES = 10 * 1024 * 1024;

    /**
     * Hard cap on the <em>total</em> bytes inlined across all eval result slots in one call —
     * counts text ({@code value}, a file's {@code text}) and small-binary {@code base64} together. Applied as a
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
        ExternalRequest.Param script = ExternalRequest.getUrlParam(getRun(args, "script"), UTF8, "script");
        ExternalResponse response = remoteLogics.eval(token, connectionInfo, /*action=*/ false, script, buildRequest(args, envelope));
        return toJson(response, files, placeholderId);
    }

    /**
     * The exec tool: {@link #eval} for an action that is in the logic, named by {@code action} - as {@code /exec} is
     * {@code /eval} for one. The rest of the call and the answer are the same.
     */
    public static JSONObject exec(JSONObject args, RemoteLogics remoteLogics,
                                  AuthenticationToken token, ConnectionInfo connectionInfo,
                                  ExternalRequest envelope,
                                  LinkedHashMap<String, NamedFileData> files, String placeholderId) throws Exception {
        String action = getRun(args, "action");
        ExternalResponse response = remoteLogics.exec(token, connectionInfo, action, buildRequest(args, envelope));
        return toJson(response, files, placeholderId);
    }

    /** What a call is to run - a script, the name of an action: the argument of that name, which has to be there. */
    private static String getRun(JSONObject args, String name) {
        String run = JsonToolArgs.getString(args, name);
        if (run == null || run.isEmpty()) {
            throw new IllegalArgumentException("'" + name + "' is required (non-empty string)");
        }
        return run;
    }

    /**
     * Whether the tool's {@code form} addresses a form - false when there is none. An address is an object of the
     * parts {@code ACTIVATE FORM} addresses an open form by - its name, the label it was opened with, the window it
     * was opened into -, at least one. The application's page reads it by the same rule (WebMCP, {@code isFormAddress}).
     */
    private static boolean isFormAddress(Object form) {
        if (form == null || JSONObject.NULL.equals(form)) {
            return false;
        }
        if (!(form instanceof JSONObject)) {
            throw new IllegalArgumentException("`form` must be an object; got " + form.getClass().getSimpleName());
        }
        JSONObject address = (JSONObject) form;
        boolean addressed = false;
        for (String part : address.keySet()) {
            if (!(part.equals("name") || part.equals("label") || part.equals("window"))) {
                throw new IllegalArgumentException("`form`: unknown field `" + part + "`; a form is addressed by `name`, `label`, `window`");
            }
            addressed |= JsonToolArgs.getStringAt(address, part, "`form." + part + "`") != null;
        }
        if (!addressed) {
            throw new IllegalArgumentException("`form` must address a form: by `name`, `label` or `window`");
        }
        return true;
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
        ExternalRequest.Param[] params = getParams(args.opt("params"));
        // the last checks: the application's page sends here what is no call of the tool, whatever its context, for
        // the server to say what is wrong with it
        String context = JsonToolArgs.getString(args, "context");
        if (isFormAddress(args.opt("form"))) {
            if (context != null && !context.equals("form")) {
                throw new IllegalArgumentException("`form` addresses the form to run in: `context` is then `form`, or omitted; got `" + context + "`");
            }
            context = "form";
        }
        if (context != null && !context.equals("logics")) {
            // where the user works a call is run by the application's page, through the controller of the form or
            // of the navigator (WebMCP): a call that came here has no user at a page to run it for
            throw new IllegalArgumentException(context.equals("navigator") || context.equals("form")
                    ? "`context` `" + context + "` runs where the user works: it is there only where the tool is called by the application's page"
                    : "`context` is `logics`, `navigator` or `form`; got `" + context + "`");
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
                params,
                envelope.headerNames, envelope.headerValues, envelope.cookieNames, envelope.cookieValues,
                envelope.appHost, envelope.appPort, envelope.exportName,
                envelope.scheme, envelope.method, envelope.webHost, envelope.webPort,
                envelope.contextPath, envelope.servletPath, envelope.pathInfo, envelope.query,
                envelope.contentType, envelope.sessionId, envelope.body,
                envelope.signature, envelope.returnMultiType,
                envelope.needNotificationId, envelope.isInteractiveClient);
    }

    /**
     * The tool's {@code params} as the params of an http request, one per parameter of the
     * script's {@code run}, in its order: for the call the tool makes itself, and for a JS
     * controller's call made as the tool's ({@code controller.mcp}).
     */
    public static ExternalRequest.Param[] getParams(Object params) {
        List<ExternalRequest.Param> paramList = new ArrayList<>();
        if (params instanceof JSONArray) {
            JSONArray arr = (JSONArray) params;
            for (int i = 0; i < arr.length(); i++) {
                paramList.add(toImplicitParam(arr.opt(i), "params[" + i + "]"));
            }
        } else if (params != null && !JSONObject.NULL.equals(params)) {
            throw new IllegalArgumentException("`params` must be an array; got "
                    + params.getClass().getSimpleName());
        }
        return paramList.toArray(new ExternalRequest.Param[0]);
    }

    /**
     * Turn one user-supplied JSON value into an implicit
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
     *
     * <p>An object that describes a file ({@link #isFile}) is that file. Any other value is given as
     * an http request gives it, so that the parameter's class parses it as it parses the request's:
     * a string as it is, a number as the string of it ({@link #numberString}), a boolean as
     * {@code "true"} or {@code "false"}, an array and any other object as its JSON text, null as the
     * null file, which is NULL for a parameter of a class.
     */
    private static ExternalRequest.Param toImplicitParam(Object value, String origin) {
        if (value instanceof JSONObject && isFile((JSONObject) value)) {
            return FileClass.buildFileParam((JSONObject) value, origin);
        }
        Object httpValue;
        if (JSONObject.NULL.equals(value)) {
            httpValue = FileData.NULL;
        } else if (value instanceof Number) {
            httpValue = numberString((Number) value);
        } else {
            httpValue = value.toString();
        }
        return new ExternalRequest.Param(httpValue, /*url=*/ false, UTF8, /*name=*/ "");
    }

    /**
     * An object that describes a file: it carries the file's content, or says it is a file as an
     * entry of {@code results[]} does - so one that came with a link in place of its content is
     * refused as a file, and not taken as JSON data. The class of the parameter is not known here:
     * the script is compiled later, by the call the params are made for.
     */
    private static boolean isFile(JSONObject object) {
        return object.has("text") || object.has("base64") || "file".equals(object.opt("type"));
    }

    /**
     * A JSON number as the string an http caller writes it as: an integer as its digits, however
     * it is written ({@code 21.0} and {@code 2.1e1} are {@code "21"} - an integral class reads no
     * fraction and no exponent), any other number as {@link BigDecimal#toString} writes it. Both
     * are read back as the same number, by the JDK's contract, and neither writes an exponent
     * out: {@code 1e999999999} stays {@code "1E+999999999"}.
     */
    private static String numberString(Number value) {
        String string = value.toString();
        BigDecimal number = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(string);
        if (number.signum() == 0) { // a DOUBLE has two zeros, and org.json reads -0 as one; 0e19 is no long for an early JDK 11
            return string.startsWith("-") ? "-0" : "0";
        }
        try {
            return Long.toString(number.longValueExact());
        } catch (ArithmeticException e) { // a fraction, or beyond a long
            return number.toString();
        }
    }

    private static JSONObject toJson(ExternalResponse response,
                                     LinkedHashMap<String, NamedFileData> files, String placeholderId) {
        JSONObject out = new JSONObject().put("status", response.getStatusHttp());
        if (response instanceof ResultExternalResponse) {
            ResultExternalResponse r = (ResultExternalResponse) response;
            JSONArray results = new JSONArray();
            // Per-call running total — best-fit budget for INLINE payloads only (text values +
            // small binary `base64`). URL-delivered binaries don't count against this,
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

    /**
     * What the tool answers for what a script returned - {@code {results, status}} -, for a caller that makes the
     * links of the files itself, a JS controller's call made as the tool's ({@code controller.mcp}): the JSON text of
     * the answer. Where a file is too big to inline, the text is cut at the placeholder of its url and the file put
     * there - a string with files, which the web server saves and the page writes the links into, as the link of a
     * file result is made. The placeholders are in the text in the order the files were put, that of the results
     * they are of.
     */
    public static Object render(ExternalResponse response) {
        LinkedHashMap<String, NamedFileData> files = new LinkedHashMap<>();
        String json = toJson(response, files, BaseUtils.randomString(20)).toString();
        if (files.isEmpty()) {
            return json;
        }
        String[] prefixes = new String[files.size() + 1];
        Serializable[] links = new Serializable[files.size()];
        int i = 0;
        int start = 0;
        for (Map.Entry<String, NamedFileData> file : files.entrySet()) {
            int at = json.indexOf(file.getKey(), start);
            prefixes[i] = json.substring(start, at);
            links[i++] = file.getValue();
            start = at + file.getKey().length();
        }
        prefixes[i] = json.substring(start);
        return new StringWithFiles(prefixes, links, json);
    }

    private static JSONObject renderResult(ExternalRequest.Result result, int index, int[] inlineRemaining,
                                           LinkedHashMap<String, NamedFileData> files, String placeholderId) {
        JSONObject item = new JSONObject();
        if (result.name != null) item.put("name", result.name);
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
            // Same case-insensitivity rule as ClasspathFileTools: lowercase before MIME lookup,
            // octet-stream fallback when the extension isn't in the catalog.
            String extKey = extension == null ? null : extension.toLowerCase(Locale.ROOT);
            String mimeType = "application/octet-stream";
            if (extKey != null && MIMETypeUtils.isFileExtensionMIMEType(extKey)) {
                mimeType = MIMETypeUtils.MIMETypeForFileExtension(extKey);
            }
            item.put("type", "file");
            if (result.fileName != null) { // with its extension, as a file's name is given in a parameter
                item.put("fileName", extension.isEmpty() ? result.fileName : result.fileName + "." + extension);
            }
            item.put("extension", extension);
            item.put("mimeType", mimeType);
            item.put("size", bytes != null ? bytes.length : 0);
            if (bytes == null) return item;
            if (FileContentUtils.isLikelyText(extension, bytes, 0, bytes.length)) {
                // a text of an extension the catalog has no type for is a plain text, not unknown bytes: txt itself
                // is not there - text/plain is what a plain string goes over http as, so it stands for no extension
                if (mimeType.equals("application/octet-stream")) {
                    item.put("mimeType", "text/plain");
                }
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
     * AND fits inline budget → inline {@code base64} (clients that can't reuse the MCP
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
            item.put("base64", Base64.getEncoder().encodeToString(bytes));
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
     * Inline a text-classified file as {@code text} (UTF-8 string), with the same dual cap
     * as {@link #inlineString} but charging the raw byte length we already have. The content
     * of a file is named in a result as it is in a parameter ({@link FileClass#buildFileParam}) -
     * {@code text}, {@code base64} -, so a result's file can be the next call's parameter.
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
        item.put("text", new String(bytes, StandardCharsets.UTF_8));
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
