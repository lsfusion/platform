package lsfusion.server.physics.exec.db.controller.init;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.physics.admin.SystemProperties;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class FirstRecalculateStatsAndMaterializationsTask extends SimpleBLTask {

    public String getCaption() {
        return "Recalculating Stats and Materializations at first start";
    }

    @Override
    public boolean isStartLoggable() {
        return isEnabled();
    }

    @Override
    public void run(Logger logger) {
        if (isEnabled()) {
            try (DataSession session = createSession()) {
                getDbManager().firstRecalculateStatsAndMaterializations(session);
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private boolean isEnabled() {
        return !SystemProperties.lightStart;
    }
}