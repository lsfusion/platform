package lsfusion.server.logics.navigator.controller.env;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import java.util.Map;

public class GroupObjectCache extends OrderedMap<GroupObjectEntity, Long> {

    public GroupObjectCache() {
    }

    public GroupObjectCache(GroupObjectCache classCache) {
        super(classCache);
    }

    public Long put(GroupObjectEntity groupObject, Long value) {

        if (groupObject == null) {
            throw new RuntimeException("Unable to put null key to cache");
        }

        if (containsKey(groupObject)) remove(groupObject);

        if (value != null)
            return super.put(groupObject, value);
        else
            return null;
    }

    public Long getObject(GroupObjectEntity groupObject) {

        Long objectID = null;
        for (Map.Entry<GroupObjectEntity,Long> entry : entrySet())
            if (entry.getKey() == groupObject)
                objectID = entry.getValue();

        return objectID;
    }
}
