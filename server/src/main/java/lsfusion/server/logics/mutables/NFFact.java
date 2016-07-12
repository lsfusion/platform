package lsfusion.server.logics.mutables;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.logics.mutables.impl.*;
import lsfusion.server.logics.mutables.interfaces.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NFFact {
    
    public static <K> NFOrderSet<K> orderSet() {
        return new NFOrderSetImpl<K>();
    }

    public static <K> NFOrderSet<K> orderSet(boolean allowVersionFinalRead) {
        return new NFOrderSetImpl<K>(allowVersionFinalRead);
    }

    public static <K> NFOrderSet<K> simpleOrderSet(ImOrderSet<K> orderSet) {
        return new NFSimpleOrderSetImpl<K>(orderSet);
    }

    public static <K> NFOrderSet<K> simpleOrderSet() {
        return simpleOrderSet(SetFact.<K>EMPTYORDER());
    }
    
    public static <K, V> Map<K, V> simpleMap(Map<K, V> map) {
        return new ConcurrentHashMap<K, V>(map);
    }

    public static <K> NFList<K> list() {
        return new NFListImpl<K>();
    }
    
    public static <K> NFList<K> finalList(ImList<K> list) {
        return new NFListImpl<K>(list);
    }

    public static <K> NFList<K> finalList(List<K> list) {
        return finalList(ListFact.fromJavaList(list));
    }

    public static <K> NFOrderSet<K> finalOrderSet(ImOrderSet<K> list) {
        return new NFOrderSetImpl<K>(list);
    }

    public static <K> NFOrderSet<K> finalOrderSet(List<K> list) {
        return finalOrderSet(SetFact.fromJavaOrderSet(list));
    }

    public static <K> NFSet<K> finalSet(ImSet<K> set) {
        return new NFSetImpl<K>(set); 
    }
    
    public static <K> NFSet<K> finalSet(Set<K> set) {
        return finalSet(SetFact.fromJavaSet(set));
    }

    public static <K> NFSet<K> set() {
        return new NFSetImpl<K>();
    }
    
    public static <K, V> NFOrderMap<K, V> orderMap() {
        return new NFOrderMapImpl<K, V>();
    }

    public static <K, V> NFOrderMap<K, V> finalOrderMap(ImOrderMap<K, V> map) {
        return new NFOrderMapImpl<K, V>(map);
    }

    public static <K, V> NFMapList<K, V> mapList() {
        return new NFMapListImpl<K, V>();
    }

    public static <K, V> NFMapList<K, V> finalMapList(ImMap<K, ImList<V>> map) {
        return new NFMapListImpl<K, V>(map);
    }

    public static <K> NFProperty<K> property() {
        return new NFPropertyImpl<K>();
    }

    public static <K> NFProperty<K> property(boolean allowVersionFinalRead) {
        return property(allowVersionFinalRead, null);
    }
    public static <K> NFProperty<K> property(boolean allowVersionFinalRead, Object debugInfo) {
        return new NFPropertyImpl<K>(allowVersionFinalRead, debugInfo);
    }

    public static <K> NFProperty<K> finalProperty(K key) {
        return new NFPropertyImpl<K>(key);                
    }
}
