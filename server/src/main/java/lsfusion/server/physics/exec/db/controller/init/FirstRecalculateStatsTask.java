package lsfusion.server.physics.exec.db.controller.init;

import com.google.common.base.Throwables;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.controller.init.SimpleBLTask;
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