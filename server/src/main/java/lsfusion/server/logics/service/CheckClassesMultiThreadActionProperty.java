package lsfusion.server.logics.service;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.table.ImplementTable;
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

public class CheckClassesMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;

    public CheckClassesMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
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

            final TaskPool taskPool = new TaskPool(LM.baseLM.tableFactory.getImplementTables(), context.getBL().getStoredDataProperties(false));
            final Context threadLocalContext = ThreadLocalContext.get();
            final List<String> messages = new ArrayList<>();
            executorService = Executors.newFixedThreadPool(threadCount);

            try(DataSession session = context.getDbManager().createSession()) {
                messages.add(DataSession.checkClasses(session.sql, LM.baseLM.baseClass));
                session.apply(context);
            }

            //Check Table Classes
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            try (DataSession session = context.getDbManager().createSession()) {
                                while (!Thread.currentThread().isInterrupted() && taskPool.hasTables()) {
                                    ImplementTable table = taskPool.getTable();
                                    if (table != null) {
                                        String result = checkTableClasses(session, table);
                                        if(result != null && !result.isEmpty()) {
                                            messages.add(result);
                                        }
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
            executorService.awaitTermination(12, TimeUnit.HOURS);

            for (int i = 0; i < threadCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            try (DataSession session = context.getDbManager().createSession()) {
                                while (!Thread.currentThread().isInterrupted() && taskPool.hasProperties()) {
                                    CalcProperty property = taskPool.getProperty();
                                    if (property != null) {
                                        String result = checkClasses(session, property);
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
            executorService.awaitTermination(12, TimeUnit.HOURS);

            String message = "";
            for(String m : messages)
                message += m + '\n';
            context.delayUserInterfaction(new MessageClientAction(getString("logics.check.was.completed") + '\n' + '\n' + message, getString("logics.checking.data.classes"), true));
        } catch (Exception e) {
            serviceLogger.error("Check Properties error", e);
            if(executorService != null)
                executorService.shutdownNow();
        } finally {
            if (executorService != null && !executorService.isShutdown())
                executorService.shutdown();
        }
    }

    private String checkTableClasses(DataSession session, ImplementTable table) throws SQLException, SQLHandledException {
        return DataSession.checkTableClasses(table, session.sql, LM.baseLM.baseClass);
    }

    private String checkClasses(DataSession session, CalcProperty property) throws SQLException, SQLHandledException {
        return DataSession.checkClasses(property, session.sql, LM.baseLM.baseClass);
    }

    public class TaskPool {
        int i, j;
        ImSet<ImplementTable> implementTables;
        ImOrderSet<CalcProperty> storedProperties;

        public TaskPool(ImSet<ImplementTable> implementTables, ImOrderSet<CalcProperty> storedProperties) {
            this.implementTables = implementTables;
            this.storedProperties = storedProperties;
            i = 0;
            j = 0;
        }

        //метод, выдающий ImplementTable подпотокам
        synchronized ImplementTable getTable() {
            if (implementTables.size() > i) {
                ImplementTable table = implementTables.get(i);
                i++;
                return table;
            } else return null;
        }

        synchronized boolean hasTables() {
            return i < implementTables.size();
        }

        //метод, выдающий CalcProperty подпотокам
        synchronized CalcProperty getProperty() {
            if (storedProperties.size() > j) {
                CalcProperty property = storedProperties.get(j);
                j++;
                return property;
            } else return null;
        }

        synchronized boolean hasProperties() {
            return j < storedProperties.size();
        }
    }

}