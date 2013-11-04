package lsfusion.interop.form;

import java.io.Serializable;
import java.util.List;

public class FormUserPreferences implements Serializable {
    private List<GroupObjectUserPreferences> groupObjectGeneralPreferencesList;
    private List<GroupObjectUserPreferences> groupObjectUserPreferencesList;
    
    public FormUserPreferences(List<GroupObjectUserPreferences> groupObjectGeneralPreferencesList, List<GroupObjectUserPreferences> groupObjectUserPreferencesList) {
        this.groupObjectGeneralPreferencesList = groupObjectGeneralPreferencesList;
        this.groupObjectUserPreferencesList = groupObjectUserPreferencesList;
    }

    public List<GroupObjectUserPreferences> getGroupObjectUserPreferencesList() {
        return groupObjectUserPreferencesList;
    }
    
    public List<GroupObjectUserPreferences> getGroupObjectGeneralPreferencesList() {
        return groupObjectGeneralPreferencesList;
    }
}
