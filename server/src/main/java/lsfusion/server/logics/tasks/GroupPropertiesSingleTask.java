package lsfusion.server.logics.tasks;

import lsfusion.server.Settings;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class GroupPropertiesSingleTask extends GroupSingleTask<Object> {
    Context threadLocalContext;
    private List<String> currentTasks;
    private List<String> messages;
    protected long maxRecalculateTime;

    @Override
    protected boolean isGraph() {
        return true;
    }

    @Override
    protected String getElementCaption(Object element, int all, int current) {
        return null;
    }

    @Override
    public String getCaption() {
        return null;
    }

    protected void initContext() {
        if(ThreadLocalContext.get() == null)
            ThreadLocalContext.set(threadLocalContext);
    }

    protected void startedTask(String task) {
        initContext();
        currentTasks.add(task);
    }

    protected void finishedTask(String task) {
        currentTasks.remove(task);
    }

    public void logTimeoutTasks() {
        for(String task : currentTasks){
            messages.add(String.format("General Task Timeout: %s", task));
        }
    }

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        threadLocalContext = ThreadLocalContext.get();
        maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        setBL(context.getBL());
        setDependencies(new HashSet<PublicTask>());
        currentTasks = new ArrayList<>();
        messages = new ArrayList<>();
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
        if(messages != null) {
            for (String m : messages)
                message += '\n' + m;
        }
        return message;
    }

    public DBManager getDbManager() {
        return getBL().getDbManager();
    }
}
