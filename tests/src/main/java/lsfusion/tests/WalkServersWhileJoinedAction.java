package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

// the database servers walked, as a connection taken walks them for the least loaded one, while a server joins in
// another thread, as a replica does : the walk stops at its first server until the join is done. The server joining is
// the master once more, taken out afterwards. What the test saw goes into rsFailure
public class WalkServersWhileJoinedAction extends InternalAction {

    public WalkServersWhileJoinedAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataAdapter adapter = context.getDbManager().getAdapter();
            Field field = DataAdapter.class.getDeclaredField("servers");
            field.setAccessible(true);
            List<DataAdapter.Server> servers = (List<DataAdapter.Server>) field.get(adapter);
            DataAdapter.Server joining = adapter.getMaster();

            ExecutorService thread = ExecutorFactory.createNewThreadService((ExecutionContext) context, 1, false);
            String failure = null;
            AtomicBoolean joined = new AtomicBoolean();
            try {
                adapter.runOnAllServers(server -> {
                    if (!joined.getAndSet(true))
                        join(thread, () -> servers.add(joining));
                });
            } catch (ConcurrentModificationException e) {
                failure = "the walk over the servers failed as a server joined : " + e;
            } finally {
                if (joined.get())
                    servers.remove(servers.lastIndexOf(joining));
                thread.shutdownNow();
            }
            if (failure == null && !joined.get())
                failure = "the walk met no server";

            findProperty("rsFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static void join(ExecutorService thread, Runnable join) {
        try {
            thread.submit(join).get(1, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw Throwables.propagate(e);
        }
    }
}
