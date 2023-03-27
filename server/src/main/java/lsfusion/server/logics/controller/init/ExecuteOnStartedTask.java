package lsfusion.server.logics.controller.init;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ExecuteOnStartedTask extends ExecuteActionTask {

    @Override
    public String getCaption() {
        return "Executing onStarted action";
    }

    @Override
    protected LA getLA(BusinessLogics BL) {
        return BL.systemEventsLM.onStarted;
    }
}
