package lsfusion.interop.form;

import java.util.List;
import java.io.Serializable;

public class FormUserPreferences implements Serializable {
    private List<GroupObjectUserPreferences> groupObjectUserPreferencesList;
    
    public FormUserPreferences(List<GroupObjectUserPreferences> groupObjectUserPreferencesList) {
       this.groupObjectUserPreferencesList = groupObjectUserPreferencesList;
    }

    public List<GroupObjectUserPreferences> getGroupObjectUserPreferencesList() {
        return groupObjectUserPreferencesList;
    }
}
