package lsfusion.server.physics.exec.db.controller.init;

import com.google.common.base.Throwables;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class SynchronizeDBTask extends SimpleBLTask {

    public String getCaption() {
        return "Synchronizing DB";
    }

    public void run(Logger logger) {
        try {
            getDbManager().synchronizeDB();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
