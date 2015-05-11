package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.WrapperContext;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.context.Context;
import lsfusion.server.context.ContextAwareThread;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;
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

public class Scheduler extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = ServerLoggers.systemLogger;

    public ScheduledExecutorService daemonTasksExecutor;

    private Context instanceContext;

    private BusinessLogics businessLogics;
    private DBManager dbManager;

    public Scheduler() {
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
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
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(instanceContext, "logicsInstance must be specified");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        if(dbManager.isServer()) {
            logger.info("Starting Scheduler.");

            try {
                setupScheduledTasks(dbManager.createSession());
                setupCurrentDateSynchronization();
            } catch (SQLException | ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
                throw new RuntimeException("Error starting Scheduler: ", e);
            }
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
            DataSession session = dbManager.createSession();

            java.sql.Date currentDate = (java.sql.Date) businessLogics.timeLM.currentDate.read(session);
            java.sql.Date newDate = DateConverter.getCurrentDate();
            if (currentDate == null || currentDate.getDate() != newDate.getDate() || currentDate.getMonth() != newDate.getMonth() || currentDate.getYear() != newDate.getYear()) {
                businessLogics.timeLM.currentDate.change(newDate, session);
                session.apply(businessLogics);
            }

            session.close();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void setupScheduledTasks(DataSession session) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {

        KeyExpr scheduledTaskExpr = new KeyExpr("scheduledTask");
        ImRevMap<Object, KeyExpr> scheduledTaskKeys = MapFact.<Object, KeyExpr>singletonRev("scheduledTask", scheduledTaskExpr);

        QueryBuilder<Object, Object> scheduledTaskQuery = new QueryBuilder<>(scheduledTaskKeys);
        scheduledTaskQuery.addProperty("runAtStartScheduledTask", businessLogics.schedulerLM.runAtStartScheduledTask.getExpr(scheduledTaskExpr));
        scheduledTaskQuery.addProperty("startDateScheduledTask", businessLogics.schedulerLM.startDateScheduledTask.getExpr(scheduledTaskExpr));
        scheduledTaskQuery.addProperty("timeFromScheduledTask", businessLogics.schedulerLM.timeFromScheduledTask.getExpr(scheduledTaskExpr));
        scheduledTaskQuery.addProperty("timeToScheduledTask", businessLogics.schedulerLM.timeToScheduledTask.getExpr(scheduledTaskExpr));
        scheduledTaskQuery.addProperty("periodScheduledTask", businessLogics.schedulerLM.periodScheduledTask.getExpr(scheduledTaskExpr));
        scheduledTaskQuery.addProperty("daysOfWeekScheduledTask", businessLogics.schedulerLM.daysOfWeekScheduledTask.getExpr(scheduledTaskExpr));
        scheduledTaskQuery.addProperty("daysOfMonthScheduledTask", businessLogics.schedulerLM.daysOfMonthScheduledTask.getExpr(scheduledTaskExpr));
        scheduledTaskQuery.addProperty("schedulerStartTypeScheduledTask", businessLogics.schedulerLM.schedulerStartTypeScheduledTask.getExpr(scheduledTaskExpr));

        scheduledTaskQuery.and(businessLogics.schedulerLM.activeScheduledTask.getExpr(scheduledTaskExpr).getWhere());

        if (daemonTasksExecutor != null)
            daemonTasksExecutor.shutdown();

        daemonTasksExecutor = Executors.newScheduledThreadPool(3, new DaemonThreadFactory("scheduler-daemon"));

        Object afterFinish = ((ConcreteCustomClass) businessLogics.schedulerLM.findClass("SchedulerStartType")).getDataObject("afterFinish").object;

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> scheduledTaskResult = scheduledTaskQuery.execute(session);
        for (int i = 0, size = scheduledTaskResult.size(); i < size; i++) {
            ImMap<Object, Object> key = scheduledTaskResult.getKey(i);
            ImMap<Object, Object> value = scheduledTaskResult.getValue(i);
            DataObject currentScheduledTaskObject = new DataObject(key.getValue(0), businessLogics.schedulerLM.scheduledTask);
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
            scheduledTaskDetailQuery.addProperty("canonicalNamePropertyScheduledTaskDetail", businessLogics.schedulerLM.canonicalNamePropertyScheduledTaskDetail.getExpr(scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.addProperty("ignoreExceptionsScheduledTaskDetail", businessLogics.schedulerLM.ignoreExceptionsScheduledTaskDetail.getExpr(scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.addProperty("orderScheduledTaskDetail", businessLogics.schedulerLM.orderScheduledTaskDetail.getExpr(scheduledTaskDetailExpr));
            scheduledTaskDetailQuery.and(businessLogics.schedulerLM.activeScheduledTaskDetail.getExpr(scheduledTaskDetailExpr).getWhere());
            scheduledTaskDetailQuery.and(businessLogics.schedulerLM.scheduledTaskScheduledTaskDetail.getExpr(scheduledTaskDetailExpr).compare(currentScheduledTaskObject, Compare.EQUALS));

            TreeMap<Integer, Pair<LAP, Boolean>> propertySIDMap = new TreeMap<>();
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> propertyResult = scheduledTaskDetailQuery.execute(session);
            int defaultOrder = propertyResult.size() + 100;
            for (ImMap<Object, Object> propertyValues : propertyResult.valueIt()) {
                String canonicalName = (String) propertyValues.get("canonicalNamePropertyScheduledTaskDetail");
                Boolean ignoreExceptions = (Boolean) propertyValues.get("ignoreExceptionsScheduledTaskDetail");
                Integer orderProperty = (Integer) propertyValues.get("orderScheduledTaskDetail");
                if (canonicalName != null) {
                    if (orderProperty == null) {
                        orderProperty = defaultOrder;
                        defaultOrder++;
                    }
                    LAP lap = (LAP) businessLogics.findProperty(canonicalName.trim());
                    if(lap != null)
                        propertySIDMap.put(orderProperty, Pair.create(lap, ignoreExceptions));
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

    class SchedulerTask implements Runnable {
        Time defaultTime = new Time(0, 0, 0);
        TreeMap<Integer, Pair<LAP, Boolean>> lapMap;
        private DataObject scheduledTask;
        private Time timeFrom;
        private Time timeTo;
        private Set<String> daysOfWeek;
        private Set<String> daysOfMonth;
        private DataObject currentScheduledTaskLogFinishObject;
        private DataSession afterFinishLogSession;

        public SchedulerTask(TreeMap<Integer, Pair<LAP, Boolean>> lapMap, DataObject scheduledTask, Time timeFrom, Time timeTo, Set<String> daysOfWeek, Set<String> daysOfMonth) {
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
                    for (Map.Entry<Integer, Pair<LAP, Boolean>> entry : lapMap.entrySet()) {
                        if (entry.getValue() != null) {
                            LAP lap = entry.getValue().first;
                            boolean ignoreExceptions = entry.getValue().second != null && entry.getValue().second;
                            if (!executeLAP(lap, ignoreExceptions, scheduledTask))
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error while running scheduler task (in SchedulerTask.run()):", e);
            }
        }

        private boolean isTimeToRun(Time timeFrom, Time timeTo, Set<String> daysOfWeek, Set<String> daysOfMonth) {
            if(timeFrom.equals(defaultTime) && timeTo.equals(defaultTime)) return true;

            Calendar currentCal = Calendar.getInstance();

            if((!daysOfWeek.isEmpty() && !daysOfWeek.contains(String.valueOf(currentCal.get(Calendar.DAY_OF_WEEK) - 1)))
                    || (!daysOfMonth.isEmpty() && !daysOfMonth.contains(String.valueOf(currentCal.get(Calendar.DAY_OF_MONTH)))))
                return false;

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

        private boolean executeLAP(LAP lap, boolean ignoreExceptions, DataObject scheduledTask) throws SQLException, SQLHandledException {
            ThreadLocalContext.set(new SchedulerContext());
            
            DataSession beforeStartLogSession = dbManager.createSession();
            DataObject currentScheduledTaskLogStartObject = beforeStartLogSession.addObject(businessLogics.schedulerLM.scheduledTaskLog);
            afterFinishLogSession = dbManager.createSession(getLogSql());
            currentScheduledTaskLogFinishObject = afterFinishLogSession.addObject(businessLogics.schedulerLM.scheduledTaskLog);
            try {
                businessLogics.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) beforeStartLogSession, currentScheduledTaskLogStartObject);
                businessLogics.schedulerLM.propertyScheduledTaskLog.change(lap.property.caption + " (" + lap.property.getSID() + ")", beforeStartLogSession, currentScheduledTaskLogStartObject);
                businessLogics.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), beforeStartLogSession, currentScheduledTaskLogStartObject);
                businessLogics.schedulerLM.resultScheduledTaskLog.change("Запущено", beforeStartLogSession, currentScheduledTaskLogStartObject);
                beforeStartLogSession.apply(businessLogics);

                DataSession mainSession = dbManager.createSession();
                lap.execute(mainSession);
                String applyResult = mainSession.applyMessage(businessLogics);

                businessLogics.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) afterFinishLogSession, currentScheduledTaskLogFinishObject);
                businessLogics.schedulerLM.propertyScheduledTaskLog.change(lap.property.caption + " (" + lap.property.getSID() + ")", afterFinishLogSession, currentScheduledTaskLogFinishObject);
                businessLogics.schedulerLM.resultScheduledTaskLog.change(applyResult == null ? "Выполнено успешно" : BaseUtils.truncate(applyResult, 200), afterFinishLogSession, currentScheduledTaskLogFinishObject);
                if(applyResult != null)
                    businessLogics.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, beforeStartLogSession, currentScheduledTaskLogStartObject);
                businessLogics.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), afterFinishLogSession, currentScheduledTaskLogFinishObject);

                String finishResult = afterFinishLogSession.applyMessage(businessLogics);
                if (finishResult != null)
                    logger.error("Error while saving scheduler task result " + lap.property.caption + " : " + finishResult);
                return applyResult == null || ignoreExceptions;
            } catch (Exception e) {
                logger.error("Error while running scheduler task (in executeLAP()) " + lap.property.caption + " : ", e);

                try {
                    businessLogics.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) afterFinishLogSession, currentScheduledTaskLogFinishObject);
                    businessLogics.schedulerLM.propertyScheduledTaskLog.change(lap.property.caption + " (" + lap.property.getSID() + ")", afterFinishLogSession, currentScheduledTaskLogFinishObject);
                    businessLogics.schedulerLM.resultScheduledTaskLog.change(BaseUtils.truncate(String.valueOf(e), 200), afterFinishLogSession, currentScheduledTaskLogFinishObject);
                    businessLogics.schedulerLM.exceptionOccurredScheduledTaskLog.change(true, afterFinishLogSession, currentScheduledTaskLogFinishObject);
                    businessLogics.schedulerLM.dateScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), afterFinishLogSession, currentScheduledTaskLogFinishObject);

                    DataObject scheduledClientTaskLogObject = afterFinishLogSession.addObject(businessLogics.schedulerLM.scheduledClientTaskLog);
                    businessLogics.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                            .change(currentScheduledTaskLogFinishObject, (ExecutionEnvironment) afterFinishLogSession, scheduledClientTaskLogObject);
                    businessLogics.schedulerLM.messageScheduledClientTaskLog.change(ExceptionUtils.getStackTrace(e), afterFinishLogSession, scheduledClientTaskLogObject);

                    afterFinishLogSession.apply(businessLogics);
                } catch (Exception ie) {
                    logger.error("Error while reporting exception in scheduler task (in executeLAP()) " + lap.property.caption + " : ", e);
                }

                return ignoreExceptions;
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
                    message = ((LogMessageClientAction) action).message;
                } else if (action instanceof MessageClientAction) {
                    message = ((MessageClientAction) action).message;
                }
                if (message != null) {
                    try {
                        DataObject scheduledClientTaskLogObject = afterFinishLogSession.addObject(businessLogics.schedulerLM.scheduledClientTaskLog);
                        businessLogics.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                                .change(currentScheduledTaskLogFinishObject, (ExecutionEnvironment) afterFinishLogSession, scheduledClientTaskLogObject);
                        businessLogics.schedulerLM.messageScheduledClientTaskLog.change(message, afterFinishLogSession, scheduledClientTaskLogObject);
                    } catch (SQLException | SQLHandledException e) {
                        throw Throwables.propagate(e);
                    }
                }
                super.delayUserInteraction(action);
            }
        }
    }
}
