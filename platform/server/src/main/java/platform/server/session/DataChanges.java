package platform.server.session;

public interface DataChanges<U extends DataChanges<U>> {

    boolean hasChanges();

    void add(U changes);    
}
