package platform.server.form.navigator;

import platform.base.OrderedMap;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;

import java.util.Map;

class ClassCache extends OrderedMap<ConcreteCustomClass, Integer> {

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

        Integer objectID = null;
        for (Map.Entry<ConcreteCustomClass,Integer> entry : entrySet())
            if (entry.getKey().isChild(cls))
                objectID = entry.getValue();

        return objectID;
    }
}
