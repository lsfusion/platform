package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

// two threads adding statements to one form at once, as two modules parsed at once that extend one form do. The form's
// list of statements is swapped for one that holds the first add inside it until the second thread either is inside too
// - two adds at once, which a plain list does not survive - or waits for the form the adds are made under. What the test
// saw goes into fsFailure, and so does a run that did not have the two at once
public class AddFormStatementsAtOnceAction extends InternalAction {

    public AddFormStatementsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormEntity form = context.getBL().findForm("FormStatements.fsForm");
        List<String> statements = form.formOrDesignStatementList;
        HoldingList holding = new HoldingList();
        form.formOrDesignStatementList = holding;
        ExecutorService threads = Executors.newFixedThreadPool(2);
        String failure;
        try {
            try {
                failure = addAtOnce(form, holding, threads);
            } finally {
                // both threads stopped before the form gets its list back, so that neither adds to it then
                threads.shutdownNow();
                threads.awaitTermination(1, TimeUnit.MINUTES);
                form.formOrDesignStatementList = statements;
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }

        try {
            findProperty("fsFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String addAtOnce(FormEntity form, HoldingList holding, ExecutorService threads) throws InterruptedException {
        Future<?> first = threads.submit(() -> form.addFormOrDesignStatementTokens(Arrays.asList("EXTEND FORM fsForm", ";")));
        if (!holding.entered.await(30, TimeUnit.SECONDS)) {
            holding.release.countDown();
            String failure = failure("the first statement", first);
            return failure != null ? failure : "the first statement was not added in 30 seconds";
        }

        AtomicReference<Thread> secondThread = new AtomicReference<>();
        Future<?> second = threads.submit(() -> {
            secondThread.set(Thread.currentThread());
            form.addFormOrDesignStatementTokens(Arrays.asList("DESIGN fsForm", ";"));
        });
        boolean met = poll(() -> holding.overlapped || second.isDone() || waitsFor(secondThread.get(), form));
        holding.release.countDown(); // the first add goes on, and the second one after it

        // both waited for, so that neither outlives the test
        String secondFailure = failure("the second statement", second);
        String firstFailure = failure("the first statement", first);
        if (holding.overlapped)
            return "two threads added statements to the form at once";
        if (holding.expired)
            return "the first statement was let go after a minute, not by the test";
        if (secondFailure != null)
            return secondFailure;
        if (firstFailure != null)
            return firstFailure;
        if (!met)
            return "the second statement neither waited for the first one nor was added in 30 seconds";
        List<String> added = new ArrayList<>(holding);
        Collections.sort(added);
        return added.equals(Arrays.asList("DESIGN fsForm;", "EXTEND FORM fsForm;")) ? null : "the form kept the statements " + added;
    }

    // a list whose first add waits inside it until the test lets it go, and which notes another add made meanwhile. A
    // wait that ran out is noted too : with the first add gone on, the second one would have met nothing to wait for
    private static class HoldingList extends ArrayList<String> {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger inside = new AtomicInteger();
        private volatile boolean overlapped;
        private volatile boolean expired;

        @Override
        public boolean add(String statement) {
            if (inside.incrementAndGet() > 1)
                overlapped = true;
            try {
                if (entered.getCount() > 0) {
                    entered.countDown();
                    if (!release.await(1, TimeUnit.MINUTES))
                        expired = true;
                }
                return super.add(statement);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            } finally {
                inside.decrementAndGet();
            }
        }
    }

    private static boolean poll(BooleanSupplier check) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!check.getAsBoolean()) {
            if (System.nanoTime() - deadline > 0)
                return false;
            Thread.sleep(10);
        }
        return true;
    }

    // whether the thread waits for the form's monitor. Being blocked is not enough : before it adds, it could be blocked
    // on something else, and the first add would be let go too early
    private static boolean waitsFor(Thread thread, FormEntity form) {
        ThreadInfo info = thread != null ? ManagementFactory.getThreadMXBean().getThreadInfo(thread.getId()) : null;
        LockInfo lock = info != null ? info.getLockInfo() : null;
        return lock != null && lock.getIdentityHashCode() == System.identityHashCode(form);
    }

    // what a thread ended with, null when it added its statement
    private static String failure(String statement, Future<?> future) throws InterruptedException {
        try {
            future.get(1, TimeUnit.MINUTES);
            return null;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            StackTraceElement[] trace = cause.getStackTrace();
            return statement + " failed: " + cause + (trace.length > 0 ? " at " + trace[0] : "");
        } catch (TimeoutException e) {
            return statement + " was not added in a minute";
        }
    }
}
