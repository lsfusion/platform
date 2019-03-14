package lsfusion.server.physics.admin.authentication;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;

public class CheckIsServerActionProperty extends ScriptingActionProperty {
    BaseLogicsModule baseLM;
    public CheckIsServerActionProperty(BaseLogicsModule LM) {
        super(LM);
        this.baseLM = LM;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        baseLM.isServer.change(context.getDbManager().isServer() ? true : null, context);
    }
}