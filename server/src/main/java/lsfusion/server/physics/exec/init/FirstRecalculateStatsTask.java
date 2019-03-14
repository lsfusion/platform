package lsfusion.server.physics.exec.init;

import com.google.common.base.Throwables;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.init.SimpleBLTask;
import lsfusion.server.logics.action.session.DataSession;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class FirstRecalculateStatsTask extends SimpleBLTask {

    public String getCaption() {
        return "Recalculating Stats at first start";
    }

    @Override
    public void run(Logger logger) {
        if(!SystemProperties.lightStart)
            try(DataSession session = createSession()) {
                getDbManager().firstRecalculateStats(session);
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
    }
}