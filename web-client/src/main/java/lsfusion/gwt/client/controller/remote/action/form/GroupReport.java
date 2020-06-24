package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;

public class GroupReport extends FormRequestCountingAction<GroupReportResult> {
    public Integer groupObjectID;
    public GFormUserPreferences preferences;

    @SuppressWarnings({"UnusedDeclaration"})
    public GroupReport() {
    }

    public GroupReport(Integer groupObjectID, GFormUserPreferences preferences) {
        this.groupObjectID = groupObjectID;
        this.preferences = preferences;
    }
}
