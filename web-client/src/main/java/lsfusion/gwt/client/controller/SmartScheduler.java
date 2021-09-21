package lsfusion.gwt.client.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.impl.SchedulerImpl;

import java.util.HashMap;
import java.util.Map;

public class SmartScheduler extends SchedulerImpl {
    private static SmartScheduler instance;
    private final Map<Integer, ScheduledCommand> commandsMap = new HashMap<>();
    private int id = 0;
    private SmartScheduler() {
    }

    public static SmartScheduler getInstance() {
        if (instance == null)
            instance = new SmartScheduler();
        return instance;
    }

    public void scheduleDeferred(boolean deferred, ScheduledCommand cmd) {
        if(deferred)
            scheduleDeferred(cmd);
        else
            cmd.execute();
    }

    @Override
    public void scheduleDeferred(ScheduledCommand cmd) {
        id++;
        ScheduledCommand finalCmd = cmd;

        cmd = () -> {
            if(commandsMap.remove(id) != null)
                finalCmd.execute();
        };
        commandsMap.put(id, cmd);

        Scheduler.get().scheduleDeferred(cmd);
    }

    public void flush() {
        this.commandsMap.values().forEach(ScheduledCommand::execute);
    }
}
