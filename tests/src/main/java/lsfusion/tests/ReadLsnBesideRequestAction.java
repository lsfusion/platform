package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.AssertSynchronizedAspect;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.navigator.controller.env.SQLSessionLSNProvider;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;

// the connection balancing reading the LSN of this action's connection while a request of the connection is running in
// another thread. A request marks the connection as used by its thread (@AssertSynchronized : the navigator's
// getChanges, updateLSN at a commit), so the test puts that mark for another thread - into the aspect's map, reached
// by reflection, as is the connection behind the session's LSN provider - and reads the LSN as the balancing does, through
// the provider. What the read ended with goes into clFailure
public class ReadLsnBesideRequestAction extends InternalAction {

    public ReadLsnBesideRequestAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String failure;
        try {
            SQLSessionLSNProvider provider = context.getSession().sql.lsnProvider;
            Field weakThis = provider.getClass().getDeclaredField("weakThis");
            weakThis.setAccessible(true);
            Object connection = ((WeakReference<?>) weakThis.get(provider)).get();
            Field map = AssertSynchronizedAspect.class.getDeclaredField("map");
            map.setAccessible(true);
            Map<Object, WeakReference<Thread>> inside = (Map<Object, WeakReference<Thread>>) map.get(null);

            Thread request = new Thread(); // the mark is the thread's identity alone
            inside.put(connection, new WeakReference<>(request));
            try {
                provider.getLSN();
                failure = null;
            } catch (AssertionError e) {
                failure = "reading the LSN beside a request of the connection failed: " + String.valueOf(e.getMessage()).split("\n")[0];
            } finally {
                inside.remove(connection);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }

        try {
            findProperty("clFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
