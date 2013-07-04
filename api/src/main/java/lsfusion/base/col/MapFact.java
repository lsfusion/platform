package lsfusion.base.col;

import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.Result;
import lsfusion.base.col.implementations.ArIndexedMap;
import lsfusion.base.col.implementations.ArMap;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.implementations.order.ArOrderMap;
import lsfusion.base.col.implementations.order.HOrderMap;
import lsfusion.base.col.implementations.simple.EmptyOrderMap;
import lsfusion.base.col.implementations.simple.EmptyRevMap;
import lsfusion.base.col.implementations.simple.SingletonOrderMap;
import lsfusion.base.col.implementations.simple.SingletonRevMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapFact {
    private final static AddValue<Object, Integer> addLinear = new SymmAddValue<Object, Integer>() {
        public Integer addValue(Object key, Integer prevValue, Integer newValue) {
            return prevValue + newValue;
        }
    };

    // IMMUTABLE

    public static <K, V> ImMap<K, V> EMPTY(){
        return EmptyRevMap.INSTANCE();
    }

    public static <K, V> ImOrderMap<K, V> EMPTYORDER(){
        return EmptyOrderMap.INSTANCE();
    }

    public static <K, V> ImRevMap<K, V> EMPTYREV(){
        return EmptyRevMap.INSTANCE();
    }

    public static <K, V> ImMap<K, V> singleton(K key, V value) {
        return new SingletonRevMap<K, V>(key, value);
    }

    public static <K, V> ImRevMap<K, V> singletonRev(K key, V value) {
        return new SingletonRevMap<K, V>(key, value);
    }

    public static <K, V> ImOrderMap<K, V> singletonOrder(K key, V value) {
        return new SingletonOrderMap<K, V>(key, value);
    }

    public static <K, V> ImMap<K, V> toMap(K key1, V value1, K key2, V value2) {
        MExclMap<K, V> mMap = MapFact.mExclMap(2);
        mMap.exclAdd(key1, value1);
        mMap.exclAdd(key2, value2);
        return mMap.immutable();
    }

    public static <K, V> ImRevMap<K, V> toRevMap(K key1, V value1, K key2, V value2) {
        MRevMap<K, V> mMap = MapFact.mRevMap(2);
        mMap.revAdd(key1, value1);
        mMap.revAdd(key2, value2);
        return mMap.immutableRev();
    }

    public static <K, V> ImMap<K, V> override(ImMap<? extends K, ? extends V> map1, ImMap<? extends K, ? extends V> map2) {
        return ((ImMap<K, V>)map1).override(map2);
    }

    public static <K, V> ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map, K key, V value) {
        return ((ImMap<K, V>)map).addExcl(key, value);
    }

    public static <K, V> ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map1, ImMap<? extends K, ? extends V> map2) {
        return ((ImMap<K, V>)map1).addExcl(map2);
    }

    public static <K, V> ImOrderMap<K, V> addOrderExcl(ImOrderMap<? extends K, ? extends V> map1, ImOrderMap<? extends K, ? extends V> map2) {
        return ((ImOrderMap<K, V>)map1).addOrderExcl(map2);
    }

    public static <K, V> ImRevMap<K, V> addRevExcl(ImRevMap<? extends K, ? extends V> map1, K key, V value) {
        return ((ImRevMap<K, V>)map1).addRevExcl(key, value);
    }

    public static <K, V> ImRevMap<K, V> addRevExcl(ImRevMap<? extends K, ? extends V> map1, ImRevMap<? extends K, ? extends V> map2) {
        return ((ImRevMap<K, V>)map1).addRevExcl(map2);
    }

    public static <K, V> ImMap<K, V> toMap(ImSet<? extends K> keys, final V value) {
        return ((ImSet<K>)keys).toMap(value);
    }

    public static <K, V, T> ImMap<K, T> innerCrossValues(ImMap<? extends K, ? extends V> map1 , ImRevMap<? extends T, ? extends V> map2) {
        return ((ImMap<K,V>)map1).innerCrossValues(map2);
    }

    public static <K, E, V> ImMap<K, V> innerJoin(ImMap<K, ? extends E> map, ImMap<? extends E, V> joinMap) {
        return ((ImMap<K,E>)map).innerJoin(joinMap);
    }

    public static <K, E, V> ImMap<K, V> nullInnerJoin(ImMap<K, ? extends E> map, ImMap<? extends E, V> joinMap) {
        return joinMap==null ? null : ((ImMap<K,E>)map).innerJoin(joinMap);
    }

    public static <K, V, T> ImOrderMap<V, T> orderMap(ImMap<K, T> map, ImRevMap<K, V> revMap, Result<ImOrderSet<K>> order) {
        int sizeNames = revMap.size();
        MOrderExclMap<V, T> mResult = MapFact.mOrderExclMap(sizeNames);
        if(order.result==null) {
            MOrderExclSet<K> mOrder = SetFact.mOrderExclSet(sizeNames); // последействие
            for (int i=0;i<sizeNames;i++) {
                K key = revMap.getKey(i);
                mResult.exclAdd(revMap.getValue(i), map.get(key));
                mOrder.exclAdd(key);
            }
            order.set(mOrder.immutableOrder());
        } else {
            for (K expr : order.result)
                mResult.exclAdd(revMap.get(expr), map.get(expr));
        }
        return mResult.immutableOrder();
    }

    public static <KA, KB, V> ImRevMap<KA, KB> mapValues(ImMap<KA, V> map, ImMap<KB, V> equals) {
        if (map.size() != equals.size()) return null;

        ImRevValueMap<KA, KB> mvMapKeys = map.mapItRevValues();
        boolean[] mapped = new boolean[map.size()];
        for (int i=0,size=map.size();i<size;i++) {
            V mapValue = map.getValue(i);

            int mapJ = -1;
            for (int j=0;j<size;j++) {
                if (!mapped[j] && BaseUtils.hashEquals(mapValue, equals.getValue(j))) {
                    mapJ = j;
                    break;
                }
            }
            if (mapJ < 0) return null;
            mvMapKeys.mapValue(i, equals.getKey(mapJ));
            mapped[mapJ] = true;
        }
        return mvMapKeys.immutableValueRev();
    }

    public static <BK, K extends BK, V> ImRevMap<K, V> splitRevKeys(ImRevMap<BK, V> map, ImSet<K> keys, Result<ImRevMap<BK, V>> rest) {
        return BaseUtils.immutableCast(map.splitRevKeys(BaseUtils.<ImSet<BK>>immutableCast(keys), BaseUtils.<Result<ImRevMap<BK, V>>>immutableCast(rest)));
    }

    public static <K, V> ImMap<K, V> mergeMaps(ImCol<ImMap<K, V>> maps, AddValue<K, V> addValue) {
        MMap<K, V> mResult = MapFact.mMap(addValue);
        for (ImMap<K, V> map : maps)
            mResult.addAll(map);
        return mResult.immutable();
    }

    public static <V> ImRevMap<V, V> mergeMaps(ImRevMap<V, V>[] maps) {
        MRevMap<V, V> result = MapFact.mRevMap();
        for (ImRevMap<V, V> map : maps)
            result.revAddAll(map);
        return result.immutableRev();
    }

    public static int colHash(int h) { // копися с hashSet'а
//        return h;
        h ^= (h >>> 20) ^ (h >>> 12);
        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    public static int objHash(int h) { // копися с hashSet'а
        return h;
//        h ^= (h >>> 20) ^ (h >>> 12);
//        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    // MUTABLE

    public static <K, V> ImOrderMap<K, ImSet<V>> immutable(MOrderExclMap<K, MSet<V>> mMap) {
        return mMap.immutableOrder().mapOrderValues(new GetValue<ImSet<V>, MSet<V>>() { // некрасиво конечно, но что поделаешь
            public ImSet<V> getMapValue(MSet<V> value) {
                return value.immutable();
            }});
    }

    public static <K, V> ImOrderMap<K, ImOrderSet<V>> immutableOrder(MOrderExclMap<K, MOrderExclSet<V>> mMap) {
        return mMap.immutableOrder().mapOrderValues(new GetValue<ImOrderSet<V>, MOrderExclSet<V>>() { // некрасиво конечно, но что поделаешь
            public ImOrderSet<V> getMapValue(MOrderExclSet<V> value) {
                return value.immutableOrder();
            }});
    }

    public static <K, V> ImMap<K, ImSet<V>> immutable(MExclMap<K, MExclSet<V>> mMap) {
        return mMap.immutable().mapValues(new GetValue<ImSet<V>, MExclSet<V>>() {
            public ImSet<V> getMapValue(MExclSet<V> value) {
                return value.immutable();
            }});
    }

    public static <G, K, V> ImMap<G, ImOrderMap<K, V>> immutableOrder(MExclMap<G, MOrderExclMap<K, V>> mMap) {
        return mMap.immutable().mapValues(new GetValue<ImOrderMap<K, V>, MOrderExclMap<K, V>>() {
            public ImOrderMap<K, V> getMapValue(MOrderExclMap<K, V> value) {
                return value.immutableOrder();
            }});
    }

    // map

    public static <K, V> MMap<K, V> mMap(AddValue<K, V> addInterface) {
        return new HMap<K, V>(addInterface);
    }

    public static <K, V> MMap<K, V> mMap(ImMap<? extends K, ? extends V> map, AddValue<K, V> addInterface) { // долно быть достаточно быстрой (см. использование)
        if(map instanceof HMap)
            return new HMap<K, V>((HMap<? extends K,? extends V>) map, addInterface);

        MMap<K, V> mMap = mMap(addInterface);
        mMap.addAll(map);
        return mMap;
    }

    public static <K, V> MMap<K, V> mMapMax(int size, AddValue<K, V> addInterface) {
        if(size < SetFact.useArrayMax)
            return new ArMap<K, V>(size, addInterface);
        return new HMap<K, V>(size, addInterface);
    }

    private final static AddValue<Object, Object> keep = new SimpleAddValue<Object, Object>() {
        public Object addValue(Object key, Object prevValue, Object newValue) {
            return prevValue;
        }

        public boolean reversed() {
            return true;
        }

        public AddValue<Object, Object> reverse() {
            return override;
        }
    };
    public static <K, V> AddValue<K, V> keep() {
        return (AddValue<K, V>) keep;
    }

    private final static AddValue<Object, Object> override = new SimpleAddValue<Object, Object>() {
        public Object addValue(Object key, Object prevValue, Object newValue) {
            return newValue;
        }

        public boolean reversed() {
            return true;
        }

        public AddValue<Object, Object> reverse() {
            return keep;
        }
    };
    public static <K, V> AddValue<K, V> override() {
        return (AddValue<K, V>) override;
    }

    private final static AddValue<Object, Object> keepNewRef = new SimpleAddValue<Object, Object>() {
        public Object addValue(Object key, Object prevValue, Object newValue) {
            if(BaseUtils.hashEquals(prevValue, newValue))
                return newValue;
            return prevValue;
        }

        public boolean reversed() {
            return true;
        }

        public AddValue<Object, Object> reverse() {
            return overridePrevRef;
        }
    };
    public static <K, V> AddValue<K, V> keepNewRef() {
        return (AddValue<K, V>) keepNewRef;
    }

    private final static AddValue<Object, Object> overridePrevRef = new SimpleAddValue<Object, Object>() {
        public Object addValue(Object key, Object prevValue, Object newValue) {
            if(BaseUtils.hashEquals(prevValue, newValue))
                return prevValue;
            return newValue;
        }

        public boolean reversed() {
            return true;
        }

        public AddValue<Object, Object> reverse() {
            return keepNewRef;
        }
    };
    public static <K, V> AddValue<K, V> overridePrevRef() {
        return (AddValue<K, V>) overridePrevRef;
    }

    // exclusive
    
    private final static AddValue<Object, Object> exclusive = new SimpleAddValue<Object, Object>() {
        public Object addValue(Object key, Object prevValue, Object newValue) {
            throw new UnsupportedOperationException(); // при duplicate keys в executeSelect, см. exception в executeDML
        }

        public boolean reversed() {
            throw new UnsupportedOperationException();
        }

        public AddValue<Object, Object> reverse() {
            throw new UnsupportedOperationException();
        }
    };
    public static <K, V> AddValue<K, V> exclusive() {
        return (AddValue<K, V>) exclusive;
    }

    public static <K, V> MExclMap<K, V> mExclMap() {
        return new HMap<K, V>(MapFact.<K, V>exclusive());
    }

    public static <K, V> MExclMap<K, V> mExclMap(int size) { // для массивов
        return mExclMapMax(size);
   }

    public static <K, V> MExclMap<K, V> mExclMapMax(int size) {
        if(size<SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArMap<K, V>(size, MapFact.<K, V>exclusive());
        return new HMap<K, V>(size, MapFact.<K, V>exclusive());
    }

    public static <K, V> MExclMap<K, V> mExclMap(ImMap<? extends K, ? extends V> map) {
        if(map instanceof HMap)
            return new HMap<K, V>((HMap<? extends K,? extends V>) map, MapFact.<K, V>exclusive());

        MExclMap<K, V> mMap = mExclMap();
        mMap.exclAddAll(map);
        return mMap;
    }

    public static <K, V> MFilterMap<K, V> mFilter(ImMap<K, V> map) {
        int size = map.size();
        if(map instanceof ArIndexedMap)
            return new ArIndexedMap<K, V>(size, MapFact.<K, V>exclusive());
        if(size < SetFact.useArrayMax)
            return new ArMap<K, V>(size, MapFact.<K, V>exclusive());
        return new HMap<K, V>(size, MapFact.<K, V>exclusive());
    }

    public static <K, V> ImMap<K, V> imFilter(MFilterMap<K, V> mMap, ImMap<K, V> map) {
        ImMap<K, V> result = mMap.immutable();
        if(result.size()==map.size()) {
//            assert BaseUtils.hashEquals(result, map); // так как может null содержать в PropertyChanges.replace например
            return map;
        }
        return result;
    }


    // reversed

    public static <K, V> MRevMap<K, V> mRevMap() {
        return new HMap<K, V>(MapFact.<K, V>exclusive());
    }

    public static <K, V> MRevMap<K, V> mRevMap(int size) {
        return mRevMapMax(size);
    }

    public static <K, V> MRevMap<K, V> mRevMapMax(int size) {
        if(size<SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArMap<K, V>(size, MapFact.<K, V>exclusive());
        return new HMap<K, V>(size, MapFact.<K, V>exclusive());
    }

    public static <K, V> MRevMap<K, V> mRevMap(ImRevMap<K, V> map) {
        if(map instanceof HMap)
            return new HMap<K, V>((HMap<? extends K,? extends V>) map, MapFact.<K, V>exclusive());

        MRevMap<K, V> mMap = mRevMap();
        mMap.revAddAll(map);
        return mMap;
    }

    public static <K, V> MFilterRevMap<K, V> mRevFilter(ImRevMap<K, V> map) {
        int size = map.size();
        if(map instanceof ArIndexedMap)
            return new ArIndexedMap<K, V>(size, MapFact.<K, V>exclusive());
        if(size < SetFact.useArrayMax)
            return new ArMap<K, V>(size, MapFact.<K, V>exclusive());
        return new HMap<K, V>(size, MapFact.<K, V>exclusive());
    }

    public static <K, V> ImRevMap<K, V> imRevFilter(MFilterRevMap<K, V> mMap, ImRevMap<K, V> map) {
        ImRevMap<K, V> result = mMap.immutableRev();
        if(result.size()==map.size()) {
            assert BaseUtils.hashEquals(result, map);
            return map;
        }
        return result;
    }

    // ordered

    public static <K, V> MOrderMap<K, V> mOrderMap() {
        return mOrderMap(MapFact.<K, V>keep());
    }

    public static <K, V> MOrderMap<K, V> mOrderMap(AddValue<K, V> addValue) {
        return new HOrderMap<K, V>(addValue);
    }

    public static <K, V> MOrderMap<K, V> mOrderMap(ImOrderMap<K, V> map) {
        if(map instanceof HOrderMap)
            return new HOrderMap<K, V>((HOrderMap<K,V>) map, MapFact.<K,V>keep());

        MOrderMap<K, V> mOrderMap = mOrderMap();
        mOrderMap.addAll(map);
        return mOrderMap;
    }

    public static <K, V> MOrderMap<K, V> mOrderMapMax(int size) {
        if(size < SetFact.useArrayMax)
            return new ArOrderMap<K, V>(size, MapFact.<K,V>keep());
        return new HOrderMap<K, V>(size, MapFact.<K,V>keep());
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMap() {
        return new HOrderMap<K, V>(MapFact.<K,V>exclusive());
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMap(int size) {
        return mOrderExclMapMax(size);
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMapMax(int size) {
        if(size<SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArOrderMap<K, V>(size, MapFact.<K, V>exclusive());
        return new HOrderMap<K, V>(size, MapFact.<K,V>exclusive());
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMap(ImOrderMap<? extends K, ? extends V> map) {
        if(map instanceof HOrderMap)
            return new HOrderMap<K, V>((HOrderMap<K,V>) map, MapFact.<K,V>exclusive());

        MOrderExclMap<K, V> mOrderMap = mOrderExclMap();
        mOrderMap.exclAddAll(map);
        return mOrderMap;
    }

    public static <K, V> MOrderFilterMap<K, V> mOrderFilter(ImOrderMap<K, V> map) {
        int size = map.size();
//        if(map instanceof ArOrderIndexedMap) keep сложнее поддерживать
//            return new ArOrderIndexedMap<K, V>(size, MapFact.<K, V>exclusive());
        if(size < SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin)
            return new ArOrderMap<K, V>(size, MapFact.<K, V>exclusive());
        return new HOrderMap<K, V>(size, MapFact.<K, V>exclusive());
    }

    public static <K, V> ImOrderMap<K, V> imOrderFilter(MOrderFilterMap<K, V> mMap, ImOrderMap<K, V> map) {
        ImOrderMap<K, V> result = mMap.immutableOrder();
        if(result.size()==map.size()) {
            assert BaseUtils.hashEquals(result, map);
            return map;
        }
        return result;
    }

    // map'ы по определению mutable, без явных imutable интерфейсов

    // mutable'ы на заполнение, то есть кэши, локальные обработки
    public static <K, V> MAddMap<K, V> mAddOverrideMap() {
        return mAddMap(MapFact.<K, V>override());
    }

    public static <K, V> MAddMap<K, V> mAddMap(AddValue<K, V> addValue) {
        return new HMap<K, V>(addValue);
    }

    public static <K, V> MAddExclMap<K, V> mAddExclMap() {
        return new HMap<K, V>(MapFact.<K, V>exclusive());
    }

    public static <K, V> MAddExclMap<K, V> mBigStrongMap() {
        return mAddExclMap();
    }

    public static <K, V> MAddExclMap<K, V> mSmallStrongMap() {
        return new ArIndexedMap<K, V>(MapFact.<K, V>exclusive());
    }

    public static <K, V> MAddExclMap<K, V> mAddExclMap(ImMap<K, V> map) {
        if(map instanceof HMap)
            return new HMap<K, V>((HMap<K, V>)map, MapFact.<K, V>exclusive());

        MAddExclMap<K, V> mResult = mAddExclMap();
        for(int i=0,size=map.size();i<size;i++)
            mResult.exclAdd(map.getKey(i), map.getValue(i));
        return mResult;
    }

    public static <K, V> MAddMap<K, V> mAddExclMapMax(int size) {
        return new HMap<K, V>(size, MapFact.<K, V>exclusive());
    }

    // реальный mutable с добавлением \ удалением
    public static <K, V> Map<K, V> mAddRemoveMap() {
        return new HashMap<K, V>();
    }

    public static <K, V> OrderedMap<K, V> mAddRemoveOrderMap() {
        return new OrderedMap<K, V>();
    }

    // remove Map'ы

    public static <K, V> ImMap<K, V> fromJavaMap(Map<? extends K, ? extends V> map) {
        MExclMap<K, V> mResult = MapFact.mExclMap(map.size());
        for(Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            mResult.exclAdd(entry.getKey(), entry.getValue());
        return mResult.immutable();
    }
    public static <K, V> ImRevMap<K, V> fromJavaRevMap(Map<K, V> map) {
        MRevMap<K, V> mResult = MapFact.mRevMap(map.size());
        for(Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            mResult.revAdd(entry.getKey(), entry.getValue());
        return mResult.immutableRev();
    }
    public static <K, V> ImOrderMap<K, V> fromJavaOrderMap(OrderedMap<K, V> map) {
        MOrderExclMap<K, V> mResult = MapFact.mOrderExclMap(map.size());
        for(Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            mResult.exclAdd(entry.getKey(), entry.getValue());
        return mResult.immutableOrder();
    }

    public static <K, V> void addJavaAll(Map<K, V> map, ImMap<K, V> add) {
        for(int i=0,size=add.size();i<size;i++)
            map.put(add.getKey(i), add.getValue(i));
    }
    public static <K, V> boolean disjointJava(ImSet<K> set1, Set<K> set2) {
        for(K element : set2)
            if(set1.contains(element))
                return false;
        return true;
    }

    public static <K> AddValue<K, Integer> addLinear() {
        return (AddValue<K, Integer>) addLinear;
    }
}
