package lsfusion.gwt.form.shared.view;

import java.io.Serializable;
import java.util.Map;

public class GGroupObjectUserPreferences implements Serializable {
    private Map<String, GColumnUserPreferences> columnUserPreferences;
    private String groupObjectSID;
    private GFont font;
    private Integer pageSize;
    private Integer headerHeight;
    private  boolean hasUserPreferences;

    @SuppressWarnings("UnusedDeclaration")
    public GGroupObjectUserPreferences() {
    }

    public GGroupObjectUserPreferences(Map<String, GColumnUserPreferences> columnUserPreferences,
                                      String groupObjectSID, GFont font, Integer pageSize, Integer headerHeight, boolean hasUserPreferences) {
        this.columnUserPreferences = columnUserPreferences;
        this.groupObjectSID = groupObjectSID;
        this.font = font;
        this.pageSize = pageSize;
        this.headerHeight = headerHeight;
        this.hasUserPreferences = hasUserPreferences;
    }

    public Map<String, GColumnUserPreferences> getColumnUserPreferences() {
        return columnUserPreferences;
    }

    public String getGroupObjectSID() {
        return groupObjectSID;
    }

    public GFont getFont() {
        return font;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getHeaderHeight() {
        return headerHeight;
    }
    
    public boolean hasUserPreferences() {
        return hasUserPreferences;
    }
}
