package lsfusion.gwt.client.base;

public class EscapeUtils {
    public static final String UNICODE_NBSP = "\u00A0";
    public static final String UNICODE_BULLET = "\u2022";

    public static String toHtml(String plainString) {
        //todo isContainHtmlTag работает странно. надо переделать regex
        return plainString == null ? "" : GwtClientUtils.isContainHtmlTag(plainString) ? plainString : escapeLineBreakHTML(plainString);
    }

    private static String escapeLineBreakHTML(String value) {
        return value.replaceAll("(\r\n|\n\r|\r|\n)", "<br/>");
    }
}
