package lsfusion.server.data;

public class ThreadDebugInfo {
    public String threadName;
    public String threadStackTrace;

    public ThreadDebugInfo(String threadName, String threadStackTrace) {
        this.threadName = threadName;
        this.threadStackTrace = threadStackTrace;
    }
}