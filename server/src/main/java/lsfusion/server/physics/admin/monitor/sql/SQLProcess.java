package lsfusion.server.physics.admin.monitor.sql;

import lsfusion.server.physics.admin.monitor.StatusMessage;

import java.time.LocalDateTime;

public class SQLProcess {
    public LocalDateTime dateTimeCall;
    public String query;
    public String fullQuery;
    public Long user;
    public Long computer;
    public String addressUser;
    public LocalDateTime dateTime;
    public Boolean isActive;
    public Boolean fusionInTransaction;
    public Boolean baseInTransaction;
    public Long startTransaction;
    public String attemptCount;
    public String status;
    public StatusMessage statusMessage;
    public String lockOwnerId;
    public String lockOwnerName;
    public Integer sqlId;
    public Boolean isDisabledNestLoop;
    public Integer queryTimeout;
    public String debugInfo;
    public String threadName;
    public String threadStackTrace;

    public SQLProcess(LocalDateTime dateTimeCall, String query, String fullQuery, Long user, Long computer, String addressUser,
                      LocalDateTime dateTime, Boolean isActive, Boolean fusionInTransaction, Boolean baseInTransaction,
                      Long startTransaction, String attemptCount, String status, StatusMessage statusMessage, String lockOwnerId, String lockOwnerName,
                      Integer sqlId, Boolean isDisabledNestLoop, Integer queryTimeout, String debugInfo, String threadName, String threadStackTrace) {
        this.dateTimeCall = dateTimeCall;
        this.query = query;
        this.fullQuery = fullQuery;
        this.user = user;
        this.computer = computer;
        this.addressUser = addressUser;
        this.dateTime = dateTime;
        this.isActive = isActive;
        this.fusionInTransaction = fusionInTransaction;
        this.baseInTransaction = baseInTransaction;
        this.startTransaction = startTransaction;
        this.attemptCount = attemptCount;
        this.status = status;
        this.statusMessage = statusMessage;
        this.lockOwnerId = lockOwnerId;
        this.lockOwnerName = lockOwnerName;
        this.sqlId = sqlId;
        this.isDisabledNestLoop = isDisabledNestLoop;
        this.queryTimeout = queryTimeout;
        this.debugInfo = debugInfo;
        this.threadName = threadName;
        this.threadStackTrace = threadStackTrace;
    }
}