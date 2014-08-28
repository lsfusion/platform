package lsfusion.server.logics.tasks.impl.simple;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.debug.ActionDebugInfo;
import lsfusion.server.logics.debug.ActionPropertyDebugger;
import lsfusion.server.logics.tasks.GroupSplitTask;
import lsfusion.server.logics.tasks.SimpleBLTask;

import java.io.File;
import java.io.IOException;

public class InitActionDebuggerTask extends GroupSplitTask<String> {
    @Override
    public String getCaption() {
        return "Initialiazing actions' debugger";
    }

    File sourceDir;
    ImMap<String, ImSet<ActionDebugInfo>> groupDelegates;
    
    @Override
    protected void runGroupTask(ImSet<String> objSet) {
        try {
            ActionPropertyDebugger.getInstance().compileDelegatesHolders(sourceDir, groupDelegates.filter(objSet));
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    protected ImSet<String> getObjects(BusinessLogics<?> BL) {
        return groupDelegates.keys();
    }

    @Override
    protected boolean prerun() {
        
        if(!ActionPropertyDebugger.getInstance().isEnabled())
            return false;

        groupDelegates = ActionPropertyDebugger.getInstance().getGroupDelegates();
        try {
            sourceDir = IOUtils.createTempDirectory("lsfusiondebug");
        } catch (Exception e) {
            Throwables.propagate(e);
        }

        return true;
    }
}
