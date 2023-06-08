package lsfusion.gwt.client.form.object.table.grid.view;

import lsfusion.gwt.client.base.jsni.NativeHashMap;

public interface DiffObjectInterface<K, V> {
    K getKey(V object);
    NativeHashMap<K, V> getOldObjectsList();
    void setOldObjectsList(NativeHashMap<K, V> optionsList);
}
