package lsfusion.server.physics.admin.authentication.security.policy;

import java.util.*;

public class AbstractSecurityPolicy<T> {
    private Map<T, Boolean> permission = new HashMap<>();

    public void setPermission(T obj, Boolean value) {
        permission.put(obj, value);
    }

    public Boolean checkPermission(T obj) {
        return permission.get(obj);
    }
}
