package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.WrapperContext;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.lifecycle.MonitorServer;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

import static org.apache.commons.lang3.StringUtils.trim;

public class Scheduler extends MonitorServer implements InitializingBean {
    private static final Logger logger = ServerLoggers.systemLogger;
    public static final Logger schedulerLogger = ServerLoggers.schedulerLogger;

    public ScheduledExecutorService daemonTasksExecutor;

    private LogicsInstance logicsInstance;

    private BusinessLogics BL;
    private DBManager dbManager;

    private Map<Integer, List<ScheduledFuture>> futuresMap = new HashMap<>();

    public Scheduler() {
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.BL = businessLogics;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    @IdentityStrongLazy
    private SQLSession getLogSql() throws SQLException { // нужен отдельный так как сессия будет использоваться внутри другой транзакции (delayUserInteraction)
        try {
            return dbManager.createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(BL, "businessLogics must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        Assert.notNull(logicsInstance, "logicsInstance must be specified");
    }

    private void changeCurrentDate(ExecutionStack stack) {
        try {
            try (DataSession session = dbManager.createSession()) {
                session.setNoCancelInTransaction(true);

                java.sql.Date currentDate = (java.sql.Date) BL.timeLM.currentDate.read(session);
                java.sql.Date newDate = DateConverter.getCurrentDate();
                if (currentDate == null || currentDate.getDate() != newDate.getDate() || currentDate.getMonth() != newDate.getMonth() || currentDate.getYear() != newDate.getYear()) {
                    logger.info(String.format("ChangeCurrentDate started: from %s to %s", currentDate, newDate));
                    BL.timeLM.currentDate.change(newDate, session);
                    String result = session.applyMessage(BL, stack);
                    if (result == null)
                        logger.info("ChangeCurrentDate finished");
                    else
                        logger.error(String.format("ChangeCurrentDate failed: %s", result));
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String getEventName() {
        return "scheduler-daemon";
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    public void setupScheduledTask(DataSession session, DataObject scheduledTaskObject, String nameScheduledTask) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {
        if (daemonTasksExecutor != null) {
            Integer scheduledTaskId = (Integer) scheduledTaskObject.getValue();
            List<ScheduledFuture> futures = futuresMap.remove(scheduledTaskId);
            if (futures != null) {
                schedulerLogger.info("Stopped scheduler task: " + nameScheduledTask);
                for (ScheduledFuture future : futures) {
                    future.cancel(true);
                }
            }

            if(BL.schedulerLM.activeScheduledTask.read(session, scheduledTaskObject) != null) {
                Time timeFrom = (Time) BL.schedulerLM.timeFromScheduledTask.read(session, scheduledTaskObject);
                Time timeTo = (Time) BL.schedulerLM.timeToScheduledTask.read(session, scheduledTaskObject);
                String daysOfWeek = (String) BL.schedulerLM.daysOfWeekScheduledTask.read(session, scheduledTaskObject);
                String daysOfMonth = (String) BL.schedulerLM.daysOfMonthScheduledTask.read(session, scheduledTaskObject);
                boolean runAtStart = BL.schedulerLM.runAtStartScheduledTask.read(session, scheduledTaskObject) != null;
                Timestamp startDate = (Timestamp) BL.schedulerLM.startDateScheduledTask.read(session, scheduledTaskObject);
                Integer period = (Integer) BL.schedulerLM.periodScheduledTask.read(session, scheduledTaskObject);
                Object schedulerStartType = BL.schedulerLM.schedulerStartTypeScheduledTask.read(session, scheduledTaskObject);
                Object afterFinish = ((ConcreteCustomClass) BL.schedulerLM.findClass("SchedulerStartType")).getDataObject("afterFinish").object;
                boolean fixedDelay = afterFinish.equals(schedulerStartType);

                if (startDate != null) {
                    schedulerLogger.info("Scheduled scheduler task: " + nameScheduledTask);
                    SchedulerTask task = new SchedulerTask(nameScheduledTask, readUserSchedulerTask(session, session.getModifier(), scheduledTaskObject, nameScheduledTask,
                            timeFrom, timeTo, daysOfWeek, daysOfMonth), scheduledTaskId, runAtStart, startDate, period, fixedDelay);
                    futures = task.schedule(daemonTasksExecutor);
                    futuresMap.put(scheduledTaskId, futures);
                }
            }
        }
    }

    public void executeScheduledTask(DataSession session, DataObject scheduledTaskObject, String nameScheduledTask) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {
        if (daemonTasksExecutor != null) {
            Integer scheduledTaskId = (Integer) scheduledTaskObject.getValue();
            List<ScheduledFuture> futures = futuresMap.get(scheduledTaskId);
            if (futures == null)
                futures = new ArrayList<>();

            schedulerLogger.info("Execute scheduler task: " + nameScheduledTask);
            SchedulerTask task = new SchedulerTask(nameScheduledTask, readUserSchedulerTask(session, session.getModifier(), scheduledTaskObject, nameScheduledTask,
                    null, null, null, null), scheduledTaskId, true, null, 0, false);
            futures.add(task.execute(daemonTasksExecutor));
            futuresMap.put(scheduledTaskId, futures);
        }
    }

    public void setupScheduledTasks(DataSession session) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {

        if (daemonTasksExecutor != null)
            daemonTasksExecutor.shutdownNow();

        daemonTasksExecutor = ExecutorFactory.createMonitorScheduledThreadService(3, this);

        List<SchedulerTask> tasks = new ArrayList<>();
        fillSystemScheduledTasks(tasks);
        fillUserScheduledTasks(session, tasks);

        for (SchedulerTask task : tasks) {
            List<ScheduledFuture> futures = futuresMap.get(task.scheduledTaskId);
            if (futures == null)
                futures = new ArrayList<>();
            futures.addAll(task.schedule(daemonTasksExecutor));
            futuresMap.put(task.scheduledTaskId, futures);
        }
    }

    private void fillSystemScheduledTasks(List<SchedulerTask> tasks) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        tasks.add(new SystemSchedulerTask(new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                changeCurrentDate(stack);
            }
        }, -1, true, Settings.get().getCheckCurrentDate(), true, "Changing current date"));
        tasks.addAll(BL.getSystemTasks(this));
    }

    private void fillUserScheduledTasks(DataSession session, List<SchedulerTask> tasks) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        Modifier modifier = session.getModifier();
        KeyExpr scheduledTaskExpr = new KeyExpr("scheduledTask");
        ImRevMap<Object, KeyExpr> scheduledTaskKeys = MapFact.<Object, KeyExpr>singletonRev("scheduledTask", scheduledTaskExpr);

        QueryBuilder<Object, Object> scheduledTaskQuery = new QueryBuilder<>(scheduledTaskKeys);
        scheduledTaskQuery.addProperty("nameScheduledTask", BL.schedulerLM.nameScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("runAtStartScheduledTask", BL.schedulerLM.runAtStartScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("startDateScheduledTask", BL.schedulerLM.startDateScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("timeFromScheduledTask", BL.schedulerLM.timeFromScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("timeToScheduledTask", BL.schedulerLM.timeToScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("periodScheduledTask", BL.schedulerLM.periodScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("daysOfWeekScheduledTask", BL.schedulerLM.daysOfWeekScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("daysOfMonthScheduledTask", BL.schedulerLM.daysOfMonthScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("schedulerStartTypeScheduledTask", BL.schedulerLM.schedulerStartTypeScheduledTask.getExpr(modifier, scheduledTaskExpr));

        scheduledTaskQuery.and(BL.schedulerLM.activeScheduledTask.getExpr(modifier, scheduledTaskExpr).getWhere());

        Object afterFinish = ((ConcreteCustomClass) BL.schedulerLM.findClass("SchedulerStartType")).getDataObject("afterFinish").object;

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> scheduledTaskResult = scheduledTaskQuery.execute(session);
        for (int i = 0, size = scheduledTaskResult.size(); i < size; i++) {
            ImMap<Object, Object> key = scheduledTaskResult.getKey(i);
            ImMap<Object, Object> value = scheduledTaskResult.getValue(i);
            Integer scheduledTaskId = (Integer) key.getValue(0);
            DataObject currentScheduledTaskObject = new DataObject(scheduledTaskId, BL.schedulerLM.scheduledTask);
            String nameScheduledTask = trim((String) value.get("nameScheduledTask"));
            Boolean runAtStart = value.get("runAtStartScheduledTask") != null;
            Timestamp startDate = (Timestamp) value.get("startDateScheduledTask");
            Time timeFrom = (Time) value.get("timeFromScheduledTask");
            Time timeTo = (Time) value.get("timeToScheduledTask");
            Integer period = (Integer) value.get("periodScheduledTask");
            Object schedulerStartType = value.get("schedulerStartTypeScheduledTask");
            boolean fixedDelay = afterFinish.equals(schedulerStartType);

            String daysOfWeekScheduledTask = (String) value.get("daysOfWeekScheduledTask");
            Set<String> daysOfWeek = daysOfWeekScheduledTask == null ? new HashSet<>() : new HashSet(Arrays.asList(daysOfWeekScheduledTask.split(",| ")));
            daysOfWeek.remove("");
            String daysOfMonthScheduledTask = (String) value.get("daysOfMonthScheduledTask");
            Set<String> daysOfMonth = daysOfMonthScheduledTask == null ? new HashSet<>() : new HashSet(Arrays.asList(daysOfMonthScheduledTask.split(",| ")));
            daysOfMonth.remove("");

            if(startDate != null)
                tasks.add(new SchedulerTask(nameScheduledTask, readUserSchedulerTask(session, modifier, currentScheduledTaskObject, nameScheduledTask, timeFrom, timeTo,
                    daysOfWeekScheduledTask, daysOfMonthScheduledTask), scheduledTaskId, runAtStart, startDate, period, fixedDelay));
        }
    }

    private UserSchedulerTask readUserSchedulerTask(DataSession session, Modifier modifier, DataObject scheduledTaskObject, String nameScheduledTask,
                                                    Time timeFrom, Time timeTo, String daysOfWeekScheduledTask, String daysOfMonthScheduledTask) throws SQLException, SQLHandledException {
        KeyExpr scheduledTaskDetailExpr = new KeyExpr("scheduledTaskDetail");
        ImRevMap<Object, KeyExpr> scheduledTaskDetailKeys = MapFact.<Object, KeyExpr>singletonRev("scheduledTaskDetail", scheduledTaskDetailExpr);

        QueryBuilder<Object, Object> scheduledTaskDetailQuery = new QueryBuilder<>(scheduledTaskDetailKeys);
        scheduledTaskDetailQuery.addProperty("canonicalNameProperty", BL.schedulerLM.canonicalNamePropertyScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("ignoreExceptions", BL.schedulerLM.ignoreExceptionsScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("order", BL.schedulerLM.orderScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("script", BL.schedulerLM.scriptScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("timeout", BL.schedulerLM.timeoutScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("parameter", BL.schedulerLM.parameterScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.and(BL.schedulerLM.activeScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr).getWhere());
        scheduledTaskDetailQuery.and(BL.schedulerLM.scheduledTaskScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr).compare(scheduledTaskObject, Compare.EQUALS));

        TreeMap<Integer, ScheduledTaskDetail> propertySIDMap = new TreeMap<>();
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> propertyResult = scheduledTaskDetailQuery.execute(session);
        int defaultOrder = propertyResult.size() + 100;
        for (ImMap<Object, Object> propertyValues : propertyResult.valueIt()) {
            String canonicalName = (String) propertyValues.get("canonicalNameProperty");
            String script = (String) propertyValues.get("script");
            if(script != null && !script.isEmpty())
                script = String.format("run() = ACTION {%s;\n};", script);
            boolean ignoreExceptions = propertyValues.get("ignoreExceptions") != null;
            Integer timeout = (Integer) propertyValues.get("timeout");
            String parameter = (String) propertyValues.get("parameter");
            List<String> params = new ArrayList<>();
            if(parameter != null)
                Collections.addAll(params, parameter.split(","));
            Integer orderProperty = (Integer) propertyValues.get("order");
            if (canonicalName != null || script != null) {
                if (orderProperty == null) {
                    orderProperty = defaultOrder;
                    defaultOrder++;
                }
                LAP lap = script == null ? (LAP) BL.findProperty(canonicalName.trim()) : BL.schedulerLM.evalScript;
                if(lap != null)
                    propertySIDMap.put(orderProperty, new ScheduledTaskDetail(lap, script, ignoreExceptions, timeout, params));
            }
        }
        Set<String> daysOfWeek = daysOfWeekScheduledTask == null ? new HashSet<>() : new HashSet(Arrays.asList(daysOfWeekScheduledTask.split(",| ")));
        daysOfWeek.remove("");
        Set<String> daysOfMonth = daysOfMonthScheduledTask == null ? new HashSet<>() : new HashSet(Arrays.asList(daysOfMonthScheduledTask.split(",| ")));
        daysOfMonth.remove("");
        return new UserSchedulerTask(nameScheduledTask, scheduledTaskObject, propertySIDMap, timeFrom, timeTo, daysOfWeek, daysOfMonth);
    }

    public class SystemSchedulerTask extends SchedulerTask {

        public SystemSchedulerTask(final EExecutionStackRunnable task, Integer scheduledTaskId, boolean runAtStart, Integer period, boolean fixedDelay, String name) {
            super(name, task, scheduledTaskId, runAtStart, null, period, fixedDelay);
        }
    }

    public SchedulerTask createSystemTask(EExecutionStackRunnable task, boolean runAtStart, Integer period, boolean fixedDelay, String name) {
        return new SystemSchedulerTask(task, -1, runAtStart, period, fixedDelay, name);
    }

    public class SchedulerTask {
        private final Integer scheduledTaskId;
        private final boolean runAtStart;
        private final Timestamp startDate;
        private final Integer period;
        private final boolean fixedDelay;
        private final Runnable task;

        public SchedulerTask(final String name, final EExecutionStackRunnable task, Integer scheduledTaskId, boolean runAtStart, Timestamp startDate, Integer period, boolean fixedDelay) {
            this.task = new Runnable() {
                public void run() {
                    schedulerLogger.info("Started running scheduler task - " + name);
                    try {
                        task.run(getStack());
                        schedulerLogger.info("Finished running scheduler task - " + name);
                    } catch (Throwable e) {
                        schedulerLogger.error("Error while running scheduler task - " + name + " :", e);
                        throw new RuntimeException(e);
                    }
                }
            };
            this.scheduledTaskId = scheduledTaskId;
            this.runAtStart = runAtStart;
            this.startDate = startDate;
            this.period = period;
            this.fixedDelay = fixedDelay;
            assert startDate != null || period != null;
        }

        public ScheduledFuture execute(ScheduledExecutorService service) {
            return service.schedule(task, 0, TimeUnit.MILLISECONDS);
        }

        public List<ScheduledFuture> schedule(ScheduledExecutorService service) {
            List<ScheduledFuture> scheduledFutureList = new ArrayList<>();
            if (runAtStart) {
                scheduledFutureList.add(service.schedule(task, 0, TimeUnit.MILLISECONDS));
            }
            Long start = null;
            if (startDate != null)
                start = startDate.getTime();
            long currentTime = System.currentTimeMillis();
            if (period != null) {
                long longPeriod = period.longValue() * 1000;
                long delay;
                if(start != null) {
                    if (start < currentTime) {
                        int periods = (int) ((currentTime - start) / longPeriod) + 1;
                        start += periods * longPeriod;
                    }
                    delay = start - currentTime;
                } else {
                    if(runAtStart)
                        delay = longPeriod;
                    else
                        delay = 0L;
                }
                if (fixedDelay)
                    scheduledFutureList.add(service.scheduleWithFixedDelay(task, delay, longPeriod, TimeUnit.MILLISECONDS));
                else
                    scheduledFutureList.add(service.scheduleAtFixedRate(task, delay, longPeriod, TimeUnit.MILLISECONDS));
            } else {
                assert start != null;
                if (start > currentTime) {
                    scheduledFutureList.add(service.schedule(task, start - currentTime, TimeUnit.MILLISECONDS));
                }
            }
            return scheduledFutureList;
        }
    }

    public void stopScheduledTasks() {
        if (daemonTasksExecutor != null)
            daemonTasksExecutor.shutdownNow();
    }

    private class UserSchedulerTask implements EExecutionStackRunnable {
        String nameScheduledTask;
        private DataObject scheduledTaskObject;
        Time defaultTime = new Time(0, 0, 0);
        TreeMap<Integer, ScheduledTaskDetail> lapMap;
        private Time timeFrom;
        private Time timeTo;
        private Set<String> daysOfWeek;
        private Set<String> daysOfMonth;
        private DataObject currentScheduledTaskLogFinishObject;
        private DataSession afterFinishLogSession;
        private boolean afterFinishErrorOccurred;

        public UserSchedulerTask(String nameScheduledTask, DataObject scheduledTaskObject, TreeMap<Integer, ScheduledTaskDetail> lapMap, Time timeFrom, Time timeTo, Set<String> daysOfWeek, Set<String> daysOfMonth) {
            this.nameScheduledTask = nameScheduledTask;
            this.scheduledTaskObject = scheduledTaskObject;
            this.lapMap = lapMap;
            this.timeFrom = timeFrom == null ? defaultTime : timeFrom;
            this.timeTo = timeTo == null ? defaultTime : timeTo;
            this.daysOfWeek = daysOfWeek;
            this.daysOfMonth = daysOfMonth;
        }

        public void run(ExecutionStack stack) throws Exception {
            final Result<Long> threadId = new Result<>();
            Future<Boolean> future = null;
            try {
                boolean isTimeToRun = isTimeToRun(timeFrom, timeTo, daysOfWeek, daysOfMonth);
                schedulerLogger.info(String.format("Task %s. TimeFrom %s, TimeTo %s, daysOfWeek %s, daysOfMonth %s. %s",
                        nameScheduledTask, timeFrom == null ? "-" : timeFrom, timeTo == null ? "-" : timeTo,
                        daysOfWeek.isEmpty() ? "-" : daysOfWeek, daysOfMonth.isEmpty() ? "-" : daysOfMonth,
                        isTimeToRun ? "Started successful" : "Not started due to conditions"));

                if(isTimeToRun) {
                    ExecutorService mirrorMonitorService = null;
                    try {
                        for (final ScheduledTaskDetail detail : lapMap.values()) {
                            if (detail != null) {

                                if (mirrorMonitorService == null)
                                    mirrorMonitorService = ExecutorFactory.createMonitorMirrorSyncService(Scheduler.this);

                                future = mirrorMonitorService.submit(new Callable<Boolean>() {
                                    public Boolean call() throws Exception {
                                        threadId.set(Thread.currentThread().getId());
                                        try {
                                            return run(detail);
                                        } finally {
                                            threadId.set(null);
                                        }
                                    }});
                                boolean succeeded;
                                try {
                                    if (detail.timeout == null)
                                        succeeded = future.get();
                                    else
                                        succeeded = future.get(detail.timeout, TimeUnit.SECONDS);
                                } catch (TimeoutException e) {
                                    afterFinishErrorOccurred = true;
                                    ThreadUtils.interruptThread(afterFinishLogSession.sql, threadId.result, future);
                                    schedulerLogger.error("Timeout error while running scheduler task (in executeLAP()) " + detail.lap.property.caption);
                                    try (DataSession timeoutLogSession = dbManager.createSession(getLogSql())) {
                                        DataObject timeoutScheduledTaskLogFinishObject = timeoutLogSession.addObject(BL.schedulerLM.scheduledTaskLog);
                                        BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTaskObject, (ExecutionEnvironment) timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                        BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                        BL.schedulerLM.resultScheduledTaskLog.change("Timeout error", timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                        BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                        BL.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), timeoutLogSession, timeoutScheduledTaskLogFinishObject);

                                        timeoutLogSession.apply(BL, stack);
                                    } catch (Exception ie) {
                                        schedulerLogger.error("Error while reporting exception in scheduler task (in executeLAPThread) " + detail.lap.property.caption + " : ", ie);
                                    }
                                    succeeded = false;
                                }

                                if (!succeeded && !detail.ignoreExceptions)
                                    break;
                            }
                        }
                    } finally {
                        if(mirrorMonitorService != null)
                            mirrorMonitorService.shutdown();
                    }
                }
            } catch (Exception e) {
                if (future != null) {
                    try {
                        ThreadUtils.interruptThread(afterFinishLogSession.sql, threadId.result, future);
                    } catch (SQLException | SQLHandledException ignored) {
                    }
                }
                throw e;
            }
        }

        private boolean isTimeToRun(Time timeFrom, Time timeTo, Set<String> daysOfWeek, Set<String> daysOfMonth) {
            Calendar currentCal = Calendar.getInstance();

            if((!daysOfWeek.isEmpty() && !daysOfWeek.contains(String.valueOf(currentCal.get(Calendar.DAY_OF_WEEK) - 1)))
                    || (!daysOfMonth.isEmpty() && !daysOfMonth.contains(String.valueOf(currentCal.get(Calendar.DAY_OF_MONTH)))))
                return false;

            if(timeFrom.equals(defaultTime) && timeTo.equals(defaultTime)) return true;

            Calendar calendarFrom = Calendar.getInstance();
            calendarFrom.setTime(timeFrom);
            calendarFrom.set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH));
            calendarFrom.set(Calendar.MONTH, currentCal.get(Calendar.MONTH));
            calendarFrom.set(Calendar.YEAR, currentCal.get(Calendar.YEAR));

            Calendar calendarTo = Calendar.getInstance();
            calendarTo.setTime(timeTo);
            calendarTo.set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH));
            if(timeFrom.compareTo(timeTo) > 0)
                calendarTo.add(Calendar.DAY_OF_MONTH, 1);
            calendarTo.set(Calendar.MONTH, currentCal.get(Calendar.MONTH));
            calendarTo.set(Calendar.YEAR, currentCal.get(Calendar.YEAR));

            return currentCal.getTimeInMillis() >= calendarFrom.getTimeInMillis() && currentCal.getTimeInMillis() <= calendarTo.getTimeInMillis();
        }

        public boolean run(ScheduledTaskDetail detail) {
            try {
                ExecutionStack stack = getStack(); // иначе assertion'ы внутри с проверкой контекста валятся
                Context prevContext = ThreadLocalContext.wrapContext(new SchedulerContext());
                try {
                    return executeLAP(detail, stack);
                } finally {
                    ThreadLocalContext.unwrapContext(prevContext);
                }
            } catch (SQLException | SQLHandledException e) {
                schedulerLogger.error("Error while running scheduler task (in ExecuteLAPThread):", e);
                return false;
            }
        }

        private boolean executeLAP(ScheduledTaskDetail detail, ExecutionStack stack) throws SQLException, SQLHandledException {
            try (DataSession beforeStartLogSession = dbManager.createSession()) {
                DataObject currentScheduledTaskLogStartObject = beforeStartLogSession.addObject(BL.schedulerLM.scheduledTaskLog);
                afterFinishLogSession = dbManager.createSession(getLogSql());
                afterFinishErrorOccurred = false;
                currentScheduledTaskLogFinishObject = afterFinishLogSession.addObject(BL.schedulerLM.scheduledTaskLog);
                try {
                    BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTaskObject, (ExecutionEnvironment) beforeStartLogSession, currentScheduledTaskLogStartObject);
                    BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", beforeStartLogSession, currentScheduledTaskLogStartObject);
                    BL.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), beforeStartLogSession, currentScheduledTaskLogStartObject);
                    BL.schedulerLM.resultScheduledTaskLog.change("Запущено" + (detail.script == null ? "" : (" " + BaseUtils.truncate(detail.script, 191))), beforeStartLogSession, currentScheduledTaskLogStartObject);
                    beforeStartLogSession.apply(BL, stack);

                    try (DataSession mainSession = dbManager.createSession()) {
                        if (detail.script != null)
                            BL.schedulerLM.scriptText.change(detail.script, mainSession);
                        ImOrderSet<ClassPropertyInterface> interfaces = detail.lap.listInterfaces;
                        schedulerLogger.info("Before execute " + detail.lap.property.getSID());
                        if (interfaces.isEmpty())
                            detail.lap.execute(mainSession, stack);
                        else if (detail.params.isEmpty())
                            detail.lap.execute(mainSession, stack, NullValue.instance);
                        else {
                            List<ObjectValue> parsedParameters = new ArrayList<>();
                            for(int i = 0; i < interfaces.size(); i++) {
                                ValueClass valueClass = interfaces.get(i).interfaceClass;
                                ObjectValue parsedParameter;
                                try {
                                    parsedParameter = detail.params.size() < i ? NullValue.instance : (valueClass == IntegerClass.instance ?
                                            new DataObject(((IntegerClass) valueClass).parseString(detail.params.get(i))) : new DataObject(detail.params.get(i)));
                                } catch (Exception e) {
                                    parsedParameter = null;
                                }
                                parsedParameters.add(parsedParameter);
                            }
                            detail.lap.execute(mainSession, stack, parsedParameters.toArray(new ObjectValue[parsedParameters.size()]));
                        }
                        schedulerLogger.info("Before apply " + detail.lap.property.getSID());
                        String applyResult = mainSession.applyMessage(BL, stack);
                        schedulerLogger.info("After apply " + detail.lap.property.getSID());
                        BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTaskObject, (ExecutionEnvironment) afterFinishLogSession, currentScheduledTaskLogFinishObject);
                        BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", afterFinishLogSession, currentScheduledTaskLogFinishObject);
                        BL.schedulerLM.resultScheduledTaskLog.change(applyResult == null ? (afterFinishErrorOccurred ? "Выполнено с ошибками" : "Выполнено успешно") : BaseUtils.truncate(applyResult, 200), afterFinishLogSession, currentScheduledTaskLogFinishObject);
                        if (applyResult != null)
                            BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, beforeStartLogSession, currentScheduledTaskLogStartObject);
                        if (afterFinishErrorOccurred)
                            BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, afterFinishLogSession, currentScheduledTaskLogFinishObject);
                        afterFinishErrorOccurred = false;
                        BL.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), afterFinishLogSession, currentScheduledTaskLogFinishObject);

                        String finishResult = afterFinishLogSession.applyMessage(BL, stack);
                        if (finishResult != null)
                            schedulerLogger.error("Error while saving scheduler task result " + detail.lap.property.caption + " : " + finishResult);
                        return applyResult == null || detail.ignoreExceptions;
                    }
                } catch (Exception e) {
                    //not timeout exception
                    if (e.getMessage() == null || !e.getMessage().contains("FATAL: terminating connection due to administrator command")) {
                        schedulerLogger.error("Error while running scheduler task (in executeLAP()) " + detail.lap.property.caption + " : ", e);

                        try {
                            Timestamp time = new Timestamp(System.currentTimeMillis());
                            BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTaskObject, (ExecutionEnvironment) afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            BL.schedulerLM.resultScheduledTaskLog.change(BaseUtils.truncate(String.valueOf(e), 200), afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            BL.schedulerLM.dateScheduledTaskLog.change(time, afterFinishLogSession, currentScheduledTaskLogFinishObject);

                            DataObject scheduledClientTaskLogObject = afterFinishLogSession.addObject(BL.schedulerLM.scheduledClientTaskLog);
                            BL.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                                    .change(currentScheduledTaskLogFinishObject, (ExecutionEnvironment) afterFinishLogSession, scheduledClientTaskLogObject);
                            BL.schedulerLM.messageScheduledClientTaskLog.change(ExceptionUtils.getStackTrace(e), afterFinishLogSession, scheduledClientTaskLogObject);
                            BL.schedulerLM.dateScheduledClientTaskLog.change(time, afterFinishLogSession, scheduledClientTaskLogObject);

                            afterFinishLogSession.apply(BL, stack);
                        } catch (Exception ie) {
                            schedulerLogger.error("Error while reporting exception in scheduler task (in executeLAPThread) " + detail.lap.property.caption + " : ", ie);
                        }
                    }
                    return detail.ignoreExceptions;
                }
            }
        }

        public class SchedulerContext extends WrapperContext {

            @Override
            public void delayUserInteraction(ClientAction action) {
                String message = null;
                if (action instanceof LogMessageClientAction) {
                    LogMessageClientAction logAction = (LogMessageClientAction) action;
                    message = logAction.message + "\n" + LogicsInstanceContext.errorDataToTextTable(logAction.titles, logAction.data);
                } else if (action instanceof MessageClientAction) {
                    message = ((MessageClientAction) action).message;
                }
                if (message != null) {
                    ServerLoggers.serviceLogger.info(message);
                    try {
                        DataObject scheduledClientTaskLogObject = afterFinishLogSession.addObject(BL.schedulerLM.scheduledClientTaskLog);
                        BL.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                                .change(currentScheduledTaskLogFinishObject, (ExecutionEnvironment) afterFinishLogSession, scheduledClientTaskLogObject);
                        BL.schedulerLM.messageScheduledClientTaskLog.change(message, afterFinishLogSession, scheduledClientTaskLogObject);
                        BL.schedulerLM.dateScheduledClientTaskLog.change(new Timestamp(System.currentTimeMillis()), afterFinishLogSession, scheduledClientTaskLogObject);
                    } catch (SQLException | SQLHandledException e) {
                        throw Throwables.propagate(e);
                    }
                }
                try {
                    super.delayUserInteraction(action);
                } catch (Exception e) {
                    schedulerLogger.error("Error while executing delayUserInteraction in SchedulerContext", e);
                    afterFinishErrorOccurred = true;
                }
            }
        }
    }

    private class ScheduledTaskDetail {
        public LAP lap;
        public String script;
        public boolean ignoreExceptions;
        public Integer timeout;
        public List<String> params;

        public ScheduledTaskDetail(LAP lap, String script, boolean ignoreExceptions, Integer timeout, List<String> params) {
            this.lap = lap;
            this.script = script;
            this.ignoreExceptions = ignoreExceptions;
            this.timeout = timeout;
            this.params = params;
        }
    }
}
