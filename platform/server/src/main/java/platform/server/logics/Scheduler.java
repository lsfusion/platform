package platform.server.logics;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.ContextAwareDaemonThreadFactory;
import platform.server.SchedulerContext;
import platform.server.classes.ConcreteCustomClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.linear.LAP;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    public BusinessLogics BL;
    public DataObject currentScheduledTaskObject;
    public DataObject currentScheduledTaskLogObject;
    public DataSession currentLogSession;

    public Scheduler(BusinessLogics BL) {
        this.BL = BL;
    }

    protected void runScheduler() throws IOException, SQLException {
        DataSession session = BL.createSession();

        KeyExpr scheduledTask1Expr = new KeyExpr("scheduledTask");
        ImRevMap<Object, KeyExpr> scheduledTaskKeys = MapFact.<Object, KeyExpr>singletonRev("scheduledTask", scheduledTask1Expr);

        QueryBuilder<Object, Object> scheduledTaskQuery = new QueryBuilder<Object, Object>(scheduledTaskKeys);
        scheduledTaskQuery.addProperty("runAtStartScheduledTask", BL.schedulerLM.runAtStartScheduledTask.getExpr(scheduledTaskKeys.singleValue()));
        scheduledTaskQuery.addProperty("startDateScheduledTask", BL.schedulerLM.startDateScheduledTask.getExpr(scheduledTaskKeys.singleValue()));
        scheduledTaskQuery.addProperty("periodScheduledTask", BL.schedulerLM.periodScheduledTask.getExpr(scheduledTaskKeys.singleValue()));

        scheduledTaskQuery.and(BL.schedulerLM.activeScheduledTask.getExpr(scheduledTask1Expr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> scheduledTaskResult = scheduledTaskQuery.execute(session.sql);
        for (int i=0,size=scheduledTaskResult.size();i<size;i++) {
            ImMap<Object, Object> key = scheduledTaskResult.getKey(i); ImMap<Object, Object> value = scheduledTaskResult.getValue(i);
            currentScheduledTaskObject = new DataObject(key.getValue(0), BL.schedulerLM.scheduledTask);
            Boolean runAtStart = value.get("runAtStartScheduledTask") != null;
            Timestamp startDate = (Timestamp) value.get("startDateScheduledTask");
            Integer period = (Integer) value.get("periodScheduledTask");

            KeyExpr propertyExpr = new KeyExpr("property");
            KeyExpr scheduledTaskExpr = new KeyExpr("scheduledTask");
            ImRevMap<Object, KeyExpr> propertyKeys = MapFact.<Object, KeyExpr>toRevMap("property", propertyExpr, "scheduledTask", scheduledTaskExpr);

            QueryBuilder<Object, Object> propertyQuery = new QueryBuilder<Object, Object>(propertyKeys);
            propertyQuery.addProperty("SIDProperty", BL.reflectionLM.SIDProperty.getExpr(propertyExpr));
            propertyQuery.addProperty("orderScheduledTaskProperty", BL.schedulerLM.orderScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr));
            propertyQuery.and(BL.schedulerLM.inScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr).getWhere());
            propertyQuery.and(BL.schedulerLM.activeScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr).getWhere());
            propertyQuery.and(scheduledTaskExpr.compare(currentScheduledTaskObject, Compare.EQUALS));

            ScheduledExecutorService daemonTasksExecutor = Executors.newScheduledThreadPool(1, new ContextAwareDaemonThreadFactory(new SchedulerContext(this)));

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
                    LAP lap = BL.getLAP(sidProperty.trim());
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
                    daemonTasksExecutor.scheduleWithFixedDelay(new SchedulerTask(propertySIDMap, currentScheduledTaskObject), start - System.currentTimeMillis(), period, TimeUnit.MILLISECONDS);

                } else if (start > System.currentTimeMillis()) {
                    daemonTasksExecutor.schedule(new SchedulerTask(propertySIDMap, currentScheduledTaskObject), start - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private boolean executeLAP(LAP lap, DataObject scheduledTask) throws SQLException {
        currentLogSession = BL.createSession();
        currentScheduledTaskLogObject = currentLogSession.addObject(BL.schedulerLM.scheduledTaskLog);
        try {
            BL.schedulerLM.scheduledTaskScheduledTaskLog.change(scheduledTask.getValue(), currentLogSession, currentScheduledTaskLogObject);
            BL.schedulerLM.propertyScheduledTaskLog.change(lap.property.caption + " (" + lap.property.getSID() + ")", currentLogSession, currentScheduledTaskLogObject);
            BL.schedulerLM.dateStartScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);

            DataSession session = BL.createSession();
            lap.execute(session);
            String applyResult = session.applyMessage(BL);

            BL.schedulerLM.resultScheduledTaskLog.change(applyResult == null ? "Выполнено успешно" : applyResult, currentLogSession, currentScheduledTaskLogObject);
            BL.schedulerLM.dateFinishScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);
            currentLogSession.apply(BL);
            return applyResult == null;
        } catch (Exception e) {
            BL.schedulerLM.resultScheduledTaskLog.change(e, currentLogSession, currentScheduledTaskLogObject);
            BL.schedulerLM.dateFinishScheduledTaskLog.change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);
            currentLogSession.apply(BL);
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
                //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
