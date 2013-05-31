package platform.gwt.form.shared.actions.form;

import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.form.shared.view.GFormUserPreferences;

public class SaveUserPreferencesAction extends FormRequestIndexCountingAction<VoidResult> {
    public GFormUserPreferences formUserPreferences;
    public boolean forAllUsers;

    @SuppressWarnings("Unused")
    public SaveUserPreferencesAction() {
    }

    public SaveUserPreferencesAction(GFormUserPreferences userPreferences, boolean forAllUsers) {
        this.formUserPreferences = userPreferences;
        this.forAllUsers = forAllUsers;
    }
}
