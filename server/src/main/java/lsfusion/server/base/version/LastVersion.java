package lsfusion.server.base.version;

public class LastVersion extends Version {

    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    public boolean canSee(Version version) {
        return true;
    }
    
    // placed here to prevent class initialization deadlocks 
    static final Version LAST = new LastVersion();
    static final Version CURRENT = new LastVersion();
}
