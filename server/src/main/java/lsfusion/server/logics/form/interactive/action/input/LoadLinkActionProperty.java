package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.interop.action.LoadLinkClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class LoadLinkActionProperty extends ScriptingAction {

    public LoadLinkActionProperty(BaseLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getBL().LM.baseLM.networkPath.change((String)context.requestUserInteraction(new LoadLinkClientAction()), context);
    }
}