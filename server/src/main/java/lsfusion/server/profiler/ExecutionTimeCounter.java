package lsfusion.server.profiler;

public class ExecutionTimeCounter {
    public long sqlTime;
    public long userInteractionTime;
    
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
