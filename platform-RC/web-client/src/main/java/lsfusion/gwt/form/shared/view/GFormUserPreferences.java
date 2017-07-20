package lsfusion.gwt.form.shared.view;

import java.io.Serializable;
import java.util.List;

public class GFormUserPreferences implements Serializable {
    private List<GGroupObjectUserPreferences> groupObjectGeneralPreferencesList;
    private List<GGroupObjectUserPreferences> groupObjectUserPreferencesList;

    @SuppressWarnings("UnusedDeclaration")
    public GFormUserPreferences() {
    }

    public GFormUserPreferences(List<GGroupObjectUserPreferences> groupObjectGeneralPreferencesList, List<GGroupObjectUserPreferences> groupObjectUserPreferencesList) {
        this.groupObjectGeneralPreferencesList = groupObjectGeneralPreferencesList;
        this.groupObjectUserPreferencesList = groupObjectUserPreferencesList;
    }

    public List<GGroupObjectUserPreferences> getGroupObjectGeneralPreferencesList() {
        return groupObjectGeneralPreferencesList;
    }

    public List<GGroupObjectUserPreferences> getGroupObjectUserPreferencesList() {
        return groupObjectUserPreferencesList;
    }
}
