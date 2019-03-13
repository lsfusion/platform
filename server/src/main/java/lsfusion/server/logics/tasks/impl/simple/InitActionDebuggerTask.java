package lsfusion.server.logics.tasks.impl.simple;

import com.google.common.base.Throwables;
import lsfusion.base.file.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.physics.dev.debug.ActionPropertyDebugger;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.logics.debug.DebuggerService;
import lsfusion.server.logics.debug.LocalhostClientSocketFactory;
import lsfusion.server.logics.tasks.GroupSplitTask;
import org.apache.log4j.Logger;
import sun.management.jmxremote.LocalRMIServerSocketFactory;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static lsfusion.server.context.ThreadLocalContext.getRmiManager;

public class InitActionDebuggerTask extends GroupSplitTask<String> {
    @Override
    public String getCaption() {
        return "Initializing actions' debugger";
    }

    File sourceDir;
    ImMap<String, ImSet<DebugInfo>> groupDelegates;
    
    @Override
    protected void runGroupTask(ImSet<String> objSet, Logger logger) {
        try {
            ActionPropertyDebugger.getInstance().compileDelegatesHolders(sourceDir, groupDelegates.filter(objSet));
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    protected ImSet<String> getObjects(BusinessLogics BL) {
        return groupDelegates.keys();
    }

    @Override
    protected boolean prerun() {
        
        if(!ActionPropertyDebugger.getInstance().isEnabled())
            return false;

        groupDelegates = ActionPropertyDebugger.getInstance().getGroupDelegates();
        try {
            sourceDir = IOUtils.createTempDirectory("lsfusiondebug");

            DebuggerService stub = (DebuggerService) UnicastRemoteObject.exportObject(ActionPropertyDebugger.getInstance(), 0, new LocalhostClientSocketFactory(), new LocalRMIServerSocketFactory());
            int port = getRmiManager().getDebuggerPort();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("lsfDebuggerService", stub);

        } catch (Exception e) {
            Throwables.propagate(e);
        }

        return true;
    }
}
