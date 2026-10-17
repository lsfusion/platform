package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

// the changes the form the action runs in answers its client with, read the way the form reads them before it answers
// it - a form opened with no client never does it by itself -, and of each group the answer says it of, whether the
// form holds the group's changes back, marked in updatePendingInForm by the group's name. A group the answer says
// nothing of keeps the mark it had, as a client's update button and the group's dimmed rows do
public class ReadUpdatePendingAction extends InternalAction {

    public ReadUpdatePendingAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        LP<?> pending;
        try {
            pending = findProperty("updatePendingInForm[STRING]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

        FormInstance form = context.getFormInstance(false, true);
        FormChanges changes = form.getChanges(context.stack, form.context);
        for (GroupObjectInstance group : form.getGroups()) {
            Boolean updateState = changes.getUpdateState(group);
            if (updateState != null)
                pending.change(updateState ? true : null, context, new DataObject(group.getSID()));
        }
    }
}
