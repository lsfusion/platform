package lsfusion.server.logics.property.actions.form;

import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public abstract class FormFlowActionProperty extends FormToolbarActionProperty {

    public FormFlowActionProperty(ScriptingLogicsModule lm) {
        super(lm);
    }

    public FormFlowActionProperty(ScriptingLogicsModule lm, boolean showCaption) {
        super(lm, showCaption);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance<?> formInstance = context.getFormFlowInstance();
        if(formInstance != null)
            executeForm(formInstance, context);
    }

    protected abstract void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException;
}
