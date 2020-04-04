package lsfusion.server.physics.admin.authentication.security.policy;

import java.util.*;

public class ElementSecurityPolicy<T> {
    private Map<T, Boolean> permission = new HashMap<>();

    synchronized public void setPermission(T obj, Boolean value) {

        permission.put(obj, value);
    }

    synchronized public Boolean checkPermission(T obj) {
        return permission.get(obj);
    }
}
