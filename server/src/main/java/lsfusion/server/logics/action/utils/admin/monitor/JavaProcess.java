package lsfusion.server.logics.action.utils.admin.monitor;

public class JavaProcess {
    public String stackTrace;
    public String name;
    public String status;
    public String lockName;
    public String lockOwnerId;
    public String lockOwnerName;
    public String computer;
    public String user;
    public String lsfStackTrace;
    public Long threadAllocatedBytes;
    public Long lastThreadAllocatedBytes;

    public JavaProcess(String stackTrace, String name, String status, String lockName, String lockOwnerId, String lockOwnerName,
                       String computer, String user, String lsfStackTrace, Long threadAllocatedBytes, Long lastThreadAllocatedBytes) {
        this.stackTrace = stackTrace;
        this.name = name;
        this.status = status;
        this.lockName = lockName;
        this.lockOwnerId = lockOwnerId;
        this.lockOwnerName = lockOwnerName;
        this.computer = computer;
        this.user = user;
        this.lsfStackTrace = lsfStackTrace;
        this.threadAllocatedBytes = threadAllocatedBytes;
        this.lastThreadAllocatedBytes = lastThreadAllocatedBytes;
    }
}