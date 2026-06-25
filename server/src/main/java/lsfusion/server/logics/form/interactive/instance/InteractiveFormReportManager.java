package lsfusion.server.logics.form.interactive.instance;

import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.server.logics.form.stat.print.FormReportManager;
import lsfusion.server.logics.form.stat.print.InteractiveFormReportInterface;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;

public class InteractiveFormReportManager extends FormReportManager {

    public InteractiveFormReportManager(FormInstance form, Integer groupId, FormUserPreferences preferences) {
        this(form, groupId, null, preferences);
    }

    public InteractiveFormReportManager(FormInstance form, Integer groupId, TreeGroupEntity treeGroup, FormUserPreferences preferences) {
        super(new InteractiveFormReportInterface(form, groupId, treeGroup, preferences));
    }
}
