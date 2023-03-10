package lsfusion.server.base.version;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.version.impl.*;
import lsfusion.server.base.version.interfaces.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NFFact {
    
    public static <K> NFComplexOrderSet<K> complexOrderSet() {
        return new NFComplexOrderSetImpl<>();
    }
    public static <K> NFComplexOrderSet<K> complexOrderSet(boolean allowVersionFinalRead) {
        return new NFComplexOrderSetImpl<>(allowVersionFinalRead);
    }

    public static <K> NFOrderSet<K> orderSet() {
        return new NFOrderSetImpl<>();
    }

    public static <K> NFOrderSet<K> orderSet(boolean allowVersionFinalRead) {
        return new NFOrderSetImpl<>(allowVersionFinalRead);
    }

    public static <K> NFOrderSet<K> simpleOrderSet(ImOrderSet<K> orderSet) {
        return new NFSimpleOrderSetImpl<>(orderSet);
    }

    public static <K, V> Map<K, V> concurrentMap(Map<K, V> map) {
        return new ConcurrentHashMap<>(map);
    }

    public static <K> NFList<K> list() {
        return new NFListImpl<>();
    }
    
    public static <K> NFList<K> finalList(ImList<K> list) {
        return new NFListImpl<>(list);
    }

    public static <K> NFList<K> finalList(List<K> list) {
        return finalList(ListFact.fromJavaList(list));
    }

    public static <K> NFSet<K> finalSet(ImSet<K> set) {
        return new NFSetImpl<>(set); 
    }
    
    public static <K> NFSet<K> finalSet(Set<K> set) {
        return finalSet(SetFact.fromJavaSet(set));
    }

    public static <K> NFSet<K> set() {
        return new NFSetImpl<>();
    }

    public static <K, V> NFMap<K, V> map() {
        return new NFMapImpl<>();
    }

    public static <K, V> NFOrderMap<K, V> orderMap() {
        return new NFOrderMapImpl<>();
    }

    public static <K, V> NFOrderMap<K, V> finalOrderMap(ImOrderMap<K, V> map) {
        return new NFOrderMapImpl<>(map);
    }

    public static <K, V> NFMapList<K, V> mapList() {
        return new NFMapListImpl<>();
    }

    public static <K, V> NFMapList<K, V> finalMapList(ImMap<K, ImList<V>> map) {
        return new NFMapListImpl<>(map);
    }

    public static <K> NFProperty<K> property() {
        return new NFPropertyImpl<>();
    }

    public static <K> NFProperty<K> property(boolean allowVersionFinalRead) {
        return property(allowVersionFinalRead, null);
    }
    public static <K> NFProperty<K> property(boolean allowVersionFinalRead, Object debugInfo) {
        return new NFPropertyImpl<>(allowVersionFinalRead, debugInfo);
    }

    public static <K> NFProperty<K> finalProperty(K key) {
        return new NFPropertyImpl<>(key);                
    }
}
