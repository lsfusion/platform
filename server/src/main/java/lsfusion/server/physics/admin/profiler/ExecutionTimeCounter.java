package lsfusion.server.physics.admin.profiler;

import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.controller.stack.ExecutionStackItem;

import java.util.List;
import java.util.Stack;

public class ExecutionTimeCounter {
    public long sqlTime;
    public long userInteractionTime;

    public MList<String> info;

    public ExecutionTimeCounter() {
        this(0, 0);
    }
    
    public ExecutionTimeCounter(long sqlTime, long userInteractionTime) {
        this.sqlTime = sqlTime;
        this.userInteractionTime = userInteractionTime;
    }
    
    public void addSql(long sqlTime) {
        this.sqlTime += sqlTime;
    }
    
    public void addUI(long userInteractionTime) {
        this.userInteractionTime += userInteractionTime;
    }
}
