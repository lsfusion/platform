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
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.IDTable;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

// two threads of the server asking for IDs, the second while the first reserves the next ones. The reservation is
// held up, so that it is still running when the second one asks: a transaction of this action's own, on a connection
// of its own, holds the counter's row, which a reservation updates. Once the second thread waits for the lock the
// reservation runs under, or is done, the row is let go. What either thread ended with goes into idgFailure, and so does
// a run that did not have the two at once - a reservation that never waited for the row, or a second thread that neither
// waited for it nor got its ID in time -, as its second thread would have asked while nothing was reserved
public class GenerateIDWhileReservingAction extends InternalAction {

    public GenerateIDWhileReservingAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DBManager dbManager = context.getDbManager();
        DataAdapter adapter = dbManager.getAdapter();
        long count = Settings.get().getReserveIDStep() + 1; // more than the pool ever holds, so the first thread reserves
        ExecutorService threads = ExecutorFactory.createNewThreadService((ExecutionContext) context, 2, false);
        AtomicReference<String> failure = new AtomicReference<>();
        try {
            adapter.runMaintenance(adapter.getMaster(), connection -> {
                connection.setAutoCommit(false);
                try {
                    failure.set(generateWhileReserving(dbManager, connection, threads, count));
                } catch (InterruptedException e) {
                    throw Throwables.propagate(e);
                } finally {
                    connection.rollback();
                }
            });
        } finally {
            threads.shutdown();
        }

        try {
            findProperty("idgFailure[]").change(failure.get(), context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String generateWhileReserving(DBManager dbManager, Connection connection, ExecutorService threads, long count) throws SQLException, InterruptedException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM idtable WHERE id = " + IDTable.OBJECT + " FOR UPDATE");
        }

        Future<?> reserving = threads.submit(() -> dbManager.generateIDs(count));
        if (!poll(() -> reserving.isDone() || waitedFor(connection)) || reserving.isDone()) {
            connection.rollback();
            String failure = failure("the reservation", reserving);
            return failure != null ? failure : "the reservation did not wait for the counter's row the test holds";
        }

        AtomicReference<Thread> askingThread = new AtomicReference<>();
        Future<?> asking = threads.submit(() -> {
            askingThread.set(Thread.currentThread());
            return dbManager.generateID();
        });
        boolean asked = poll(() -> asking.isDone() || waitsForReservation(askingThread.get()));
        connection.rollback(); // the reservation goes on, and the second thread after it

        // both waited for, so that neither outlives the test
        String askingFailure = failure("an ID asked for while another thread reserved", asking);
        String reservingFailure = failure("the reservation", reserving);
        if (askingFailure != null)
            return askingFailure;
        if (reservingFailure != null)
            return reservingFailure;
        return asked ? null : "an ID asked for while another thread reserved neither waited for the reservation nor was given in 30 seconds";
    }

    private interface Check {
        boolean done() throws SQLException;
    }

    private static boolean poll(Check check) throws SQLException, InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (!check.done()) {
            if (System.currentTimeMillis() > deadline)
                return false;
            Thread.sleep(10);
        }
        return true;
    }

    // whether another transaction waits for the one of this connection - the reservation, for the counter's row
    private static boolean waitedFor(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet waiting = statement.executeQuery("SELECT 1 FROM pg_locks WHERE NOT granted AND pg_backend_pid() = ANY(pg_blocking_pids(pid))")) {
            return waiting.next();
        }
    }

    // whether the thread waits for the lock IDTable reserves IDs under. Being blocked is not enough : before it asks
    // for its ID, it could be blocked on something else, and the row would be let go too early
    private static boolean waitsForReservation(Thread thread) {
        ThreadInfo info = thread != null ? ManagementFactory.getThreadMXBean().getThreadInfo(thread.getId()) : null;
        LockInfo lock = info != null ? info.getLockInfo() : null;
        return lock != null && lock.getIdentityHashCode() == System.identityHashCode(IDTable.instance);
    }

    // what a thread ended with, null when it got its IDs
    private static String failure(String thread, Future<?> future) throws InterruptedException {
        try {
            future.get(1, TimeUnit.MINUTES);
            return null;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            StackTraceElement[] trace = cause.getStackTrace();
            return thread + " failed: " + cause + (trace.length > 0 ? " at " + trace[0] : "");
        } catch (TimeoutException e) {
            return thread + " did not finish in a minute";
        }
    }
}
