package lsfusion.interop.logics.remote;

import lsfusion.base.file.NamedFileData;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * MCP dispatch result. Carries the JSON-RPC response envelope plus a side-map of binary
 * payloads referenced from {@link #json} via placeholder strings.
 *
 * <p>Server-side {@code MCPDispatcher} keeps large binaries out of the JSON-RPC envelope —
 * inlining base64 would either blow the JSON-RPC size budget or hit MCP-client limits on
 * embedded binary resources. Instead each large binary slot gets a placeholder in
 * {@link #json} and a real {@link NamedFileData} in {@link #files}; the web tier resolves
 * placeholders into download URLs (via {@code FileUtils.saveMCPFile}) before writing the
 * HTTP response.
 *
 * <p>Small binaries (under {@code MCPBinaryContent.MAX_INLINE_BINARY_BYTES} AND fitting the
 * per-call inline budget) are inlined as {@code valueBase64} directly inside {@link #json}
 * and do <em>not</em> appear in {@link #files} — cheap to embed and visible to clients that
 * can't reuse the MCP connector's auth context for a secondary GET. Anything else — too big
 * to inline OR inline budget exhausted — flows through {@link #files}; binary is never
 * truncated by inline caps.
 *
 * <p>{@code null} {@link #json} signals a JSON-RPC notification (request without {@code id});
 * the web tier acknowledges with HTTP 202 and no body, per spec. {@link #files} is always
 * non-null but may be empty.
 */
public class MCPResult implements Serializable {
    public final String json;
    public final LinkedHashMap<String, NamedFileData> files;

    public MCPResult(String json, LinkedHashMap<String, NamedFileData> files) {
        this.json = json;
        this.files = files != null ? files : new LinkedHashMap<>();
    }
}
