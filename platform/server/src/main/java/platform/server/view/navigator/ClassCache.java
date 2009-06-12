package platform.server.view.navigator;

import platform.server.data.classes.ConcreteCustomClass;
import platform.server.data.classes.CustomClass;

import java.util.LinkedHashMap;
import java.util.Map;

class ClassCache extends LinkedHashMap<ConcreteCustomClass, Integer> {

    public ClassCache() {
    }

    public ClassCache(ClassCache classCache) {
        super(classCache);
    }

    public Integer put(ConcreteCustomClass cls, Integer value) {

        if (cls == null) {
            throw new RuntimeException("Unable to put null key to cache");
        }

        if (containsKey(cls)) remove(cls);

        if (value != null)
            return super.put(cls, value);
        else
            return null;
    }

    public Integer getObject(CustomClass cls) {

        Integer objectID = -1;
        for (Map.Entry<ConcreteCustomClass,Integer> entry : entrySet()) {
            if (entry.getKey().isChild(cls)) objectID = entry.getValue();
        }

        return objectID;
    }
}
