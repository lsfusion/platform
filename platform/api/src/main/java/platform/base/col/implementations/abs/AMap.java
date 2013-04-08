package platform.base.col.implementations.abs;

import platform.base.*;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.implementations.simple.FilterValueMap;
import platform.base.NotFunctionSet;
import platform.base.SFunctionSet;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.mapvalue.*;

import java.util.*;

public abstract class AMap<K, V> extends AColObject implements ImMap<K, V> {

    public String toString() {
        return toString(" - ", ",");
    }

    public String toString(String conc, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getKey(i) + conc + getValue(i));
        }
        return builder.toString();
    }

    public String toString(GetKeyValue<String, K, V> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.getMapValue(getKey(i), getValue(i)));
        }
        return builder.toString();
    }

    public Iterable<K> keyIt() {
        return new Iterable<K>() {
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    int i=0;

                    public boolean hasNext() {
                        return i<size();
                    }

                    public K next() {
                        return getKey(i++);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public Iterable<V> valueIt() {
        return new Iterable<V>() {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    int i=0;

                    public boolean hasNext() {
                        return i<size();
                    }

                    public V next() {
                        return getValue(i++);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public boolean isEmpty() {
        return size()==0;
    }

    public boolean containsKey(K key) {
        return keys().contains(key);
    }

    public boolean containsValue(V value) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(getValue(i), value))
                return true;
        return false;
    }

    public ImMap<V, ImSet<K>> groupValues() {
        MExclMap<V, MExclSet<K>> mResult = MapFact.mExclMapMax(size());
        for (int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            V group = getValue(i);
            if(group!=null) {
                MExclSet<K> groupList = mResult.get(group);
                if (groupList == null) {
                    groupList = SetFact.mExclSetMax(size);
                    mResult.exclAdd(group, groupList);
                }
                groupList.exclAdd(key);
            }
        }
        return MapFact.immutable(mResult);
    }

    public ImSet<K> keys() {
        MExclSet<K> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getKey(i));
        return mResult.immutable();
    }

    public ImCol<V> values() {
        MCol<V> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getValue(i));
        return mResult.immutableCol();
    }

    public ImRevMap<K, V> toRevMap() {
        MRevMap<V, K> mResult = MapFact.mRevMapMax(size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(!mResult.containsKey(value))
                mResult.revAdd(value, getKey(i));
        }
        return mResult.immutableRev().reverse();
    }

    public ImRevMap<K, V> toRevExclMap() {
        MRevMap<K, V> mResult = MapFact.mRevMapMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getKey(i), getValue(i));
        return mResult.immutableRev();
    }

    public ImOrderMap<K, V> toOrderMap() {
        MOrderExclMap<K, V> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getKey(i), getValue(i));
        return mResult.immutableOrder();
    }

    public V get(K key) {
        return getObject(key);
    }

    public V getPartial(K key) {
        return getObject(key);
    }

    public V getObject(Object key) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(getKey(i), key))
                return getValue(i);
        return null;
    }

    public K singleKey() {
        assert size()==1;
        return getKey(0);
    }

    public V singleValue() {
        assert size()==1;
        return getValue(0);
    }

    public boolean identity() {
        for(int i=0,size=size();i<size;i++)
            if(!BaseUtils.hashEquals(getKey(i), getValue(i)))
                return false;
        return true;
    }

    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> imMap, AddValue<K, V> add) {
        if(imMap.isEmpty()) return this;
        
        if(add.symmetric() && size() < imMap.size()) return ((ImMap<K, V>)imMap).merge(this, add);

        MMap<K, V> mResult = MapFact.mMap(this, add);
        if(!mResult.addAll(imMap))
            return null;
        return mResult.immutable();
    }

    public ImMap<K, V> addExcl(K key, V value) {
        MExclMap<K, V> mResult = MapFact.mExclMap(this);
        mResult.exclAdd(key, value);
        return mResult.immutable();
    }

    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> imMap) {
        if(imMap.isEmpty()) return this;
        
        if(size() < imMap.size()) return ((ImMap<K,V>)imMap).addExcl(this);

        MExclMap<K, V> mResult = MapFact.mExclMap(this);
        mResult.exclAddAll(imMap);
        return mResult.immutable();
    }

    public ImMap<K, V> addEquals(ImMap<? extends K, ? extends V> imMap) {
        assert keys().containsAll(imMap.keys());

        return ((ImMap<K, V>)imMap).filterFn(new GetKeyValue<Boolean, K, V>() {
            public Boolean getMapValue(K key, V value) {
                return BaseUtils.hashEquals(get(key), value);
            }
        });
    }

    public <M> ImMap<K, M> join(ImMap<? super V, M> joinMap) {
        return mapValues(((ImMap<V, M>) joinMap).fnGetValue());
    }

    public <M> ImMap<K, M> rightJoin(ImMap<? extends V, M> joinMap) {
        assert values().toSet().containsAll(joinMap.keys());

        return innerJoin(joinMap);
    }

    public <M> ImMap<K, M> innerJoin(ImMap<? extends V, M> joinMap) {
        MExclMap<K, M> mResult = MapFact.mExclMap(joinMap.size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(((ImMap<V, M>)joinMap).containsKey(value))
                mResult.exclAdd(getKey(i), ((ImMap<V, M>) joinMap).get(value));
        }
        return mResult.immutable();
    }

    public <T> ImMap<K, T> innerCrossValues(ImRevMap<? extends T, ? extends V> imRevMap) {
        return innerJoin(((ImRevMap<T, V>) imRevMap).reverse());
    }

    public ImMap<K, V> filterFn(GetKeyValue<Boolean, K, V> filter) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            K key = getKey(i);
            if(filter.getMapValue(key, value))
                mResult.keep(key, value);
        }
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> filterFn(FunctionSet<K> filter) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            if(filter.contains(key))
                mResult.keep(key, getValue(i));
        }
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> filterFnValues(FunctionSet<V> filter) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(filter.contains(value))
                mResult.keep(getKey(i), value);
        }
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> splitKeys(GetKeyValue<Boolean, K, V> filter, Result<ImMap<K, V>> rest) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        MFilterMap<K, V> mRest = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            K key = getKey(i);
            if(filter.getMapValue(key, value))
                mResult.keep(key, value);
            else
                mRest.keep(key, value);
        }
        rest.set(MapFact.imFilter(mRest, this));
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> splitKeys(FunctionSet<K> filter, Result<ImMap<K, V>> rest) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        MFilterMap<K, V> mRest = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            K key = getKey(i);
            if(filter.contains(key))
                mResult.keep(key, value);
            else
                mRest.keep(key, value);
        }
        rest.set(MapFact.imFilter(mRest, this));
        return MapFact.imFilter(mResult, this);
    }

    public <EK extends K> ImMap<EK, V> filter(ImSet<? extends EK> keys) {
        if(size()<=keys.size())
            return BaseUtils.immutableCast(filterFn(BaseUtils.<FunctionSet<K>>immutableCast(keys)));

        ImFilterValueMap<EK, V> mapFilter = ((ImSet<EK>)keys).mapFilterValues();
        for(int i=0,size=keys.size();i<size;i++) {
            V value = get(keys.get(i));
            if(value!=null)
                mapFilter.mapValue(i, value);
        }
        return mapFilter.immutableValue();
    }

    public <EK extends K> ImMap<EK, V> filterIncl(ImSet<? extends EK> keys) {
        assert keys().containsAll(keys);
        return ((ImSet<EK>)keys).mapValues(BaseUtils.<GetValue<V, EK>>immutableCast(fnGetValue()));
    }

    public ImMap<K, V> remove(ImSet<? extends K> keys) {
        return filterFn(new NotFunctionSet<K>((FunctionSet<K>) keys));
    }

    public <EV extends V> ImMap<K, EV> filterValues(ImSet<EV> values) {
        return BaseUtils.immutableCast(filterFnValues(BaseUtils.<FunctionSet<V>>immutableCast(values)));
    }

    public ImMap<K, V> remove(final K remove) {
        return filterFn(new SFunctionSet<K>() {
            public boolean contains(K element) {
                return !BaseUtils.hashEquals(element, remove);
            }
        });
    }

    public ImMap<K, V> removeValues(final V value) {
        return filterFnValues(new SFunctionSet<V>() {
            public boolean contains(V element) {
                return !BaseUtils.hashEquals(element, value);
            }
        });
    }

    public ImMap<K, V> removeNotEquals(final ImMap<K, V> full) {
        return filterFn(new GetKeyValue<Boolean, K, V>() {
            public Boolean getMapValue(K key, V value) {
                return BaseUtils.hashEquals(full.get(key), value);
            }});
    }

    public ImMap<K, V> replaceValues(final V value) {
        return mapValues(new GetStaticValue<V>() {
            public V getMapValue() {
                return value;
            }});
    }

    public ImMap<K, V> override(K key, V value) {
        if(containsKey(key)) // оптимизация
            return replaceValue(key, value);
        else
            return addExcl(key, value);
    }

    public ImMap<K, V> replaceValue(final K replaceKey, final V replaceValue) {
        return mapValues(new GetKeyValue<V, K, V>() {
            public V getMapValue(K key, V value) {
                return BaseUtils.hashEquals(key, replaceKey) ? replaceValue : value;
            }
        });
    }

    public ImMap<K, V> replaceValues(final ImMap<? extends V, ? extends V> map) {
        return mapValues(new GetValue<V, V>() {
            public V getMapValue(V value) {
                V mapValue = ((ImMap<V, V>) map).get(value);
                return mapValue != null ? mapValue : value;
            }
        });
    }

    public ImMap<K, V> override(ImMap<? extends K, ? extends V> imMap) {
        return merge(imMap, MapFact.<K, V>override());
    }

    public <M> ImMap<K, M> mapItValues(GetValue<M, V> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(getValue(i)));
        return mvResult.immutableValue();
    }

    public <M> ImMap<K, M> mapValues(GetValue<M, V> getter) {
        return mapItValues(getter);
    }

    public <M> ImMap<K, M> mapKeyValues(GetValue<M, K> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(getKey(i)));
        return mvResult.immutableValue();
    }

    public <M> ImSet<M> mapMergeSetValues(GetKeyValue<M, K, V> getter) {
        MSet<M> mResult = SetFact.mSetMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(getKey(i), getValue(i)));
        return mResult.immutable();
    }

    public <M> ImMap<K, M> mapValues(GetStaticValue<M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue());
        return mvResult.immutableValue();
    }

    public <M> ImMap<K, M> mapValues(GetKeyValue<M, K, V> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(getKey(i), getValue(i)));
        return mvResult.immutableValue();
    }

    public <M> ImRevMap<K, M> mapRevValues(GetIndex<M> getter) {
        ImRevValueMap<K, M> mvResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(i));
        return mvResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(GetValue<M, V> getter) {
        ImRevValueMap<K, M> mvResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(getValue(i)));
        return mvResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(GetKeyValue<M, K, V> getter) {
        ImRevValueMap<K, M> mvResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(getKey(i), getValue(i)));
        return mvResult.immutableValueRev();
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(GetValue<MK, K> getterKey, GetValue<MV, V> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getterKey.getMapValue(getKey(i)), getterValue.getMapValue(getValue(i)));
        return mResult.immutable();
    }

    public <M> ImMap<M, V> mapKeys(GetValue<M, K> getter) {
        MExclMap<M, V> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.getMapValue(getKey(i)), getValue(i));
        return mResult.immutable();
    }

    public <M> ImCol<M> mapColValues(GetKeyValue<M, K, V> getter) {
        MCol<M> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(getKey(i), getValue(i)));
        return mResult.immutableCol();
    }

    public ImMap<K, V> mapAddValues(final ImMap<K, V> map, final AddValue<K, V> addValue) {
        return mapValues(new GetKeyValue<V, K, V>() {
            public V getMapValue(K key, V value) {
                return addValue.addValue(key, value, map.get(key));
            }});
    }

    public ImOrderMap<K, V> sort(Comparator<K> comparator) { // можно indexes с перегруженным comparator'ом делать
        return keys().sort(comparator).toOrderExclSet().mapOrderMap(this);
    }

    public ImOrderMap<K, V> sort() {
        return keys().sort().mapOrderMap(this);
    }

    public Map<K, V> toJavaMap() {
        Map<K, V> result = new HashMap<K, V>();
        for(int i=0,size=size();i<size;i++)
            result.put(getKey(i), getValue(i));
        return result;
    }

    public GetValue<V, K> fnGetValue() {
        return new GetValue<V, K>() {
            public V getMapValue(K value) {
                return get(value);
            }};
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImMap)) return false;

        ImMap<K, V> map = (ImMap) obj;
        if (map.size() != size()) return false;

        for (int i = 0; i < size(); i++) {
            V mapValue = map.get(getKey(i));
            if (mapValue == null || !BaseUtils.hashEquals(mapValue, getValue(i))) return false;
        }
        return true;
    }

    public int immutableHashCode() {
        int hash = 0;
        for (int i = 0, size = size(); i < size; i++)
            hash += getKey(i).hashCode() ^ getValue(i).hashCode();
        return hash;
    }

    public <M> ImFilterValueMap<K, M> mapFilterValues() {
        return new FilterValueMap<K, M>(this.<M>mapItValues());
    }

    private static final AddValue<Object, ImMap<Object, ImSet<Object>>> addMergeMapSets = new SimpleAddValue<Object, ImMap<Object, ImSet<Object>>>() {
        public ImMap<Object, ImSet<Object>> addValue(Object key, ImMap<Object, ImSet<Object>> prevValue, ImMap<Object, ImSet<Object>> newValue) {
            return prevValue.merge(newValue, ASet.addMergeSet());
        }

        public boolean symmetric() {
            return true;
        }
    };
    public static <K, KV, V> AddValue<K, ImMap<KV, ImSet<V>>> addMergeMapSets() {
        return BaseUtils.immutableCast(addMergeMapSets);
    }
}
