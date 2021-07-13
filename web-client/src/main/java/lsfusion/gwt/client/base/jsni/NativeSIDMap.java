package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class NativeSIDMap<K extends HasNativeSID, V> {

    private JavaScriptObject keyMap;
    private JavaScriptObject valueMap;

    public NativeSIDMap() {
        init();
    }

    public void clear() {
        init();
    }

    private void init() {
        keyMap = JavaScriptObject.createObject();
        valueMap = JavaScriptObject.createObject();
    }

    public boolean containsKey(K key) {
        return jsContainsKey(key.getNativeSID(), keyMap);
    }

    public boolean containsValue(final Object val) {
        return jsContainsValue(val, valueMap);
    }

    public V get(K key) {
        return jsGet(key.getNativeSID());
    }

    public V put(K key, V value) {
        return jsPut(key.getNativeSID(), key, value);
    }

    public void putAll(NativeSIDMap<? extends K, ? extends V> m) {
        m.foreachEntry(this::put);
    }

    public V remove(K key) {
        return jsRemove(key.getNativeSID());
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return jsSize();
    }

    public K singleKey() {
        return jsSingleKey(valueMap);
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
        foreach(keyMap, f);
    }

    public void foreachValue(Function<V> f) {
        foreach(valueMap, f);
    }

    public void foreachEntry(Function2<K, V> f) {
        jsForeachEntry(f);
    }

    private <T> void foreach(JavaScriptObject jsMap, Function<T> f) {
        jsForeach(f, jsMap);
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

    // Prepend ':' to avoid conflicts with built-in Object properties.
    public native V jsGet(String key) /*-{
        return this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::valueMap[':' + key];
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native V jsPut(String sKey, K key, V value) /*-{
        sKey = ':' + sKey;

        var keyMap = this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::keyMap;
        var valueMap = this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::valueMap;

        var previous = valueMap[sKey];

        keyMap[sKey] = key;
        valueMap[sKey] = value;

        return previous;
    }-*/;

    // only count keys with ':' prefix
    public native int jsSize() /*-{
        var value = this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::keyMap;
        var count = 0;
        for(var key in value) {
            if (key.charCodeAt(0) == 58) ++count;
        }
        return count;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native V jsRemove(String sKey) /*-{
        sKey = ':' + sKey;

        var keyMap = this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::keyMap;
        var valueMap = this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::valueMap;

        var previous = valueMap[sKey];
        delete valueMap[sKey];
        delete keyMap[sKey];

        return previous;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native boolean jsContainsKey(String key, JavaScriptObject keyMap) /*-{
        return (':' + key) in keyMap;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native boolean jsContainsValue(Object value, JavaScriptObject valueMap) /*-{
        for (var sKey in valueMap) {
            if (sKey.charCodeAt(0) == 58) {
                var entryValue = valueMap[sKey];
                if (this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(value, entryValue)) {
                    return true;
                }
            }
        }
        return false;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native K jsSingleKey(JavaScriptObject valueMap) /*-{
        for (var sKey in valueMap) {
            if (sKey.charCodeAt(0) == 58)
                return sKey;
        }
        return null;
    }-*/;

    // only iterate keys with ':' prefix
    private native void jsForeach(Function f, JavaScriptObject map) /*-{
        for (var sKey in map) {
            if (sKey.charCodeAt(0) == 58) {
                this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::bridgeApply(Llsfusion/gwt/client/base/jsni/Function;Ljava/lang/Object;)(f, map[sKey]);
            }
        }
    }-*/;

    // only iterate keys with ':' prefix
    private native void jsForeachEntry(Function2 f) /*-{
        var keyMap = this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::keyMap;
        var valueMap = this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::valueMap;
        for (var sKey in keyMap) {
            if (sKey.charCodeAt(0) == 58) {
                this.@lsfusion.gwt.client.base.jsni.NativeSIDMap::bridgeApply2(Llsfusion/gwt/client/base/jsni/Function2;Ljava/lang/Object;Ljava/lang/Object;)
                        (f, keyMap[sKey], valueMap[sKey]);
            }
        }
    }-*/;
}
