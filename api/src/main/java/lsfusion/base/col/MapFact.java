package lsfusion.base.col;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashMap;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap;
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
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.lru.LRUUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap.DEFAULT_INITIAL_CAPACITY;
import static lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap.DEFAULT_LOAD_FACTOR;

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
        return new SingletonRevMap<>(key, value);
    }

    public static <K, V> ImRevMap<K, V> singletonRev(K key, V value) {
        return new SingletonRevMap<>(key, value);
    }

    public static <K, V> ImOrderMap<K, V> singletonOrder(K key, V value) {
        return new SingletonOrderMap<>(key, value);
    }

    public static <K, V> ImMap<K, V> toMap(K key1, V value1, K key2, V value2) {
        MExclMap<K, V> mMap = MapFact.mExclMap(2);
        mMap.exclAdd(key1, value1);
        mMap.exclAdd(key2, value2);
        return mMap.immutable();
    }
    
    public static <K, V> ImMap<K, V> toMap(K key1, V value1, K key2, V value2, K key3, V value3) {
        MExclMap<K, V> mMap = MapFact.mExclMap(3);
        mMap.exclAdd(key1, value1);
        mMap.exclAdd(key2, value2);
        mMap.exclAdd(key3, value3);
        return mMap.immutable();
    }

    public static <K, V> ImMap<K, V> toMap(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
        MExclMap<K, V> mMap = MapFact.mExclMap(3);
        mMap.exclAdd(key1, value1);
        mMap.exclAdd(key2, value2);
        mMap.exclAdd(key3, value3);
        mMap.exclAdd(key4, value4);
        return mMap.immutable();
    }

    public static <K, V> ImMap<K, V> toMap(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7) {
        MExclMap<K, V> mMap = MapFact.mExclMap(7);
        mMap.exclAdd(key1, value1);
        mMap.exclAdd(key2, value2);
        mMap.exclAdd(key3, value3);
        mMap.exclAdd(key4, value4);
        mMap.exclAdd(key5, value5);
        mMap.exclAdd(key6, value6);
        mMap.exclAdd(key7, value7);
        return mMap.immutable();
    }

    public static <K, V> ImOrderMap<K, V> toOrderMap(K key1, V value1, K key2, V value2) {
        MOrderMap<K, V> mMap = MapFact.mOrderMap();
        mMap.add(key1, value1);
        mMap.add(key2, value2);
        return mMap.immutableOrder();
    }

    public static <K, V> ImRevMap<K, V> toRevMap(K key1, V value1, K key2, V value2) {
        MRevMap<K, V> mMap = MapFact.mRevMap(2);
        mMap.revAdd(key1, value1);
        mMap.revAdd(key2, value2);
        return mMap.immutableRev();
    }

    public static <K, V> ImMap<K, V> toMap(K[] keys, V[] values) {
        MExclMap<K, V> mMap = MapFact.mExclMap(keys.length);
        for(int i=0;i<keys.length;i++)
            mMap.exclAdd(keys[i], values[i]);
        return mMap.immutable();
    }

    public static <V> ImMap<Integer, V> toIndexedMap(V[] values) {
        MExclMap<Integer, V> mMap = MapFact.mExclMap(values.length);
        for(int i=0;i<values.length;i++)
            mMap.exclAdd(i, values[i]);
        return mMap.immutable();
    }

    public static <K, V> ImMap<K, V> toMap(K[] keys, Function<K, V> values) {
        MExclMap<K, V> mMap = MapFact.mExclMap(keys.length);
        for(int i=0;i<keys.length;i++) {
            K key = keys[i];
            mMap.exclAdd(key, values.apply(key));
        }
        return mMap.immutable();
    }

    public static <K, V> ImMap<K, V> override(ImMap<? extends K, ? extends V> map1, ImMap<? extends K, ? extends V> map2) {
        return ((ImMap<K, V>)map1).override(map2);
    }

    public static <B, K extends B, V> ImRevMap<K, V> replaceValues(ImRevMap<K, ? extends V> map1, final ImRevMap<B, ? extends V> map2) {
        return map1.mapRevValues((key, value) -> {
            V value2 = map2.get(key);
            if (value2 != null)
                return value2;
            return value;
        });
    }

    public static <B, K extends B, V> ImMap<K, V> replaceValues(ImMap<K, ? extends V> map1, final ImMap<B, ? extends V> map2) {
        return map1.mapValues((key, value) -> {
            V value2 = map2.get(key);
            if(value2 != null)
                return value2;
            return value;
        });
    }
    public static <K, V> ImRevMap<K, V> filterRev(ImRevMap<? extends K, V> map, ImSet<K> set) {
        return ((ImRevMap<K, V>)map).filterRev(set);
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

    public static <T, P, V> ImMap<P, V> nullCrossJoin(ImMap<T, V> map, ImRevMap<T, P> mapping) {
        return map == null ? null : mapping.crossJoin(map);
    }

    public static <K, V> ImMap<K, V> nullRemove(ImMap<K, V> map, ImSet<K> set) {
        return map == null ? null : map.remove(set);
    }

    public static <K, V> ImMap<K, V> nullFilter(ImMap<K, V> map, ImSet<K> set) {
        return map == null ? null : map.filter(set);
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
        return BaseUtils.immutableCast(map.splitRevKeys(BaseUtils.<ImSet<BK>>immutableCast(keys), BaseUtils.immutableCast(rest)));
    }

    public static <K, V> ImMap<K, V> mergeMaps(ImCol<ImMap<K, V>> maps, AddValue<K, V> addValue) {
        MMap<K, V> mResult = MapFact.mMap(addValue);
        for (ImMap<K, V> map : maps)
            mResult.addAll(map);
        return mResult.immutable();
    }

    public static <K, V> ImOrderMap<K, V> mergeOrderMapsExcl(Iterable<ImOrderMap<K, V>> maps) {
        MOrderExclMap<K, V> mResult = MapFact.mOrderExclMap();
        for (ImOrderMap<K, V> orderMap : maps)
            mResult.exclAddAll(orderMap);
        return mResult.immutableOrder();
    }

    public static <V> ImRevMap<V, V> mergeMaps(ImRevMap<V, V>[] maps) {
        MRevMap<V, V> result = MapFact.mRevMap();
        for (ImRevMap<V, V> map : maps)
            result.revAddAll(map);
        return result.immutableRev();
    }

    // https://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
    // Can return negative values!
    public static int colHash(int h) {
        h = ((h >>> 16) ^ h) * 0x45d9f3b;
        h = ((h >>> 16) ^ h) * 0x45d9f3b;
        return (h >>> 16) ^ h;
    }

    public static int objHash(int h) {
        return h;
//        h ^= (h >>> 20) ^ (h >>> 12);
//        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    // MUTABLE

    public static <K, V> ImOrderMap<K, ImSet<V>> immutable(MOrderExclMap<K, MSet<V>> mMap) {
        return mMap.immutableOrder().mapOrderValues(MSet<V>::immutable);
    }

    public static <K, V> ImOrderMap<K, ImOrderSet<V>> immutableOrder(MOrderExclMap<K, MOrderExclSet<V>> mMap) {
        return mMap.immutableOrder().mapOrderValues(MOrderExclSet<V>::immutableOrder);
    }

    public static <K, V> ImMap<K, ImOrderSet<V>> immutableMapOrder(MExclMap<K, MOrderExclSet<V>> mMap) {
        return mMap.immutable().mapValues(MOrderExclSet::immutableOrder);
    }

    public static <K, V> ImMap<K, ImList<V>> immutableList(MExclMap<K, MList<V>> mMap) {
        return mMap.immutable().mapValues(MList::immutableList);
    }

    public static <K, V> ImMap<K, ImSet<V>> immutable(MExclMap<K, MExclSet<V>> mMap) {
        return mMap.immutable().mapValues(MExclSet::immutable);
    }

    public static <K, V, T> ImMap<K, ImMap<V, T>> immutableMapMap(MExclMap<K, MMap<V, T>> mMap) {
        return mMap.immutable().mapValues(MMap::immutable);
    }

    public static <K, V> ImMap<K, ImSet<V>> immutableMap(MExclMap<K, MSet<V>> mMap) {
        return mMap.immutable().mapValues(MSet::immutable);
    }

    public static <K, V> ImMap<K, ImSet<V>> immutableMapExcl(MExclMap<K, MExclSet<V>> mMap) {
        return mMap.immutable().mapValues(MExclSet::immutable);
    }

    public static <K, V, M> ImMap<K, ImMap<V, M>> immutableMapExclMap(MExclMap<K, MExclMap<V, M>> mMap) {
        return mMap.immutable().mapValues(MExclMap::immutable);
    }

    public static <G, K, V> ImMap<G, ImOrderMap<K, V>> immutableOrder(MExclMap<G, MOrderExclMap<K, V>> mMap) {
        return mMap.immutable().mapValues(MOrderExclMap::immutableOrder);
    }

    // map

    public static <K, V> MMap<K, V> mMap(AddValue<K, V> addInterface) {
        return new HMap<>(addInterface);
    }

    public static <K, V> MMap<K, V> mMap(ImMap<? extends K, ? extends V> map, AddValue<K, V> addInterface) { // долно быть достаточно быстрой (см. использование)
        if(map instanceof HMap)
            return new HMap<>((HMap<? extends K, ? extends V>) map, addInterface);

        MMap<K, V> mMap = mMap(addInterface);
        mMap.addAll(map);
        return mMap;
    }

    public static <K, V> MMap<K, V> mMapMax(int size, AddValue<K, V> addInterface) {
        if(size < SetFact.useArrayMax)
            return new ArMap<>(size, addInterface);
        return new HMap<>(size, addInterface);
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
    private final static AddValue<Object, Integer> max = new SymmAddValue<Object, Integer>() {
        @Override
        public Integer addValue(Object key, Integer prevValue, Integer newValue) {
            return BaseUtils.max(prevValue, newValue);
        }
    };            
    public static <K> AddValue<K, Integer> max() {
        return (AddValue<K, Integer>) max;
    }
    
    private final static AddValue<Object, Boolean> or = new SymmAddValue<Object, Boolean>() {
        @Override
        public Boolean addValue(Object key, Boolean prevValue, Boolean newValue) {
            return prevValue || newValue;
        }
    };            
    public static <K> AddValue<K, Boolean> or() {
        return (AddValue<K, Boolean>) or;
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
            throw new UnsupportedOperationException("KEY : " + BaseUtils.nullToString(key) + ", PREVVALUE : " + BaseUtils.nullToString(prevValue) + ", NEWVALUE : " + BaseUtils.nullToString(newValue)); // при duplicate keys в executeSelect, см. exception в executeDML
        }

        public boolean reversed() {
            return true;
        }

        public AddValue<Object, Object> reverse() {
            return this;
        }

        @Override
        public boolean exclusive() {
            return true;
        }
    };
    public static <K, V> AddValue<K, V> exclusive() {
        return (AddValue<K, V>) exclusive;
    }

    public static <K, V> MExclMap<K, V> mExclMap() {
        return new HMap<>(MapFact.exclusive());
    }

    public static <K, V> MExclMap<K, V> mExclMap(int size) { // для массивов
        return mExclMapMax(size);
   }

    public static <K, V> MExclMap<K, V> mExclMapMax(int size) {
        if(size<SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArMap<>(size, MapFact.exclusive());
        return new HMap<>(size, MapFact.exclusive());
    }

    public static <K, V> MExclMap<K, V> mExclMap(ImMap<? extends K, ? extends V> map) {
        if(map instanceof HMap)
            return new HMap<>((HMap<? extends K, ? extends V>) map, MapFact.exclusive());

        MExclMap<K, V> mMap = mExclMap();
        mMap.exclAddAll(map);
        return mMap;
    }

    public static <K, V> MMap<K, V> mMap(boolean isExclusive) {
        if(!isExclusive)
            return mMap(MapFact.override());
        final MExclMap<K, V> mExclMap = MapFact.mExclMap();
        return new MMap<K, V>() {
            public boolean add(K key, V value) {
                mExclMap.exclAdd(key, value);
                return true;
            }

            public boolean addAll(ImMap<? extends K, ? extends V> map) {
                mExclMap.exclAddAll(map);
                return true;
            }

            public boolean addAll(ImSet<? extends K> set, V value) {
                mExclMap.exclAddAll(set, value);
                return true;
            }

            public V get(K key) {
                return mExclMap.get(key);
            }

            public ImMap<K, V> immutable() {
                return mExclMap.immutable();
            }

            public ImMap<K, V> immutableCopy() {
                return mExclMap.immutableCopy();
            }
        };
    }

    public static <K, V> MFilterMap<K, V> mFilter(ImMap<K, V> map) {
        int size = map.size();
        if(map instanceof ArIndexedMap)
            return new ArIndexedMap<>(size, MapFact.exclusive());
        if(size < SetFact.useArrayMax)
            return new ArMap<>(size, MapFact.exclusive());
        return new HMap<>(size, MapFact.exclusive());
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
        return new HMap<>(MapFact.exclusive());
    }

    public static <K, V> MRevMap<K, V> mRevMap(int size) {
        return mRevMapMax(size);
    }

    public static <K, V> MRevMap<K, V> mRevMapMax(int size) {
        if(size<SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArMap<>(size, MapFact.exclusive());
        return new HMap<>(size, MapFact.exclusive());
    }

    public static <K, V> MRevMap<K, V> mRevMap(ImRevMap<K, V> map) {
        if(map instanceof HMap)
            return new HMap<>((HMap<? extends K, ? extends V>) map, MapFact.exclusive());

        MRevMap<K, V> mMap = mRevMap();
        mMap.revAddAll(map);
        return mMap;
    }

    public static <K, V> MFilterRevMap<K, V> mRevFilter(ImRevMap<K, V> map) {
        int size = map.size();
        if(map instanceof ArIndexedMap)
            return new ArIndexedMap<>(size, MapFact.exclusive());
        if(size < SetFact.useArrayMax)
            return new ArMap<>(size, MapFact.exclusive());
        return new HMap<>(size, MapFact.exclusive());
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
        return mOrderMap(MapFact.keep());
    }

    public static <K, V> MOrderMap<K, V> mOrderMap(AddValue<K, V> addValue) {
        return new HOrderMap<>(addValue);
    }

    public static <K, V> MOrderMap<K, V> mOrderMap(ImOrderMap<K, V> map) {
        if(map instanceof HOrderMap)
            return new HOrderMap<>((HOrderMap<K, V>) map, MapFact.keep());

        MOrderMap<K, V> mOrderMap = mOrderMap();
        mOrderMap.addAll(map);
        return mOrderMap;
    }

    public static <K, V> MOrderMap<K, V> mOrderMapMax(int size) {
        if(size < SetFact.useArrayMax)
            return new ArOrderMap<>(size, MapFact.keep());
        return new HOrderMap<>(size, MapFact.keep());
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMap() {
        return new ArOrderMap<>(MapFact.exclusive());
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMap(int size) {
        return mOrderExclMapMax(size);
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMapMax(int size) {
        if(size<SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArOrderMap<>(size, MapFact.exclusive());
        return new HOrderMap<>(size, MapFact.exclusive());
    }

    public static <K, V> MOrderExclMap<K, V> mOrderExclMap(ImOrderMap<? extends K, ? extends V> map) {
        if(map instanceof HOrderMap)
            return new HOrderMap<>((HOrderMap<K, V>) map, MapFact.exclusive());

        MOrderExclMap<K, V> mOrderMap = mOrderExclMap();
        mOrderMap.exclAddAll(map);
        return mOrderMap;
    }

    public static <K, V> MOrderFilterMap<K, V> mOrderFilter(ImOrderMap<K, V> map) {
        int size = map.size();
//        if(map instanceof ArOrderIndexedMap) keep сложнее поддерживать
//            return new ArOrderIndexedMap<K, V>(size, MapFact.<K, V>exclusive());
        if(size < SetFact.useArrayMax || size >= SetFact.useIndexedArrayMin)
            return new ArOrderMap<>(size, MapFact.exclusive());
        return new HOrderMap<>(size, MapFact.exclusive());
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
        return mAddMap(MapFact.override());
    }

    public static <K, V> MAddMap<K, V> mAddMap(AddValue<K, V> addValue) {
        return new HMap<>(addValue);
    }
    
    public static <K, V> MAddMap<K, V> mAddMapMax(int size, AddValue<K, V> addValue) {
        return new HMap<>(size, addValue);
    }

    public static <K, V> MAddMap<K, V> mAddOverrideMap(ImMap<? extends K, ? extends V> map) {
        return mAddMap(map, MapFact.override());
    }

    public static <K, V> MAddMap<K, V> mAddMap(ImMap<? extends K, ? extends V> map, AddValue<K, V> addValue) {
        if(map instanceof HMap)
            return new HMap<>((HMap<? extends K, ? extends V>) map, addValue);

        HMap<K, V> mMap = new HMap<>(addValue);
        mMap.exclAddAll(map);
        return mMap;
    }

    public static <K, V> MAddExclMap<K, V> mAddExclMap() {
        return new HMap<>(MapFact.exclusive());
    }

    public static <K, V> MAddExclMap<K, V> mBigStrongMap() {
        return mAddExclMap();
    }

    public static <K, V> MAddExclMap<K, V> mSmallStrongMap() {
        return new ArIndexedMap<>(MapFact.exclusive());
    }

    public static <K, V> MAddExclMap<K, V> mAddExclMap(ImMap<K, V> map) {
        if(map instanceof HMap)
            return new HMap<>((HMap<K, V>) map, MapFact.exclusive());

        MAddExclMap<K, V> mResult = mAddExclMap();
        for(int i=0,size=map.size();i<size;i++)
            mResult.exclAdd(map.getKey(i), map.getValue(i));
        return mResult;
    }

    public static <K, V> MAddExclMap<K, V> mAddExclMapMax(int size) {
        return new HMap<>(size, MapFact.exclusive());
    }

    // реальный mutable с добавлением \ удалением
    public static <K, V> Map<K, V> mAddRemoveMap() {
        return new HashMap<>();
    }

    public static <K, V> OrderedMap<K, V> mAddRemoveOrderMap() {
        return new OrderedMap<>();
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
    public static <K, V> void addJavaAll(Map<K, V> map, MAddExclMap<K, V> add) {
        for(int i=0,size=add.size();i<size;i++)
            map.put(add.getKey(i), add.getValue(i));
    }
    public static <K, V> boolean disjointJava(ImSet<K> set1, Set<K> set2) {
        for(K element : set2)
            if(set1.contains(element))
                return false;
        return true;
    }

    public static <K extends V, V> boolean containsAll(ImSet<K> set1, ImSet<V> set2) {
        return BaseUtils.<ImSet<V>>immutableCast(set1).containsAll(set2);
    }

    public static <K> AddValue<K, Integer> addLinear() {
        return (AddValue<K, Integer>) addLinear;
    }
    
    private static <N, E> void recBuildGraphOrder(MOrderExclSet<N> orderSet, N node, ImMap<N, ImSet<E>> edges, Function<E, N> edgeTo) {
        if(orderSet.contains(node)) // значит уже были
            return;
        
        for(E edge : edges.get(node)) {
            recBuildGraphOrder(orderSet, edgeTo.apply(edge), edges, edgeTo);
        }
        
        orderSet.exclAdd(node);
    }
    
    public static <N, E> ImOrderSet<N> buildGraphOrder(ImMap<N, ImSet<E>> edges, Function<E, N> edgeTo) {
        MOrderExclSet<N> mResult = SetFact.mOrderExclSet(edges.size());
        for(N node : edges.keyIt()) {
            recBuildGraphOrder(mResult, node, edges, edgeTo);
        }
        return mResult.immutableOrder();
    }

    private final static Function<Object, ImSet<Object>> toSingleton = SetFact::singleton;

    public static <V> Function<V, ImSet<V>> toSingleton() {
        return BaseUtils.immutableCast(toSingleton);
    }

    private final static Function<Object, MSet<Object>> mSet = value -> SetFact.mSet();

    public static <K, V> Function<K, MSet<V>> mSet() {
        return BaseUtils.immutableCast(mSet);
    }

    private static <K, V> ConcurrentHashMap<K, V> getGlobalConcurrentHashMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity, DEFAULT_LOAD_FACTOR, LRUUtil.DEFAULT_CONCURRENCY_LEVEL);
    }
    
    public static <K, V> ConcurrentHashMap<K, V> getGlobalConcurrentHashMap() {
        return getGlobalConcurrentHashMap(DEFAULT_INITIAL_CAPACITY);
    }
    
    public static <K, V> ConcurrentHashMap<K, V> getGlobalConcurrentHashMap(ConcurrentHashMap<K, V> source) {
        ConcurrentHashMap<K, V> result = getGlobalConcurrentHashMap(Math.max((int) (source.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY));
        result.putAll(source);
        return result;
    }

    private static <K, V> ConcurrentWeakHashMap<K, V> getGlobalConcurrentWeakHashMap(int initialCapacity) {
        return new ConcurrentWeakHashMap<>(initialCapacity, DEFAULT_LOAD_FACTOR, LRUUtil.DEFAULT_CONCURRENCY_LEVEL);
    }

    public static <K, V> ConcurrentIdentityWeakHashMap<K, V> getGlobalConcurrentIdentityWeakHashMap() {
        return new ConcurrentIdentityWeakHashMap<>(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, LRUUtil.DEFAULT_CONCURRENCY_LEVEL);
    }

    public static <K, V> ConcurrentWeakHashMap<K, V> getGlobalConcurrentWeakHashMap() {
        return getGlobalConcurrentWeakHashMap(DEFAULT_INITIAL_CAPACITY);
    }

    public static <K, V> ConcurrentWeakHashMap<K, V> getGlobalConcurrentWeakHashMap(ConcurrentWeakHashMap<K, V> source) {
        ConcurrentWeakHashMap<K, V> result = getGlobalConcurrentWeakHashMap(Math.max((int) (source.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY));
        result.putAll(source);
        return result;
    }
}
