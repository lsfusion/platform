package lsfusion.server.logics.service.process;

import lsfusion.server.context.ThreadType;

import java.sql.Timestamp;

public class SQLProcess {
    public Timestamp dateTimeCall;
    public ThreadType threadType;
    public String query;
    public String fullQuery;
    public Long user;
    public Long computer;
    public String addressUser;
    public Timestamp dateTime;
    public Boolean isActive;
    public Boolean fusionInTransaction;
    public Boolean baseInTransaction;
    public Long startTransaction;
    public String attemptCount;
    public String status;
    public String lockOwnerId;
    public String lockOwnerName;
    public Integer sqlId;
    public Boolean isDisabledNestLoop;
    public Integer queryTimeout;
    public String debugInfo;

    public SQLProcess(Timestamp dateTimeCall, ThreadType threadType, String query, String fullQuery, Long user, Long computer, String addressUser,
                      Timestamp dateTime, Boolean isActive, Boolean fusionInTransaction, Boolean baseInTransaction,
                      Long startTransaction, String attemptCount, String status, String lockOwnerId, String lockOwnerName,
                      Integer sqlId, Boolean isDisabledNestLoop, Integer queryTimeout, String debugInfo) {
        this.dateTimeCall = dateTimeCall;
        this.threadType = threadType;
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
        this.lockOwnerId = lockOwnerId;
        this.lockOwnerName = lockOwnerName;
        this.sqlId = sqlId;
        this.isDisabledNestLoop = isDisabledNestLoop;
        this.queryTimeout = queryTimeout;
        this.debugInfo = debugInfo;
    }
}