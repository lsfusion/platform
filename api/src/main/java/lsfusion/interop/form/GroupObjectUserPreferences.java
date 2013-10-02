package lsfusion.interop.form;

import lsfusion.interop.FontInfo;

import java.io.Serializable;
import java.util.Map;

public class GroupObjectUserPreferences implements Serializable {
    private Map<String, ColumnUserPreferences> columnUserPreferences;
    public String groupObjectSID;
    public FontInfo fontInfo;
    public boolean hasUserPreferences;

    public GroupObjectUserPreferences(Map<String, ColumnUserPreferences> columnUserPreferences, String groupObjectSID,
                                      FontInfo fontInfo, boolean hasUserPreferences) {
        this.columnUserPreferences = columnUserPreferences;
        this.groupObjectSID = groupObjectSID;
        this.fontInfo = fontInfo;
        this.hasUserPreferences = hasUserPreferences;
    }

    public Map<String, ColumnUserPreferences> getColumnUserPreferences() {
        return columnUserPreferences;
    }
}
