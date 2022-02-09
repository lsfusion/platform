package lsfusion.gwt.client.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.impl.SchedulerImpl;

import java.util.*;

public class SmartScheduler extends SchedulerImpl {
    private static SmartScheduler instance;
    private final Set<Integer> commandsSet = new HashSet<>();
    private final List<ScheduledCommand> commandsList = new ArrayList<>();
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
            if(commandsSet.remove(id))
                finalCmd.execute();
        };
        commandsSet.add(id);
        commandsList.add(cmd);

        Scheduler.get().scheduleDeferred(cmd);
    }

    public void flush() {
        while(!commandsList.isEmpty()) {
            commandsList.remove(0).execute();
        }
    }
}
