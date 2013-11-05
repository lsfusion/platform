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
    
    public GroupObjectUserPreferences getGeneralPreferences(String groupObjectSID) {
        for (GroupObjectUserPreferences groupPrefs : groupObjectGeneralPreferencesList) {
            if (groupObjectSID.equals(groupPrefs.groupObjectSID)) {
                return groupPrefs;
            }
        }
        return null;
    }

    public GroupObjectUserPreferences getUserPreferences(String groupObjectSID) {
        for (GroupObjectUserPreferences groupPrefs : groupObjectUserPreferencesList) {
            if (groupObjectSID.equals(groupPrefs.groupObjectSID)) {
                return groupPrefs;
            }
        }
        return null;
    }
    
    public GroupObjectUserPreferences getUsedPreferences(String groupObjectSID) {
        GroupObjectUserPreferences groupPrefs = getUserPreferences(groupObjectSID);
        if (groupPrefs == null || !groupPrefs.hasUserPreferences) {
            groupPrefs = getGeneralPreferences(groupObjectSID);
        }
        if (groupPrefs == null || !groupPrefs.hasUserPreferences) {
            return null;
        }
        return groupPrefs;
    }
}
