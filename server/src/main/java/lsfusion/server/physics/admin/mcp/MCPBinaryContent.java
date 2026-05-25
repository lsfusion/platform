package lsfusion.server.physics.admin.mcp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Shared MCP-side helpers for handling file payloads.
 *
 * <p>Two responsibilities:
 * <ul>
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
