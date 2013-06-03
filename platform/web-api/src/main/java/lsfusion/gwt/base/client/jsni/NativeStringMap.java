package lsfusion.gwt.base.client.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class NativeStringMap<K, V> implements Map<K, V> {
    public static interface NativeMapKeyConverter<T> {
        String getKeyString(T key);
    }

    private final NativeMapKeyConverter<K> keyConverter;

    private int size = 0;
    private JavaScriptObject keyMap;
    private JavaScriptObject valueMap;

    public NativeStringMap(NativeMapKeyConverter<K> keyConverter) {
        this.keyConverter = keyConverter;
        init();
    }

    @Override
    public void clear() {
        init();
    }

    private void init() {
        size = 0;
        keyMap = JavaScriptObject.createObject();
        valueMap = JavaScriptObject.createObject();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("NativeStringMap.entrySet isn't supported");
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("NativeStringMap.keySet isn't supported");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("NativeStringMap.values isn't supported");
    }

    @Override
    public boolean containsKey(Object key) {
        return jsContainsKey(convertKey((K) key), keyMap);
    }

    @Override
    public boolean containsValue(final Object val) {
        return jsContainsValue(val, valueMap);
    }

    @Override
    public V get(Object key) {
        return jsGet(convertKey((K) key));
    }

    public V put(K key, V value) {
        String sKey = convertKey(key);
        return jsPut(sKey, key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        return jsRemove(convertKey((K) key));
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return jsSize();
    }

    public String toString() {
        final JsArrayString ts = JsArray.createArray().cast();
        ts.push("{");
        foreachEntry(new Function2<K, V>() {
            @Override
            public void apply(K key, V value) {
                ts.push(",");
                ts.push(key == null ? null : key.toString());
                ts.push("=");
                ts.push(value == null ? null : value.toString());
            }
        });
        ts.push("}");
        return ts.join();
    }

    private String convertKey(K key) {
        return keyConverter.getKeyString(key);
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
        return this.@lsfusion.gwt.base.client.jsni.NativeStringMap::valueMap[':' + key];
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native V jsPut(String sKey, K key, V value) /*-{
        sKey = ':' + sKey;

        var keyMap = this.@lsfusion.gwt.base.client.jsni.NativeStringMap::keyMap;
        var valueMap = this.@lsfusion.gwt.base.client.jsni.NativeStringMap::valueMap;

        var previous = valueMap[sKey];

        keyMap[sKey] = key;
        valueMap[sKey] = value;

        return previous;
    }-*/;

    // only count keys with ':' prefix
    public native int jsSize() /*-{
        var value = this.@lsfusion.gwt.base.client.jsni.NativeStringMap::keyMap;
        var count = 0;
        for(var key in value) {
            if (sKey.charCodeAt(0) == 58) ++count;
        }
        return count;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native V jsRemove(String sKey) /*-{
        sKey = ':' + sKey;

        var keyMap = this.@lsfusion.gwt.base.client.jsni.NativeStringMap::keyMap;
        var valueMap = this.@lsfusion.gwt.base.client.jsni.NativeStringMap::valueMap;

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
                if (this.@lsfusion.gwt.base.client.jsni.NativeStringMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(value, entryValue)) {
                    return true;
                }
            }
        }
        return false;
    }-*/;

    // only iterate keys with ':' prefix
    private native void jsForeach(Function f, JavaScriptObject map) /*-{
        for (var sKey in map) {
            if (sKey.charCodeAt(0) == 58) {
                this.@lsfusion.gwt.base.client.jsni.NativeStringMap::bridgeApply(Llsfusion/gwt/base/client/jsni/Function;Ljava/lang/Object;)(f, map[sKey]);
            }
        }
    }-*/;

    // only iterate keys with ':' prefix
    private native void jsForeachEntry(Function2 f) /*-{
        var keyMap = this.@lsfusion.gwt.base.client.jsni.NativeStringMap::keyMap;
        var valueMap = this.@lsfusion.gwt.base.client.jsni.NativeStringMap::valueMap;
        for (var sKey in keyMap) {
            if (sKey.charCodeAt(0) == 58) {
                this.@lsfusion.gwt.base.client.jsni.NativeStringMap::bridgeApply2(Llsfusion/gwt/base/client/jsni/Function2;Ljava/lang/Object;Ljava/lang/Object;)
                        (f, keyMap[sKey], valueMap[sKey]);
            }
        }
    }-*/;
}
