package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class FormCancelAction extends FormFlowAction {
    private static LP showIf = createIfProperty(new Property[] {FormEntity.isManageSession, FormEntity.isAdd, FormEntity.isEditing}, new boolean[] {false, true, true});
    private static LP readOnlyIf = createDisableIfNotProperty(DataSession.isDataChanged);

    public FormCancelAction(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formCancel(context);
    }

    @Override
    protected LP getReadOnlyIf() {
        return readOnlyIf;
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.READONLYCHANGE)
            return true;
        if (type == ChangeFlowType.HASSESSIONUSAGES)
            return true;
        return super.hasFlow(type);
    }
}
