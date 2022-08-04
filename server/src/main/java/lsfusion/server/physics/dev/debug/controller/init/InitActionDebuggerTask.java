package lsfusion.server.physics.dev.debug.controller.init;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.IOUtils;
import lsfusion.server.base.task.GroupSplitTask;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.physics.dev.debug.ActionDebugger;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.debug.DebuggerService;
import lsfusion.server.physics.dev.debug.LocalhostClientSocketFactory;
import org.apache.log4j.Logger;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.getRmiManager;

public class InitActionDebuggerTask extends GroupSplitTask<String> {
    @Override
    public String getCaption() {
        return "Initializing actions' debugger";
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        ActionDebugger.getInstance().logicsInstance = logicsInstance;
    }

    File sourceDir;
    ImMap<String, ImSet<DebugInfo>> groupDelegates;
    
    @Override
    protected void runGroupTask(ImSet<String> objSet, Logger logger) {
        try {
            ActionDebugger.getInstance().compileDelegatesHolders(sourceDir, groupDelegates.filter(objSet));
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    protected ImSet<String> getObjects() {
        return groupDelegates.keys();
    }

    @Override
    protected boolean prerun() {
        
        if(!ActionDebugger.getInstance().isEnabled())
            return false;

        groupDelegates = ActionDebugger.getInstance().getGroupDelegates();
        try {
            sourceDir = IOUtils.createTempDirectory("lsfusiondebug");

            DebuggerService stub = (DebuggerService) UnicastRemoteObject.exportObject(ActionDebugger.getInstance(), 0, new LocalhostClientSocketFactory(), null);
            int port = getRmiManager().getDebuggerPort();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("lsfDebuggerService", stub);

        } catch (Exception e) {
            Throwables.propagate(e);
        }

        return true;
    }
}
