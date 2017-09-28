package lsfusion.server.data;

public class SQLThreadInfo {
    public Thread javaThread;
    public boolean baseInTransaction;
    public Long startTransaction;
    public String attemptCount;
    public Long userActiveTask;
    public Long computerActiveTask;
    public String fullQuery;
    public boolean isDisabledNestLoop;
    public Integer queryTimeout;
    public String debugInfo;

    public SQLThreadInfo(Thread javaThread, boolean baseInTransaction, Long startTransaction, String attemptCount, Long userActiveTask,
                         Long computerActiveTask, String fullQuery, boolean isDisabledNestLoop, Integer queryTimeout, String debugInfo) {
        this.javaThread = javaThread;
        this.baseInTransaction = baseInTransaction;
        this.startTransaction = startTransaction;
        this.attemptCount = attemptCount;
        this.userActiveTask = userActiveTask;
        this.computerActiveTask = computerActiveTask;
        this.fullQuery = fullQuery;
        this.isDisabledNestLoop = isDisabledNestLoop;
        this.queryTimeout = queryTimeout;
        this.debugInfo = debugInfo;
    }
}