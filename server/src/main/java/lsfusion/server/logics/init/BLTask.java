package lsfusion.server.logics.init;

import lsfusion.server.base.task.PublicTask;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.physics.exec.DBManager;

import java.sql.SQLException;

public abstract class BLTask extends PublicTask {

    private BusinessLogics BL;

    public BusinessLogics getBL() {
        return BL;
    }

    public void setBL(BusinessLogics BL) {
        this.BL = BL;
    }

    protected DBManager getDbManager() {
        return getBL().getDbManager();
    }

    protected DataSession createSession() throws SQLException {
        return getDbManager().createSession();
    }
}
