package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.navigator.NavigatorScheduler;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.lang.reflect.Field;
import java.sql.SQLException;

// two modules that do not require each other scheduling an action for the navigator with one period, the later one in
// the module order scheduling first, as the parse threads' timing can have it : the period runs both, the earlier one's
// first. The server's own schedulers are final once a navigator has read them, so the test swaps them for a fresh NF
// list, reached by reflection, and puts the original back. What the test saw goes into nsFailure
public class AddNavigatorSchedulersInModuleOrderAction extends AtOnceAction {

    public AddNavigatorSchedulersInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);
            LA<?> earlier = findAction("nsEarlier[]");
            LA<?> later = findAction("nsLater[]");
            BaseLogicsModule baseLM = context.getBL().LM;
            Field field = BaseLogicsModule.class.getDeclaredField("navigatorSchedulers");
            field.setAccessible(true);
            Object original = field.get(baseLM);

            String failure = null;
            field.set(baseLM, NFFact.list());
            try {
                baseLM.addNavigatorScheduler(new NavigatorScheduler(60, false), later, modules[1].getVersion());
                baseLM.addNavigatorScheduler(new NavigatorScheduler(60, false), earlier, modules[0].getVersion());
                ImList<LA> actions = baseLM.getNavigatorSchedulers().get(new NavigatorScheduler(60, false));
                if (actions == null || actions.size() != 2 || actions.get(0) != earlier || actions.get(1) != later)
                    failure = "the period runs " + (actions != null ? actions.mapListValues(action -> action.action.getCanonicalName()) : "nothing")
                            + ", not both actions, " + modules[0].getName() + "'s first, earlier in the module order than "
                            + modules[1].getName() + " that scheduled first";
            } finally {
                field.set(baseLM, original);
            }

            findProperty("nsFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
