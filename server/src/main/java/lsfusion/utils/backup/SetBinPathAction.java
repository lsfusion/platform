package lsfusion.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.adapter.PostgreDataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class SetBinPathAction extends InternalAction {

    public SetBinPathAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            String binPath = ((String) findProperty("binPath[]").read(context));
                DataAdapter adapter = context.getDbManager().getAdapter();
                if(adapter instanceof PostgreDataAdapter) {
                    ((PostgreDataAdapter) adapter).setBinPath(binPath);
                }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}