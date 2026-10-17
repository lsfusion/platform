package lsfusion.tests;

import lsfusion.base.col.SetFact;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupMode;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

// a list group of the form the action runs in, by its name, switched to one of its views (GRID, PIVOT, ...), as the
// switch on a client's toolbar asks the server for it (GPlatformGridController.changeListView -> RemoteForm.changeMode),
// the page size of the view aside: the group read again whole, grouped by nothing yet for a pivot - which holds its
// changes back until it has set its own config, so it starts in a manual update -, and not grouped for any other view
public class ChangeListViewAction extends InternalAction {

    public ChangeListViewAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String groupSID = (String) getParam(0, context);
        ListViewType view = ListViewType.valueOf((String) getParam(1, context));

        FormInstance form = context.getFormInstance(false, true);
        for (GroupObjectInstance group : form.getGroups())
            if (group.getSID().equals(groupSID)) {
                boolean pivot = view == ListViewType.PIVOT;
                group.changeGroupMode(pivot ? GroupMode.create(SetFact.EMPTY(), SetFact.EMPTY(), null, form.instanceFactory) : null);
                group.forceRefresh();
                if (pivot)
                    group.setUpdateMode(UpdateMode.MANUAL);
                form.changeListViewType(group, view);
                return;
            }
        throw new RuntimeException("the form has no group " + groupSID);
    }
}
