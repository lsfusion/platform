package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class NativeStringMap<V> {

    private JavaScriptObject nativeStringMap;

    public NativeStringMap() {
        init();
    }

    public void clear() {
        init();
    }

    private void init() {
        nativeStringMap = createMap();
    }

    public native JavaScriptObject createMap() /*-{
        return new Map();
    }-*/;

    public boolean containsKey(String key) {
        return jsContainsKey(key);
    }

    public boolean containsValue(final Object value) {
        return jsContainsValue(value);
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
            ts.push(key);
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
        jsForeachValue(f);
    }

    public void foreachEntry(Function2<String, V> f) {
        jsForeachEntry(f);
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

    public native V jsGet(String key) /*-{
        return this.@NativeStringMap::nativeStringMap.get(key);
    }-*/;

    private native V jsPut(String sKey, V value) /*-{
        var nativeStringMap = this.@NativeStringMap::nativeStringMap;
        var previous = nativeStringMap.get(sKey);
        nativeStringMap.set(sKey, value);
        return previous;
    }-*/;

    public native int jsSize() /*-{
        return this.@NativeStringMap::nativeStringMap.size;
    }-*/;

    private native V jsRemove(String sKey) /*-{
        var nativeStringMap = this.@NativeStringMap::nativeStringMap;
        var previous = nativeStringMap.get(sKey);
        nativeStringMap['delete'](sKey);
        return previous;
    }-*/;

    private native boolean jsContainsKey(String key) /*-{
        return this.@NativeStringMap::nativeStringMap.has(key);
    }-*/;

    private native boolean jsContainsValue(Object value) /*-{
        var thisObj = this;
        thisObj.@NativeStringMap::nativeStringMap.forEach(function (mapValue) {
            if (thisObj.@NativeStringMap::equalsBridge(*)(mapValue, value))
                return true;
        });
        return false;
    }-*/;

    private native void jsForeachKey(Function f) /*-{
        var thisObj = this;
        thisObj.@NativeStringMap::nativeStringMap.forEach(function (value, mapKey) {
            thisObj.@NativeStringMap::bridgeApply(*)(f, mapKey);
        });
    }-*/;

    private native void jsForeachValue(Function f) /*-{
        var thisObj = this;
        thisObj.@NativeStringMap::nativeStringMap.forEach(function (value) {
            thisObj.@NativeStringMap::bridgeApply(*)(f, value);
        });
    }-*/;

    private native void jsForeachEntry(Function2 f) /*-{
        var thisObj = this;
        thisObj.@NativeStringMap::nativeStringMap.forEach(function (value, key) {
            thisObj.@NativeStringMap::bridgeApply2(*)(f, key, value);
        });
    }-*/;
}
