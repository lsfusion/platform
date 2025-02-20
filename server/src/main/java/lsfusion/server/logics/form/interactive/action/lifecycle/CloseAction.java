package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapCloseForm;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class CloseAction extends FormFlowAction {

    private static LP showIf = createIfProperty(new Property[]{FormEntity.isEditing}, new boolean[]{true});

    public CloseAction(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formClose(context);
    }

    @Override
    protected AsyncMapEventExec<ClassPropertyInterface> calculateAsyncEventExec(boolean optimistic, ImSet<Action<?>> recursiveAbstracts) {
        return new AsyncMapCloseForm<>();
    }

    //because it executes from anywhere
    //potential problems with events on close
    @Override
    protected boolean isSameSession() {
        return false;
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }

    @Override
    protected String getValueElementClass() {
        return "btn-secondary";
    }
}
