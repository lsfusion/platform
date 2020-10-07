package lsfusion.client.base;

public class EscapeUtils {

    public static String escapeLineBreakHTML(String value) {
        return value.replace("\n", "<br/>");
    }

}