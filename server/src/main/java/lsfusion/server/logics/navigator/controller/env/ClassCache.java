package lsfusion.server.logics.navigator.controller.env;

import lsfusion.base.Pair;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import java.util.ArrayList;
import java.util.Map;

public class ClassCache extends OrderedMap<ConcreteCustomClass, Pair<FormEntity, OrderedMap<GroupObjectEntity, Long>>> { //list -> map ці два лісты

    public ClassCache() {
    }

    public ClassCache(ClassCache classCache) {
        super(classCache);
    }

    public Pair<FormEntity, OrderedMap<GroupObjectEntity, Long>> put(ConcreteCustomClass cls, FormEntity form, GroupObjectEntity groupObject, Long value) {

        if (cls == null) {
            throw new RuntimeException("Unable to put null key to cache");
        }

        Pair<FormEntity, OrderedMap<GroupObjectEntity, Long>> pair = get(cls);
        if (pair == null || !pair.first.equals(form)) {
            pair = Pair.create(form, new OrderedMap<>());
        }
        pair.second.put(groupObject, value);
        return super.put(cls, pair);
    }

    public Long getObject(CustomClass cls, FormEntity form, GroupObjectEntity groupObject) {

        Long objectID = null;
        for (Map.Entry<ConcreteCustomClass, Pair<FormEntity, OrderedMap<GroupObjectEntity, Long>>> entry : entrySet()) {
            Pair<FormEntity, OrderedMap<GroupObjectEntity, Long>> pair = entry.getValue();
            OrderedMap<GroupObjectEntity, Long> groupObjectValueMap = pair.second;
            if (pair.first.equals(form)) {
                Long result = groupObjectValueMap.get(groupObject);
                if (result != null) {
                    return result;
                }
            }
            if (entry.getKey().isChild(cls)) {
                objectID = new ArrayList<>(groupObjectValueMap.entrySet()).get(groupObjectValueMap.size() - 1).getValue();
            }
        }

        return objectID;
    }
}
