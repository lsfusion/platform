package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.lang.reflect.Field;
import java.sql.SQLException;

// two threads adding BEFORE / AFTER to one action at once, as two modules parsed at once that add them to one action
// do. They are added on the action's own monitor, so the test holds it, as a first add would, and a second one has to
// wait. The action's lists - private fields, reached by reflection - are fresh for the time of the test and are put
// back afterwards. What the test saw goes into aaFailure
public class AddActionAspectsAtOnceAction extends AtOnceAction {

    public AddActionAspectsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Action action = findAction("aaDo[]").action;
            ActionMapImplement aspect = new ActionMapImplement<>(findAction("aaAspect[]").action, MapFact.EMPTYREV());
            String failure = add(context, action, "beforeAspects", () -> action.addBeforeAspect(aspect));
            if (failure == null)
                failure = add(context, action, "afterAspects", () -> action.addAfterAspect(aspect));

            findProperty("aaFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String add(ExecutionContext<ClassPropertyInterface> context, Action<?> action, String list, Runnable add) throws NoSuchFieldException, IllegalAccessException {
        Field field = Action.class.getDeclaredField(list);
        field.setAccessible(true);
        Object aspects = field.get(action);
        MCol<Object> fresh = ListFact.mCol();
        field.set(action, fresh);
        String failure;
        try {
            failure = runWhileHeld(context, action, "a thread added to the action's " + list + " while another one held the action", add);
        } finally {
            field.set(action, aspects);
        }
        return failure == null && fresh.size() != 1 ? "the action's " + list + " got " + fresh.size() + " aspects" : failure;
    }
}
