package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

// what the form the action runs in sends its client for a property, by its name in the form: the value of the cell the
// client is sent for the form's current objects (PropertyDrawInstance.getReaderProperty) - where the form builds a
// selection for the property, the list of its options. It is written into sentInForm, as text
public class ReadSentAction extends InternalAction {

    public ReadSentAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String name = (String) getParam(0, context);
        FormInstance form = context.getFormInstance(false, true);
        Object value = form.getPropertyDraw(name).getReaderProperty().read(form);
        try {
            findProperty("sentInForm[STRING]").change(value != null ? value.toString() : null, context, new DataObject(name));
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
