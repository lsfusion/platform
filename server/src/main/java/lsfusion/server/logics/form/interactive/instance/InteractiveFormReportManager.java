package lsfusion.server.logics.form.interactive.instance;

import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.server.logics.form.stat.print.FormReportManager;
import lsfusion.server.logics.form.stat.print.InteractiveFormReportInterface;

public class InteractiveFormReportManager extends FormReportManager {

    public InteractiveFormReportManager(FormInstance form, Integer groupId, FormUserPreferences preferences) {
        super(new InteractiveFormReportInterface(form, groupId, preferences));
    }
}
