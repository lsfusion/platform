package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class DropAction extends FormFlowAction {
    private static LP showIf = createIfProperty(new Property[] {FormEntity.showDrop}, new boolean[] {false});

    public DropAction(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formDrop(context);
    }

    //because it executes from anywhere
    //potential problems with events on drop
    @Override
    protected boolean isSameSession() {
        return false;
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }
}
