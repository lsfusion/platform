package platform.gwt.form.shared.view;

import java.io.Serializable;
import java.util.List;

public class GFormUserPreferences implements Serializable {
    private List<GGroupObjectUserPreferences> groupObjectUserPreferencesList;

    @SuppressWarnings("UnusedDeclaration")
    public GFormUserPreferences() {
    }

    public GFormUserPreferences(List<GGroupObjectUserPreferences> groupObjectUserPreferencesList) {
        this.groupObjectUserPreferencesList = groupObjectUserPreferencesList;
    }

    public List<GGroupObjectUserPreferences> getGroupObjectUserPreferencesList() {
        return groupObjectUserPreferencesList;
    }
}
