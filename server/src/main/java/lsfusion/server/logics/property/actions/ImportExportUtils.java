package lsfusion.server.logics.property.actions;

public class ImportExportUtils {

    public static String getPropertyTag(String sid) {
        return sid.contains("(") ? sid.substring(0, sid.indexOf("(")) : sid;
    }
}