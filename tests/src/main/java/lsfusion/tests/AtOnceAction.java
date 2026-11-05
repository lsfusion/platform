package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

// two threads of the server doing one thing at once, as two modules parsed at once do to a form both extend. What the
// thing writes to is swapped for one that goes through a Hold, which holds the first thread inside until the second one
// either is inside too - the two at once, which the thing does not survive - or waits for the monitor the two are to be
// made under; then both go on
public abstract class AtOnceAction extends InternalAction {

    public AtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    // what a swapped structure calls through : its first call waits inside until the test lets it go, and a call made
    // meanwhile is noted. A wait that ran out is noted too : with the first call gone on, the second one would have met
    // nothing to wait for
    protected static class Hold {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger inside = new AtomicInteger();
        private volatile boolean overlapped;
        private volatile boolean expired;

        public <T> T call(Supplier<T> call) {
            if (inside.incrementAndGet() > 1)
                overlapped = true;
            try {
                if (entered.getCount() > 0) {
                    entered.countDown();
                    if (!release.await(1, TimeUnit.MINUTES))
                        expired = true;
                }
                return call.get();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            } finally {
                inside.decrementAndGet();
            }
        }
    }

    // null when, the first thread held, the second one waited for the monitor and both went through; else what the test
    // saw - `overlap` for the two inside at once -, and so for a run that did not have the two at once. Both threads
    // are stopped when it returns, so that neither writes to what the caller puts back
    protected static String runAtOnce(ExecutionContext<ClassPropertyInterface> context, Hold hold, Object monitor,
                                      String overlap, Runnable first, Runnable second) {
        ExecutorService threads = ExecutorFactory.createNewThreadService((ExecutionContext) context, 2, false);
        try {
            try {
                return runAtOnce(threads, hold, monitor, overlap, first, second);
            } finally {
                threads.shutdownNow();
                threads.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String runAtOnce(ExecutorService threads, Hold hold, Object monitor, String overlap,
                                    Runnable first, Runnable second) throws InterruptedException {
        Future<?> firstDone = threads.submit(first);
        if (!hold.entered.await(30, TimeUnit.SECONDS)) {
            hold.release.countDown();
            String failure = failure("the first thread", firstDone);
            return failure != null ? failure : "the first thread did not get to the hold in 30 seconds";
        }

        AtomicReference<Thread> secondThread = new AtomicReference<>();
        Future<?> secondDone = threads.submit(() -> {
            secondThread.set(Thread.currentThread());
            second.run();
        });
        boolean met = poll(() -> hold.overlapped || secondDone.isDone() || waitsFor(secondThread.get(), monitor));
        hold.release.countDown(); // the first thread goes on, and the second one after it

        // both waited for, so that neither outlives the test
        String secondFailure = failure("the second thread", secondDone);
        String firstFailure = failure("the first thread", firstDone);
        if (hold.overlapped)
            return overlap;
        if (hold.expired)
            return "the first thread was let go after a minute, not by the test";
        if (secondFailure != null)
            return secondFailure;
        if (firstFailure != null)
            return firstFailure;
        return met ? null : "the second thread neither waited for the first one nor was done in 30 seconds";
    }

    // null when a thread doing `call` waits for the monitor the test holds - as a second thread waits while a first one
    // is inside a method synchronized on that monitor : the test stands in for that first thread where what the method
    // writes to can not be swapped for one that goes through a Hold -; else `through` for a call that went through
    // meanwhile, what it failed with, or that it did neither in 30 seconds. Once it waits, the monitor is let go and the
    // call goes on; the thread is stopped when this returns
    protected static String runWhileHeld(ExecutionContext<ClassPropertyInterface> context, Object monitor, String through,
                                         Runnable call) {
        ExecutorService threads = ExecutorFactory.createNewThreadService((ExecutionContext) context, 1, false);
        try {
            try {
                AtomicReference<Thread> thread = new AtomicReference<>();
                Future<?> done;
                boolean waited;
                boolean wentThrough;
                synchronized (monitor) {
                    Future<?> submitted = threads.submit(() -> {
                        thread.set(Thread.currentThread());
                        call.run();
                    });
                    waited = poll(() -> submitted.isDone() || waitsFor(thread.get(), monitor));
                    wentThrough = submitted.isDone();
                    done = submitted;
                }

                String failure = failure("the thread", done);
                if (failure != null)
                    return failure;
                if (wentThrough)
                    return through;
                return waited ? null : "the thread neither waited for the monitor nor was done in 30 seconds";
            } finally {
                threads.shutdownNow();
                threads.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
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

    // whether the thread waits for the monitor. Being blocked is not enough : before it gets to the hold, it could be
    // blocked on something else, and the first thread would be let go too early
    private static boolean waitsFor(Thread thread, Object monitor) {
        ThreadInfo info = thread != null ? ManagementFactory.getThreadMXBean().getThreadInfo(thread.getId()) : null;
        LockInfo lock = info != null ? info.getLockInfo() : null;
        return lock != null && lock.getIdentityHashCode() == System.identityHashCode(monitor);
    }

    // what a thread ended with, null when it went through
    private static String failure(String thread, Future<?> future) throws InterruptedException {
        try {
            future.get(1, TimeUnit.MINUTES);
            return null;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            StackTraceElement[] trace = cause.getStackTrace();
            return thread + " failed: " + cause + (trace.length > 0 ? " at " + trace[0] : "");
        } catch (TimeoutException e) {
            return thread + " was not done in a minute";
        }
    }
}
