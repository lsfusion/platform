package lsfusion.interop.form.print;

public class ReportConstants {
    public static final String sourceSuffix = "_source";
    public static final String reportSuffix = "_report";
    public static final String paramsSuffix = "_params";

    public static final String objectSuffix = ".object";
    
    public static final String headerSuffix = ".header";
    public static final String footerSuffix = ".footer";
    public static final String showIfSuffix = ".showif";
    public static final String backgroundSuffix = ".background";
    public static final String foregroundSuffix = ".foreground";
    public static final String imageSuffix = ".image";

    public static final String beginIndexMarker = "[";
    public static final String endIndexMarker = "]";

    public static boolean isCorrespondingFieldName(String name, ReportFieldExtraType type) {
        return name != null && removeIndexMarkerIfExists(name).endsWith(type.getReportFieldNameSuffix());
    }
    
    public static String removeIndexMarkerIfExists(String name) {
        if (name.matches(".*\\[\\d+]$")) {
            return name.substring(0, name.lastIndexOf('['));
        }
        return name;
    }
}
 