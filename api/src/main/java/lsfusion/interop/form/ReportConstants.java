package lsfusion.interop.form;

public class ReportConstants {
    public static final String sourceSuffix = "_source";
    public static final String reportSuffix = "_report";
    public static final String paramsSuffix = "_params";

    public static final String objectSuffix = ".object";
    public static final String headerSuffix = ".header";
    public static final String footerSuffix = ".footer";
    public static final String showIfSuffix = ".showif";

    public static final String beginIndexMarker = "[";
    public static final String endIndexMarker = "]";

    public static boolean isHeaderFieldName(String name) {
        return name != null && removeIndexMarkerIfExists(name).endsWith(headerSuffix);
    }

    public static boolean isFooterFieldName(String name) {
        return name != null && removeIndexMarkerIfExists(name).endsWith(footerSuffix);
    }

    public static boolean isShowIfFieldName(String name) {
        return name != null && removeIndexMarkerIfExists(name).endsWith(showIfSuffix);
    }

    public static String removeIndexMarkerIfExists(String name) {
        if (name.matches(".*\\[\\d+]$")) {
            return name.substring(0, name.lastIndexOf('['));
        }
        return name;
    }


}
 