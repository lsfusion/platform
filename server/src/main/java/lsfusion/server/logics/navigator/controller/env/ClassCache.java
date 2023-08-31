package lsfusion.server.logics.navigator.controller.env;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassCache extends OrderedMap<ConcreteCustomClass, List<ClassCache.ClassCacheEntry>> {

    public ClassCache() {
    }

    public ClassCache(ClassCache classCache) {
        super(classCache);
    }

    public List<ClassCacheEntry> put(ConcreteCustomClass cls, FormEntity form, GroupObjectEntity groupObject, Long value) {

        if (cls == null) {
            throw new RuntimeException("Unable to put null key to cache");
        }

        List<ClassCacheEntry> classCacheEntryList = getOrDefault(cls, new ArrayList<>());
        classCacheEntryList.removeIf(e -> e.form == form && e.groupObject == groupObject);

        if (value != null) {
            classCacheEntryList.add(new ClassCacheEntry(form, groupObject, value));
            return super.put(cls, classCacheEntryList);
        } else
            return null;
    }

    public Long getObject(CustomClass cls, FormEntity form, GroupObjectEntity groupObject) {

        Long objectID = null;
        for (Map.Entry<ConcreteCustomClass, List<ClassCacheEntry>> entry : entrySet()) {
            List<ClassCacheEntry> classCacheEntryList = entry.getValue();
            ClassCacheEntry result = classCacheEntryList.stream().filter(e -> e.form == form && e.groupObject == groupObject).findFirst().orElse(null);
            if(result != null) {
                return result.value;
            } else {
                if (entry.getKey().isChild(cls)) {
                    objectID = classCacheEntryList.get(classCacheEntryList.size() - 1).value;
                }
            }
        }

        return objectID;
    }

    public class ClassCacheEntry {
        private FormEntity form;
        private GroupObjectEntity groupObject;
        private Long value;

        public ClassCacheEntry(FormEntity form, GroupObjectEntity groupObject, Long value) {
            this.form = form;
            this.groupObject = groupObject;
            this.value = value;
        }
    }
}
