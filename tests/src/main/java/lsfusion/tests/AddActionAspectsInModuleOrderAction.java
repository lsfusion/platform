package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

// two modules that do not require each other adding BEFORE and AFTER to one action, the later one in the module order
// adding first, as the parse threads' timing can have it : the earlier one's aspect runs first, before and after alike.
// A started action keeps its aspects final, so the one added to is made here. What the test saw goes into aaOrderFailure
public class AddActionAspectsInModuleOrderAction extends AtOnceAction {

    public AddActionAspectsInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);
            ActionMapImplement earlier = new ActionMapImplement<>(findAction("aaEarlier[]").action, MapFact.EMPTYREV());
            ActionMapImplement later = new ActionMapImplement<>(findAction("aaLater[]").action, MapFact.EMPTYREV());

            Action action = PropertyFact.createListAction(SetFact.EMPTY(), ListFact.EMPTY()).action;
            action.addBeforeAspect(later, modules[1].getVersion());
            action.addAfterAspect(later, modules[1].getVersion());
            action.addBeforeAspect(earlier, modules[0].getVersion());
            action.addAfterAspect(earlier, modules[0].getVersion());
            action.finalizeAroundInit();

            String failure = check("BEFORE", action.getBeforeAspects(), earlier, later, modules);
            if (failure == null)
                failure = check("AFTER", action.getAfterAspects(), earlier, later, modules);
            findProperty("aaOrderFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String check(String kind, ImCol<?> aspects, ActionMapImplement earlier, ActionMapImplement later, LogicsModule[] modules) {
        if (aspects.size() == 2 && aspects.toList().get(0) == earlier && aspects.toList().get(1) == later)
            return null;
        return "the action runs its " + kind + " aspects in the order " + aspects + ", not " + modules[0].getName() + "'s first, earlier in the module order than "
                + modules[1].getName() + " that added last";
    }
}
