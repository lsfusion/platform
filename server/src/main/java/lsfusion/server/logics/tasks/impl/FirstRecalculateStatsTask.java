package lsfusion.server.logics.tasks.impl;

import com.google.common.base.Throwables;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.tasks.ReflectionTask;
import lsfusion.server.logics.tasks.SimpleBLTask;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static lsfusion.server.context.ThreadLocalContext.createSession;

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