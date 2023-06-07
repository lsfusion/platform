package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;

public interface DiffObjectInterface<K, V> {
    K getKey(V object);
    NativeHashMap<K, V> getOldObjectsList();
    void setOldObjectsList(NativeHashMap<K, V> optionsList);

    //todo maybe move to GwtClientUtils ???
    class JavaScriptObjectWrapper {
        private final JavaScriptObject object;

        public JavaScriptObjectWrapper(JavaScriptObject object) {
            this.object = object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof JavaScriptObjectWrapper))
                return false;

            return GwtClientUtils.isJSObjectPropertiesEquals(this.object, ((JavaScriptObjectWrapper) o).object);
        }

        @Override
        public int hashCode() {
            return GwtClientUtils.hashJSObject(object);
        }
    }
}
