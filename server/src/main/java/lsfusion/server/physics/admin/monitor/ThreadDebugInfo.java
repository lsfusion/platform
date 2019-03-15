package lsfusion.server.physics.admin.monitor;

public class ThreadDebugInfo {
    public String threadName;
    public String threadStackTrace;

    public ThreadDebugInfo(String threadName, String threadStackTrace) {
        this.threadName = threadName;
        this.threadStackTrace = threadStackTrace;
    }
}