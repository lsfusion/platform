package lsfusion.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.PostgreDataAdapter;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class SetDumpDirActionProperty extends ScriptingActionProperty {

    public SetDumpDirActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            String dumpDir = ((String) findProperty("dumpDir[]").read(context));
                DataAdapter adapter = context.getDbManager().getAdapter();
                if(adapter instanceof PostgreDataAdapter) {
                    ((PostgreDataAdapter) adapter).setDumpDir(dumpDir);
                }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}