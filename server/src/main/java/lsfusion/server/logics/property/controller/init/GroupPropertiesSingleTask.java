package lsfusion.server.logics.property.controller.init;

import lsfusion.base.lambda.E2Runnable;
import lsfusion.server.base.controller.context.Context;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.controller.init.BLGroupSingleTask;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static lsfusion.base.BaseUtils.serviceLogger;
import static lsfusion.server.physics.admin.log.ServerLoggers.runWithServiceLog;

public abstract class GroupPropertiesSingleTask<T> extends BLGroupSingleTask<T> {
    Context threadLocalContext;

    private List<String> currentTasks = Collections.synchronizedList(new ArrayList<>());
    private List<String> messages = Collections.synchronizedList(new ArrayList<>());

    protected long maxRecalculateTime;

    public GroupPropertiesSingleTask() {
        threadLocalContext = ThreadLocalContext.get();
        maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        setDependencies(new HashSet<>());
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

    protected abstract void runInnerTask(T element, ExecutionStack stack) throws SQLException, SQLHandledException;

    protected abstract String getTaskCaption(T element);

    @Override
    protected void runTask(final T element) {
        String caption = getTaskCaption(element);

        //integer - exclusiveness task
        String currentTask = element instanceof Integer ? caption : String.format(caption + ": %s", element);
        checkContext();
        currentTasks.add(currentTask);

        try {
            long time = runWithServiceLog((E2Runnable<SQLException, SQLHandledException>) () -> runInnerTask(element, ThreadLocalContext.getStack()), currentTask);
            if (time > maxRecalculateTime)
                addMessage(currentTask, time);
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
}
