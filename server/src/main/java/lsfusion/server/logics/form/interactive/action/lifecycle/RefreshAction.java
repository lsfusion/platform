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

public class RefreshAction extends FormFlowAction {

    private static LP showIf = createIfProperty(new Property[]{FormEntity.isEditing}, new boolean[]{true});

    public RefreshAction(BaseLogicsModule lm) {
        super(lm, false);
    }

    // так как вызывают откуда попало
    @Override
    protected boolean isSameSession() { // context не используется
        return false;
    }

    // так как вызывают откуда попало
    @Override
    protected boolean isAssertExists() {
        return false;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formRefresh();
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }
}
