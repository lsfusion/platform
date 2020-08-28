package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyMapImplement;

import java.sql.SQLException;

public abstract class FormFlowAction extends FormToolbarAction {

    public FormFlowAction(ScriptingLogicsModule lm) {
        super(lm);
    }

    public FormFlowAction(ScriptingLogicsModule lm, boolean showCaption) {
        super(lm, showCaption);
    }

    protected boolean isSameSession() {
        return true;
    }
    protected boolean isAssertExists() {
        return true;
    }
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
//        Property enableIf = getEnableIf();
//        if(enableIf != null && enableIf.read(context) == null)
//            return;
        FormInstance formInstance = context.getFormFlowInstance(isAssertExists(), isSameSession());
        if(formInstance != null)
            executeForm(formInstance, context);
    }

    protected abstract void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException;
}
