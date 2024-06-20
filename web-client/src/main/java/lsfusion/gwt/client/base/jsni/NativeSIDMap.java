package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class NativeSIDMap<K extends HasNativeSID, V> {

    private JavaScriptObject nativeSIDMap;

    public NativeSIDMap() {
        init();
    }

    public void clear() {
        init();
    }

    private void init() {
        nativeSIDMap = createMap();
    }

    public native JavaScriptObject createMap() /*-{
        return new Map();
    }-*/;

    public boolean containsKey(K key) {
        return jsContainsKey(key.getNativeSID(), nativeSIDMap);
    }

    public boolean containsValue(final Object val) {
        return jsContainsValue(val, nativeSIDMap);
    }

    public V get(K key) {
        return jsGet(key.getNativeSID(), nativeSIDMap);
    }

    public V put(K key, V value) {
        return jsPut(key.getNativeSID(), key, value, nativeSIDMap);
    }

    public void putAll(NativeSIDMap<? extends K, ? extends V> m) {
        m.foreachEntry(this::put);
    }

    public V remove(K key) {
        return jsRemove(key.getNativeSID(), nativeSIDMap);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return jsSize(nativeSIDMap);
    }

    public K singleKey() {
        return jsSingleKey(nativeSIDMap);
    }

    public String toString() {
        final JsArrayString ts = JsArray.createArray().cast();
        ts.push("{");
        foreachEntry((key, value) -> {
            ts.push(",");
            ts.push(key == null ? null : key.toString());
            ts.push("=");
            ts.push(value == null ? null : value.toString());
        });
        ts.push("}");
        return ts.join();
    }

    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                put(key, newValue);
                return newValue;
            }
        }

        return v;
    }

    public void foreachKey(Function<K> f) {
        jsForeachKey(f, nativeSIDMap);
    }

    public void foreachValue(Function<V> f) {
        jsForeachValue(f, nativeSIDMap);
    }

    public void foreachEntry(Function2<K, V> f) {
        jsForeachEntry(f, nativeSIDMap);
    }

    /**
     * Bridge methods from JSNI that keeps us from having to make polymorphic calls in JSNI.
     * By putting the polymorphism in Java code, the compiler can do a better job of optimizing in most cases.
     */
    private boolean equalsBridge(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }

    private void bridgeApply(Function f, Object obj) {
        f.apply(obj);
    }

    private void bridgeApply2(Function2 f, Object obj1, Object obj2) {
        f.apply(obj1, obj2);
    }

    public native V jsGet(String key, JavaScriptObject nativeSIDMap) /*-{
        var array = nativeSIDMap.get(key);
        return array ? array[1] : null;
    }-*/;

    private native V jsPut(String sKey, K key, V value, JavaScriptObject nativeSIDMap) /*-{
        var array = nativeSIDMap.get(sKey);
        var returnVal;
        if (array)
            returnVal = array[1];

        if (!array || returnVal)
            this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::nativeSIDMap.set(sKey, [key, value]);

        return returnVal;
    }-*/;

    public native int jsSize(JavaScriptObject nativeSIDMap) /*-{
        return nativeSIDMap.size;
    }-*/;

    private native V jsRemove(String sKey, JavaScriptObject nativeSIDMap) /*-{
        var array = nativeSIDMap.get(key);

        if (array) {
            nativeSIDMap['delete'](key);
            return array[1];
        }

        return null;
    }-*/;

    private native boolean jsContainsKey(String key, JavaScriptObject nativeSIDMap) /*-{
        return  nativeSIDMap.has(key);
    }-*/;

    private native boolean jsContainsValue(Object value, JavaScriptObject nativeSIDMap) /*-{
        var thisObj = this;
        nativeSIDMap.values().forEach(function (array) {
            if (thisObj.@lsfusion.gwt.client.base.jsni.NativeSIDMap::equalsBridge(*)(value, array[1]))
                return true;
        });
        return false;
    }-*/;

    private native K jsSingleKey(JavaScriptObject nativeSIDMap) /*-{
        return nativeSIDMap.keys().next().value;
    }-*/;

    private native void jsForeachKey(Function f, JavaScriptObject nativeSIDMap) /*-{
        var thisObj = this;
        nativeSIDMap.forEach(function (array) {
            thisObj.@lsfusion.gwt.client.base.jsni.NativeSIDMap::bridgeApply(*)(f, array[0]);
        });
    }-*/;

    private native void jsForeachValue(Function f, JavaScriptObject nativeSIDMap) /*-{
        var thisObj = this;
        nativeSIDMap.values().forEach(function (array) {
            thisObj.@lsfusion.gwt.client.base.jsni.NativeSIDMap::bridgeApply(*)(f, array[1]);
        });
    }-*/;

    private native void jsForeachEntry(Function2 f, JavaScriptObject nativeSIDMap) /*-{
        var thisObj = this;
        nativeSIDMap.values().forEach(function (array) {
            thisObj.@NativeSIDMap::bridgeApply2(*)(f, array[0], array[1]);
        });
    }-*/;
}
