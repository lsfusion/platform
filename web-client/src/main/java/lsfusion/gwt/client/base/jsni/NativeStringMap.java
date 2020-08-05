package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class NativeStringMap<V> {

    private JavaScriptObject map;

    public NativeStringMap() {
        map = JavaScriptObject.createObject();
    }

    public void clear() {
        map = JavaScriptObject.createObject();
    }

    public boolean containsKey(String key) {
        return jsContainsKey(key, map);
    }

    public V get(String key) {
        return jsGet(key);
    }

    public V put(String key, V value) {
        return jsPut(key, value);
    }

    public void putAll(NativeStringMap<? extends V> m) {
        m.foreachEntry(this::put);
    }

    public V remove(String key) {
        return jsRemove(key);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return jsSize();
    }

    public String toString() {
        final JsArrayString ts = JsArray.createArray().cast();
        ts.push("{");
        foreachEntry((key, value) -> {
            ts.push(",");
            ts.push(key == null ? null : key);
            ts.push("=");
            ts.push(value == null ? null : value.toString());
        });
        ts.push("}");
        return ts.join();
    }

    public void foreachKey(Function<String> f) {
        jsForeachKey(f);
    }

    public void foreachValue(Function<V> f) {
        foreach(map, f);
    }

    public void foreachEntry(Function2<String, V> f) {
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
        return this.@lsfusion.gwt.client.base.jsni.NativeStringMap::map[':' + key];
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native V jsPut(String sKey, V value) /*-{
        sKey = ':' + sKey;

        var map = this.@lsfusion.gwt.client.base.jsni.NativeStringMap::map;

        var previous = map[sKey];
        map[sKey] = value;
        return previous;
    }-*/;

    // only count keys with ':' prefix
    public native int jsSize() /*-{
        var map = this.@lsfusion.gwt.client.base.jsni.NativeStringMap::map;
        var count = 0;
        for(var key in map) {
            if (sKey.charCodeAt(0) == 58) ++count;
        }
        return count;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native V jsRemove(String sKey) /*-{
        sKey = ':' + sKey;

        var map = this.@lsfusion.gwt.client.base.jsni.NativeStringMap::map;

        var previous = map[sKey];
        delete map[sKey];

        return previous;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native boolean jsContainsKey(String key, JavaScriptObject map) /*-{
        return (':' + key) in map;
    }-*/;

    // Prepend ':' to avoid conflicts with built-in Object properties.
    private native boolean jsContainsValue(Object value, JavaScriptObject map) /*-{
        for (var sKey in map) {
            if (sKey.charCodeAt(0) == 58) {
                var entryValue = map[sKey];
                if (this.@lsfusion.gwt.client.base.jsni.NativeStringMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(value, entryValue)) {
                    return true;
                }
            }
        }
        return false;
    }-*/;

    // only iterate keys with ':' prefix
    private native void jsForeachKey(Function f) /*-{
        var map = this.@lsfusion.gwt.client.base.jsni.NativeStringMap::map;
        for (var sKey in map) {
            if (sKey.charCodeAt(0) == 58) {
                this.@lsfusion.gwt.client.base.jsni.NativeStringMap::bridgeApply(Llsfusion/gwt/client/base/jsni/Function;Ljava/lang/Object;)(f, sKey.substr(1));
            }
        }
    }-*/;

    // only iterate keys with ':' prefix
    private native void jsForeach(Function f, JavaScriptObject map) /*-{
        for (var sKey in map) {
            if (sKey.charCodeAt(0) == 58) {
                this.@lsfusion.gwt.client.base.jsni.NativeStringMap::bridgeApply(Llsfusion/gwt/client/base/jsni/Function;Ljava/lang/Object;)(f, map[sKey]);
            }
        }
    }-*/;

    // only iterate keys with ':' prefix
    private native void jsForeachEntry(Function2 f) /*-{
        var map = this.@lsfusion.gwt.client.base.jsni.NativeStringMap::map;
        for (var sKey in map) {
            if (sKey.charCodeAt(0) == 58) {
                this.@lsfusion.gwt.client.base.jsni.NativeStringMap::bridgeApply2(Llsfusion/gwt/client/base/jsni/Function2;Ljava/lang/Object;Ljava/lang/Object;)
                (f, sKey.substr(1), map[sKey]);
            }
        }
    }-*/;
}
