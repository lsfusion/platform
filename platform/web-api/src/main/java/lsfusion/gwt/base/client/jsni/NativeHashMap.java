package lsfusion.gwt.base.client.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * modified com/google/gwt/emul/java/util/AbstractHashMap.java
 */
public class NativeHashMap<K, V> implements Map<K, V> {

    @SuppressWarnings("UnusedDeclaration")
    private JavaScriptObject hasCodeMap;
    private int size = 0;

    public NativeHashMap() {
        init();
    }

    private void init() {
        size = 0;
        hasCodeMap = JavaScriptObject.createObject();
    }

    @Override
    public void clear() {
        init();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("NativeMap.entrySet isn't supported");
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("NativeMap.keySet isn't supported");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("NativeMap.values isn't supported");
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            throw new NullPointerException("null keys aren't allowed");
        }
        return jsGet(key, getHashCode(key));
    }

    public V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("null keys aren't allowed");
        }
        return jsPut(key, value, getHashCode(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        return jsRemove(key, getHashCode(key));
    }

    @Override
    public boolean containsKey(Object key) {
        return jsContainsKey(key, getHashCode(key));
    }

    @Override
    public boolean containsValue(final Object value) {
        return jsContainsValue(value);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
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

    private int getHashCode(Object key) {
        return key.hashCode();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void foreachKey(Function<K> f) {
        jsForeachKey(f);
    }

    @SuppressWarnings("UnusedDeclaration")
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

    public native V jsGet(Object key, int hashCode) /*-{
        var array = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap[hashCode];
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                var entry = array[i];
                if (this.@lsfusion.gwt.base.client.jsni.NativeHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, entry[0])) {
                    return entry[1];
                }
            }
        }
        return null;
    }-*/;

    private native V jsPut(K key, V value, int hashCode) /*-{
        var array = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap[hashCode];
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                var entry = array[i];
                if (this.@lsfusion.gwt.base.client.jsni.NativeHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, entry[0])) {
                    // Found an exact match, just update the existing entry
                    var previous = entry[1];
                    entry[1] = value;
                    return previous;
                }
            }
        } else {
            array = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap[hashCode] = []
        }

        array.push([key, value]);
        ++this.@lsfusion.gwt.base.client.jsni.NativeHashMap::size;
        return null;
    }-*/;

    private native V jsRemove(Object key, int hashCode) /*-{
        var array = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap[hashCode];
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                var entry = array[i];
                if (this.@lsfusion.gwt.base.client.jsni.NativeHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, entry[0])) {
                    if (array.length == 1) {
                        // remove the whole array
                        delete this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap[hashCode];
                    } else {
                        array.splice(i, 1)
                    }
                    --this.@lsfusion.gwt.base.client.jsni.NativeHashMap::size;
                    return entry[1]
                }
            }
        }
        return null;
    }-*/;

    private native boolean jsContainsKey(Object key, int hashCode) /*-{
        var array = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap[hashCode];
        if (array) {
            for (var i = 0, c = array.length; i < c; ++i) {
                if (this.@lsfusion.gwt.base.client.jsni.NativeHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, array[i][0])) {
                    return true;
                }
            }
        }
        return false;
    }-*/;

    private native boolean jsContainsValue(Object value) /*-{
        var hashCodeMap = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap;
        for (var hashCode in hashCodeMap) {
            // sanity check that it's really one of ours
            var hashCodeInt = parseInt(hashCode, 10);
            if (hashCode == hashCodeInt) {
                var array = hashCodeMap[hashCodeInt];
                for (var i = 0, c = array.length; i < c; ++i) {
                    if (this.@lsfusion.gwt.base.client.jsni.NativeHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(value, array[i][1])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }-*/;

    private native void jsForeachKey(Function f) /*-{
        var hashCodeMap = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap;
        for (var hashCode in hashCodeMap) {
            // sanity check that it's really one of ours
            var hashCodeInt = parseInt(hashCode, 10);
            if (hashCode == hashCodeInt) {
                var array = hashCodeMap[hashCodeInt];
                for (var i = 0, c = array.length; i < c; ++i) {
                    this.@lsfusion.gwt.base.client.jsni.NativeHashMap::bridgeApply(Llsfusion/gwt/base/client/jsni/Function;Ljava/lang/Object;)(f, array[i][0]);
                }
            }
        }
    }-*/;

    private native void jsForeachValue(Function f) /*-{
        var hashCodeMap = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap;
        for (var hashCode in hashCodeMap) {
            // sanity check that it's really one of ours
            var hashCodeInt = parseInt(hashCode, 10);
            if (hashCode == hashCodeInt) {
                var array = hashCodeMap[hashCodeInt];
                for (var i = 0, c = array.length; i < c; ++i) {
                    this.@lsfusion.gwt.base.client.jsni.NativeHashMap::bridgeApply(Llsfusion/gwt/base/client/jsni/Function;Ljava/lang/Object;)(f, array[i][1]);
                }
            }
        }
    }-*/;

    private native void jsForeachEntry(Function2 f) /*-{
        var hashCodeMap = this.@lsfusion.gwt.base.client.jsni.NativeHashMap::hasCodeMap;
        for (var hashCode in hashCodeMap) {
            // sanity check that it's really one of ours
            var hashCodeInt = parseInt(hashCode, 10);
            if (hashCode == hashCodeInt) {
                var array = hashCodeMap[hashCodeInt];
                for (var i = 0, c = array.length; i < c; ++i) {
                    this.@lsfusion.gwt.base.client.jsni.NativeHashMap::bridgeApply2(Llsfusion/gwt/base/client/jsni/Function2;Ljava/lang/Object;Ljava/lang/Object;)
                            (f, array[i][0], array[i][1]);
                }
            }
        }
    }-*/;
}
