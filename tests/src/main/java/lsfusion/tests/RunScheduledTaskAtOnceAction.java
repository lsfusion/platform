package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.base.controller.thread.WrappingScheduledExecutorService;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.scheduler.controller.manager.Scheduler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

// two runs of one scheduled task at once - the scheduled one and an EXECUTE, say -, in two threads, as the scheduler's
// threads run them. Its threads change the set of the tasks running at once, so the test checks the kind of the set; and
// it swaps the set for one that holds the first run right after its look at the set, while the second run is done : that
// one has to find the task running. Then a run fails right after it added its task - in the log of its start, which reads
// the scheduler's pool, as a STOP drops it -, and has to take the task out all the same. The task - of the scheduler's
// class for a user's task, a private one reached by reflection - has no steps and an id no task has. What the test saw
// goes into stFailure
public class RunScheduledTaskAtOnceAction extends AtOnceAction {

    public RunScheduledTaskAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
            Field field = Scheduler.class.getDeclaredField("executingTasks");
            field.setAccessible(true);
            Set<Object> executing = (Set<Object>) field.get(scheduler);
            String failure = executing instanceof ConcurrentHashMap.KeySetView ? null :
                    "the scheduler keeps the tasks running in a " + executing.getClass().getName() + ", which its threads change at once";

            Class<?> taskClass = Class.forName(Scheduler.class.getName() + "$UserSchedulerTask");
            Constructor<?> constructor = taskClass.getDeclaredConstructor(Scheduler.class, String.class, DataObject.class, TreeMap.class,
                    LocalTime.class, LocalTime.class, Set.class, Set.class);
            constructor.setAccessible(true);
            long id = -System.nanoTime();
            Object task = constructor.newInstance(scheduler, "stTask", new DataObject(id, context.getBL().schedulerLM.userScheduledTask),
                    new TreeMap<>(), null, null, new HashSet<>(), new HashSet<>());
            Method run = taskClass.getMethod("run", ExecutionStack.class);
            run.setAccessible(true);

            Hold hold = new Hold();
            AtomicInteger runs = new AtomicInteger();
            field.set(scheduler, new HoldingSet(executing, hold, runs));
            String held;
            try {
                held = runWhileHolding(context, hold, () -> invoke(run, task, context.stack), () -> invoke(run, task, context.stack));
            } finally {
                field.set(scheduler, executing);
            }
            if (failure == null)
                failure = held;
            if (failure == null && runs.get() != 1)
                failure = runs.get() + " runs of one scheduled task went: the second one while the first, past its look at the set, was held";
            if (failure == null && executing.contains(id))
                failure = "the task stayed among the running ones";

            ScheduledExecutorService pool = scheduler.daemonTasksExecutor;
            ScheduledThreadPoolExecutor unused = new ScheduledThreadPoolExecutor(1);
            scheduler.daemonTasksExecutor = new WrappingScheduledExecutorService(unused) {
                @Override
                protected <T> Callable<T> wrapTask(Callable<T> callable) {
                    return callable;
                }

                @Override
                public String getThreadPoolInfo() {
                    throw new IllegalStateException("the scheduler was stopped");
                }
            };
            try {
                invoke(run, task, context.stack);
            } catch (RuntimeException e) {
                // the run fails, as it is to
            } finally {
                scheduler.daemonTasksExecutor = pool;
                unused.shutdown();
            }
            if (failure == null && executing.contains(id))
                failure = "a run that failed after adding its task left it among the running ones";

            findProperty("stFailure[]").change(failure, context);
        } catch (ReflectiveOperationException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static void invoke(Method run, Object task, ExecutionStack stack) {
        try {
            run.invoke(task, stack);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
    }

    // the set of the tasks running, its first look - whether a task runs, or adding it - held in the hold after it is
    // made; a run that went takes its task out at the end, so takings out are the runs
    private static class HoldingSet extends AbstractSet<Object> {
        private final Set<Object> set;
        private final Hold hold;
        private final AtomicInteger runs;

        HoldingSet(Set<Object> set, Hold hold, AtomicInteger runs) {
            this.set = set;
            this.hold = hold;
            this.runs = runs;
        }

        @Override
        public boolean contains(Object o) {
            boolean contains = set.contains(o);
            return hold.call(() -> contains);
        }

        @Override
        public boolean add(Object o) {
            boolean added = set.add(o);
            return hold.call(() -> added);
        }

        @Override
        public boolean remove(Object o) {
            runs.incrementAndGet();
            return set.remove(o);
        }

        @Override
        public Iterator<Object> iterator() {
            return set.iterator();
        }

        @Override
        public int size() {
            return set.size();
        }
    }
}
