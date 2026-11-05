package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.interop.navigator.NavigatorScheduler;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// two threads scheduling actions for the navigator at once, as two modules parsed at once with NAVIGATOR { SCHEDULE
// ... } do. They put them into the base module's map on the base module's monitor, so the test holds it, as a first
// put would, and a second one has to wait. The map is a fresh one for the time of the test and is put back afterwards.
// What the test saw goes into nsFailure
public class AddNavigatorSchedulersAtOnceAction extends AtOnceAction {

    public AddNavigatorSchedulersAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            BaseLogicsModule baseLM = context.getBL().LM;
            LA<?> tick = findAction("nsTick[]");
            Map<NavigatorScheduler, LA> schedulers = baseLM.navigatorSchedulers;
            Map<NavigatorScheduler, LA> fresh = new HashMap<>();
            baseLM.navigatorSchedulers = fresh;
            String failure;
            try {
                failure = runWhileHeld(context, baseLM, "a thread scheduled an action while another one held the base module",
                        () -> LM.addNavigatorScheduler(tick, new NavigatorScheduler(60, false)));
            } finally {
                baseLM.navigatorSchedulers = schedulers;
            }
            if (failure == null && fresh.size() != 1)
                failure = "the navigator got " + fresh.size() + " schedulers";

            findProperty("nsFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
