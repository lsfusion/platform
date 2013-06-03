package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.shared.view.GFormUserPreferences;

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
