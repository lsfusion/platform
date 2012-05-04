package platform.interop.form;

import java.util.Map;
import java.io.Serializable;

public class FormUserPreferences implements Serializable {
    private Map<String, FormColumnUserPreferences> formColumnUserPreferences;

    public FormUserPreferences(Map<String, FormColumnUserPreferences> formColumnUserPreferences) {
        this.formColumnUserPreferences = formColumnUserPreferences;
    }

    public Map<String, FormColumnUserPreferences> getFormColumnUserPreferences() {
        return formColumnUserPreferences;
    }
}
