package lsfusion.gwt.shared.form.actions.form;

import lsfusion.gwt.shared.form.view.GFormUserPreferences;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class GroupReport extends FormRequestIndexCountingAction<StringResult> {
    public Integer groupObjectID;
    public boolean toExcel;
    public GFormUserPreferences preferences;

    @SuppressWarnings({"UnusedDeclaration"})
    public GroupReport() {
    }

    public GroupReport(Integer groupObjectID, boolean toExcel, GFormUserPreferences preferences) {
        this.groupObjectID = groupObjectID;
        this.toExcel = toExcel;
        this.preferences = preferences;
    }
}
