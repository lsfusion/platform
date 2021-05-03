package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.interop.action.LoadLinkClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class LoadLinkAction extends InternalAction {

    public LoadLinkAction(BaseLogicsModule LM, ValueClass...  classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getBL().LM.baseLM.networkPath.change((String)context.requestUserInteraction(new LoadLinkClientAction(getParam(0, context) != null)), context);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}