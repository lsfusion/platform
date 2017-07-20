package lsfusion.server.logics.property.actions;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.ApplyFilter;

import java.sql.SQLException;

public class ApplyFilterProperty extends ScriptingActionProperty {

    private final ApplyFilter type;

    public ApplyFilterProperty(BaseLogicsModule lm, ApplyFilter type) {
        super(lm);
        this.type = type;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getSession().setApplyFilter(type);
    }
}
