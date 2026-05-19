package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;

public class TreeGroupReport extends FormRequestCountingAction<GroupReportResult> {
    public int groupObjectID;
    public GFormUserPreferences preferences;

    @SuppressWarnings({"UnusedDeclaration"})
    public TreeGroupReport() {
    }

    public TreeGroupReport(int groupObjectID, GFormUserPreferences preferences) {
        this.groupObjectID = groupObjectID;
        this.preferences = preferences;
    }
}
