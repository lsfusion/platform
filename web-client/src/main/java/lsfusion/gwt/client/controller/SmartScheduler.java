package lsfusion.gwt.client.controller;

import com.google.gwt.core.client.impl.SchedulerImpl;

import java.util.ArrayList;
import java.util.List;

public class SmartScheduler extends SchedulerImpl {
    private static SmartScheduler instance;
    public final List<ScheduledCommand> commands = new ArrayList<>();
    private SmartScheduler() {
    }

    public static SmartScheduler getInstance() {
        if (instance == null)
            instance = new SmartScheduler();
        return instance;
    }

    @Override
    public void scheduleEntry(ScheduledCommand cmd) {
        commands.add(cmd);
    }

    public void executeAll(boolean clear){
        commands.forEach(ScheduledCommand::execute);
        if (clear && !commands.isEmpty())
            commands.clear();
    }
}
