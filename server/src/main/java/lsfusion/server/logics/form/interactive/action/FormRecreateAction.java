package lsfusion.server.logics.form.interactive.action;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class FormRecreateAction extends InternalAction {

    public FormRecreateAction(BaseLogicsModule lm) {
        super(lm);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        FormInstance form = context.getFormInstance(true, true);
        form.formClose(context, true);

        Pair<FormEntity, ImRevMap<ObjectEntity, ObjectEntity>> resolvedForm = form.entity.getForm().getForm(context.getBL(), context.getSession(), form.options.mapObjects);
        FormEntity formEntity = resolvedForm.first;
        FormInstance newFormInstance = context.createFormInstance(formEntity, form.inputObjects, form.options.mapObjects, context.getSession(),
                form.isModal(), form.options.noCancel, form.options.manageSession, form.checkOnOk, form.showDrop,
                true, form.options.type.getWindowType(), form.options.contextFilters, form.options.showReadonly, form.options);
        context.requestFormUserInteraction(newFormInstance, form.options.type, form.options.forbidDuplicate, form.options.syncType, form.options.formId);

        context.getSession().navigator.refresh();
    }
}