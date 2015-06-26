package lsfusion.server.logics.tasks;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ScriptParsingException;
import org.antlr.runtime.RecognitionException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// выполняет задания по одному, с зависимостями или без
public abstract class GroupSingleTask<T> extends GroupProgramTask {

    protected abstract boolean isGraph();

    protected abstract void runTask(T module) throws RecognitionException;

    protected long getTaskComplexity(T module) {
        return 1;
    }

    protected abstract List<T> getElements();

    protected abstract String getElementCaption(T element, int all, int current);

    protected abstract String getElementCaption(T element);

    protected abstract String getErrorsDescription(T element);

    protected abstract ImSet<T> getDependElements(T key);

    @Override
    protected Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> initTasks() {
        MRevMap<T, SingleProgramTask> mMapTasks = MapFact.mRevMap();
        final BusinessLogics<?> BL = (BusinessLogics<?>) getBL();
        List<T> elements = getElements();
        final int elementCount = elements.size();
        final AtomicInteger elementOrder = new AtomicInteger(0);

        // генерим таски
        for (final T element : elements) {
            SingleProgramTask task = new SingleProgramTask() {
                public String getCaption() {
                    return GroupSingleTask.this.getCaption() + " for " + getElementCaption(element, elementCount, elementOrder.incrementAndGet());
                }

                public String toString() {
                    return getElementCaption(element);
                }

                @Override
                public boolean isLoggable() {
                    return GroupSingleTask.this.isGroupLoggable();
                }

                @Override
                protected long getBaseComplexity() {
                    return getTaskComplexity(element);
                }

                public void run() {
                    Exception runException = null;
                    try {
                        long l = System.currentTimeMillis();
                        runTask(element);
                        runTime = System.currentTimeMillis() - l;
                    } catch (RecognitionException e) {
                        runException = new ScriptParsingException(e.getMessage());
                    } catch (Exception e) {
                        runException = e;
                    }

                    String syntaxErrors = getErrorsDescription(element);

                    if (!syntaxErrors.isEmpty()) {
                        if (runException != null) {
                            syntaxErrors = syntaxErrors + runException.getMessage();
                        }
                        throw new ScriptParsingException(syntaxErrors);
                    } else if (runException != null) {
                        throw Throwables.propagate(runException);
                    }
                }
            };
            mMapTasks.revAdd(element, task);
        }
        ImRevMap<T, SingleProgramTask> mapTasks = mMapTasks.immutableRev();

        MExclSet<SingleProgramTask> mRootTasks = SetFact.mExclSet();
        MSet<SingleProgramTask> mDepTasks = SetFact.mSet();
        for (int i = 0, size = mapTasks.size(); i < size; i++) {
            T key = mapTasks.getKey(i);
            SingleProgramTask task = mapTasks.getValue(i);
            ImSet<T> dependElements = getDependElements(key);
            if(dependElements.isEmpty()) {
                mRootTasks.exclAdd(task);
            } else {
                for (T reqModule : dependElements) {
                    SingleProgramTask depTask = mapTasks.get(reqModule);
                    task.addDependency(depTask);
                    mDepTasks.add(depTask);
                }
            }
        }
        return new Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>>(mRootTasks.immutable(), mapTasks.valuesSet().remove(mDepTasks.immutable()));
    }

    public Pair<List<Task>, Long> calcFullDiameter(Task currentTask, Map<Task, Pair<List<Task>, Long>> marks, Map<Task, Map<Task, Long>> overMap, Task stopTask) { // initialTasks а не executor чтобы не synchronize'ть dependsFrom
        if (marks.containsKey(currentTask)) {
            return marks.get(currentTask);
        }

        Pair<List<Task>, Long> current = new Pair<List<Task>, Long>(new ArrayList<Task>(), (long) 0);
        if (currentTask == stopTask) {
            return current;
        }

        Map<Task, Long> allDependencies = BaseUtils.override(BaseUtils.toMap(currentTask.dependsFrom.keySet(), getRuntime(currentTask)), overMap.get(currentTask));
        for (Map.Entry<Task, Long> depend : allDependencies.entrySet()) {
            Pair<List<Task>, Long> depCalc = calcFullDiameter(depend.getKey(), marks, overMap, stopTask);
            long depDiam = depCalc.second + depend.getValue();
            if (depDiam > current.second) {
                current = new Pair<List<Task>, Long>(depCalc.first, depDiam);
            }
        }
        current = new Pair<List<Task>, Long>(BaseUtils.add(current.first, currentTask), current.second);
        marks.put(currentTask, current);
        return current;
    }

    public void run() {
        if (isGraph()) {
            Map<Task, Map<Task, Long>> overMap = new HashMap<Task, Map<Task, Long>>();
            markDiameters(preTask, overMap, this);
//            Pair<List<Task>, Long> maxPath = calcFullDiameter(preTask, new HashMap<Task, Pair<List<Task>, Long>>(), overMap, this);
//            System.out.println(maxPath);
        }
    }

    private static long getRuntime(Task currentTask) {
        return currentTask instanceof SingleProgramTask ? ((SingleProgramTask) currentTask).runTime : 0;
    }

    public static void markDiametersForTask(Task currentTask, PairTask edge, Map<Task, PairTask> marks, Task stopTask) { // initialTasks а не executor чтобы не synchronize'ть dependsFrom
        PairTask currentMark = marks.get(currentTask);
        if (currentMark != null) { // если диаметр меньше, и tasksToDo все обработаны
            if (edge.diameter < currentMark.diameter && currentMark.tasksToDo.containsAll(edge.tasksToDo)) {
                return;
            }
        } else {
            currentMark = new PairTask();
            marks.put(currentTask, currentMark);
        }

        currentMark.tasksToDo.addAll(edge.tasksToDo);
        currentMark.diameter = BaseUtils.max(currentMark.diameter, edge.diameter);

        if (currentTask == stopTask) {
            return;
        }

        edge = new PairTask(BaseUtils.addSet(edge.tasksToDo, currentTask), edge.diameter + getRuntime(currentTask));

        Set<Task> allDependencies = currentTask.dependsFrom.keySet();
        for (Task depend : allDependencies) {
            markDiametersForTask(depend, edge, marks, stopTask);
        }
    }

    private static long getTotalRuntime(Set<Task> tasksToDo) {
        long result = 0;
        for (Task task : tasksToDo) {
            result += getRuntime(task);
        }
        return result / TaskRunner.availableProcessors();
    }

    public static void markDiameters(Task currentTask, Map<Task, Map<Task, Long>> overDiameters, Task stopTask) { // initialTasks а не executor чтобы не synchronize'ть dependsFrom
        if (overDiameters.containsKey(currentTask)) {
            return;
        }

        Map<Task, PairTask> marks = new HashMap<Task, PairTask>();
        markDiametersForTask(currentTask, new PairTask(), marks, stopTask);
        Map<Task, Long> critical = new HashMap<Task, Long>();
        for (Map.Entry<Task, PairTask> entry : marks.entrySet()) {
            PairTask pairTask = entry.getValue();
            long tasksToDo = getTotalRuntime(pairTask.tasksToDo);
            if (tasksToDo > pairTask.diameter) {
                critical.put(entry.getKey(), tasksToDo);
            }
        }

        overDiameters.put(currentTask, critical);
        Set<Task> allDependencies = currentTask.dependsFrom.keySet();
        for (Task depend : allDependencies) {
            markDiameters(depend, overDiameters, stopTask);
        }
    }

/*    public void markAll(Task currentTask, Set<Task> tasks, Task stopTask) {
        if(!tasks.add(currentTask))
            return;

        if(currentTask == stopTask)
            return;

        Set<Task> allDependencies = currentTask.dependsFrom.keySet();
        for(Task depend : allDependencies)
            markAll(depend, tasks, stopTask);
    }*/

    private static class PairTask {
        public Set<Task> tasksToDo;
        public long diameter;

        private PairTask() {
            tasksToDo = new HashSet<Task>();
            diameter = 0;
        }

        private PairTask(Set<Task> tasksToDo, Long diameter) {
            this.tasksToDo = tasksToDo;
            this.diameter = diameter;
        }
    }

}
