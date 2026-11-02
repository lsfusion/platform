package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;

// a map by the native SID of a key, as NativeSIDMap, whose keys go in the order they were last put in - a key put
// again goes last -, as its contract; NativeSIDMap promises no order
public class NativeLinkedSIDMap<K extends HasNativeSID, V> {

    private final JavaScriptObject nativeLinkedSIDMap = createMap();

    private static native JavaScriptObject createMap() /*-{
        return new Map();
    }-*/;

    public boolean containsKey(K key) {
        return jsContainsKey(key.getNativeSID());
    }

    public V get(K key) {
        return jsGet(key.getNativeSID());
    }

    // ... put last: a JS Map keeps a key it has in its place, so the key is taken out first
    public void put(K key, V value) {
        remove(key);
        jsSet(key.getNativeSID(), key, value);
    }

    public void remove(K key) {
        jsRemove(key.getNativeSID());
    }

    // ... in that order, over the map as it is: a key removed meanwhile - the one given too - is not gone over, a key
    // put again is gone over again
    public void foreachKey(Function<K> f) {
        jsForeachKey(f);
    }

    private void bridgeApply(Function f, Object obj) {
        f.apply(obj);
    }

    private native boolean jsContainsKey(String sKey) /*-{
        return this.@NativeLinkedSIDMap::nativeLinkedSIDMap.has(sKey);
    }-*/;

    private native V jsGet(String sKey) /*-{
        var array = this.@NativeLinkedSIDMap::nativeLinkedSIDMap.get(sKey);
        return array ? array[1] : null;
    }-*/;

    private native void jsSet(String sKey, K key, V value) /*-{
        this.@NativeLinkedSIDMap::nativeLinkedSIDMap.set(sKey, [key, value]);
    }-*/;

    private native void jsRemove(String sKey) /*-{
        $wnd.jsMapDelete(this.@NativeLinkedSIDMap::nativeLinkedSIDMap, sKey);
    }-*/;

    private native void jsForeachKey(Function f) /*-{
        var thisObj = this;
        thisObj.@NativeLinkedSIDMap::nativeLinkedSIDMap.forEach(function (array) {
            thisObj.@NativeLinkedSIDMap::bridgeApply(*)(f, array[0]);
        });
    }-*/;
}
