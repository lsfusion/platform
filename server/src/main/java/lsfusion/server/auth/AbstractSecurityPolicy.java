package lsfusion.server.auth;

import lsfusion.server.ServerLoggers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AbstractSecurityPolicy<T> {

    private Set<T> permitted = new HashSet<T>();
    private Set<T> denied = new HashSet<T>();

    public boolean replaceMode = false;
    public Boolean defaultPermission;

    public void permit(T obj) {
        denied.remove(obj);
        permitted.add(obj);
    }

    public void deny(T obj) {
        permitted.remove(obj);
        denied.add(obj);
    }

    public void permit(T... objs) { for (T obj : objs) permit(obj); }
    public void deny(T... objs) { for (T obj : objs) deny(obj); }

    public void permit(Collection<? extends T> colObj) {
        denied.removeAll(colObj);
        permitted.addAll(colObj);
    }

    public void deny(Collection<? extends T> colObj) {
        permitted.removeAll(colObj);
        denied.addAll(colObj);
    }

    protected void override(AbstractSecurityPolicy<T> policy) {

        if (policy.replaceMode) {
            permitted.clear();
            denied.clear();
        }

        deny(policy.denied);
        permit(policy.permitted);

        if (policy.defaultPermission != null)
            defaultPermission = policy.defaultPermission;
    }

    public boolean checkPermission(T obj) {

        if (permitted.contains(obj))
            return true;
        if (denied.contains(obj)) {
//            ServerLoggers.securityLogger.info("Denied : " + obj);
            return false;
        }
        if (defaultPermission != null && !defaultPermission) {
//            ServerLoggers.securityLogger.info("No default permission : " + obj);
            return false;
        }
        return true;
    }
}
