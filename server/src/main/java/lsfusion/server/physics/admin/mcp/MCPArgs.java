package lsfusion.server.physics.admin.mcp;

import org.json.JSONObject;

/**
 * Strict argument unwrappers for MCP tool calls. Replaces {@code JSONObject.optString} /
 * {@code optInt} for fields that come straight from an external client: those silently
 * stringify non-strings ({@code text:123} → {@code "123"}) and silently default non-ints
 * ({@code limit:"abc"} → 0), which surfaces as confusing downstream behaviour. The helpers
 * here fail fast with a clear {@link IllegalArgumentException} so the dispatcher's catch
 * block can surface a precise tool-error result to the caller.
 */
final class MCPArgs {
    private MCPArgs() {}

    /**
     * Read {@code obj[key]} as a string. Returns {@code null} if the key is absent or
     * explicitly JSON-null. Throws if present-but-not-a-string.
     */
    static String getString(JSONObject obj, String key) {
        return getStringAt(obj, key, "`" + key + "`");
    }

    /**
     * Same as {@link #getString(JSONObject, String)} but lets the caller stamp a richer
     * origin path on the error message — useful for nested file-object validation, where
     * "{@code params[2].extension must be a string}" reads better than "{@code `extension`
     * must be a string}".
     */
    static String getStringAt(JSONObject obj, String key, String origin) {
        if (!obj.has(key)) return null;
        Object v = obj.opt(key);
        if (v == null || JSONObject.NULL.equals(v)) return null;
        if (!(v instanceof String)) {
            throw new IllegalArgumentException(origin + " must be a string, got "
                    + v.getClass().getSimpleName());
        }
        return (String) v;
    }

    /**
     * Read {@code obj[key]} as an int. Returns {@code defaultValue} if the key is absent or
     * explicitly JSON-null. Throws if present-but-not-numeric or out of int range. Strings
     * like {@code "42"} are NOT parsed — strict type only, matching JSON semantics.
     */
    static int getInt(JSONObject obj, String key, int defaultValue) {
        if (!obj.has(key)) return defaultValue;
        Object v = obj.opt(key);
        if (v == null || JSONObject.NULL.equals(v)) return defaultValue;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Long) {
            long l = (Long) v;
            if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("`" + key + "` is out of int range: " + l);
            }
            return (int) l;
        }
        throw new IllegalArgumentException("`" + key + "` must be an integer, got "
                + v.getClass().getSimpleName());
    }
}
