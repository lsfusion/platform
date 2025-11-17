package lsfusion.server.logics.form.interactive.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.action.lifecycle.FormFlowAction;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

public class FormCustomizeAction extends FormFlowAction {

    private static LP showIf = createIfProperty(new Property[]{FormEntity.isAdd, FormEntity.isEditing}, new boolean[]{true, true});

    public FormCustomizeAction(BaseLogicsModule LM) {
        super(LM);
    }

    @Override
    protected boolean isSameSession() {
        return false;
    }

    @Override
    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormEntity formEntity = nvl(form.entity.getOriginalForm(), form.entity);
        context.getBL().systemEventsLM.customize.execute(context, new DataObject(formEntity.getSID()), new DataObject(formEntity.getCode()));
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }
}
