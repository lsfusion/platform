package lsfusion.gwt.client.base;

import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;

public class EscapeUtils {
    public static final String UNICODE_NBSP = "\u00A0";
    public static final String UNICODE_BULLET = "\u2022";

    public static String toHtml(String plainString) {
        if (plainString == null) {
            return "";
        }
        return SimpleHtmlSanitizer.sanitizeHtml(plainString).asString().replaceAll("(\r\n|\n\r|\r|\n)", "<br />");
    }

    public static String escapeLineBreakHTML(String value) {
        return value.replace("\n", "<br/>");
    }

    public static boolean isContainHtmlTag(String value) {
        return value.matches(".*\\<[^>]+\\>(.|\n|\r)*");
    }
}
