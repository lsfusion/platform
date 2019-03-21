package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.form.object.table.grid.user.design.GFormUserPreferences;

public class GroupReport extends FormRequestIndexCountingAction<GroupReportResult> {
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
