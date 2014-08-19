package lsfusion.server.logics.tasks.impl.simple;

import com.google.common.base.Throwables;
import lsfusion.server.logics.debug.ActionPropertyDebugger;
import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitActionDebuggerTask extends SimpleBLTask {
    @Override
    public String getCaption() {
        return "Initialiazing actions' debugger";
    }

    @Override
    public void run() {
        try {
            ActionPropertyDebugger.getInstance().compileDelegatesHolders();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
