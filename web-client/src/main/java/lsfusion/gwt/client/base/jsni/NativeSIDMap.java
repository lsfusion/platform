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
        return jsContainsKey(key.getNativeSID());
    }

    public boolean containsValue(final Object val) {
        return jsContainsValue(val);
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
        return jsSingleKey();
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
        jsForeachKey(f);
    }

    public void foreachValue(Function<V> f) {
        jsForeachValue(f);
    }

    public void foreachEntry(Function2<K, V> f) {
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

    public native V jsGet(String sKey) /*-{
        var array = this.@NativeSIDMap::nativeSIDMap.get(sKey);
        return array ? array[1] : null;
    }-*/;

    private native V jsPut(String sKey, K key, V value) /*-{
        var nativeSIDMap = this.@NativeSIDMap::nativeSIDMap;
        var array = nativeSIDMap.get(sKey);
        var previous = null;

        if (array)
            previous = array[1];

        nativeSIDMap.set(sKey, [key, value]);

        return previous;
    }-*/;

    public native int jsSize() /*-{
        return this.@NativeSIDMap::nativeSIDMap.size;
    }-*/;

    private native V jsRemove(String sKey) /*-{
        var nativeSIDMap = this.@NativeSIDMap::nativeSIDMap;
        var array = nativeSIDMap.get(sKey);
        var previous = null;

        if (array)
            previous = array[1];

        jsMapDelete(sKey);

        return previous;
    }-*/;

    private native boolean jsContainsKey(String sKey) /*-{
        return this.@NativeSIDMap::nativeSIDMap.has(sKey);
    }-*/;

    private native boolean jsContainsValue(Object value) /*-{
        var thisObj = this;
        thisObj.@NativeSIDMap::nativeSIDMap.forEach(function (array) {
            if (thisObj.@NativeSIDMap::equalsBridge(*)(value, array[1]))
                return true;
        });
        return false;
    }-*/;

    private native K jsSingleKey() /*-{
        return this.@NativeSIDMap::nativeSIDMap.keys().next().value;
    }-*/;

    private native void jsForeachKey(Function f) /*-{
        var thisObj = this;
        thisObj.@NativeSIDMap::nativeSIDMap.forEach(function (array) {
            thisObj.@NativeSIDMap::bridgeApply(*)(f, array[0]);
        });
    }-*/;

    private native void jsForeachValue(Function f) /*-{
        var thisObj = this;
        thisObj.@NativeSIDMap::nativeSIDMap.forEach(function (array) {
            thisObj.@NativeSIDMap::bridgeApply(*)(f, array[1]);
        });
    }-*/;

    private native void jsForeachEntry(Function2 f) /*-{
        var thisObj = this;
        thisObj.@NativeSIDMap::nativeSIDMap.forEach(function (array) {
            thisObj.@NativeSIDMap::bridgeApply2(*)(f, array[0], array[1]);
        });
    }-*/;
}
