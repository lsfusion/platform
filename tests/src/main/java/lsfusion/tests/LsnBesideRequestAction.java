package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.AssertSynchronizedAspect;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.navigator.controller.env.SQLSessionLSNProvider;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;

// the LSN of this action's connection read or written while a request of the connection is running in another thread.
// A request marks the connection as used by its thread (@AssertSynchronized : the navigator's getChanges), so the test
// puts that mark for another thread - into the aspect's map, reached by reflection, as is the connection behind the
// session's LSN provider -, and then, by the parameter: `read` reads the LSN as the connection balancing does, through
// the provider; `commit` commits a change in a session of the connection, which writes the LSN, as the commit of another
// request of the navigator does. What it ended with goes into clFailure
public class LsnBesideRequestAction extends InternalAction {

    public LsnBesideRequestAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        boolean read = getParam(0, context).equals("read");
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
                if (read)
                    provider.getLSN();
                else
                    try (DataSession session = context.getSession().createSession()) {
                        findProperty("clCommitted[]").change(System.nanoTime(), session);
                        session.applyException(context.getBL(), context.stack);
                    }
                failure = null;
            } catch (AssertionError e) {
                failure = (read ? "reading the LSN" : "a commit") + " beside a request of the connection failed: " + String.valueOf(e.getMessage()).split("\n")[0];
            } finally {
                inside.remove(connection);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

        try {
            findProperty("clFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
