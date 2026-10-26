package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
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
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

// the rows the groups of the form the action runs in have now. The form reads them the way it reads them before it
// answers its client - a form opened with no client never does it by itself - and each object of a row's own group is
// marked in shownInForm: of a tree's row, only the object of its own level, not the ones of the rows above it. A row of
// a tree the client can open, as the form says when it sends it, is marked so in expandableInForm
public class ReadShownAction extends InternalAction {

    public ReadShownAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        LP<?> shown;
        LP<?> expandable;
        try {
            shown = findProperty("shownInForm[Object]");
            expandable = findProperty("expandableInForm[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

        FormInstance form = context.getFormInstance(false, true);
        FormChanges changes = form.getChanges(context.stack, form.context);
        for (GroupObjectInstance group : form.getGroups())
            for (ImMap<ObjectInstance, DataObject> row : group.keys.keyIt())
                for (ObjectInstance object : group.objects)
                    shown.change(true, context, row.get(object));

        ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Long>> expandables = (ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Long>>) ReflectionUtils.getPrivateFieldValue(changes, "expandables");
        for (int i = 0; i < expandables.size(); i++) {
            GroupObjectInstance group = expandables.getKey(i);
            ImMap<ImMap<ObjectInstance, DataObject>, Long> rows = expandables.getValue(i);
            for (int j = 0; j < rows.size(); j++)
                if (rows.getValue(j) > 0)
                    for (ObjectInstance object : group.objects)
                        expandable.change(true, context, rows.getKey(j).get(object));
        }
    }
}
