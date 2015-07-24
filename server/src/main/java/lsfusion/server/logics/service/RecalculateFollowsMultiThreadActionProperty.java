package lsfusion.server.logics.service;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.exceptions.LogMessageLogicsException;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static lsfusion.base.BaseUtils.serviceLogger;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateFollowsMultiThreadActionProperty extends ScriptingActionProperty {
    public RecalculateFollowsMultiThreadActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        int threadsCount = BaseUtils.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        final ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);

        final boolean singleTransaction = singleTransaction(context);

        try {
            final TaskPool taskPool = new TaskPool(context.getBL().getPropertyList());
            final Context threadLocalContext = ThreadLocalContext.get();
            for (int i = 0; i < threadsCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            while (!Thread.currentThread().isInterrupted() && taskPool.hasTables()) {
                                Property property = taskPool.getProperty();
                                if(property != null)
                                    recalculateFollows(context, property, !singleTransaction);
                            }
                        } catch (SQLException | SQLHandledException e) {
                            serviceLogger.error("Recalculate Follows error", e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(24, TimeUnit.HOURS);
            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.follows")));
        } catch (InterruptedException e) {
            serviceLogger.error("Recalculate Follows error", e);
        } finally {
            if (!executorService.isShutdown())
                executorService.shutdown();
        }
    }


    public void recalculateFollows(SessionCreator creator, Property property, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        if (property instanceof ActionProperty) {
            final ActionProperty<?> action = (ActionProperty) property;
            if (action.hasResolve()) {
                long start = System.currentTimeMillis();
                try {
                    DBManager.runData(creator, isolatedTransaction, new DBManager.RunServiceData() {
                        public void run(SessionCreator session) throws SQLException, SQLHandledException {
                            ((DataSession) session).resolve(action);
                        }
                    });
                } catch (LogMessageLogicsException e) { // suppress'им так как понятная ошибка
                    serviceLogger.info(e.getMessage());
                }
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Recalculate Follows: %s, %sms", property.getSID(), time));
            }
        }
    }

    public static boolean singleTransaction(ExecutionContext context) throws SQLException, SQLHandledException {
        return context.getBL().serviceLM.singleTransaction.read(context) != null;
    }

    public class TaskPool {
        int i;
        ImOrderSet<Property> propertyList;
        public TaskPool(ImOrderSet<Property> propertyList) {
            this.propertyList = propertyList;
            i = 0;
        }

        //метод, выдающий задания подпотокам
        synchronized Property getProperty() {
            if (propertyList.size() > i) {
                Property property = propertyList.get(i);
                i++;
                return property;
            } else return null;
        }

        synchronized boolean hasTables() {
            return i < propertyList.size();
        }
    }

}