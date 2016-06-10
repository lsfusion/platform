package lsfusion.server.logics.tasks;

import lsfusion.server.Settings;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.context.ExecutionStack;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static lsfusion.base.BaseUtils.serviceLogger;

public abstract class GroupPropertiesSingleTask<T> extends GroupSingleTask<T> {
    Context threadLocalContext;

    private List<String> currentTasks = Collections.synchronizedList(new ArrayList<String>());
    private List<String> messages = Collections.synchronizedList(new ArrayList<String>());

    protected long maxRecalculateTime;

    public GroupPropertiesSingleTask() {
        threadLocalContext = ThreadLocalContext.get();
        maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        setDependencies(new HashSet<PublicTask>());
    }

    @Override
    protected boolean isGraph() {
        return true;
    }

    @Override
    protected String getElementCaption(T element, int all, int current) {
        return null;
    }

    @Override
    public String getCaption() {
        return null;
    }

    protected void checkContext() {
        ThreadLocalContext.assureContext(threadLocalContext);
    }

    protected abstract void runInnerTask(T element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException;

    protected abstract String getTaskCaption(T element);

    @Override
    protected void runTask(final T element) throws RecognitionException {
        String caption = getTaskCaption(element);

        String currentTask = String.format(caption + ": %s", element);
        checkContext();
        currentTasks.add(currentTask);

        try {
            long start = System.currentTimeMillis();
            serviceLogger.info(currentTask);

            runInnerTask(element, ThreadLocalContext.getStack());

            long time = System.currentTimeMillis() - start;
            if (time > maxRecalculateTime)
                addMessage(element, time);
            serviceLogger.info(String.format(caption + " : %s, %sms", currentTask, time));
        } catch (SQLException | SQLHandledException e) {
            addMessage(caption + " :", element, e);
            serviceLogger.info(currentTask, e);
        } finally {
            currentTasks.remove(currentTask);
        }
    }

    public void logTimeoutTasks() {
        for(String task : currentTasks){
            messages.add(String.format("General Task Timeout: %s", task));
        }
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    public void addMessage(Object property, long time) {
        messages.add(String.format("Long task %s: %sms", property, time));
    }

    public void addMessage(String prefix, Object property, Exception e) {
        messages.add(e.getMessage() != null && e.getMessage().contains("FATAL: terminating connection due to administrator command") ?
                String.format("Single Task Timeout: %s %s", prefix, property) :
                e.getMessage() != null && e.getMessage().contains("This connection has been closed") ? String.format("Connection closed: %s %s", prefix, property) :
                        String.format("Exception occurred:\n%s %s\n%s\n", prefix, property, e));
    }

    public String getMessages() {
        String message = "";
        for (String m : messages)
            message += '\n' + m;
        return message;
    }

    public DBManager getDbManager() {
        return getBL().getDbManager();
    }
}
