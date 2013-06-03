package lsfusion.interop.form;

import java.io.Serializable;
import java.util.Map;

public class GroupObjectUserPreferences implements Serializable {
    private Map<String, ColumnUserPreferences> columnUserPreferences;
    public String groupObjectSID;
    public boolean hasUserPreferences;

    public GroupObjectUserPreferences(Map<String, ColumnUserPreferences> columnUserPreferences,
                                      String groupObjectSID, boolean hasUserPreferences) {
        this.columnUserPreferences = columnUserPreferences;
        this.groupObjectSID = groupObjectSID;
        this.hasUserPreferences = hasUserPreferences;
    }

    public Map<String, ColumnUserPreferences> getColumnUserPreferences() {
        return columnUserPreferences;
    }
}
