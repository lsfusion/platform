package platform.server.view.navigator;

import platform.server.logics.classes.RemoteClass;

import java.util.LinkedHashMap;
import java.util.Map;

class ClassCache extends LinkedHashMap<RemoteClass, Integer> {

    public ClassCache() {
    }

    public ClassCache(ClassCache classCache) {
        super(classCache);
    }

    public Integer put(RemoteClass cls, Integer value) {

        if (cls == null) {
            throw new RuntimeException("Unable to put null key to cache");
        }

        if (containsKey(cls)) remove(cls);

        if (value != null)
            return super.put(cls, value);
        else
            return null;
    }

    public Integer getObject(RemoteClass cls) {

        Integer objectID = -1;
        for (Map.Entry<RemoteClass, Integer> entry : entrySet()) {
            if (entry.getKey().isParent(cls)) objectID = entry.getValue();
        }

        return objectID;
    }
}
