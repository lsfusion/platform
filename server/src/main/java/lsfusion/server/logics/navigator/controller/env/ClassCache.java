package lsfusion.server.logics.navigator.controller.env;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import java.util.Map;

// the objects the user is working with : the interactive forms register their current objects here, and the forms
// opened later (and NEW ... AUTOSET) use them as the "current object" of the class
public class ClassCache {

    // in use order, the most recently used last, so that the last matching entry of a scan is the current one
    private final OrderedMap<Key, Long> cache = new OrderedMap<>();

    // synchronized, because the forms of one navigator run in their own threads and every put reorders the map
    public synchronized void put(ConcreteCustomClass cls, FormEntity form, GroupObjectEntity groupObject, Long value) {
        Key key = new Key(cls, form, groupObject);
        cache.remove(key); // the re-put has to move the entry to the end, LinkedHashMap keeps the insertion order of a known key
        cache.put(key, value);
    }

    // the object of the class the given form is working with : its own group object first, then any other object of that
    // form, and only then the most recently used object of another form (the form is null when there is no form context)
    public synchronized Long getObject(CustomClass cls, FormEntity form, GroupObjectEntity groupObject) {
        Long groupObjectObject = null;
        Long formObject = null;
        Long lastObject = null;

        for (Map.Entry<Key, Long> entry : cache.entrySet()) {
            Key key = entry.getKey();
            if (!key.cls.isChild(cls))
                continue;

            if (key.form.equals(form)) {
                if (key.groupObject.equals(groupObject))
                    groupObjectObject = entry.getValue();
                else
                    formObject = entry.getValue();
            }
            lastObject = entry.getValue();
        }

        if (groupObjectObject != null)
            return groupObjectObject;
        return formObject != null ? formObject : lastObject;
    }

    private static class Key {
        private final ConcreteCustomClass cls;
        private final FormEntity form;
        private final GroupObjectEntity groupObject;

        private Key(ConcreteCustomClass cls, FormEntity form, GroupObjectEntity groupObject) {
            this.cls = cls;
            this.form = form;
            this.groupObject = groupObject;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Key))
                return false;
            Key key = (Key) o;
            return cls.equals(key.cls) && form.equals(key.form) && groupObject.equals(key.groupObject);
        }

        public int hashCode() {
            return 31 * (31 * cls.hashCode() + form.hashCode()) + groupObject.hashCode();
        }
    }
}
