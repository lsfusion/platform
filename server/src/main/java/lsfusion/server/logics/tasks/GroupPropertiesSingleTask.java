package lsfusion.server.logics.tasks;

import lsfusion.server.Settings;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class GroupPropertiesSingleTask extends GroupSingleTask<Object> {
    public List<String> messages;
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

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        setBL(context.getBL());
        setDependencies(new HashSet<PublicTask>());
        messages = new ArrayList<>();
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    public void addMessage(Object property, long time) {
        messages.add(String.format("Long task %s: %sms", property, time));
    }

    public void addMessage(String prefix, Object property, Exception e) {
        messages.add(String.format("\nException occurred:\n%s %s\n%s\n", prefix, property, e));
    }

    public String getMessages() {
        String message = "";
        for(String m : messages)
            message += '\n' + m;
        return message;
    }

    public DBManager getDbManager() {
        return getBL().getDbManager();
    }
}
