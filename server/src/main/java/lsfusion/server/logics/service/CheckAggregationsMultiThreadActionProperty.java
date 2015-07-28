package lsfusion.server.logics.service;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static lsfusion.base.BaseUtils.serviceLogger;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CheckAggregationsMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;

    public CheckAggregationsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();

    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ExecutorService executorService = null;
        try {
            Integer threadCount = (Integer) context.getDataKeyValue(threadCountInterface).object;
            if(threadCount == null || threadCount == 0)
                threadCount = BaseUtils.max(Runtime.getRuntime().availableProcessors() / 2, 1);

            final TaskPool taskPool = new TaskPool(context.getBL().getAggregateStoredProperties());
            final Context threadLocalContext = ThreadLocalContext.get();
            final List<String> messages = new ArrayList<>();
            executorService = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            try (DataSession session = context.getDbManager().createSession()) {
                                while (!Thread.currentThread().isInterrupted() && taskPool.hasProperties()) {
                                    AggregateProperty property = taskPool.getProperty();
                                    if (property != null) {
                                        String result = checkProperty(session, property);
                                        if(result != null && !result.isEmpty())
                                            messages.add(result);
                                    }
                                }
                                session.apply(context);
                            }
                        } catch (SQLException | SQLHandledException e) {
                            serviceLogger.error("Check Properties error", e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(24, TimeUnit.HOURS);

            String message = "";
            for(String m : messages)
                message += m + '\n';
            context.delayUserInterfaction(new MessageClientAction(getString("logics.check.was.completed") + '\n' + '\n' + message, getString("logics.checking.aggregations"), true));
        } catch (Exception e) {
            serviceLogger.error("Check Properties error", e);
            if(executorService != null)
                executorService.shutdownNow();
        } finally {
            if (executorService != null && !executorService.isShutdown())
                executorService.shutdown();
        }
    }

    private String checkProperty(DataSession session, AggregateProperty property) throws SQLException, SQLHandledException {
        return property.checkAggregation(session.sql, LM.baseLM.baseClass);
    }

    public class TaskPool {
        int i;
        List<AggregateProperty> checkProperties;

        public TaskPool(List<AggregateProperty> checkProperties) {
            this.checkProperties = checkProperties;
            i = 0;
        }

        //метод, выдающий AggregateProperty подпотокам
        synchronized AggregateProperty getProperty() {
            if (checkProperties.size() > i) {
                AggregateProperty property = checkProperties.get(i);
                i++;
                return property;
            } else return null;
        }

        synchronized boolean hasProperties() {
            return i < checkProperties.size();
        }
    }

}