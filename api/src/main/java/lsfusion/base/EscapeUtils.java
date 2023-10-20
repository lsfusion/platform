package lsfusion.base;

import java.util.regex.Pattern;

public class EscapeUtils {

    public static String toHtml(String plainString) {
        return plainString == null ? "" : isContainHtmlTag(plainString) ? plainString : escapeLineBreakHTML(plainString);
    }

    public static String escapeLineBreakHTML(String value) {
        return value.replaceAll("(\r\n|\n\r|\r|\n)", "<br/>");
    }

    public static boolean isContainHtmlTag(String value) {
        return Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>").matcher(value).find();
    }
}
