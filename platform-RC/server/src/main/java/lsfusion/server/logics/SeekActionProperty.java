package lsfusion.server.logics;

import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public abstract class SeekActionProperty extends SystemExplicitActionProperty {

    public SeekActionProperty(LocalizedString caption, ValueClass... classes) {
        super(caption, classes);
    }
    
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance<?> formInstance = context.getFormInstance(false, true);
        if(formInstance != null)
            executeForm(formInstance, context);
        else
            ServerLoggers.assertLog(false, "FORM ALWAYS SHOULD EXIST");
    }

    protected abstract void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException;
}
