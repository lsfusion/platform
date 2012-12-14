package platform.gwt.base.client;

import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;

public class EscapeUtils {
    public static final String UNICODE_NBSP = "\u00A0";
    public static final String UNICODE_AMP = "\u00A0";
    public static final String UNICODE_LT = "\u00A0";
    public static final String UNICODE_GT = "\u00A0";
    public static final String UNICODE_QUOT = "\u00A0";
    public static final String UNICODE_SINGLE_QUOT = "\u00A0";

    public static String toHtml(String plainString) {
        if (plainString == null) {
            return "";
        }
        return SimpleHtmlSanitizer.sanitizeHtml(plainString).asString().replaceAll("(\r\n|\n\r|\r|\n)", "<br />");
    }

    private static final RegExp AMP_RE = RegExp.compile("&", "g");
    private static final RegExp GT_RE = RegExp.compile(">", "g");
    private static final RegExp LT_RE = RegExp.compile("<", "g");
    private static final RegExp SQUOT_RE = RegExp.compile("\'", "g");
    private static final RegExp QUOT_RE = RegExp.compile("\"", "g");

    public static String unicodeEscape(String s) {
        if (s.contains("&")) {
            s = AMP_RE.replace(s, UNICODE_AMP);
        }
        if (s.contains("<")) {
            s = LT_RE.replace(s, UNICODE_LT);
        }
        if (s.contains(">")) {
            s = GT_RE.replace(s, UNICODE_GT);
        }
        if (s.contains("\"")) {
            s = QUOT_RE.replace(s, UNICODE_QUOT);
        }
        if (s.contains("'")) {
            s = SQUOT_RE.replace(s, UNICODE_SINGLE_QUOT);
        }
        return s;
    }
}
