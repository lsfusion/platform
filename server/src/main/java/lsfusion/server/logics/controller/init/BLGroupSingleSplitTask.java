package lsfusion.server.logics.controller.init;

import lsfusion.server.base.task.GroupSingleSplitTask;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.TableFactory;

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
    
    protected DBNamingPolicy getDBNamingPolicy() {
        return getDbManager().getNamingPolicy();
    }

    protected TableFactory getTableFactory() {
        return getBL().LM.tableFactory;
    }

    protected DataSession createSession() throws SQLException {
        return getDbManager().createSession();
    }
}
