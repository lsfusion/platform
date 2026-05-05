package lsfusion.server.physics.admin.mcp;

import lsfusion.base.MIMETypeUtils;
import lsfusion.server.base.ResourceUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Server-classpath browsing tools surfaced as MCP tools. Caps below are intentionally tight;
 * an arbitrary AI client can drive these, so every entry point bounds wall-clock time, total
 * files visited, and bytes scanned per file.
 */
public class MCPFileTools {

    public static final int DEFAULT_LIST_LIMIT = 500;
    public static final int MAX_LIST_LIMIT = 5000;

    public static final int DEFAULT_SEARCH_LIMIT = 200;
    public static final int MAX_SEARCH_LIMIT = 2000;

    public static final int DEFAULT_READ_MAX_BYTES = 256 * 1024;
    public static final int MAX_READ_MAX_BYTES = 4 * 1024 * 1024;

    public static final int DEFAULT_SEARCH_TIMEOUT_SECS = 5;
    public static final int MAX_SEARCH_TIMEOUT_SECS = 30;

    public static final int DEFAULT_SEARCH_MAX_FILES = 2_000;
    public static final int MAX_SEARCH_MAX_FILES = 20_000;

    public static final int DEFAULT_SEARCH_MAX_FILE_BYTES = 512 * 1024;
    public static final int MAX_SEARCH_MAX_FILE_BYTES = 4 * 1024 * 1024;

    /**
     * Hard per-line character cap during {@link #search} — caps the StringBuilder a single
     * minified line can grow to, regardless of {@code maxFileBytes}. A long matching line is
     * still reported, but with its tail elided, so per-call memory stays bounded by
     * {@code limit * MAX_LINE_CHARS}.
     */
    private static final int MAX_LINE_CHARS = 16 * 1024;

    /** Default file types for search — text-y formats found in lsFusion projects, glob form. */
    static final String DEFAULT_SEARCH_PATH_GLOB = "**/*.{lsf,lsfp,java,properties,xml,sql,md,json,yaml,yml}";

    /**
     * One-shot snapshot of every classpath resource. The classpath is fixed at JVM startup, so
     * walking + sorting it once is enough; subsequent regex filters are an in-memory linear
     * scan, which keeps {@link #list} and {@link #search} cheap regardless of the user pattern.
     */
    private static volatile List<String> ALL_RESOURCES_CACHE;

    /** {@code .*}-equivalent: matches every resource. */
    private static final Pattern MATCH_ALL = Pattern.compile(".*");

    private static List<String> getAllResources() {
        List<String> cached = ALL_RESOURCES_CACHE;
        if (cached != null) return cached;
        synchronized (MCPFileTools.class) {
            if (ALL_RESOURCES_CACHE == null) {
                // ResourceUtils walks every classpath element and concatenates results, so the
                // same path can appear once per overlapping classpath entry. Dedup via a
                // LinkedHashSet keyed on the full path — preserves the (already sorted) order
                // ResourceUtils returned and eliminates the spurious duplicates that would
                // otherwise leak into list/search output.
                List<String> raw = ResourceUtils.getResources(MATCH_ALL);
                LinkedHashSet<String> dedup = new LinkedHashSet<>(raw.size());
                dedup.addAll(raw);
                ALL_RESOURCES_CACHE = Collections.unmodifiableList(new ArrayList<>(dedup));
            }
            return ALL_RESOURCES_CACHE;
        }
    }

    /**
     * Filter the cached classpath by regex, deadline-aware. {@code status} (if non-null) reports
     * which limit ended the scan: index 0 = wall-clock timeout, index 1 = hit {@code hardCap}.
     *
     * <p>Always returns full classpath paths — capturing groups in the user pattern are ignored
     * for the result string, since downstream tools (read / search) need real paths to feed
     * back into {@code getResourceAsStream}. Capturing groups still work for the user's match
     * logic (lookahead, alternation), they just don't rewrite the output.
     */
    private static List<String> filterResources(Pattern pattern, long deadlineNanos, int hardCap, boolean[] status) {
        List<String> all = getAllResources();
        boolean noOpPattern = ".*".equals(pattern.pattern());
        if (noOpPattern && hardCap >= all.size()) {
            return all;
        }

        List<String> out = new ArrayList<>(Math.min(all.size(), 1024));
        int i = 0;
        for (String path : all) {
            if (++i % 1024 == 0 && deadlineNanos > 0 && System.nanoTime() > deadlineNanos) {
                if (status != null) status[0] = true;
                break;
            }
            if (noOpPattern) {
                out.add(path);
            } else {
                // Wrap the path in DeadlineCharSequence too: paths are short, but a pathological
                // pathPattern can still backtrack quadratically/exponentially per path × many
                // paths, which adds up.
                Matcher m = pattern.matcher(new DeadlineCharSequence(path, deadlineNanos));
                try {
                    if (m.matches()) {
                        out.add(path);
                    }
                } catch (RegexDeadlineException e) {
                    if (status != null) status[0] = true;
                    break;
                }
            }
            if (out.size() >= hardCap) {
                if (status != null) status[1] = true;
                break;
            }
        }
        return out;
    }

    public static JSONObject list(JSONObject args) {
        String glob = MCPArgs.getString(args, "pathPattern");
        int limit = clamp(MCPArgs.getInt(args, "limit", DEFAULT_LIST_LIMIT), 1, MAX_LIST_LIMIT);
        int offset = Math.max(0, MCPArgs.getInt(args, "offset", 0));

        Pattern compiled = compileGlob(glob);
        // Hard cap matched-list size at offset+limit+1; the +1 lets us tell whether more
        // resources matched beyond the page that we are surfacing.
        int matchedCap = offset + limit + 1;
        if (matchedCap < 0) matchedCap = Integer.MAX_VALUE; // overflow guard
        // Internal wall-clock guard so a pathological user pattern cannot wedge the call.
        long deadlineNanos = System.nanoTime() + DEFAULT_SEARCH_TIMEOUT_SECS * 1_000_000_000L;
        boolean[] status = new boolean[2];
        List<String> matched = filterResources(compiled, deadlineNanos, matchedCap, status);

        JSONArray files = new JSONArray();
        int end = Math.min(matched.size(), offset + limit);
        for (int i = offset; i < end; i++) {
            files.put(matched.get(i));
        }

        boolean truncated = status[1] || matched.size() > end;
        return new JSONObject()
                .put("files", files)
                .put("offset", offset)
                .put("limit", limit)
                .put("truncated", truncated)
                .put("timedOut", status[0]);
    }

    public static JSONObject search(JSONObject args) {
        String regex = MCPArgs.getString(args, "regex");
        if (regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("'regex' is required (non-empty string)");
        }
        String pathGlobRaw = MCPArgs.getString(args, "pathPattern");
        String pathGlob = pathGlobRaw == null ? DEFAULT_SEARCH_PATH_GLOB : pathGlobRaw;
        int limit = clamp(MCPArgs.getInt(args, "limit", DEFAULT_SEARCH_LIMIT), 1, MAX_SEARCH_LIMIT);
        int contextChars = clamp(MCPArgs.getInt(args, "contextChars", 120), 0, 500);
        int timeoutSecs = clamp(MCPArgs.getInt(args, "timeoutSeconds", DEFAULT_SEARCH_TIMEOUT_SECS), 1, MAX_SEARCH_TIMEOUT_SECS);
        int maxFiles = clamp(MCPArgs.getInt(args, "maxScannedFiles", DEFAULT_SEARCH_MAX_FILES), 1, MAX_SEARCH_MAX_FILES);
        int maxFileBytes = clamp(MCPArgs.getInt(args, "maxFileBytes", DEFAULT_SEARCH_MAX_FILE_BYTES), 1, MAX_SEARCH_MAX_FILE_BYTES);

        Pattern bodyRegex = compile(regex);
        Pattern pathRegex = compileGlob(pathGlob);

        long deadlineNanos = System.nanoTime() + timeoutSecs * 1_000_000_000L;

        // Filter the cached classpath snapshot to candidates — bounded by both the deadline and
        // maxFiles, so a pathological path regex on a huge classpath cannot run away. status[0]
        // = enumeration timed out, status[1] = enumeration hit the hard cap (more candidates
        // existed than we are scanning).
        boolean[] enumStatus = new boolean[2];
        List<String> candidates = filterResources(pathRegex, deadlineNanos, maxFiles, enumStatus);

        JSONArray hits = new JSONArray();
        boolean truncated = enumStatus[1]; // candidates list is itself a truncated set
        boolean timedOut = enumStatus[0];
        int scanned = 0;
        outer:
        for (String path : candidates) {
            if (System.nanoTime() > deadlineNanos) { timedOut = true; break; }
            scanned++;
            InputStream raw = ResourceUtils.getResourceAsStream(path, false);
            if (raw == null) continue;
            try {
                // Cap total bytes per file: BoundedInputStream returns EOF once maxFileBytes
                // bytes have been consumed, so InputStreamReader can never pull more than that
                // into memory regardless of how the underlying resource is encoded.
                InputStream bounded = new BoundedInputStream(raw, maxFileBytes);
                BufferedReader reader = new BufferedReader(new InputStreamReader(bounded, StandardCharsets.UTF_8));
                LineScanResult lineResult = scanLines(reader, bodyRegex, path, hits, limit, contextChars, deadlineNanos);
                if (lineResult.truncated) truncated = true;
                if (lineResult.timedOut) { timedOut = true; break outer; }
                if (hits.length() >= limit) { truncated = true; break outer; }
                // Probe: BoundedInputStream silently EOFs at the cap. If the underlying resource
                // had bytes past the cap, raw is now positioned at exactly maxFileBytes — read
                // one more byte directly from raw to find out, so the response surfaces
                // `truncated=true` instead of pretending the scan was complete.
                if (lineResult.consumedToEof) {
                    try {
                        if (raw.read() != -1) truncated = true;
                    } catch (Exception ignored) { /* probe is best-effort */ }
                }
            } catch (RegexDeadlineException e) {
                timedOut = true;
                break outer;
            } catch (Exception ignored) {
                // skip unreadable resources
            } finally {
                try { raw.close(); } catch (Exception ignored) { /* nothing useful to do */ }
            }
        }

        return new JSONObject()
                .put("hits", hits)
                .put("scannedFiles", scanned)
                .put("candidates", candidates.size())
                .put("truncated", truncated)
                .put("timedOut", timedOut);
    }

    /**
     * Read lines from {@code reader} with a per-line character cap ({@link #MAX_LINE_CHARS}),
     * matching each against {@code bodyRegex} and appending hits. Returns flags for whether
     * any line was truncated (long-line elision) and whether the deadline expired.
     */
    private static LineScanResult scanLines(BufferedReader reader, Pattern bodyRegex, String path,
                                            JSONArray hits, int hitLimit, int contextChars,
                                            long deadlineNanos) throws java.io.IOException {
        LineScanResult result = new LineScanResult();
        StringBuilder line = new StringBuilder(256);
        int lineNum = 0;
        boolean lineWasTruncated = false;
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '\n') {
                lineNum++;
                if (matchAndRecord(line, lineWasTruncated, lineNum, path, bodyRegex, hits, contextChars, deadlineNanos)) {
                    result.truncated = result.truncated || lineWasTruncated;
                    if (hits.length() >= hitLimit) return result;
                }
                line.setLength(0);
                lineWasTruncated = false;
                if (System.nanoTime() > deadlineNanos) {
                    result.timedOut = true;
                    return result;
                }
            } else if (c != '\r') {
                if (line.length() < MAX_LINE_CHARS) {
                    line.append((char) c);
                } else {
                    lineWasTruncated = true; // discard the rest of this line
                    result.truncated = true;
                }
            }
        }
        // Trailing line without newline.
        if (line.length() > 0 || lineWasTruncated) {
            lineNum++;
            matchAndRecord(line, lineWasTruncated, lineNum, path, bodyRegex, hits, contextChars, deadlineNanos);
        }
        // Reader hit EOF naturally (the BoundedInputStream returned -1, either because the
        // file ended or because maxFileBytes was reached). The caller probes `raw` to tell
        // those two apart.
        result.consumedToEof = true;
        return result;
    }

    private static boolean matchAndRecord(StringBuilder line, boolean lineWasTruncated, int lineNum,
                                          String path, Pattern bodyRegex, JSONArray hits,
                                          int contextChars, long deadlineNanos) {
        // Wrap the line in a deadline-aware CharSequence so a pathological body regex (e.g.
        // catastrophic backtracking) cannot hang past the search-wide timeout — charAt() polls
        // the deadline and throws RegexDeadlineException, which the search caller treats as a
        // timed-out search and returns whatever hits were already collected.
        if (!bodyRegex.matcher(new DeadlineCharSequence(line, deadlineNanos)).find()) return false;
        int len = line.length();
        String excerpt = len > contextChars ? line.substring(0, contextChars) + "…" : line.toString();
        if (lineWasTruncated) excerpt = excerpt + " [line truncated]";
        hits.put(new JSONObject()
                .put("path", path)
                .put("line", lineNum)
                .put("excerpt", excerpt));
        return true;
    }

    private static final class LineScanResult {
        boolean truncated;
        boolean timedOut;
        boolean consumedToEof;
    }

    /**
     * CharSequence wrapper that throws {@link RegexDeadlineException} from {@link #charAt(int)}
     * once the deadline has passed. Java's {@code Pattern.matcher} drives matching through
     * {@code charAt}, so wrapping the input is enough to make pathological backtracking break
     * out at the next char access instead of hanging indefinitely.
     *
     * <p>The poll uses a coarse counter so the wall-clock check happens roughly once every
     * 4096 chars — hot enough to bound a runaway, cheap enough that normal matches pay almost
     * nothing.
     */
    private static final class DeadlineCharSequence implements CharSequence {
        private final CharSequence wrapped;
        private final long deadlineNanos;
        private int charCount;

        DeadlineCharSequence(CharSequence wrapped, long deadlineNanos) {
            this.wrapped = wrapped;
            this.deadlineNanos = deadlineNanos;
        }

        @Override public int length() { return wrapped.length(); }

        @Override
        public char charAt(int index) {
            if (((++charCount) & 0xFFF) == 0 && deadlineNanos > 0 && System.nanoTime() > deadlineNanos) {
                throw new RegexDeadlineException();
            }
            return wrapped.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            // Re-wrap so backtracking on a substring also gets the deadline.
            return new DeadlineCharSequence(wrapped.subSequence(start, end), deadlineNanos);
        }

        @Override public String toString() { return wrapped.toString(); }
    }

    private static final class RegexDeadlineException extends RuntimeException {
        RegexDeadlineException() { super(null, null, false, false); }
    }

    public static JSONObject read(JSONObject args) {
        String path = MCPArgs.getString(args, "path");
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("'path' is required (non-empty string)");
        }
        int maxBytes = clamp(MCPArgs.getInt(args, "maxBytes", DEFAULT_READ_MAX_BYTES), 1, MAX_READ_MAX_BYTES);
        int offset = Math.max(0, MCPArgs.getInt(args, "offset", 0));

        try (InputStream in = ResourceUtils.getResourceAsStream(path, false)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            long skipped = 0;
            while (skipped < offset) {
                long s = in.skip(offset - skipped);
                if (s <= 0) break;
                skipped += s;
            }
            byte[] buf = new byte[maxBytes];
            int total = 0;
            int n;
            while (total < maxBytes && (n = in.read(buf, total, maxBytes - total)) > 0) {
                total += n;
            }

            // Lowercase the extension so `.JSON` / `.XML` / `.LSF` resolve to the same MIME
            // catalog entry as their lowercase form — the catalog itself is keyed lowercase.
            String extension = pathExtension(path);
            String extKey = extension == null ? null : extension.toLowerCase(Locale.ROOT);
            boolean isKnownText = MCPBinaryContent.isKnownTextExtension(extKey);

            // Forward-progress read-ahead: if the entire window is one incomplete UTF-8 char
            // (caller passed maxBytes < 4 and we landed on a multi-byte char start), read up
            // to 3 more bytes so we can include at least one complete character. Without
            // this a chunked read loop would spin in place on `bytesRead:0`. Done before the
            // eof probe — if the file genuinely ends mid-char the extension reads will
            // simply hit EOF early and we leave `total` as-is; the next eof probe then sees
            // -1 and we skip the trim below, letting the validator route the partial bytes
            // into base64.
            if (isKnownText && total > 0 && total < 4) {
                int safeLen = MCPBinaryContent.utf8SafeLength(buf, 0, total);
                if (safeLen == 0) {
                    byte[] extended = new byte[total + 3];
                    System.arraycopy(buf, 0, extended, 0, total);
                    int extra = 0;
                    while (extra < 3) {
                        int got = in.read(extended, total + extra, 3 - extra);
                        if (got < 0) break;
                        extra += got;
                    }
                    if (extra > 0) {
                        buf = extended;
                        total += extra;
                    }
                }
            }

            boolean eof = in.read() == -1;

            // UTF-8 boundary trim: ONLY when there's more data past the window. At EOF a
            // partial trailing UTF-8 sequence reflects the actual file contents, so we
            // preserve the bytes and let the validator decide — typically falling through
            // to base64. Trimming at EOF would silently drop the tail (response would say
            // `eof:true, truncated:false` while the file actually has those bytes).
            boolean trimmedForUtf8 = false;
            if (!eof && isKnownText && total > 0) {
                int safeLen = MCPBinaryContent.utf8SafeLength(buf, 0, total);
                if (safeLen < total) {
                    total = safeLen;
                    trimmedForUtf8 = true;
                }
            }

            JSONObject result = new JSONObject()
                    .put("path", path)
                    .put("offset", offset)
                    .put("bytesRead", total)
                    .put("eof", eof)
                    // !eof means there were more bytes past the maxBytes window — surface this
                    // explicitly so a client doesn't store the head as if it were the whole file.
                    .put("truncated", !eof);
            if (trimmedForUtf8) {
                // Tell the client we kept UTF-8 alignment; the trimmed tail bytes are the
                // start of the next chunk's window (their `offset` should be `offset + bytesRead`).
                result.put("utf8BoundaryTrimmed", true);
            }

            // Always populate `mimeType`. Fall back to `application/octet-stream` if the path
            // has no extension OR the extension isn't in MIMETypes.properties — otherwise
            // MIMETypeForFileExtension would fabricate `application/<ext>` (e.g. `.lsf` →
            // `application/lsf`) and the descriptor's "octet-stream fallback" promise would
            // be a lie.
            String mimeType = "application/octet-stream";
            if (extKey != null && MIMETypeUtils.isFileExtensionMIMEType(extKey)) {
                mimeType = MIMETypeUtils.MIMETypeForFileExtension(extKey);
            }
            result.put("mimeType", mimeType);

            // Text vs binary split: text files (lsf/java/json/xml/csv/...) inline as a UTF-8
            // string in `content`; binary (xlsx/pdf/zip/jar/class/...) goes to `contentBase64`
            // so the bytes survive JSON transport intact. The dispatcher additionally surfaces
            // binary reads as MCP `resource` content entries. Classification combines the
            // extension's MIME type with UTF-8 validation, so PDF / DOCX / etc. don't slip
            // through the byte heuristic and get UTF-8-mangled.
            if (MCPBinaryContent.isLikelyText(extension, buf, 0, total)) {
                result.put("content", new String(buf, 0, total, StandardCharsets.UTF_8));
            } else {
                result.put("contentBase64",
                        Base64.getEncoder().encodeToString(Arrays.copyOf(buf, total)));
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + path + ": " + e.getMessage(), e);
        }
    }

    private static String pathExtension(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot < slash) return null;
        return path.substring(dot + 1);
    }

    private static Pattern compile(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex '" + regex + "': " + e.getDescription());
        }
    }

    /**
     * Compile a gitignore-style glob into a Java regex anchored against full classpath paths.
     *
     * <p>Supported syntax:
     * <ul>
     *   <li>{@code *} — any chars except {@code /}</li>
     *   <li>{@code **} — any chars including {@code /}; immediately following {@code /} is
     *       optional, so {@code **}/{@code foo} matches both {@code /foo} and {@code /a/b/foo}</li>
     *   <li>{@code ?} — one char except {@code /}</li>
     *   <li>{@code [abc]}, {@code [a-z]}, {@code [!abc]} — character classes (with negation)</li>
     *   <li>{@code {a,b,c}} — alternation between literal branches</li>
     *   <li>{@code \X} — literal {@code X}</li>
     *   <li>Globs starting with {@code /} are anchored at the path root; otherwise the match
     *       is allowed at any depth, so {@code *.lsf} matches {@code /a/b/foo.lsf}.</li>
     * </ul>
     *
     * <p>Empty / null glob matches everything. Returns the same kind of {@link Pattern}
     * {@link #filterResources} expects, including the {@code .*} short-circuit.
     */
    static Pattern compileGlob(String glob) {
        if (glob == null || glob.isEmpty()) return MATCH_ALL;
        String original = glob;
        StringBuilder sb = new StringBuilder();
        if (glob.startsWith("/")) {
            sb.append('/');
            glob = glob.substring(1);
        } else {
            // Allow any prefix ending at a path separator, plus the empty prefix.
            sb.append("(?:.*/)?");
        }
        int n = glob.length();
        for (int i = 0; i < n; ) {
            char c = glob.charAt(i);
            if (c == '*') {
                if (i + 1 < n && glob.charAt(i + 1) == '*') {
                    // ** — match any chars (including /). If immediately followed by /, make
                    // the slash optional so `**/foo` matches `/foo` (zero intermediate dirs).
                    sb.append(".*");
                    i += 2;
                    if (i < n && glob.charAt(i) == '/') {
                        sb.append("/?");
                        i++;
                    }
                } else {
                    sb.append("[^/]*");
                    i++;
                }
            } else if (c == '?') {
                sb.append("[^/]");
                i++;
            } else if (c == '[') {
                int close = glob.indexOf(']', i);
                if (close < 0) {
                    sb.append("\\[");
                    i++;
                } else {
                    sb.append('[');
                    String body = glob.substring(i + 1, close);
                    if (body.startsWith("!")) {
                        sb.append('^');
                        body = body.substring(1);
                    }
                    for (int j = 0; j < body.length(); j++) {
                        char cc = body.charAt(j);
                        if (cc == '\\' || cc == ']') sb.append('\\');
                        sb.append(cc);
                    }
                    sb.append(']');
                    i = close + 1;
                }
            } else if (c == '{') {
                int rclose = glob.indexOf('}', i);
                if (rclose < 0) {
                    sb.append("\\{");
                    i++;
                } else {
                    String[] alts = glob.substring(i + 1, rclose).split(",", -1);
                    sb.append("(?:");
                    for (int j = 0; j < alts.length; j++) {
                        if (j > 0) sb.append('|');
                        sb.append(Pattern.quote(alts[j]));
                    }
                    sb.append(')');
                    i = rclose + 1;
                }
            } else if (c == '\\' && i + 1 < n) {
                sb.append(Pattern.quote(String.valueOf(glob.charAt(i + 1))));
                i += 2;
            } else if (".^$|+()".indexOf(c) >= 0) {
                sb.append('\\').append(c);
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        try {
            return Pattern.compile(sb.toString());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid glob '" + original + "': " + e.getDescription());
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
