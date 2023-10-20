package lsfusion.gwt.client.base;

import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;

public class EscapeUtils {
    public static final String UNICODE_NBSP = "\u00A0";
    public static final String UNICODE_BULLET = "\u2022";

    public static String toHtml(String plainString, boolean sanitize) {
        if (plainString == null) {
            return "";
        }
        return escapeLineBreakHTML(sanitize ? SimpleHtmlSanitizer.sanitizeHtml(plainString).asString() : plainString);
    }

    public static String toHtml(String plainString) {
        return toHtml(plainString, true);
    }

    private static String escapeLineBreakHTML(String value) {
        return value.replaceAll("(\r\n|\n\r|\r|\n)", "<br/>");
    }
}
