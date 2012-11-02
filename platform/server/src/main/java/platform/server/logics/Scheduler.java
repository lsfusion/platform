package platform.server.logics;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.ContextAwareDaemonThreadFactory;
import platform.server.SchedulerContext;
import platform.server.classes.ConcreteCustomClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.linear.LAP;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
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
        Map<Object, KeyExpr> scheduledTaskKeys = new HashMap<Object, KeyExpr>();
        scheduledTaskKeys.put("scheduledTask", scheduledTask1Expr);

        Query<Object, Object> scheduledTaskQuery = new Query<Object, Object>(scheduledTaskKeys);
        scheduledTaskQuery.properties.put("runAtStartScheduledTask", BL.LM.runAtStartScheduledTask.getExpr(BaseUtils.singleValue(scheduledTaskKeys)));
        scheduledTaskQuery.properties.put("startDateScheduledTask", BL.LM.startDateScheduledTask.getExpr(BaseUtils.singleValue(scheduledTaskKeys)));
        scheduledTaskQuery.properties.put("periodScheduledTask", BL.LM.periodScheduledTask.getExpr(BaseUtils.singleValue(scheduledTaskKeys)));

        scheduledTaskQuery.and(BL.LM.activeScheduledTask.getExpr(scheduledTask1Expr).getWhere());

        OrderedMap<Map<Object, Object>, Map<Object, Object>> scheduledTaskResult = scheduledTaskQuery.execute(session.sql);
        for (Map.Entry<Map<Object, Object>, Map<Object, Object>> rows : scheduledTaskResult.entrySet()) {
            currentScheduledTaskObject = new DataObject(rows.getKey().entrySet().iterator().next().getValue(), BL.LM.scheduledTask);
            Boolean runAtStart = rows.getValue().get("runAtStartScheduledTask") != null;
            Timestamp startDate = (Timestamp) rows.getValue().get("startDateScheduledTask");
            Integer period = (Integer) rows.getValue().get("periodScheduledTask");

            KeyExpr propertyExpr = new KeyExpr("property");
            KeyExpr scheduledTaskExpr = new KeyExpr("scheduledTask");
            Map<Object, KeyExpr> propertyKeys = new HashMap<Object, KeyExpr>();
            propertyKeys.put("property", propertyExpr);
            propertyKeys.put("scheduledTask", scheduledTaskExpr);

            Query<Object, Object> propertyQuery = new Query<Object, Object>(propertyKeys);
            propertyQuery.properties.put("SIDProperty", BL.LM.SIDProperty.getExpr(propertyExpr));
            propertyQuery.properties.put("orderScheduledTaskProperty", BL.LM.orderScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr));
            propertyQuery.and(BL.LM.inScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr).getWhere());
            propertyQuery.and(BL.LM.activeScheduledTaskProperty.getExpr(scheduledTaskExpr, propertyExpr).getWhere());
            propertyQuery.and(scheduledTaskExpr.compare(currentScheduledTaskObject, Compare.EQUALS));

            ScheduledExecutorService daemonTasksExecutor = Executors.newScheduledThreadPool(1, new ContextAwareDaemonThreadFactory(new SchedulerContext(this)));

            TreeMap<Integer, LAP> propertySIDMap = new TreeMap<Integer, LAP>();
            OrderedMap<Map<Object, Object>, Map<Object, Object>> propertyResult = propertyQuery.execute(session.sql);
            int defaultOrder = propertyResult.size() + 100;
            for (Map<Object, Object> propertyValues : propertyResult.values()) {
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
        currentScheduledTaskLogObject = currentLogSession.addObject((ConcreteCustomClass) BL.LM.getClassBySID("scheduledTaskLog"));
        try {
            BL.getLCP("scheduledTaskScheduledTaskLog").change(scheduledTask.getValue(), currentLogSession, currentScheduledTaskLogObject);
            BL.getLCP("propertyScheduledTaskLog").change(lap.property.caption + " (" + lap.property.getSID() + ")", currentLogSession, currentScheduledTaskLogObject);
            BL.getLCP("dateStartScheduledTaskLog").change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);

            DataSession session = BL.createSession();
            lap.execute(session);
            String applyResult = session.applyMessage(BL);

            BL.getLCP("resultScheduledTaskLog").change(applyResult == null ? "Выполнено успешно" : applyResult, currentLogSession, currentScheduledTaskLogObject);
            BL.getLCP("dateFinishScheduledTaskLog").change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);
            currentLogSession.apply(BL);
            return applyResult == null;
        } catch (Exception e) {
            BL.getLCP("resultScheduledTaskLog").change(e, currentLogSession, currentScheduledTaskLogObject);
            BL.getLCP("dateFinishScheduledTaskLog").change(new Timestamp(System.currentTimeMillis()), currentLogSession, currentScheduledTaskLogObject);
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
