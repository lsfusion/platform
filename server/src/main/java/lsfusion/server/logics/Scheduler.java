package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.Compare;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.WrapperContext;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.Context;
import lsfusion.server.context.ContextAwareThread;
import lsfusion.server.context.LogicsInstanceContext;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Reader;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.trimToNull;

public class Scheduler extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = ServerLoggers.systemLogger;

    public ScheduledExecutorService daemonTasksExecutor;

    private Context instanceContext;

    private BusinessLogics BL;
    private DBManager dbManager;

    public Scheduler() {
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.BL = businessLogics;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        instanceContext = logicsInstance.getContext();
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
        //assert logicsInstance by checking the context
        Assert.notNull(instanceContext, "logicsInstance must be specified");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        if(dbManager.isServer()) {
            logger.info("Starting Scheduler.");

            try (DataSession session = dbManager.createSession()) {
                setupScheduledTasks(session);
                BL.schedulerLM.findProperty("isStartedScheduler").change(true, session);
                session.apply(BL);
                setupCurrentDateSynchronization();
            } catch (SQLException | ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
                throw new RuntimeException("Error starting Scheduler: ", e);
            }
        } else {
            logger.info("Scheduler disabled, change serverComputer() to enable");
        }
    }

    private void setupCurrentDateSynchronization() {
        changeCurrentDate();

        Thread thread = new ContextAwareThread(instanceContext, new Runnable() {
            long time = 1000;
            boolean first = true;

            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR);
                calendar.get(Calendar.HOUR);
                calendar.get(Calendar.HOUR_OF_DAY);
                if (calendar.get(Calendar.AM_PM) == Calendar.PM) {
                    hour += 12;
                }
                time = (23 - hour) * 500 * 60 * 60;
                while (true) {
                    try {
                        calendar = Calendar.getInstance();
                        hour = calendar.get(Calendar.HOUR);
                        if (calendar.get(Calendar.AM_PM) == Calendar.PM) {
                            hour += 12;
                        }
                        if (hour == 0 && first) {
                            changeCurrentDate();
                            time = 12 * 60 * 60 * 1000;
                            first = false;
                        }
                        if (hour == 23) {
                            first = true;
                        }
                        Thread.sleep(time);
                        time = time / 2;
                        if (time < 1000) {
                            time = 1000;
                        }
                    } catch (Exception ignore) { // todo : сделать нормальную обработку ошибок
                        try {
                            Thread.sleep(10 * 60 * 1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void changeCurrentDate() {
        try {
            try (DataSession session = dbManager.createSession()) {
                java.sql.Date currentDate = (java.sql.Date) BL.timeLM.currentDate.read(session);
                java.sql.Date newDate = DateConverter.getCurrentDate();
                logger.info(String.format("ChangeCurrentDate started: from %s to %s", currentDate, newDate));
                if (currentDate == null || currentDate.getDate() != newDate.getDate() || currentDate.getMonth() != newDate.getMonth() || currentDate.getYear() != newDate.getYear()) {
                    BL.timeLM.currentDate.change(newDate, session);
                    String result = session.applyMessage(BL);
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

    public void setupScheduledTasks(DataSession session) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {

        Modifier modifier = session.getModifier();
        KeyExpr scheduledTaskExpr = new KeyExpr("scheduledTask");
        ImRevMap<Object, KeyExpr> scheduledTaskKeys = MapFact.<Object, KeyExpr>singletonRev("scheduledTask", scheduledTaskExpr);

        QueryBuilder<Object, Object> scheduledTaskQuery = new QueryBuilder<>(scheduledTaskKeys);
        scheduledTaskQuery.addProperty("runAtStartScheduledTask", BL.schedulerLM.runAtStartScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("startDateScheduledTask", BL.schedulerLM.startDateScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("timeFromScheduledTask", BL.schedulerLM.timeFromScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("timeToScheduledTask", BL.schedulerLM.timeToScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("periodScheduledTask", BL.schedulerLM.periodScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("daysOfWeekScheduledTask", BL.schedulerLM.daysOfWeekScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("daysOfMonthScheduledTask", BL.schedulerLM.daysOfMonthScheduledTask.getExpr(modifier, scheduledTaskExpr));
        scheduledTaskQuery.addProperty("schedulerStartTypeScheduledTask", BL.schedulerLM.schedulerStartTypeScheduledTask.getExpr(modifier, scheduledTaskExpr));

        scheduledTaskQuery.and(BL.schedulerLM.activeScheduledTask.getExpr(modifier, scheduledTaskExpr).getWhere());

        if (daemonTasksExecutor != null)
            daemonTasksExecutor.shutdownNow();

        daemonTasksExecutor = Executors.newScheduledThreadPool(3, new DaemonThreadFactory("scheduler-daemon"));

        Object afterFinish = ((ConcreteCustomClass) BL.schedulerLM.findClass("SchedulerStartType")).getDataObject("afterFinish").object;

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> scheduledTaskResult = scheduledTaskQuery.execute(session);
        for (int i = 0, size = scheduledTaskResult.size(); i < size; i++) {
            ImMap<Object, Object> key = scheduledTaskResult.getKey(i);
            ImMap<Object, Object> value = scheduledTaskResult.getValue(i);
            DataObject currentScheduledTaskObject = new DataObject(key.getValue(0), BL.schedulerLM.scheduledTask);
            Boolean runAtStart = value.get("runAtStartScheduledTask") != null;
            Timestamp startDate = (Timestamp) value.get("startDateScheduledTask");
            Time timeFrom = (Time) value.get("timeFromScheduledTask");
            Time timeTo = (Time) value.get("timeToScheduledTask");
            Integer period = (Integer) value.get("periodScheduledTask");
            Object schedulerStartType = value.get("schedulerStartTypeScheduledTask");

            String daysOfWeekScheduledTask = (String) value.get("daysOfWeekScheduledTask");
            Set<String> daysOfWeek = daysOfWeekScheduledTask == null ? new HashSet<>() : new HashSet(Arrays.asList(daysOfWeekScheduledTask.split(",| ")));
            daysOfWeek.remove("");
            String daysOfMonthScheduledTask = (String) value.get("daysOfMonthScheduledTask");
            Set<String> daysOfMonth = daysOfMonthScheduledTask == null ? new HashSet<>() : new HashSet(Arrays.asList(daysOfMonthScheduledTask.split(",| ")));
            daysOfMonth.remove("");

            KeyExpr scheduledTaskDetailExpr = new KeyExpr("scheduledTaskDetail");
            ImRevMap<Object, KeyExpr> scheduledTaskDetailKeys = MapFact.<Object, KeyExpr>singletonRev("scheduledTaskDetail", scheduledTaskDetailExpr);

            QueryBuilder<Object, Object> scheduledTaskDetailQuery = new QueryBuilder<>(scheduledTaskDetailKeys);
            scheduledTaskDetailQuery.addProperty("canonicalNamePropertyScheduledTaskDetail", BL.schedulerLM.canonicalNamePropertyScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.addProperty("ignoreExceptionsScheduledTaskDetail", BL.schedulerLM.ignoreExceptionsScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.addProperty("orderScheduledTaskDetail", BL.schedulerLM.orderScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.addProperty("scriptScheduledTaskDetail", BL.schedulerLM.scriptScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.addProperty("timeoutScheduledTaskDetail", BL.schedulerLM.timeoutScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.addProperty("parameterScheduledTaskDetail", BL.schedulerLM.parameterScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.and(BL.schedulerLM.activeScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr).getWhere());
            scheduledTaskDetailQuery.and(BL.schedulerLM.scheduledTaskScheduledTaskDetail.getExpr(modifier, scheduledTaskDetailExpr).compare(currentScheduledTaskObject, Compare.EQUALS));

            TreeMap<Integer, ScheduledTaskDetail> propertySIDMap = new TreeMap<>();
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> propertyResult = scheduledTaskDetailQuery.execute(session);
            int defaultOrder = propertyResult.size() + 100;
            for (ImMap<Object, Object> propertyValues : propertyResult.valueIt()) {
                String canonicalName = (String) propertyValues.get("canonicalNamePropertyScheduledTaskDetail");
                String script = (String) propertyValues.get("scriptScheduledTaskDetail");
                if(script != null && !script.isEmpty())
                    script = String.format("run() = ACTION {%s;\n};", script);
                boolean ignoreExceptions = propertyValues.get("ignoreExceptionsScheduledTaskDetail") != null;
                Integer timeout = (Integer) propertyValues.get("timeoutScheduledTaskDetail");
                String parameter = (String) propertyValues.get("parameterScheduledTaskDetail");
                Integer orderProperty = (Integer) propertyValues.get("orderScheduledTaskDetail");
                if (canonicalName != null || script != null) {
                    if (orderProperty == null) {
                        orderProperty = defaultOrder;
                        defaultOrder++;
                    }
                    LAP lap = script == null ? (LAP) BL.findProperty(canonicalName.trim()) : BL.schedulerLM.evalScript;
                    if(lap != null)
                        propertySIDMap.put(orderProperty, new ScheduledTaskDetail(lap, script, ignoreExceptions, timeout, parameter));
                }
            }

            if (runAtStart) {
                daemonTasksExecutor.schedule(new SchedulerTask(propertySIDMap, currentScheduledTaskObject, timeFrom, timeTo, daysOfWeek, daysOfMonth), 0, TimeUnit.MILLISECONDS);
            }
            if (startDate != null) {
                long start = startDate.getTime();
                long currentTime = System.currentTimeMillis();
                if (period != null) {
                    long longPeriod = period.longValue() * 1000;
                    if (start < currentTime) {
                        int periods = (int) ((currentTime - start) / longPeriod) + 1;
                        start += periods * longPeriod;
                    }
                    if (afterFinish.equals(schedulerStartType))
                        daemonTasksExecutor.scheduleWithFixedDelay(new SchedulerTask(propertySIDMap, currentScheduledTaskObject, timeFrom, timeTo, daysOfWeek, daysOfMonth), start - currentTime, longPeriod, TimeUnit.MILLISECONDS);
                    else
                        daemonTasksExecutor.scheduleAtFixedRate(new SchedulerTask(propertySIDMap, currentScheduledTaskObject, timeFrom, timeTo, daysOfWeek, daysOfMonth), start - currentTime, longPeriod, TimeUnit.MILLISECONDS);
                } else if (start > currentTime) {
                    daemonTasksExecutor.schedule(new SchedulerTask(propertySIDMap, currentScheduledTaskObject, timeFrom, timeTo, daysOfWeek, daysOfMonth), start - currentTime, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    public void stopScheduledTasks() {
        if (daemonTasksExecutor != null)
            daemonTasksExecutor.shutdownNow();
    }

    class SchedulerTask implements Runnable {
        Time defaultTime = new Time(0, 0, 0);
        Context threadLocalContext;
        TreeMap<Integer, ScheduledTaskDetail> lapMap;
        private DataObject scheduledTask;
        private Time timeFrom;
        private Time timeTo;
        private Set<String> daysOfWeek;
        private Set<String> daysOfMonth;
        private DataObject currentScheduledTaskLogFinishObject;
        private DataSession afterFinishLogSession;
        private boolean afterFinishErrorOccurred;

        public SchedulerTask(TreeMap<Integer, ScheduledTaskDetail> lapMap, DataObject scheduledTask, Time timeFrom, Time timeTo, Set<String> daysOfWeek, Set<String> daysOfMonth) {
            this.threadLocalContext = ThreadLocalContext.get();
            this.lapMap = lapMap;
            this.scheduledTask = scheduledTask;
            this.timeFrom = timeFrom == null ? defaultTime : timeFrom;
            this.timeTo = timeTo == null ? defaultTime : timeTo;
            this.daysOfWeek = daysOfWeek;
            this.daysOfMonth = daysOfMonth;
        }

        public void run() {
            try {
                if(isTimeToRun(timeFrom, timeTo, daysOfWeek, daysOfMonth)) {
                    for (ScheduledTaskDetail detail : lapMap.values()) {
                        if (detail != null) {

                            ExecuteLAPThread worker = new ExecuteLAPThread(detail);
                            worker.start();
                            worker.join(detail.timeout == null ? 0 : detail.timeout * 1000);
                            if(worker.isAlive()) {
                                if(ThreadLocalContext.get() == null)
                                    ThreadLocalContext.set(threadLocalContext);
                                Map<Integer, Integer> sqlProcesses = dbManager.getAdapter().getSyntaxType() == SQLSyntaxType.POSTGRES ? getPostgresProcesses() : getMSSQLProcesses();
                                Integer sqlProcess = sqlProcesses.get((int) worker.getId());
                                if(sqlProcess != null)
                                    dbManager.getAdapter().killProcess(sqlProcess);
                                worker.interrupt();
                                logger.error("Timeout error while running scheduler task (in executeLAP()) " + detail.lap.property.caption);
                                try (DataSession timeoutLogSession = dbManager.createSession(getLogSql())){
                                    DataObject timeoutScheduledTaskLogFinishObject = timeoutLogSession.addObject(BL.schedulerLM.scheduledTaskLog);
                                    BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                    BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                    BL.schedulerLM.resultScheduledTaskLog.change("Timeout error", timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                    BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, timeoutLogSession, timeoutScheduledTaskLogFinishObject);
                                    BL.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), timeoutLogSession, timeoutScheduledTaskLogFinishObject);

                                    timeoutLogSession.apply(BL);
                                } catch (Exception ie) {
                                    logger.error("Error while reporting exception in scheduler task (in executeLAPThread) " + detail.lap.property.caption + " : ", ie);
                                }

                            }
                            if(worker.exit == null && !detail.ignoreExceptions)
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error while running scheduler task (in SchedulerTask.run()):", e);
            }
        }

        private Map<Integer, Integer> getMSSQLProcesses() throws SQLException, SQLHandledException {
            Map<Integer, List<Object>> sessionThreadMap = SQLSession.getSQLThreadMap();
            String originalQuery = "Select A.session_id, text\n" +
                    "from sys.dm_exec_sessions A\n" +
                    "Left Join sys.dm_exec_requests B\n" +
                    "On A.[session_id]=B.[session_id]\n" +
                    "Left Join sys.dm_exec_connections C\n" +
                    "On A.[session_id]=C.[session_id]\n" +
                    "CROSS APPLY sys.dm_exec_sql_text(sql_handle) AS sqltext";

            MExclSet<String> keyNames = SetFact.mExclSet();
            keyNames.exclAdd("numberrow");
            keyNames.immutable();

            MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
            keyReaders.exclAdd("numberrow", new CustomReader());
            keyReaders.immutable();

            MExclSet<String> propertyNames = SetFact.mExclSet();
            propertyNames.exclAdd("text");
            propertyNames.exclAdd("session_id");
            propertyNames.immutable();

            MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
            propertyReaders.exclAdd("text", StringClass.get(1000));
            propertyReaders.exclAdd("session_id", IntegerClass.instance);
            propertyReaders.immutable();

            try(DataSession session = dbManager.createSession()) {
                ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap()
                        , 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

                Map<Integer, Integer> resultMap = new HashMap<>();
                for (Object rsValue : rs.values()) {
                    HMap entry = (HMap) rsValue;
                    String query = trimToNull((String) entry.get("text"));
                    Integer processId = (Integer) entry.get("session_id");

                    if (!query.equals(originalQuery)) {
                        List<Object> sessionThread = sessionThreadMap.get(processId);
                        if (sessionThread != null && sessionThread.get(0) != null) {
                            resultMap.put((Integer) sessionThread.get(0), processId);
                        }
                    }
                }
                return resultMap;
            }
        }

        private Map<Integer, Integer> getPostgresProcesses() throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

            String originalQuery = String.format("SELECT * FROM pg_stat_activity WHERE datname='%s'", BL.getDataBaseName());

            MExclSet<String> keyNames = SetFact.mExclSet();
            keyNames.exclAdd("numberrow");
            keyNames.immutable();

            MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
            keyReaders.exclAdd("numberrow", new CustomReader());
            keyReaders.immutable();

            MExclSet<String> propertyNames = SetFact.mExclSet();
            propertyNames.exclAdd("query");
            propertyNames.exclAdd("pid");
            propertyNames.immutable();

            MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
            propertyReaders.exclAdd("query", StringClass.get(1000));
            propertyReaders.exclAdd("pid", IntegerClass.instance);
            propertyReaders.immutable();

            try(DataSession session = dbManager.createSession()) {
                ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                        0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

                Map<Integer, List<Object>> sessionThreadMap = SQLSession.getSQLThreadMap();

                Map<Integer, Integer> resultMap = new HashMap<>();
                for (Object rsValue : rs.values()) {
                    HMap entry = (HMap) rsValue;
                    String query = trimToNull((String) entry.get("query"));
                    Integer processId = (Integer) entry.get("pid");
                    if (!query.equals(originalQuery)) {
                        List<Object> sessionThread = sessionThreadMap.get(processId);
                        if(sessionThread != null && sessionThread.get(0) != null)
                            resultMap.put((Integer) sessionThread.get(0), processId);
                    }
                }
                return resultMap;
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

        class ExecuteLAPThread extends Thread {
            ScheduledTaskDetail detail;
            Integer exit = null;

            public ExecuteLAPThread(ScheduledTaskDetail detail) {
                this.detail = detail;
            }

            @Override
            public void run() {
                try {
                    if (executeLAP(detail))
                        exit = 0;
                } catch (SQLException | SQLHandledException e) {
                    logger.error("Error while running scheduler task (in ExecuteLAPThread):", e);
                }
            }

            private boolean executeLAP(ScheduledTaskDetail detail) throws SQLException, SQLHandledException {
                ThreadLocalContext.set(new SchedulerContext());

                try (DataSession beforeStartLogSession = dbManager.createSession()) {
                    DataObject currentScheduledTaskLogStartObject = beforeStartLogSession.addObject(BL.schedulerLM.scheduledTaskLog);
                    afterFinishLogSession = dbManager.createSession(getLogSql());
                    afterFinishErrorOccurred = false;
                    currentScheduledTaskLogFinishObject = afterFinishLogSession.addObject(BL.schedulerLM.scheduledTaskLog);
                    try {
                        BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) beforeStartLogSession, currentScheduledTaskLogStartObject);
                        BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", beforeStartLogSession, currentScheduledTaskLogStartObject);
                        BL.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), beforeStartLogSession, currentScheduledTaskLogStartObject);
                        BL.schedulerLM.resultScheduledTaskLog.change("Запущено" + (detail.script == null ? "" : (" " + BaseUtils.truncate(detail.script, 191))), beforeStartLogSession, currentScheduledTaskLogStartObject);
                        beforeStartLogSession.apply(BL);

                        try (DataSession mainSession = dbManager.createSession()) {
                            if (detail.script != null)
                                BL.schedulerLM.scriptText.change(detail.script, mainSession);
                            ImOrderSet<ClassPropertyInterface> interfaces = detail.lap.listInterfaces;
                            if (interfaces.isEmpty())
                                detail.lap.execute(mainSession);
                            else if (detail.parameter == null)
                                detail.lap.execute(mainSession, NullValue.instance);
                            else {
                                ValueClass valueClass = interfaces.get(0).interfaceClass;
                                DataObject parsedParameter;
                                try {
                                    parsedParameter = valueClass == IntegerClass.instance ?
                                            new DataObject(((IntegerClass) valueClass).parseString(detail.parameter)) : new DataObject(detail.parameter);
                                } catch (Exception e) {
                                    parsedParameter = null;
                                }
                                detail.lap.execute(mainSession, parsedParameter);
                            }
                            String applyResult = mainSession.applyMessage(BL);

                            BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            BL.schedulerLM.resultScheduledTaskLog.change(applyResult == null ? (afterFinishErrorOccurred ? "Выполнено с ошибками" : "Выполнено успешно") : BaseUtils.truncate(applyResult, 200), afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            if (applyResult != null)
                                BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, beforeStartLogSession, currentScheduledTaskLogStartObject);
                            if (afterFinishErrorOccurred)
                                BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, afterFinishLogSession, currentScheduledTaskLogFinishObject);
                            afterFinishErrorOccurred = false;
                            BL.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), afterFinishLogSession, currentScheduledTaskLogFinishObject);

                            String finishResult = afterFinishLogSession.applyMessage(BL);
                            if (finishResult != null)
                                logger.error("Error while saving scheduler task result " + detail.lap.property.caption + " : " + finishResult);
                            return applyResult == null || detail.ignoreExceptions;
                        }
                    } catch (Exception e) {
                        //not timeout exception
                        if (e.getMessage() == null || !e.getMessage().contains("FATAL: terminating connection due to administrator command")) {
                            logger.error("Error while running scheduler task (in executeLAP()) " + detail.lap.property.caption + " : ", e);

                            try {
                                BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) afterFinishLogSession, currentScheduledTaskLogFinishObject);
                                BL.schedulerLM.propertyScheduledTaskLog.change(detail.lap.property.caption + " (" + detail.lap.property.getSID() + ")", afterFinishLogSession, currentScheduledTaskLogFinishObject);
                                BL.schedulerLM.resultScheduledTaskLog.change(BaseUtils.truncate(String.valueOf(e), 200), afterFinishLogSession, currentScheduledTaskLogFinishObject);
                                BL.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, afterFinishLogSession, currentScheduledTaskLogFinishObject);
                                BL.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), afterFinishLogSession, currentScheduledTaskLogFinishObject);

                                DataObject scheduledClientTaskLogObject = afterFinishLogSession.addObject(BL.schedulerLM.scheduledClientTaskLog);
                                BL.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                                        .change(currentScheduledTaskLogFinishObject, (ExecutionEnvironment) afterFinishLogSession, scheduledClientTaskLogObject);
                                BL.schedulerLM.messageScheduledClientTaskLog.change(ExceptionUtils.getStackTrace(e), afterFinishLogSession, scheduledClientTaskLogObject);

                                afterFinishLogSession.apply(BL);
                            } catch (Exception ie) {
                                logger.error("Error while reporting exception in scheduler task (in executeLAPThread) " + detail.lap.property.caption + " : ", ie);
                            }
                        }
                        return detail.ignoreExceptions;
                    }
                }
            }
        }

        public class SchedulerContext extends WrapperContext {
            public SchedulerContext() {
                super(instanceContext);
            }

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
                    try {
                        DataObject scheduledClientTaskLogObject = afterFinishLogSession.addObject(BL.schedulerLM.scheduledClientTaskLog);
                        BL.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                                .change(currentScheduledTaskLogFinishObject, (ExecutionEnvironment) afterFinishLogSession, scheduledClientTaskLogObject);
                        BL.schedulerLM.messageScheduledClientTaskLog.change(message, afterFinishLogSession, scheduledClientTaskLogObject);
                    } catch (SQLException | SQLHandledException e) {
                        throw Throwables.propagate(e);
                    }
                }
                try {
                    super.delayUserInteraction(action);
                } catch (Exception e) {
                    logger.error("Error while executing delayUserInteraction in SchedulerContext", e);
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
        public String parameter;

        public ScheduledTaskDetail(LAP lap, String script, boolean ignoreExceptions, Integer timeout, String parameter) {
            this.lap = lap;
            this.script = script;
            this.ignoreExceptions = ignoreExceptions;
            this.timeout = timeout;
            this.parameter = parameter;
        }
    }
}
