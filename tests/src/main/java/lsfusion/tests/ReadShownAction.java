package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

// the rows the groups of the form the action runs in have now. The form reads them the way it reads them before it
// answers its client - a form opened with no client never does it by itself - and each object of a row's own group is
// marked in shownInForm: of a tree's row, only the object of its own level, not the ones of the rows above it
public class ReadShownAction extends InternalAction {

    public ReadShownAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        LP<?> shown;
        try {
            shown = findProperty("shownInForm[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

        FormInstance form = context.getFormInstance(false, true);
        form.getChanges(context.stack, form.context);
        for (GroupObjectInstance group : form.getGroups())
            for (ImMap<ObjectInstance, DataObject> row : group.keys.keyIt())
                for (ObjectInstance object : group.objects)
                    shown.change(true, context, row.get(object));
    }
}
