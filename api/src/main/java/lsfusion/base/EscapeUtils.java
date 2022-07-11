package lsfusion.base;

public class EscapeUtils {

    public static String escapeLineBreakHTML(String value) {
        return isContainHtmlTag(value) ? value.replace("\n", "<br/>") : value;
    }

    public static boolean isContainHtmlTag(String value) {
        return value.matches(".*\\<[^>]+>.*");
    }
}
