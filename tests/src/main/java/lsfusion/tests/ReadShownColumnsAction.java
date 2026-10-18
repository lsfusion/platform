package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

// the columns of the groups of the form the action runs in - their list properties - the form takes as shown, read the
// way the form reads its answer to its client - a form opened with no client never does it by itself -: what it sends
// the client the values of, and the drop of once it stops showing one. Each property of the form is marked in
// shownColumnInForm by its name in the form
public class ReadShownColumnsAction extends InternalAction {

    public ReadShownColumnsAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        LP<?> shown;
        try {
            shown = findProperty("shownColumnInForm[STRING]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

        FormInstance form = context.getFormInstance(false, true);
        form.getChanges(context.stack, form.context);
        Set<PropertyDrawEntity> columns = new HashSet<>();
        for (GroupObjectInstance group : form.getGroups())
            for (PropertyDrawEntity column : form.getPropertyEntitiesShownInGroup(group))
                columns.add(column);
        for (PropertyDrawInstance<?> property : form.properties)
            shown.change(columns.contains(property.entity) ? true : null, context, new DataObject(property.getSID()));
    }
}
