package platform.server.session;

public interface Changes<U extends Changes<U>> {

    boolean hasChanges();

    void add(U changes);    
}
