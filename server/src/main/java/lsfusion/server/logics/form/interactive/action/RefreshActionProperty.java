package lsfusion.server.logics.form.interactive.action;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

public class RefreshActionProperty extends FormFlowActionProperty {

    public RefreshActionProperty(BaseLogicsModule lm) {
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
        form.refreshData();
    }
}
