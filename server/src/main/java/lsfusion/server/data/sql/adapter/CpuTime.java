package lsfusion.server.data.sql.adapter;

public class CpuTime {
    private final long user;
    private final long nice;
    private final long system;
    private final long idle;
    private final long iowait;
    private final boolean snmp;

    public CpuTime(long user, long nice, long system, long idle, long iowait, boolean snmp) {
        this.user = user;
        this.nice = nice;
        this.system = system;
        this.idle = idle;
        this.iowait = iowait;
        this.snmp = snmp;
    }

    public long getUser() {
        return user;
    }

    public long getNice() {
        return nice;
    }

    public long getSystem() {
        return system;
    }

    public long getIdle() {
        return idle;
    }

    public long getIowait() {
        return iowait;
    }

    public boolean isSnmp() {
        return snmp;
    }
}