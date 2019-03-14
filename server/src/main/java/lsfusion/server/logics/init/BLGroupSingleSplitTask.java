package lsfusion.server.logics.init;

import lsfusion.server.base.task.GroupSingleSplitTask;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.physics.exec.DBManager;
import lsfusion.server.logics.action.session.DataSession;

import java.sql.SQLException;

public abstract class BLGroupSingleSplitTask<T> extends GroupSingleSplitTask<T> {
    
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
