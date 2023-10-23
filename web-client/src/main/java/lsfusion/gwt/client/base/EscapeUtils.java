package lsfusion.gwt.client.base;

public class EscapeUtils {
    public static final String UNICODE_NBSP = "\u00A0";
    public static final String UNICODE_BULLET = "\u2022";

    public static String toHtml(String plainString) {
        return plainString == null ? "" : HtmlSanitizerUtil.sanitizeHtml(plainString).asString().replaceAll("(\r\n|\n\r|\r|\n)", "<br/>");
    }
}
