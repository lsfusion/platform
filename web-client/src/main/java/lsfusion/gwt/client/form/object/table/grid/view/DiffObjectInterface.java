package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
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

        public JavaScriptObject get() {
            return GStateTableView.fromObject(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            JavaScriptObjectWrapper that = (JavaScriptObjectWrapper) o;
            if (o == null || getClass() != o.getClass())
                return false;

            return GwtClientUtils.isJSObjectPropertiesEquals(this.object, that.object);
        }

        @Override
        public int hashCode() {
            int hash = 0;
            JsArrayString keys = GwtClientUtils.getKeys(object);
            for (int i = 0; i < keys.length(); i++) {
                hash += keys.get(i).hashCode();
            }
            return hash;
        }
    }
}
