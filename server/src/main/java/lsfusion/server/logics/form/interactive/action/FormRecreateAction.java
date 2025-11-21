package lsfusion.server.logics.form.interactive.action;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
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

        Pair<FormEntity, ImRevMap<ObjectEntity, ObjectEntity>> resolvedForm = form.entity.getForm().getForm(context.getBL(), context.getSession(), MapFact.EMPTY());
        context.createAndRequestFormInstance(resolvedForm.first, form.inputObjects, MapFact.EMPTY(), form.checkOnOk, form.showDrop, form.options);
    }
}