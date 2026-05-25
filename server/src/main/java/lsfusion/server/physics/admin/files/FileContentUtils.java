package lsfusion.server.physics.admin.files;

import lsfusion.base.MIMETypeUtils;

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
 * Transport-agnostic text/binary classification and UTF-8 helpers for classpath file reads,
 * used by both the plain-HTTP {@code /files} path and MCP.
 */
public final class FileContentUtils {

    private FileContentUtils() {}

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
    public static boolean isLikelyText(String extension, byte[] bytes, int offset, int length) {
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
    public static boolean isKnownTextExtension(String extKey) {
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
    public static int utf8SafeLength(byte[] buf, int offset, int length) {
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
}
