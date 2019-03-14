package lsfusion.server.logics.action.session;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class ApplyFilterAction extends ScriptingAction {

    private final ApplyFilter type;

    public ApplyFilterAction(BaseLogicsModule lm, ApplyFilter type) {
        super(lm);
        this.type = type;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getSession().setApplyFilter(type);
    }
}
