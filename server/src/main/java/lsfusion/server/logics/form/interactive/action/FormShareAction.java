package lsfusion.server.logics.form.interactive.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.lifecycle.FormFlowAction;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class FormShareAction extends FormFlowAction {

    private static LP showIf = createIfProperty(new Property[]{FormEntity.isAdd, FormEntity.isEditing}, new boolean[]{true, true});

    public FormShareAction(BaseLogicsModule LM) {
        super(LM);
    }

    @Override
    protected boolean isSameSession() {
        return false;
    }

    @Override
    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.instanceFactory.getInstance(form.entity.getShareAction()).execute(context.getEnv(), context.stack, null, null, form);
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }
}
