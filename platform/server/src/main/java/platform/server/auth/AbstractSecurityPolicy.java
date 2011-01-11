package platform.server.auth;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AbstractSecurityPolicy<T> {

    private Set<T> permitted = new HashSet();
    private Set<T> denied = new HashSet();

    public boolean permitAll;
    public boolean denyAll;

    public boolean defaultPermission = true;

    public void permit(T obj) { permitted.add(obj); }
    public void deny(T obj) { denied.add(obj); }

    public void permit(Collection<? extends T> colObj) { permitted.addAll(colObj); }
    public void deny(Collection<? extends T> colObj) { denied.addAll(colObj); }

    protected void override(AbstractSecurityPolicy<T> policy) {

        if (policy.permitAll) {
            permitted.clear();
            denied.clear();
            permitAll = true;
            defaultPermission = true;
        }

        if (policy.denyAll) {
            permitted.clear();
            denied.clear();
            denyAll = true;
            defaultPermission = false;
        }

        for (T obj : policy.denied) {
            permitted.remove(obj);
            denied.add(obj);
        }

        for (T obj : policy.permitted) {
            denied.remove(obj);
            permitted.add(obj);
        }

        defaultPermission = policy.defaultPermission;
    }

    public boolean checkPermission(T obj) {

        if (permitted.contains(obj)) return true;
        if (denied.contains(obj)) return false;
        return defaultPermission;
    }
}
