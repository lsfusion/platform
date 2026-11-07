package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
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
import java.util.function.Function;
import java.util.function.Supplier;

// two threads of the server doing one thing at once, as two modules parsed at once do to a form both extend. The first
// call is held inside a Hold, or the test holds the monitor itself : the second call has to be checked while the
// first one is still there, so the test sees whether it goes through or waits for the monitor; then both go on
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

    // null when `meanwhile` - done by the caller while `first`, in another thread, is held inside the hold - left `first`
    // to go on without a failure; else what failed. The thread is stopped when this returns
    protected static String runWhileHolding(ExecutionContext<ClassPropertyInterface> context, Hold hold, Runnable first, Runnable meanwhile) {
        ExecutorService threads = ExecutorFactory.createNewThreadService((ExecutionContext) context, 1, false);
        try {
            try {
                Future<?> firstDone = threads.submit(first);
                if (!hold.entered.await(30, TimeUnit.SECONDS)) {
                    hold.release.countDown();
                    String failure = failure("the thread", firstDone);
                    return failure != null ? failure : "the thread did not get to the hold in 30 seconds";
                }
                try {
                    meanwhile.run();
                } finally {
                    hold.release.countDown();
                }
                String failure = failure("the thread", firstDone);
                if (failure != null)
                    return failure;
                return hold.expired ? "the thread was let go after a minute, not by the test" : null;
            } finally {
                threads.shutdownNow();
                threads.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    // null when a thread doing `call` waits for the monitor the test holds - as a second thread waits while a first one
    // is inside a method synchronized on that monitor : the test stands in for that first thread where what the method
    // writes to can not be swapped for one that goes through a Hold -; else `through` for a call that went through
    // meanwhile, what it failed with, or that it did neither in 30 seconds. Once it waits, the monitor is let go and the
    // call goes on; the thread is stopped when this returns
    protected static String runWhileHeld(ExecutionContext<ClassPropertyInterface> context, Object monitor, String through,
                                         Runnable call) {
        return runWhileHeld(context, monitor, through, call, thread -> null);
    }

    // as above, and the thread waiting for the monitor is looked at before it is let go : `whileWaiting` says what is
    // wrong with it - what it holds meanwhile, say -, null if nothing
    protected static String runWhileHeld(ExecutionContext<ClassPropertyInterface> context, Object monitor, String through,
                                         Runnable call, Function<Thread, String> whileWaiting) {
        ExecutorService threads = ExecutorFactory.createNewThreadService((ExecutionContext) context, 1, false);
        try {
            try {
                AtomicReference<Thread> thread = new AtomicReference<>();
                Future<?> done;
                boolean waited;
                boolean wentThrough;
                String waiting = null;
                synchronized (monitor) {
                    Future<?> submitted = threads.submit(() -> {
                        thread.set(Thread.currentThread());
                        call.run();
                    });
                    waited = poll(() -> submitted.isDone() || waitsFor(thread.get(), monitor));
                    wentThrough = submitted.isDone();
                    if (waited && !wentThrough)
                        waiting = whileWaiting.apply(thread.get());
                    done = submitted;
                }

                String failure = failure("the thread", done);
                if (failure != null)
                    return failure;
                if (wentThrough)
                    return through;
                if (waiting != null)
                    return waiting;
                return waited ? null : "the thread neither waited for the monitor nor was done in 30 seconds";
            } finally {
                threads.shutdownNow();
                threads.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    // null when a thread doing `call` goes through while the test holds the monitor; else `waited` if it was not done in
    // ten seconds, or what it failed with. The monitor is let go before the call is waited for, so a call waiting for it
    // can end too; the thread is stopped when this returns
    protected static String runWithoutWaiting(ExecutionContext<ClassPropertyInterface> context, Object monitor, String waited,
                                              Runnable call) {
        ExecutorService threads = ExecutorFactory.createNewThreadService((ExecutionContext) context, 1, false);
        try {
            try {
                CountDownLatch ended = new CountDownLatch(1);
                Future<?> done;
                boolean wentThrough;
                synchronized (monitor) {
                    done = threads.submit(() -> {
                        try {
                            call.run();
                        } finally {
                            ended.countDown();
                        }
                    });
                    wentThrough = ended.await(10, TimeUnit.SECONDS);
                }

                String failure = failure("the thread", done);
                if (failure != null)
                    return failure;
                return wentThrough ? null : waited;
            } finally {
                threads.shutdownNow();
                threads.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    // two modules that do not require each other - the given one and another -, the one earlier in the module order
    // first : the start parses such modules at once, so which of them writes a setting they share first is the parse
    // threads' timing, and what the setting keeps has to come from the later one whichever wrote first
    protected static LogicsModule[] independentModules(BusinessLogics BL, LogicsModule module) {
        for (LogicsModule other : BL.getLogicModules())
            if (!other.visible.contains(module.getVersion()) && !module.visible.contains(other.getVersion()))
                return other.order < module.order ? new LogicsModule[]{other, module} : new LogicsModule[]{module, other};
        throw new IllegalStateException("every module requires " + module.getName() + " or is required by it");
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

    // whether the thread holds the monitor of the object
    protected static boolean holds(Thread thread, Object object) {
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(new long[]{thread.getId()}, true, false)[0];
        if (info != null)
            for (MonitorInfo monitor : info.getLockedMonitors())
                if (monitor.getIdentityHashCode() == System.identityHashCode(object))
                    return true;
        return false;
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
