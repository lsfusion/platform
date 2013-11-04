package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.shared.view.GGroupObjectUserPreferences;

public class SaveUserPreferencesAction extends FormRequestIndexCountingAction<VoidResult> {
    public GGroupObjectUserPreferences groupObjectUserPreferences;
    public boolean forAllUsers;

    @SuppressWarnings("Unused")
    public SaveUserPreferencesAction() {
    }

    public SaveUserPreferencesAction(GGroupObjectUserPreferences userPreferences, boolean forAllUsers) {
        this.groupObjectUserPreferences = userPreferences;
        this.forAllUsers = forAllUsers;
    }
}
