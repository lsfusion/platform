package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class NativeStringMap<V> {

    private JavaScriptObject map;

    public NativeStringMap() {
        map = createMap();
    }

    public void clear() {
        map = createMap();
    }

    public native JavaScriptObject createMap() /*-{
        return new Map();
    }-*/;

    public boolean containsKey(String key) {
        return jsContainsKey(key, map);
    }

    public boolean containsValue(final Object value) {
        return jsContainsValue(value, map);
    }

    public V get(String key) {
        return jsGet(key, map);
    }

    public V put(String key, V value) {
        return jsPut(key, value, map);
    }

    public void putAll(NativeStringMap<? extends V> m) {
        m.foreachEntry(this::put);
    }

    public V remove(String key) {
        return jsRemove(key, map);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return jsSize(map);
    }

    public String toString() {
        final JsArrayString ts = JsArray.createArray().cast();
        ts.push("{");
        foreachEntry((key, value) -> {
            ts.push(",");
            ts.push(key);
            ts.push("=");
            ts.push(value == null ? null : value.toString());
        });
        ts.push("}");
        return ts.join();
    }

    public void foreachKey(Function<String> f) {
        jsForeachKey(f, map);
    }

    public void foreachValue(Function<V> f) {
        jsForeachValue(f, map);
    }

    public void foreachEntry(Function2<String, V> f) {
        jsForeachEntry(f, map);
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

    public native V jsGet(String key, JavaScriptObject map) /*-{
        return map.get(key);
    }-*/;

    private native V jsPut(String sKey, V value, JavaScriptObject map) /*-{
        var previous = map.get(sKey);
        map.set(sKey, value);
        return previous;
    }-*/;

    public native int jsSize(JavaScriptObject map) /*-{
        return map.size;
    }-*/;

    private native V jsRemove(String sKey, JavaScriptObject map) /*-{
        var previous = map.get(sKey);
        map['delete'](sKey);
        return previous;
    }-*/;

    private native boolean jsContainsKey(String key, JavaScriptObject map) /*-{
        var thisObj = this;
        map.keys().forEach(function (mapKey) {
            if (thisObj.@lsfusion.gwt.client.base.jsni.NativeStringMap::equalsBridge(*)(mapKey, key))
                return true;
        });
        return false;
    }-*/;

    private native boolean jsContainsValue(Object value, JavaScriptObject map) /*-{
        var thisObj = this;
        map.values().forEach(function (mapValue) {
            if (thisObj.@lsfusion.gwt.client.base.jsni.NativeStringMap::equalsBridge(*)(mapValue, value))
                return true;
        });
        return false;
    }-*/;

    private native void jsForeachKey(Function f, JavaScriptObject map) /*-{
        var thisObj = this;
        map.keys().forEach(function (mapKey) {
            thisObj.@lsfusion.gwt.client.base.jsni.NativeStringMap::bridgeApply(*)(f, mapKey);
        });
    }-*/;

    private native void jsForeachValue(Function f, JavaScriptObject map) /*-{
        var thisObj = this;
        map.values().forEach(function (value) {
            thisObj.@lsfusion.gwt.client.base.jsni.NativeStringMap::bridgeApply(*)(f, value);
        });
    }-*/;

    private native void jsForeachEntry(Function2 f, JavaScriptObject map) /*-{
        var thisObj = this;
        map.forEach(function (value, key) {
            thisObj.@lsfusion.gwt.client.base.jsni.NativeStringMap::bridgeApply2(*)(f, key, value);
        });
    }-*/;
}
