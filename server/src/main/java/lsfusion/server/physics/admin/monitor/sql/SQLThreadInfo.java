package lsfusion.server.physics.admin.monitor.sql;

import lsfusion.server.physics.admin.monitor.StatusMessage;
import lsfusion.server.physics.admin.monitor.ThreadDebugInfo;

public class SQLThreadInfo {
    public Thread javaThread;
    public ThreadDebugInfo javaThreadDebugInfo;
    public boolean baseInTransaction;
    public Long startTransaction;
    public String attemptCount;
    public Long userActiveTask;
    public Long computerActiveTask;
    public String fullQuery;
    public boolean isDisabledNestLoop;
    public Integer queryTimeout;
    public String debugInfo;
    public StatusMessage statusMessage;

    public SQLThreadInfo(Thread javaThread, ThreadDebugInfo javaThreadDebugInfo, boolean baseInTransaction, Long startTransaction,
                         String attemptCount, Long userActiveTask, Long computerActiveTask, String fullQuery, boolean isDisabledNestLoop,
                         Integer queryTimeout, String debugInfo, StatusMessage statusMessage) {
        this.javaThread = javaThread;
        this.javaThreadDebugInfo = javaThreadDebugInfo;
        this.baseInTransaction = baseInTransaction;
        this.startTransaction = startTransaction;
        this.attemptCount = attemptCount;
        this.userActiveTask = userActiveTask;
        this.computerActiveTask = computerActiveTask;
        this.fullQuery = fullQuery;
        this.isDisabledNestLoop = isDisabledNestLoop;
        this.queryTimeout = queryTimeout;
        this.debugInfo = debugInfo;
        this.statusMessage = statusMessage;
    }
}