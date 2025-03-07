package lsfusion.server.physics.admin.scheduler.controller.manager;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.StackNewThread;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.base.controller.thread.WrappingScheduledExecutorService;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.EExecutionStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.base.DateConverter.*;
import static org.apache.commons.lang3.StringUtils.trim;

public class Scheduler extends MonitorServer implements InitializingBean {
    public static final Logger schedulerLogger = ServerLoggers.schedulerLogger;

    public ScheduledExecutorService daemonTasksExecutor;

    private LogicsInstance logicsInstance;

    private BusinessLogics BL;
    private DBManager dbManager;

    private Map<Long, List<ScheduledFuture>> futuresMap = new HashMap<>();

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

    @Override
    protected void onStopping(LifecycleEvent event) {
        schedulerLogger.error("SERVER STOPPING, all scheduled tasks will be interrupted");
        super.onStopping(event);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(BL, "businessLogics must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        Assert.notNull(logicsInstance, "logicsInstance must be specified");
    }

    @Override
    public String getEventName() {
        return "scheduler-daemon";
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    private boolean isServer() {
        return dbManager.isServer();
    }

    public boolean setupScheduledTask(DataSession session, DataObject scheduledTaskObject, String nameScheduledTask) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {
        if (daemonTasksExecutor != null && isServer()) {
            Long scheduledTaskId = (Long) scheduledTaskObject.getValue();
            List<ScheduledFuture> futures = futuresMap.remove(scheduledTaskId);
            if (futures != null) {
                schedulerLogger.info("Stopped scheduler task: " + nameScheduledTask);
                for (ScheduledFuture future : futures) {
                    future.cancel(true);
                }
                executingTasks.remove(scheduledTaskId);
            }

            if(BL.schedulerLM.activeScheduledTask.read(session, scheduledTaskObject) != null) {
                LocalTime timeFrom = (LocalTime) BL.schedulerLM.timeFromScheduledTask.read(session, scheduledTaskObject);
                LocalTime timeTo = (LocalTime) BL.schedulerLM.timeToScheduledTask.read(session, scheduledTaskObject);
                String daysOfWeek = (String) BL.schedulerLM.daysOfWeekScheduledTask.read(session, scheduledTaskObject);
                String daysOfMonth = (String) BL.schedulerLM.daysOfMonthScheduledTask.read(session, scheduledTaskObject);
                boolean runAtStart = BL.schedulerLM.runAtStartScheduledTask.read(session, scheduledTaskObject) != null;
                LocalDateTime startDate = (LocalDateTime) BL.schedulerLM.startDateScheduledTask.read(session, scheduledTaskObject);
                Integer period = (Integer) BL.schedulerLM.periodScheduledTask.read(session, scheduledTaskObject);
                Object schedulerStartType = BL.schedulerLM.schedulerStartTypeScheduledTask.read(session, scheduledTaskObject);
                Object afterFinish = ((ConcreteCustomClass) BL.schedulerLM.findClass("SchedulerStartType")).getDataObject("afterFinish").object;
                boolean fixedDelay = afterFinish.equals(schedulerStartType);

                if (startDate != null || runAtStart) {
                    schedulerLogger.info("Scheduled scheduler task: " + nameScheduledTask);
                    SchedulerTask task = new SchedulerTask(nameScheduledTask, readUserSchedulerTask(session, session.getModifier(), scheduledTaskObject, nameScheduledTask,
                            timeFrom, timeTo, daysOfWeek, daysOfMonth), scheduledTaskId, runAtStart, startDate, period, fixedDelay);
                    futures = task.schedule(daemonTasksExecutor);
                    futuresMap.put(scheduledTaskId, futures);
                }
            }
            return true;
        }
        return false;
    }

    public boolean executeScheduledTask(DataSession session, DataObject scheduledTaskObject, String nameScheduledTask) throws SQLException, SQLHandledException {
        if (daemonTasksExecutor != null && isServer()) {
            Long scheduledTaskId = (Long) scheduledTaskObject.getValue();
            List<ScheduledFuture> futures = futuresMap.get(scheduledTaskId);
            if (futures == null)
                futures = new ArrayList<>();

            schedulerLogger.info("Execute scheduler task: " + nameScheduledTask);
            SchedulerTask task = new SchedulerTask(nameScheduledTask, readUserSchedulerTask(session, session.getModifier(), scheduledTaskObject, nameScheduledTask,
                    null, null, null, null), scheduledTaskId, true, null, 0, false);
            futures.add(task.execute(daemonTasksExecutor));
            futuresMap.put(scheduledTaskId, futures);

            return true;
        }

        return false;
    }

    public boolean setupScheduledTasks(ExecutionContext context, Integer threadCount) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {

        stopScheduledTasks(context);

        daemonTasksExecutor = ExecutorFactory.createMonitorScheduledThreadService((threadCount != null ? threadCount : 5) + 1, this); // +1 because resetResourcesCacheTasks() should always be running

        boolean isServer = isServer();
        List<SchedulerTask> tasks = new ArrayList<>(BL.getSystemTasks(this, isServer));
        if(isServer)
            fillUserScheduledTasks(context.getSession(), tasks);

        for (SchedulerTask task : tasks) {
            List<ScheduledFuture> futures = futuresMap.get(task.scheduledTaskId);
            if (futures == null)
                futures = new ArrayList<>();
            futures.addAll(task.schedule(daemonTasksExecutor));
            futuresMap.put(task.scheduledTaskId, futures);
        }

        return !isServer;
    }

    private void fillUserScheduledTasks(DataSession session, List<SchedulerTask> tasks) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        Modifier modifier = session.getModifier();
        KeyExpr scheduledTaskExpr = new KeyExpr("scheduledTask");
        ImRevMap<Object, KeyExpr> scheduledTaskKeys = MapFact.singletonRev("scheduledTask", scheduledTaskExpr);

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
            Long scheduledTaskId = (Long) key.getValue(0);
            DataObject currentScheduledTaskObject = new DataObject(scheduledTaskId, BL.schedulerLM.userScheduledTask);
            String nameScheduledTask = trim((String) value.get("nameScheduledTask"));
            Boolean runAtStart = value.get("runAtStartScheduledTask") != null;
            LocalDateTime startDate = (LocalDateTime) value.get("startDateScheduledTask");
            LocalTime timeFrom = (LocalTime) value.get("timeFromScheduledTask");
            LocalTime timeTo = (LocalTime) value.get("timeToScheduledTask");
            String daysOfWeekScheduledTask = (String) value.get("daysOfWeekScheduledTask");
            String daysOfMonthScheduledTask = (String) value.get("daysOfMonthScheduledTask");
            Integer period = (Integer) value.get("periodScheduledTask");
            Object schedulerStartType = value.get("schedulerStartTypeScheduledTask");
            boolean fixedDelay = afterFinish.equals(schedulerStartType);

            if(startDate != null || runAtStart)
                tasks.add(new SchedulerTask(nameScheduledTask, readUserSchedulerTask(session, modifier, currentScheduledTaskObject, nameScheduledTask, timeFrom, timeTo,
                    daysOfWeekScheduledTask, daysOfMonthScheduledTask), scheduledTaskId, runAtStart, startDate, period, fixedDelay));
        }
    }

    private UserSchedulerTask readUserSchedulerTask(DataSession session, Modifier modifier, DataObject scheduledTaskObject, String nameScheduledTask,
                                                    LocalTime timeFrom, LocalTime timeTo, String daysOfWeekScheduledTask, String daysOfMonthScheduledTask) throws SQLException, SQLHandledException {
        KeyExpr scheduledTaskDetailExpr = new KeyExpr("scheduledTaskDetail");
        ImRevMap<Object, KeyExpr> scheduledTaskDetailKeys = MapFact.singletonRev("scheduledTaskDetail", scheduledTaskDetailExpr);

        QueryBuilder<Object, Object> scheduledTaskDetailQuery = new QueryBuilder<>(scheduledTaskDetailKeys);
        scheduledTaskDetailQuery.addProperty("canonicalNameAction", BL.schedulerLM.canonicalNameActionScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("ignoreExceptions", BL.schedulerLM.ignoreExceptionsScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("order", BL.schedulerLM.orderScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("script", BL.schedulerLM.scriptScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("timeout", BL.schedulerLM.timeoutScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.addProperty("parameter", BL.schedulerLM.parameterScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
        scheduledTaskDetailQuery.and(BL.schedulerLM.activeScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr).getWhere());
        scheduledTaskDetailQuery.and(BL.schedulerLM.scheduledTaskScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr).compare(scheduledTaskObject, Compare.EQUALS));

        TreeMap<Integer, ScheduledTaskDetail> propertySIDMap = new TreeMap<>();
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> propertyResult = scheduledTaskDetailQuery.execute(session, MapFact.singletonOrder("order", false));
        int defaultOrder = propertyResult.size() + 100;
        for (ImMap<Object, Object> propertyValues : propertyResult.valueIt()) {
            String canonicalName = (String) propertyValues.get("canonicalNameAction");
            String script = (String) propertyValues.get("script");
            if(script != null && !script.isEmpty())
                script = String.format("run() {%s;\n};", script);
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
                } else {
                    while(propertySIDMap.containsKey(orderProperty)) {
                        orderProperty++;
                    }
                }
                LA LA = script == null ? BL.findAction(canonicalName.trim()) : BL.schedulerLM.evalScript;
                if(LA != null)
                    propertySIDMap.put(orderProperty, new ScheduledTaskDetail(LA, script, ignoreExceptions, timeout, params));
            }
        }
        return new UserSchedulerTask(nameScheduledTask, scheduledTaskObject, propertySIDMap, timeFrom, timeTo,
                splitAndTrim(daysOfWeekScheduledTask), splitAndTrim(daysOfMonthScheduledTask));
    }

    private Set<String> splitAndTrim(String value) {
        return value == null ? new HashSet<>() : Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    public class SystemSchedulerTask extends SchedulerTask {

        public SystemSchedulerTask(final EExecutionStackRunnable task, Long scheduledTaskId, boolean runAtStart, Integer period, boolean fixedDelay, String name) {
            super(name, task, scheduledTaskId, runAtStart, null, period, fixedDelay);
        }
    }

    public SchedulerTask createSystemTask(EExecutionStackRunnable task, boolean runAtStart, Integer period, boolean fixedDelay, String name) {
        return new SystemSchedulerTask(task, -1L, runAtStart, period, fixedDelay, name);
    }

    private static final LocalTime defaultTime = LocalTime.MIDNIGHT;

    public void stopScheduledTasks(ExecutionContext<ClassPropertyInterface> context) {
        if (daemonTasksExecutor != null) {
            ThreadUtils.interruptThreadExecutor(daemonTasksExecutor, context);
            daemonTasksExecutor = null;
        }
    }

    Set<Object> executingTasks = new HashSet<>();

    public class SchedulerTask {
        private final Long scheduledTaskId;
        private final boolean runAtStart;
        private final LocalDateTime startDate;
        private final Integer period;
        private final boolean fixedDelay;
        private final Runnable task;

        public SchedulerTask(final String name, final EExecutionStackRunnable task, Long scheduledTaskId, boolean runAtStart, LocalDateTime startDate, Integer period, boolean fixedDelay) {
            this.task = () -> {
                schedulerLogger.info("Started running scheduler task - " + name);
                try {
                    task.run(getStack());
                    schedulerLogger.info("Finished running scheduler task - " + name);
                } catch (Throwable e) {
                    schedulerLogger.error("Error while running scheduler task - " + name + " :", e);
                    throw Throwables.propagate(e);
                }
            };
            this.scheduledTaskId = scheduledTaskId;
            this.runAtStart = runAtStart;
            this.startDate = startDate;
            this.period = period;
            this.fixedDelay = fixedDelay;
            assert startDate != null || period != null || runAtStart;
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
                start = localDateTimeToSqlTimestamp(startDate).getTime();
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
                assert start != null || runAtStart;
                if (start != null && start > currentTime) {
                    scheduledFutureList.add(service.schedule(task, start - currentTime, TimeUnit.MILLISECONDS));
                }
            }
            return scheduledFutureList;
        }
    }

    private class UserSchedulerTask implements EExecutionStackRunnable {
        private final String nameScheduledTask;
        private final DataObject scheduledTaskObject;
        private final TreeMap<Integer, ScheduledTaskDetail> lapMap;
        private final LocalTime timeFrom;
        private final LocalTime timeTo;
        private final Set<String> daysOfWeek;
        private final Set<String> daysOfMonth;

        public UserSchedulerTask(String nameScheduledTask, DataObject scheduledTaskObject, TreeMap<Integer, ScheduledTaskDetail> lapMap, LocalTime timeFrom, LocalTime timeTo, Set<String> daysOfWeek, Set<String> daysOfMonth) {
            this.nameScheduledTask = nameScheduledTask;
            this.scheduledTaskObject = scheduledTaskObject;
            this.lapMap = lapMap;
            this.timeFrom = timeFrom == null ? defaultTime : timeFrom;
            this.timeTo = timeTo == null ? defaultTime : timeTo;
            this.daysOfWeek = daysOfWeek;
            this.daysOfMonth = daysOfMonth;
        }

        @StackNewThread
        @StackMessage("scheduler.scheduled.task")
        @ThisMessage
        public void run(ExecutionStack stack) {
            boolean isTimeToRun = isTimeToRun();
            boolean alreadyExecuting = executingTasks.contains(scheduledTaskObject.getValue());

            logInitTask(isTimeToRun, alreadyExecuting);

            if(isTimeToRun) {
                if (alreadyExecuting) {
                    logAlreadyExecutingTask(nameScheduledTask, stack);
                } else {
                    executingTasks.add(scheduledTaskObject.getValue());
                    try {
                        for (final ScheduledTaskDetail detail : lapMap.values())
                            if (!run(detail) && !detail.ignoreExceptions)
                                break;
                    } finally {
                        executingTasks.remove(scheduledTaskObject.getValue());
                    }
                }
            }
        }

        private boolean isTimeToRun() {
            LocalDate currentDate = LocalDate.now();
            LocalDateTime currentDateTime = LocalDateTime.now();

            if ((!daysOfWeek.isEmpty() && !daysOfWeek.contains(String.valueOf(currentDateTime.getDayOfWeek().getValue() - 1)))
                    || (!daysOfMonth.isEmpty() && !daysOfMonth.contains(String.valueOf(currentDateTime.getDayOfMonth())))) {
                return false;
            }

            if (timeFrom.equals(defaultTime) && timeTo.equals(defaultTime)) {
                return true;
            }

            LocalDateTime dateTimeFrom = timeFrom.atDate(currentDate);
            LocalDateTime dateTimeTo = timeTo.atDate(currentDate);
            if (timeFrom.isAfter(timeTo)) {
                dateTimeTo = dateTimeTo.plusDays(1);
            }

            return currentDateTime.compareTo(dateTimeFrom) >= 0 && currentDateTime.compareTo(dateTimeTo) <= 0;
        }

        @StackNewThread
        @StackMessage("scheduler.scheduled.task")
        @ThisMessage
        public boolean run(ScheduledTaskDetail detail) {
            ExecutionStack stack = getStack(); // иначе assertion'ы внутри с проверкой контекста валятся

            Long taskLogId = null;
            Throwable throwable = null;

            String taskCaption = detail.getCaption();
            
            ThreadLocalContext.pushLogMessage();
            try {
                logStartTask(taskCaption, stack);

                String applyResult = ExecutorFactory.executeWithTimeout(BL, () -> {
                    try (DataSession mainSession = createSession()) {
                        if (detail.script != null) // if LA is evalScript
                            BL.schedulerLM.scriptText.change(detail.script, mainSession);
                        ImOrderSet<ClassPropertyInterface> interfaces = detail.LA.listInterfaces;
                        if (interfaces.isEmpty())
                            detail.LA.execute(mainSession, stack);
                        else if (detail.params.isEmpty())
                            detail.LA.execute(mainSession, stack, NullValue.instance);
                        else {
                            List<ObjectValue> parsedParameters = new ArrayList<>();
                            for (int i = 0; i < interfaces.size(); i++) {
                                ValueClass valueClass = interfaces.get(i).interfaceClass;
                                ObjectValue parsedParameter;
                                try {
                                    parsedParameter = detail.params.size() < i ? NullValue.instance : (valueClass == IntegerClass.instance ? new DataObject(((IntegerClass) valueClass).parseString(detail.params.get(i))) : new DataObject(detail.params.get(i)));
                                } catch (Exception e) {
                                    parsedParameter = null;
                                }
                                parsedParameters.add(parsedParameter);
                            }
                            detail.LA.execute(mainSession, stack, parsedParameters.toArray(new ObjectValue[parsedParameters.size()]));
                        }

                        logBeforeApplyTask(taskCaption);

                        return mainSession.applyMessage(BL, stack);
                    }
                }, detail.timeout, () -> ExecutorFactory.createMonitorMirrorSyncService(Scheduler.this));

                taskLogId = logFinishTask(taskCaption, stack, applyResult);
                
                return applyResult == null;
            } catch (Throwable t) {
                taskLogId = logExceptionTask(taskCaption, t, stack);
                throwable = t;
                return false;
            } finally {
                ImList<AbstractContext.LogMessage> logMessages = ThreadLocalContext.popLogMessage();
                if(throwable != null)
                    logMessages = logMessages.addList(new AbstractContext.LogMessage(ExceptionUtils.toString(throwable), true, ExecutionStackAspect.getExceptionStackTrace()));
                if(taskLogId != null)
                    logClientTasks(logMessages, taskLogId, taskCaption, stack);
            }
        }

        private void logInitTask(boolean isTimeToRun, boolean alreadyExecuting) {
            if (daemonTasksExecutor instanceof WrappingScheduledExecutorService) {
                schedulerLogger.info(((WrappingScheduledExecutorService) daemonTasksExecutor).getThreadPoolInfo());
            }
            schedulerLogger.info(String.format("Task %s. TimeFrom %s, TimeTo %s, daysOfWeek %s, daysOfMonth %s. %s",
                    nameScheduledTask, timeFrom == null ? "-" : timeFrom, timeTo == null ? "-" : timeTo,
                    daysOfWeek.isEmpty() ? "-" : daysOfWeek, daysOfMonth.isEmpty() ? "-" : daysOfMonth,
                    isTimeToRun ? alreadyExecuting ? "Already executing" : "Started successful" : "Not started due to conditions"));
        }

        private void logBeforeApplyTask(String taskCaption) {
            schedulerLogger.info("Task " + taskCaption + " before apply");
        }

        private void logStartTask(String taskCaption, ExecutionStack stack) {
            logTask(taskCaption, ServerResourceBundle.getString("scheduler.started"), "start", stack, null);
        }

        private void logAlreadyExecutingTask(String taskCaption, ExecutionStack stack) {
            logTask(taskCaption, ServerResourceBundle.getString("scheduler.already.executing"), "start", stack, null, true);
        }

        private Long logFinishTask(String taskCaption, ExecutionStack stack, String applyResult) {
            return logTask(taskCaption, applyResult == null ? ServerResourceBundle.getString("scheduler.finished.successfully") : BaseUtils.truncate(applyResult, 200), "exception", stack, null);
        }

        private Long logExceptionTask(String taskCaption, Throwable t, ExecutionStack stack) {
            return logTask(taskCaption, BaseUtils.truncate(String.valueOf(t), 200), "exception", stack, t);
        }

        private Long logTask(String message, String result, String phase, ExecutionStack stack, Throwable t) {
            return logTask(message, result, phase, stack, t, false);
        }

        private Long logTask(String message, String result, String phase, ExecutionStack stack, Throwable t, boolean error) {
            if(t != null)
                schedulerLogger.error("Exception in task : " + message + " - " + result, t);
            else
                schedulerLogger.info("Task " + message + " - " + result + " " + phase);
            
            try (DataSession session = createSession()) {
                DataObject taskLogObject = session.addObject(BL.schedulerLM.scheduledTaskLog);

                BL.schedulerLM.scheduledTaskScheduledTaskLog.change(new DataObject((Long)scheduledTaskObject.getValue(), BL.schedulerLM.userScheduledTask), (ExecutionEnvironment) session, taskLogObject);
                BL.schedulerLM.propertyScheduledTaskLog.change(message, session, taskLogObject);
                BL.schedulerLM.dateScheduledTaskLog.change(LocalDateTime.now(), session, taskLogObject);
                if(t != null || error)
                    BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, session, taskLogObject);
                BL.schedulerLM.resultScheduledTaskLog.change(result, session, taskLogObject);

                session.applyException(BL, stack);

                return (long)taskLogObject.object;
            } catch (Exception le) {
                schedulerLogger.error("Error while logging scheduler task " + phase + " : " + message + " " + result, le);
                return null;
            }
        }

        private void logClientTasks(ImList<AbstractContext.LogMessage> logMessages, long taskLogId, String taskCaption, ExecutionStack stack) {
            DataObject taskLog = new DataObject(taskLogId, BL.schedulerLM.scheduledTaskLog);
            try (DataSession session = createSession()) {
                
                for(AbstractContext.LogMessage logMessage : logMessages)
                    logClientTask(session, taskLog, logMessage);

                session.applyException(BL, stack);
            } catch (Exception e) {
                schedulerLogger.error("Error while logging scheduler messages : " + taskCaption, e);
            }
        }

        private void logClientTask(DataSession session, DataObject taskLog, AbstractContext.LogMessage logMessage) throws SQLException, SQLHandledException {            
            ServerLoggers.schedulerLogger.info(logMessage.message);
            
            DataObject clientTaskLog = session.addObject(BL.schedulerLM.scheduledClientTaskLog);
            BL.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                    .change(taskLog, (ExecutionEnvironment) session, clientTaskLog);
            BL.schedulerLM.messageScheduledClientTaskLog.change(logMessage.message, session, clientTaskLog);
            String lsfStack = logMessage.lsfStackTrace;
            if(lsfStack != null)
                BL.schedulerLM.lsfStackScheduledClientTaskLog.change(lsfStack, session, clientTaskLog);
            if(logMessage.failed)
                BL.schedulerLM.failedScheduledClientTaskLog.change(true, session, clientTaskLog);
            BL.schedulerLM.dateScheduledClientTaskLog.change(sqlTimestampToLocalDateTime(new Timestamp(logMessage.time)), session, clientTaskLog);
        }
    }

    private class ScheduledTaskDetail {
        public LA LA;
        public String script;
        public boolean ignoreExceptions;
        public Integer timeout;
        public List<String> params;
        
        public String getCaption() {
            return (script == null ? (localize(LA.action.caption) + " (" + LA.action.getSID() + ")") : (" " + BaseUtils.truncate(script, 191)));
        }

        public ScheduledTaskDetail(LA LA, String script, boolean ignoreExceptions, Integer timeout, List<String> params) {
            this.LA = LA;
            this.script = script;
            this.ignoreExceptions = ignoreExceptions;
            this.timeout = timeout;
            this.params = params;
        }
    }
}
