package lsfusion.base;

import java.util.regex.Pattern;

public class EscapeUtils {

    public static String escapeLineBreakHTML(String value) {
        return value.replace("\n", "<br/>");
    }

    public static boolean isContainHtmlTag(String value) {
        return Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>").matcher(value).find();
    }
}
