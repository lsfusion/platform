package lsfusion.server.data;

import lsfusion.server.context.ThreadType;

public class ThreadDebugInfo {
    public String threadName;
    public ThreadType threadType;
    public String threadStackTrace;

    public ThreadDebugInfo(String threadName, ThreadType threadType, String threadStackTrace) {
        this.threadName = threadName;
        this.threadType = threadType;
        this.threadStackTrace = threadStackTrace;
    }
}