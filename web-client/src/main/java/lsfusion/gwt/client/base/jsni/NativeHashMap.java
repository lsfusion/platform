package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import lsfusion.gwt.client.base.Result;

public class NativeHashMap<K, V> {

    private JavaScriptObject hashCodeMap;

    public NativeHashMap() {
    }

    public void clear() {
        hashCodeMap = null;
    }

    public V firstValue() {
        Result<V> resultValue = new Result<>();
        foreachValue(result -> {
            if(resultValue.result == null)
                resultValue.set(result);
        });
        return resultValue.result;
    }
    public K firstKey() {
        Result<K> resultValue = new Result<>();
        foreachKey(result -> {
            if(resultValue.result == null)
                resultValue.set(result);
        });
        return resultValue.result;
    }
    public void putAll(NativeHashMap<? extends K, ? extends V> map) {
        map.foreachEntry(this::put);
    }

    public V get(K key) {
        if(hashCodeMap == null)
            return null;
        return jsGet(key, key.hashCode());
    }

    private boolean containsAll(NativeHashMap<K, V> map) {
        Result<Boolean> result = new Result<>();
        map.foreachEntry((key, value) -> {
            if(!equalsBridge(get(key), value))
                result.set(true);
        });
        return result.result == null;
    }

    // very rarely used so that not very efficient implementation
    @Override
    public int hashCode() {
        Result<Integer> result = new Result<>(0);
        foreachEntry((key, value) -> {
            result.set(result.result + (key != null ? key.hashCode() : 0) ^ (value != null ? value.hashCode() : 0));
        });
        return result.result;
    }

    @Override
    public boolean equals(Object obj) {
        NativeHashMap<K, V> map = (NativeHashMap<K, V>) obj;
        if(hashCodeMap == null || map.hashCodeMap == null)
            return hashCodeMap == null && map.hashCodeMap == null;

        return containsAll(map) && map.containsAll(this);
    }

    public V put(K key, V value) {
        if(hashCodeMap == null)
            hashCodeMap = createMap();
        return jsPut(key, value, key.hashCode());
    }

    public native JavaScriptObject createMap() /*-{
        return new Map();
    }-*/;

    public V remove(Object key) {
        if(hashCodeMap == null)
            return null;

        V removed = jsRemove(key, key.hashCode());

        if(jsIsEmpty(hashCodeMap))
            hashCodeMap = null;
        return removed;
    }

    public boolean containsKey(Object key) {
        return hashCodeMap != null && jsContainsKey(key, key.hashCode());
    }

    public boolean containsValue(final Object value) {
        return hashCodeMap != null && jsContainsValue(value);
    }

    public boolean isEmpty() {
        return hashCodeMap == null;
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

    public void foreachKey(Function<K> f) {
        if(hashCodeMap != null)
            jsForeachKey(f);
    }

    public void foreachValue(Function<V> f) {
        if(hashCodeMap != null)
            jsForeachValue(f);
    }

    public void foreachEntry(Function2<K, V> f) {
        if(hashCodeMap != null)
            jsForeachEntry(f);
    }

    public int size() {
        return jsSize();
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

    public native V jsGet(Object key, int hashCode) /*-{
        var array = this.@NativeHashMap::hashCodeMap.get(hashCode);
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                var entry = array[i];
                if (this.@NativeHashMap::equalsBridge(*)(key, entry[0]))
                    return entry[1];
            }
        }
        return null;
    }-*/;

    private native V jsPut(K key, V value, int hashCode) /*-{
        var array = this.@NativeHashMap::hashCodeMap.get(hashCode);
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                var entry = array[i];
                if (this.@NativeHashMap::equalsBridge(*)(key, entry[0])) {
                    // Found an exact match, just update the existing entry
                    var previous = entry[1];
                    entry[1] = value;
                    return previous;
                }
            }
        } else {
            array = this.@NativeHashMap::hashCodeMap.set(hashCode, []).get(hashCode);
        }

        array.push([key, value]);
        return null;
    }-*/;

    private native V jsRemove(Object key, int hashCode) /*-{
        var array = this.@NativeHashMap::hashCodeMap.get(hashCode);
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                var entry = array[i];
                if (this.@NativeHashMap::equalsBridge(*)(key, entry[0])) {
                    if (array.length == 1) {
                        // remove the whole array
                        this.@NativeHashMap::hashCodeMap['delete'](hashCode);
                    } else {
                        array.splice(i, 1)
                    }
                    return entry[1]
                }
            }
        }
        return null;
    }-*/;

    private native boolean jsContainsKey(Object key, int hashCode) /*-{
        var array = this.@NativeHashMap::hashCodeMap.get(hashCode);
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                if (this.@NativeHashMap::equalsBridge(*)(key, array[i][0]))
                    return true;
            }
        }
        return false;
    }-*/;

    private native boolean jsContainsValue(Object value) /*-{
        var thisObj = this;
        thisObj.@NativeHashMap::hashCodeMap.forEach(function (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                if (thisObj.@NativeHashMap::equalsBridge(*)(value, array[i][1]))
                    return true;
            }
        });
        return false;
    }-*/;

    private native boolean jsIsEmpty(JavaScriptObject hashCodeMap) /*-{
        return hashCodeMap.size === 0;
    }-*/;

    private native void jsForeachKey(Function f) /*-{
        var thisObj = this;
        thisObj.@NativeHashMap::hashCodeMap.forEach(function (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                thisObj.@NativeHashMap::bridgeApply(*)(f, array[i][0]);
            }
        });
    }-*/;

    private native void jsForeachValue(Function f) /*-{
        var thisObj = this;
        thisObj.@NativeHashMap::hashCodeMap.forEach(function (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                thisObj.@NativeHashMap::bridgeApply(*)(f, array[i][1]);
            }
        });
    }-*/;

    private native void jsForeachEntry(Function2 f) /*-{
        var thisObj = this;
        thisObj.@NativeHashMap::hashCodeMap.forEach(function (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                thisObj.@NativeHashMap::bridgeApply2(*)(f, array[i][0], array[i][1]);
            }
        });
    }-*/;

    private native int jsSize() /*-{
        return this.@NativeHashMap::hashCodeMap.size;
    }-*/;
}
