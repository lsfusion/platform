package lsfusion.server.logics.tasks.impl.simple;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.tasks.SimpleBLTask;
import lsfusion.server.logics.action.session.DataSession;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class WriteModulesHashTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Writing modules hash";
    }

    @Override
    public void run(Logger logger) {
        try(DataSession session = createSession()) {
            getBL().getDbManager().writeModulesHash(session);
        } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
