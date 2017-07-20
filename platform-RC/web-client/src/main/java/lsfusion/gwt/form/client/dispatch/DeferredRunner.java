package lsfusion.gwt.form.client.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import lsfusion.gwt.form.shared.view.GGroupObject;

import java.util.HashMap;
import java.util.Map;

public class DeferredRunner {
    public final int DEFAULT_DELAY = 50;

    private static DeferredRunner instance;

    public static DeferredRunner get() {
        if (instance == null) {
            instance = new DeferredRunner();
        }
        return instance;
    }

    private final Map<String, Command> commands = new HashMap<>();

    private DeferredRunner() {}

    public void reschedule(String sid, Command cmd) {
        reschedule(sid, cmd, DEFAULT_DELAY);
    }

    public void reschedule(final String sid, final Command cmd, int delay) {
        Log.debug("Rescheduling command: " + sid);

        cancel(sid);

        commands.put(sid, cmd);

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (!cmd.isCanceled()) {
                    Log.debug("Executing command: " + sid);
                    cmd.execute();
                    commands.remove(sid);
                }

                return false;
            }
        }, delay);
    }

    public void executeNow(String sid) {
        Command command = commands.get(sid);
        if (command != null) {
            command.execute();
        }
    }

    public void cancel(String sid) {
        Command command = commands.get(sid);
        if (command != null) {
            command.cancel();
            commands.remove(sid);
        }
    }

    public void scheduleDelayedGroupObjectChange(GGroupObject groupObject, Command cmd) {
        reschedule(groupObjectChangeCommandID(groupObject), cmd);
    }

    public void cancelDelayedGroupObjectChange(GGroupObject groupObject) {
        if (groupObject != null) {
            cancel(groupObjectChangeCommandID(groupObject));
        }
    }

    public void commitDelayedGroupObjectChange(GGroupObject groupObject) {
        if (groupObject != null) {
            executeNow(groupObjectChangeCommandID(groupObject));
        }
    }

    private static String groupObjectChangeCommandID(GGroupObject groupObject) {
        return "groupObjectChange" + groupObject.ID;
    }

    public void scheduleChangePageSize(GGroupObject groupObject, Command cmd) {
        reschedule(changePageSizeCommandID(groupObject), cmd, 100);
    }

    private String changePageSizeCommandID(GGroupObject groupObject) {
        return "changePageSize" + groupObject.ID;
    }

    public interface Command {
        void execute();

        void cancel();

        boolean isCanceled();

    }

    public static abstract class AbstractCommand implements Command {
        private boolean canceled = false;

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;

        }
    }
}
