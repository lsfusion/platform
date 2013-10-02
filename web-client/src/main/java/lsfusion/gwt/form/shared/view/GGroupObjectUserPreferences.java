package lsfusion.gwt.form.shared.view;

import java.io.Serializable;
import java.util.Map;

public class GGroupObjectUserPreferences implements Serializable {
    private Map<String, GColumnUserPreferences> columnUserPreferences;
    private String groupObjectSID;
    private GFont fontInfo;
    private  boolean hasUserPreferences;

    @SuppressWarnings("UnusedDeclaration")
    public GGroupObjectUserPreferences() {
    }

    public GGroupObjectUserPreferences(Map<String, GColumnUserPreferences> columnUserPreferences,
                                      String groupObjectSID, GFont fontInfo, boolean hasUserPreferences) {
        this.columnUserPreferences = columnUserPreferences;
        this.groupObjectSID = groupObjectSID;
        this.fontInfo = fontInfo;
        this.hasUserPreferences = hasUserPreferences;
    }

    public Map<String, GColumnUserPreferences> getColumnUserPreferences() {
        return columnUserPreferences;
    }

    public String getGroupObjectSID() {
        return groupObjectSID;
    }

    public GFont getFontInfo() {
        return fontInfo;
    }
    
    public boolean hasUserPreferences() {
        return hasUserPreferences;
    }
}
