package lsfusion.server.logics.mutables;

public abstract class Version implements Comparable<Version> { // в TreeMap в NFChangeImpl используется

    public static final Version LAST = new LastVersion();

    public static final Version CURRENT = new LastVersion(); // в случаях когда целостность \ детерминированность гарантируется использующим алгоритмом

    public static final Version GLOBAL = new GlobalVersion();
    
    public static final Version DESCRIPTOR = null;
    
    public int compareTo(Version o) {
        return getOrder().compareTo(o.getOrder());
    }

    public abstract Integer getOrder();
    public abstract boolean canSee(Version version);
    
}
