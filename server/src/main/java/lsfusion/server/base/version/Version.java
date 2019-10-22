package lsfusion.server.base.version;

public abstract class Version implements Comparable<Version> { // в TreeMap в NFChangeImpl используется
    // converted to static methods to prevent class initialization deadlocks 
    public static Version last() {
        return LastVersion.LAST;
    } 
    
    public static Version current() {
        return LastVersion.CURRENT; // в случаях когда целостность / детерминированность гарантируется использующим алгоритмом
    }
    
    public static Version global() {
        return GlobalVersion.GLOBAL;    
    }

    public static Version descriptor() {
        return null;
    }

    public int compareTo(Version o) {
        return getOrder().compareTo(o.getOrder());
    }

    public abstract Integer getOrder();
    public abstract boolean canSee(Version version);
}
