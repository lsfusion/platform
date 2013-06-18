package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import lsfusion.base.DateConverter;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.WrapperContext;
import lsfusion.server.context.Context;
import lsfusion.server.context.ContextAwareDaemonThreadFactory;
import lsfusion.server.context.ContextAwareThread;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = ServerLoggers.systemLogger;

    public ScheduledExecutorService daemonTasksExecutor;

    private Context instanceContext;

    private BusinessLogics businessLogics;
    private DBManager dbManager;

    private DataObject currentScheduledTaskObject;
    private DataObject currentScheduledTaskLogObject;
    private DataSession currentLogSession;

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

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(instanceContext, "logicsInstance must be specified");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Starting Scheduler.");

        try {
            setupScheduledTasks(dbManager.createSession());
            setupCurrentDateSynchronization();
        } catch (SQLException e) {
            throw new RuntimeException("Error starting Scheduler: ", e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException("Error starting Scheduler: ", e);
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
                        } catch (InterruptedException e) {
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
            if (currentDate == null || currentDate.getDay() != newDate.getDay() || currentDate.getMonth() != newDate.getMonth() || currentDate.getYear() != newDate.getYear()) {
                businessLogics.timeLM.currentDate.change(newDate, session);
                session.apply(businessLogics);
            }

            session.close();
        } catch (SQLException e) {
            logger.error("Error changing current date: ", e);
            Throwables.propagate(e);
        }
    }

    public void setupScheduledTasks(DataSession session) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        KeyExpr scheduledTask1Expr = new KeyExpr("scheduledTask");
        ImRevMap<Object, KeyExpr> scheduledTaskKeys = MapFact.<Object, KeyExpr>singletonRev("scheduledTask", scheduledTask1Expr);

        QueryBuilder<Object, Object> scheduledTaskQuery = new QueryBuilder<Object, Object>(scheduledTaskKeys);
        scheduledTaskQuery.addProperty("runAtStartScheduledTask", businessLogics.schedulerLM.runAtStartScheduledTask.getExpr(scheduledTaskKeys.singleValue()));
        scheduledTaskQuery.addProperty("startDateScheduledTask", businessLogics.schedulerLM.startDateScheduledTask.getExpr(scheduledTaskKeys.singleValue()));
        scheduledTaskQuery.addProperty("periodScheduledTask", businessLogics.schedulerLM.periodScheduledTask.getExpr(scheduledTaskKeys.singleValue()));
        scheduledTaskQuery.addProperty("schedulerStartTypeScheduledTask", businessLogics.schedulerLM.schedulerStartTypeScheduledTask.getExpr(scheduledTaskKeys.singleValue()));

        scheduledTaskQuery.and(businessLogics.schedulerLM.activeScheduledTask.getExpr(scheduledTask1Expr).getWhere());

        if(daemonTasksExecutor!=null)
            daemonTasksExecutor.shutdown();

        daemonTasksExecutor = Executors.newScheduledThreadPool(3, new ContextAwareDaemonThreadFactory(new SchedulerContext(), "-scheduler-daemon-"));

        Object afterFinish = ((ConcreteCustomClass) businessLogics.schedulerLM.findClassByCompoundName("SchedulerStartType")).getDataObject("afterFinish").object;

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> scheduledTaskResult = scheduledTaskQuery.execute(session.sql);
        for (int i = 0, size = scheduledTaskResult.size(); i < size; i++) {
            ImMap<Object, Object> key = scheduledTaskResult.getKey(i);
            ImMap<Object, Object> value = scheduledTaskResult.getValue(i);
            currentScheduledTaskObject = new DataObject(key.getValue(0), businessLogics.schedulerLM.scheduledTask);
            Boolean runAtStart = value.get("runAtStartScheduledTask") != null;
            Timestamp startDate = (Timestamp) value.get("startDateScheduledTask");
            Integer period = (Integer) value.get("periodScheduledTask");
            Object schedulerStartType = value.get("schedulerStartTypeScheduledTask");

            KeyExpr propertyExpr = new KeyExpr("property");
            KeyExpr scheduledTaskExpr = new KeyExpr("scheduledTask");
            ImRevMap<Object, KeyExpr> propertyKeys = MapFact.<Object, KeyExpr>toRevMap("property", propertyExpr, "scheduledTask", scheduledTaskExpr);

            QueryBuilder<Object, Object> propertyQuery = new QueryBuilder<Object, Object>(propertyKeys);
            propertyQuery.addProperty("SIDProperty", businessLogics.reflectionLM.SIDProperty.getExpr(propertyExpr));
            propertyQuery.addProperty("orderScheduledTaskProperty", businessLogics.schedulerLM.orderScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr));
            propertyQuery.and(businessLogics.schedulerLM.inScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr).getWhere());
            propertyQuery.and(businessLogics.schedulerLM.activeScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr).getWhere());
            propertyQuery.and(scheduledTaskExpr.compare(currentScheduledTaskObject, Compare.EQUALS));

            TreeMap<Integer, LAP> propertySIDMap = new TreeMap<Integer, LAP>();
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> propertyResult = propertyQuery.execute(session.sql);
            int defaultOrder = propertyResult.size() + 100;
            for (ImMap<Object, Object> propertyValues : propertyResult.valueIt()) {
                String sidProperty = (String) propertyValues.get("SIDProperty");
                Integer orderProperty = (Integer) propertyValues.get("orderScheduledTaskProperty");
                if (sidProperty != null) {
                    if (orderProperty == null) {
                        orderProperty = defaultOrder;
                        defaultOrder++;
                    }
                    LAP lap = businessLogics.getLAP(sidProperty.trim());
                    propertySIDMap.put(orderProperty, lap);
                }
            }

            if (runAtStart) {
                daemonTasksExecutor.schedule(new SchedulerTask(propertySIDMap, currentScheduledTaskObject), 0, TimeUnit.MILLISECONDS);
            }
            if (startDate != null) {
                long start = startDate.getTime();
                if (period != null) {
                    period = period * 1000;
                    if (start < System.currentTimeMillis()) {
                        int periods = (int) (System.currentTimeMillis() - start) / (period) + 1;
                        start += periods * period;
                    }
                    if(afterFinish.equals(schedulerStartType))
                        daemonTasksExecutor.scheduleWithFixedDelay(new SchedulerTask(propertySIDMap, currentScheduledTaskObject), start - System.currentTimeMillis(), period, TimeUnit.MILLISECONDS);
                    else
                        daemonTasksExecutor.scheduleAtFixedRate(new SchedulerTask(propertySIDMap, currentScheduledTaskObject), start - System.currentTimeMillis(), period, TimeUnit.MILLISECONDS);
                } else if (start > System.currentTimeMillis()) {
                    daemonTasksExecutor.schedule(new SchedulerTask(propertySIDMap, currentScheduledTaskObject), start - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private boolean executeLAP(LAP lap, DataObject scheduledTask) throws SQLException {
        currentLogSession = dbManager.createSession();
        currentScheduledTaskLogObject = currentLogSession.addObject(businessLogics.schedulerLM.scheduledTaskLog);
        try {
            businessLogics.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask, (ExecutionEnvironment) currentLogSession, currentScheduledTaskLogObject);
            businessLogics.schedulerLM.propertyScheduledTaskLog.change(lap.property.caption + " (" + lap.property.getSID() + ")", currentLogSession, currentScheduledTaskLogObject);
            businessLogics.schedulerLM.dateStartScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);

            DataSession session = dbManager.createSession();
            lap.execute(session);
            String applyResult = session.applyMessage(businessLogics);

            businessLogics.schedulerLM.resultScheduledTaskLog.change(applyResult == null ? "Выполнено успешно" : applyResult, currentLogSession, currentScheduledTaskLogObject);
            businessLogics.schedulerLM.dateFinishScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);
            currentLogSession.apply(businessLogics);
            return applyResult == null;
        } catch (Exception e) {
            businessLogics.schedulerLM.resultScheduledTaskLog.change(e, currentLogSession, currentScheduledTaskLogObject);
            businessLogics.schedulerLM.dateFinishScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);
            currentLogSession.apply(businessLogics);
            return false;
        }
    }

    class SchedulerTask implements Runnable {
        TreeMap<Integer, LAP> lapMap;
        private DataObject scheduledTask;


        public SchedulerTask(TreeMap<Integer, LAP> lapMap, DataObject scheduledTask) {
            this.lapMap = lapMap;
            this.scheduledTask = scheduledTask;
        }

        public void run() {
            try {
                for (Map.Entry<Integer, LAP> entry : lapMap.entrySet()) {
                    if (entry.getValue() != null) {
                        if (!executeLAP(entry.getValue(), scheduledTask))
                            break;
                    }
                }
            } catch (SQLException e) {
                Throwables.propagate(e);
            }
        }
    }

    public class SchedulerContext extends WrapperContext {
        public SchedulerContext() {
            super(instanceContext);
        }

        @Override
        public void delayUserInteraction(ClientAction action) {
            if (action instanceof MessageClientAction) {
                try {
                    DataObject scheduledClientTaskLogObject = currentLogSession.addObject(businessLogics.schedulerLM.scheduledClientTaskLog);

                    businessLogics.schedulerLM.scheduledTaskLogScheduledClientTaskLog
                            .change(currentScheduledTaskLogObject, (ExecutionEnvironment) currentLogSession, scheduledClientTaskLogObject);
                    businessLogics.schedulerLM.messageScheduledClientTaskLog
                            .change(((MessageClientAction) action).message, currentLogSession, scheduledClientTaskLogObject);
                } catch (SQLException e) {
                    Throwables.propagate(e);
                }
            } else {
                super.delayUserInteraction(action);
            }
        }
    }
}
