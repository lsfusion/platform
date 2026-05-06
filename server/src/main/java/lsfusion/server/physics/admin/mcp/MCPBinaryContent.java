package lsfusion.server.physics.admin.mcp;

import lsfusion.base.MIMETypeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared MCP-side helpers for handling file payloads.
 *
 * <p>Three responsibilities:
 * <ul>
 *   <li>{@link #isLikelyText} — robust text/binary classifier. Layered: extension allowlist
 *       ({@link #TEXT_EXTENSIONS}) → extension blocklist ({@link #BINARY_EXTENSIONS}) →
 *       MIME catalog hit (via {@code MIMETypeUtils.isFileExtensionMIMEType}) → byte
 *       heuristic. Text branches always finish with a UTF-8 validation pass through
 *       {@link CharsetDecoder} in REPORT mode, so a file that <em>claims</em> to be text but
 *       contains invalid UTF-8 falls through to binary.</li>
 *   <li>{@link #slimEval} / {@link #slimFileRead} — single-pass payload slimmer. Returns a
 *       {@link SlimResult} with (a) a slim copy of the payload for {@code content[0].text} +
 *       {@code structuredContent} and (b) a list of MCP {@code resource} content entries for
 *       large text. Eval binary delivery (small inline {@code valueBase64} or large URL
 *       placeholder) is owned by {@code MCPEvalTool} upstream — by the time payloads reach
 *       {@link #slimEval}, binary slots already carry their final shape and pass through
 *       unchanged. classpath binary reads (via {@link #slimFileRead}) still move to
 *       {@code resource.blob}. Text ≥ {@link #LARGE_TEXT_THRESHOLD_BYTES} moves to
 *       {@code resource.text} with a {@code resourceUri} pointer on the slim entry; smaller
 *       text stays inline as {@code value} / {@code content}, intentionally duplicated across
 *       the text and structured views (cheap for tiny returns, saves the client a resource
 *       dereference per slot).</li>
 *   <li>Internal {@code blobResource} / {@code textResource} / {@code makeResourceUri} —
 *       build {@code BlobResourceContents} / {@code TextResourceContents} per MCP spec
 *       2024-11-05; URIs are constructed via {@link java.net.URI} so spaces, {@code #},
 *       {@code ?} and non-ASCII path components get percent-encoded automatically.</li>
 * </ul>
 */
final class MCPBinaryContent {

    private MCPBinaryContent() {}

    /**
     * Extensions that are unconditionally text (UTF-8 still validated). Covers lsFusion
     * sources and the everyday source / config / docs filetypes — many of these have no
     * entry in {@code MIMETypes.properties}, so their MIME falls back to
     * {@code application/<ext>} and the catalog-based path would mis-classify them as binary.
     */
    private static final Set<String> TEXT_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // lsFusion + grammar
            "lsf", "lsfp", "bnf", "g", "g4",
            // mainstream source
            "java", "kt", "kts", "groovy", "scala", "py", "rb", "pl", "lua",
            "js", "jsx", "ts", "tsx", "mjs", "cjs",
            "c", "h", "cpp", "cc", "hpp", "hh", "cxx",
            "go", "rs", "swift", "m", "mm",
            "sh", "bash", "zsh", "fish", "ps1",
            // data / config
            "json", "xml", "yaml", "yml", "toml", "ini", "cfg", "conf", "env",
            "properties", "props",
            // docs
            "md", "markdown", "rst", "adoc", "txt", "log", "tex",
            // tabular
            "csv", "tsv",
            // web
            "html", "htm", "xhtml", "css", "scss", "sass", "less", "svg",
            // sql
            "sql", "ddl", "dml",
            // build
            "gradle", "make", "mk", "cmake", "dockerfile"
    )));

    /**
     * Extensions that are unconditionally binary, even when the byte heuristic happens to
     * pass on the first 512 bytes (PDF is the canonical case — its {@code %PDF-} header is
     * all printable ASCII).
     */
    private static final Set<String> BINARY_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // archives + executables
            "pdf", "zip", "gz", "tar", "tgz", "tbz", "tbz2", "bz2", "xz", "7z", "rar",
            "jar", "war", "ear", "apk", "ipa",
            "class", "exe", "dll", "so", "dylib", "bin", "obj", "o",
            // office
            "xls", "xlsx", "xlsm", "xlsb", "doc", "docx", "ppt", "pptx",
            "odt", "ods", "odp", "rtf",
            // media
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "tiff", "tif", "psd",
            "mp3", "wav", "ogg", "flac", "m4a", "aac",
            "mp4", "mov", "avi", "mkv", "webm", "wmv", "flv",
            // fonts
            "ttf", "otf", "woff", "woff2", "eot",
            // db
            "db", "sqlite", "sqlite3", "mdb"
    )));

    // ── classification ──────────────────────────────────────────────────────

    /**
     * Decide whether the byte block can be safely surfaced as a UTF-8 string. Layered gates:
     * <ol>
     *   <li><b>Extension allowlist.</b> {@link #TEXT_EXTENSIONS} forces text for the source /
     *       config / docs files an AI client typically wants to read (lsf, java, md, yaml,
     *       sql, …). These often have no entry in {@code MIMETypes.properties}, so the MIME
     *       catalog returns the {@code application/<ext>} fallback that the next gate would
     *       otherwise mis-classify as binary.</li>
     *   <li><b>Extension blocklist.</b> {@link #BINARY_EXTENSIONS} forces binary for known
     *       formats whose first 512 bytes can pass the byte heuristic — PDF is the canonical
     *       case ({@code %PDF-} header is all printable ASCII).</li>
     *   <li><b>MIME catalog.</b> If the extension <em>is</em> in {@code MIMETypes.properties}
     *       (i.e. the catalog returned a real MIME, not the {@code application/<ext>}
     *       fallback), trust it: text-family MIMEs go through UTF-8 validation, anything else
     *       is binary.</li>
     *   <li><b>Content sniff.</b> Unknown extension and unknown MIME — fall back to the byte
     *       heuristic + UTF-8 validation; {@code passesByteHeuristic} fails on NULs / control
     *       bytes outside the common whitespace + ESC set.</li>
     * </ol>
     * Text branches always finish with a {@link CharsetDecoder} pass in
     * {@link CodingErrorAction#REPORT} mode, so a file that <em>claims</em> to be text but
     * carries invalid UTF-8 still falls through to base64.
     */
    static boolean isLikelyText(String extension, byte[] bytes, int offset, int length) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        // Extension blocklist runs FIRST — empty-but-binary files (a 0-byte .pdf or .xlsx
        // produced by a failed export) must still flow into resource.blob to honour the
        // tool descriptor's "binary always lives in a resource entry" promise.
        if (BINARY_EXTENSIONS.contains(ext)) {
            return false;
        }
        if (length == 0) return true;
        if (TEXT_EXTENSIONS.contains(ext)) {
            return passesByteHeuristic(bytes, offset, length) && isValidUtf8(bytes, offset, length);
        }
        if (!ext.isEmpty() && MIMETypeUtils.isFileExtensionMIMEType(ext)) {
            // Real catalog hit (extension is in MIMETypes.properties) — trust the MIME family.
            String mime = MIMETypeUtils.MIMETypeForFileExtension(ext);
            if (isTextFamilyMime(mime)) {
                return passesByteHeuristic(bytes, offset, length) && isValidUtf8(bytes, offset, length);
            }
            return false;
        }
        return passesByteHeuristic(bytes, offset, length) && isValidUtf8(bytes, offset, length);
    }

    /** Cheap reject for obvious binary content (NULs / unexpected control bytes). */
    private static boolean passesByteHeuristic(byte[] bytes, int offset, int length) {
        int sample = Math.min(length, 512);
        for (int i = 0; i < sample; i++) {
            int b = bytes[offset + i] & 0xFF;
            if (b == 0) return false;
            if (b < 0x09 || (b > 0x0D && b < 0x20 && b != 0x1B)) return false;
        }
        return true;
    }

    private static boolean isTextFamilyMime(String mime) {
        if (mime.startsWith("text/")) return true;
        if (mime.endsWith("+xml") || mime.endsWith("+json")) return true;
        switch (mime) {
            case "application/json":
            case "application/xml":
            case "application/javascript":
            case "application/x-javascript":
            case "application/ecmascript":
            case "application/x-www-form-urlencoded":
            case "application/x-yaml":
            case "application/yaml":
                return true;
            default:
                return false;
        }
    }

    /** True if {@code extKey} is in the explicit text allowlist (lowercased extension). */
    static boolean isKnownTextExtension(String extKey) {
        return extKey != null && TEXT_EXTENSIONS.contains(extKey);
    }

    /**
     * For a byte chunk that may have been cut mid-multi-byte-character (chunked text reads
     * with {@code offset}/{@code maxBytes} not aligned to a UTF-8 boundary), return the
     * largest prefix that ends on a complete UTF-8 character. If the buffer's last 1–3 bytes
     * are an incomplete continuation sequence, those bytes get trimmed; otherwise returns
     * {@code length} unchanged. Trim is bounded at 3 bytes (UTF-8 chars are at most 4).
     *
     * <p>Caller uses this BEFORE classification so the validator does not reject a perfectly
     * good text file just because a chunk happens to slice through `é` / `й` / `字`.
     */
    static int utf8SafeLength(byte[] buf, int offset, int length) {
        if (length == 0) return 0;
        int i = length - 1;
        int continuationCount = 0;
        // UTF-8 continuation bytes have the bit pattern 10xxxxxx.
        while (i >= 0 && (buf[offset + i] & 0xC0) == 0x80) {
            i--;
            continuationCount++;
            if (continuationCount > 3) return length; // malformed — let the validator decide
        }
        if (i < 0) return length; // all continuation bytes — nothing to trim against
        int startByte = buf[offset + i] & 0xFF;
        int expected;
        if ((startByte & 0x80) == 0) expected = 1;        // 0xxxxxxx — ASCII
        else if ((startByte & 0xE0) == 0xC0) expected = 2; // 110xxxxx — 2-byte start
        else if ((startByte & 0xF0) == 0xE0) expected = 3; // 1110xxxx — 3-byte start
        else if ((startByte & 0xF8) == 0xF0) expected = 4; // 11110xxx — 4-byte start
        else return length; // invalid start byte — let the validator decide
        int actualLen = continuationCount + 1;
        if (actualLen < expected) {
            // Incomplete trailing sequence — strip it back to the last complete char.
            return i;
        }
        return length;
    }

    private static boolean isValidUtf8(byte[] bytes, int offset, int length) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(bytes, offset, length));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    // ── response slimming (single-copy invariant) ───────────────────────────

    /**
     * Threshold above which inline {@code value} text is considered "large" and gets moved to
     * a {@code resource} content entry instead of staying in the structured / text views.
     * Keeps small return values (numbers, short strings) ergonomic for AI clients while
     * preventing the text-↔-structured duplication blow-up the user flagged: a single 30 MiB
     * EXPORT CSV would otherwise serialise twice in the JSON-RPC response, plus JSON escape
     * costs for control bytes.
     */
    static final int LARGE_TEXT_THRESHOLD_BYTES = 64 * 1024;

    /**
     * Threshold below which an eval-result binary stays inline as {@code valueBase64} (when the
     * per-call inline budget allows). Above this — or once the inline budget is exhausted —
     * {@link MCPEvalTool} routes the binary through the {@code /file/temp/mcp/...} download
     * URL path instead. MCP clients that can't reuse the connector's auth context for a
     * secondary GET still see small binaries inline. Threshold is on raw byte length, not
     * base64 length.
     */
    static final int MAX_INLINE_BINARY_BYTES = 64 * 1024;

    /**
     * Result of slimming an eval payload — text branches only:
     * <ul>
     *   <li>{@code payload} — slim copy for {@code content[0].text} + {@code structuredContent}
     *       (small text stays inline, large text replaced with a {@code resourceUri} pointer);</li>
     *   <li>{@code resources} — MCP {@code resource} content entries for large text.</li>
     * </ul>
     * Binary delivery is handled upstream in {@link MCPEvalTool}, before the JSON ever reaches
     * here, so this class no longer carries a binary side-map.
     */
    static final class SlimResult {
        final JSONObject payload;
        final JSONArray resources;
        SlimResult(JSONObject payload, JSONArray resources) {
            this.payload = payload;
            this.resources = resources;
        }
    }

    /**
     * Slim an eval payload — text-only:
     * <ul>
     *   <li>Text {@code value} ≥ {@link #LARGE_TEXT_THRESHOLD_BYTES} → moved to a
     *       {@code resource.text} entry, slim retains a {@code resourceUri} pointer.</li>
     *   <li>Text {@code value} below the threshold stays inline as {@code value} (cheap to
     *       duplicate across the text and structured views).</li>
     * </ul>
     * Binary entries pass through unchanged — {@link MCPEvalTool} already decided inline vs URL
     * (and populated the dispatcher's per-call side-map for the URL case) before the payload
     * reaches us.
     */
    static SlimResult slimEval(JSONObject payload) {
        JSONObject slimPayload = shallowCopy(payload);
        JSONArray resources = new JSONArray();
        JSONArray results = payload.optJSONArray("results");
        if (results == null) return new SlimResult(slimPayload, resources);

        JSONArray slimResults = new JSONArray();
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.optJSONObject(i);
            if (r == null) {
                slimResults.put(JSONObject.NULL);
                continue;
            }
            String text = r.optString("value", null);
            if (text != null && utf8Length(text) >= LARGE_TEXT_THRESHOLD_BYTES) {
                JSONObject slim = copyWithoutKeys(r, "value");
                String mimeType = r.optString("mimeType", "text/plain");
                String fileName = pickFileName(r, i, "txt");
                String uri = makeResourceUri("eval", "/" + i + "/" + fileName, null);
                resources.put(textResource(uri, mimeType, text));
                slim.put("resourceUri", uri);
                slimResults.put(slim);
            } else {
                // Small text inline, or non-text result (binary inline / URL placeholder /
                // truncated / isNull) — pass through verbatim.
                slimResults.put(shallowCopy(r));
            }
        }
        slimPayload.put("results", slimResults);
        return new SlimResult(slimPayload, resources);
    }

    /**
     * Slim a files_read payload: binary {@code contentBase64} → blob resource entry, large
     * text {@code content} → text resource entry. Both move out of the structured / text
     * views; small text content stays inline.
     */
    static SlimResult slimFileRead(JSONObject payload) {
        // Same `has(key)` pattern as slimEval — empty binary chunk (0-byte read window) still
        // produces a `resource.blob` entry per the descriptor's "any binary → resource.blob"
        // contract, instead of being silently dropped.
        boolean isBinary = payload.has("contentBase64");
        String b64 = isBinary ? payload.optString("contentBase64", "") : null;
        String text = payload.optString("content", null);
        JSONObject slim = copyWithoutKeys(payload, "contentBase64", "content");
        JSONArray resources = new JSONArray();
        String mimeType = payload.optString("mimeType", "application/octet-stream");
        String path = payload.optString("path", "");
        boolean truncated = payload.optBoolean("truncated", false);
        int offset = payload.optInt("offset", 0);
        int bytesRead = payload.optInt("bytesRead", 0);
        String range = (truncated || offset > 0)
                ? "range=" + offset + "-" + (bytesRead > 0 ? offset + bytesRead - 1 : offset)
                : null;
        String uri = makeResourceUri("files", path, range);

        if (isBinary) {
            resources.put(blobResource(uri, mimeType, b64));
            slim.put("resourceUri", uri);
        } else if (text != null && utf8Length(text) >= LARGE_TEXT_THRESHOLD_BYTES) {
            resources.put(textResource(uri, mimeType, text));
            slim.put("resourceUri", uri);
        } else if (text != null) {
            slim.put("content", text);
        }
        return new SlimResult(slim, resources);
    }

    private static String pickFileName(JSONObject r, int idx, String defaultExt) {
        String fileName = r.optString("fileName", null);
        if (fileName == null || fileName.isEmpty()) {
            fileName = "result-" + idx + "." + r.optString("extension", defaultExt);
        }
        return fileName;
    }

    private static JSONObject shallowCopy(JSONObject src) {
        JSONObject dst = new JSONObject();
        for (String k : src.keySet()) dst.put(k, src.get(k));
        return dst;
    }

    /** Cheap UTF-8 byte-length, no allocation; surrogate-pair aware. */
    static int utf8Length(String s) {
        int n = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x80) n += 1;
            else if (c < 0x800) n += 2;
            else if (Character.isHighSurrogate(c)) { n += 4; i++; }
            else n += 3;
        }
        return n;
    }

    private static JSONObject copyWithoutKeys(JSONObject src, String... drop) {
        JSONObject dst = new JSONObject();
        outer:
        for (String k : src.keySet()) {
            for (String d : drop) {
                if (d.equals(k)) continue outer;
            }
            dst.put(k, src.get(k));
        }
        return dst;
    }

    // ── resource content entries (MCP 2024-11-05 EmbeddedResource shape) ────

    /** {@code BlobResourceContents} — binary payload + mimeType, base64-encoded. */
    private static JSONObject blobResource(String uri, String mimeType, String base64) {
        return new JSONObject()
                .put("type", "resource")
                .put("resource", new JSONObject()
                        .put("uri", uri)
                        .put("mimeType", mimeType)
                        .put("blob", base64));
    }

    /** {@code TextResourceContents} — text payload + mimeType, raw (no base64). */
    private static JSONObject textResource(String uri, String mimeType, String text) {
        return new JSONObject()
                .put("type", "resource")
                .put("resource", new JSONObject()
                        .put("uri", uri)
                        .put("mimeType", mimeType)
                        .put("text", text));
    }

    /**
     * Build a {@code lsfusion://<authority>/<path>[?<query>]} URI with proper percent-encoding
     * for any disallowed characters (spaces, {@code #}, {@code ?}, non-ASCII). Falls back to
     * the unescaped form only if the JDK URI builder rejects the components, which should not
     * happen for well-formed authority/path pairs.
     */
    private static String makeResourceUri(String authority, String path, String query) {
        String safePath = path == null || path.isEmpty()
                ? "/" : (path.startsWith("/") ? path : "/" + path);
        try {
            return new URI("lsfusion", authority, safePath, query, null).toString();
        } catch (URISyntaxException e) {
            return "lsfusion://" + authority + safePath + (query != null ? "?" + query : "");
        }
    }
}
