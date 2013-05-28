package platform.interop.form;

import java.util.List;
import java.util.Map;
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
