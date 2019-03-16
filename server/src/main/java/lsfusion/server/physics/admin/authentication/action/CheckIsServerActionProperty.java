package lsfusion.server.physics.admin.authentication.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;

import java.sql.SQLException;

public class CheckIsServerActionProperty extends ScriptingAction {
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