package lsfusion.server.logics.controller.init;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class ExecuteActionTask extends SimpleBLTask {

    protected abstract LA getLA(BusinessLogics BL);

    @Override
    public void run(Logger logger) {
        LA la = getLA(getBL());
        if(la != null) {
            try (DataSession session = createSession()) {
                la.execute(session, ThreadLocalContext.getStack());
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
