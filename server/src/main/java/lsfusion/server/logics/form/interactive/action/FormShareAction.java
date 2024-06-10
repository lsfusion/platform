package lsfusion.server.logics.form.interactive.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class FormShareAction extends InternalAction {

    public FormShareAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance form = context.getFormInstance(false, true);
        form.instanceFactory.getInstance(form.entity.getShareAction()).execute(context.getEnv(), context.stack, null, null, form);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type.isChange())
            return false;
        return super.hasFlow(type);
    }
}
