package lsfusion.server.logics.mutables;

public interface Version extends Comparable<Version> {
    
    public static final Version LAST = new LastVersion();
    
    public static final Version CURRENT = new LastVersion(); // в случаях когда целостность \ детерминированность гарантируется использующим алгоритмом

    public static final Version DESCRIPTOR = null;

    Integer getOrder();
    
    boolean canSee(Version version);
    
}
