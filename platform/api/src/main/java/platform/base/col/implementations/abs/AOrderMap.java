package platform.base.col.implementations.abs;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.mapvalue.*;

import java.util.Iterator;

public abstract class AOrderMap<K, V> extends AColObject implements ImOrderMap<K, V> {

    // дублирует AMap, так как там порядок может меняться
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

    public V get(K key) {
        return getMap().get(key);
    }

    public ImSet<K> keys() {
        return getMap().keys();
    }

    public ImCol<V> values() {
        return getMap().values();
    }

    public boolean isEmpty() {
        return getMap().isEmpty();
    }

    public boolean containsKey(K key) {
        return getMap().containsKey(key);
    }

    public V singleValue() {
        return getMap().singleValue();
    }

    public K singleKey() {
        return getMap().singleKey();
    }



    public ImOrderMap<K, V> moveStart(ImSet<K> col) {
        return filterOrder(col).mergeOrder(this);
    }

    public boolean starts(ImSet<K> col) {
        return equals(moveStart(col));
    }

    public <M> ImOrderMap<M, V> mapMergeItOrderKeys(GetValue<M, K> getter) {
        MOrderMap<M, V> mResult = MapFact.mOrderMapMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(getKey(i)), getValue(i));
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<M, V> mapMergeOrderKeys(GetValue<M, K> getter) {
        return mapMergeItOrderKeys(getter);
    }

    public <M> ImOrderSet<M> mapOrderSetValues(GetKeyValue<M, K, V> getter) {
        MOrderExclSet<M> mResult = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(getKey(i), getValue(i)));
        return mResult.immutableOrder();
    }

    public <M> ImList<M> mapListValues(GetValue<M, V> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(getValue(i)));
        return mResult.immutableList();
    }

    public <MK, MV> ImOrderMap<MK, MV> mapOrderKeyValues(GetKeyValue<MK, K, V> getterKey, GetValue<MV, V> getterValue) {
        MOrderExclMap<MK, MV> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            mResult.exclAdd(getterKey.getMapValue(getKey(i), value), getterValue.getMapValue(value));
        }
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<M, V> mapOrderKeys(GetValue<M, K> getter) {
        MOrderExclMap<M, V> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.getMapValue(getKey(i)), getValue(i));
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetStaticValue<M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue());
        return mvResult.immutableValueOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetKeyValue<M, K, V> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(getKey(i), getValue(i)));
        return mvResult.immutableValueOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetValue<M, V> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(getValue(i)));
        return mvResult.immutableValueOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetIndex<M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(i));
        return mvResult.immutableValueOrder();
    }

    public ImOrderMap<K, V> filterOrder(FunctionSet<K> set) {
        MOrderFilterMap<K, V> mResult = MapFact.mOrderFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            if(set.contains(key))
                mResult.keep(key, getValue(i));
        }
        return MapFact.imOrderFilter(mResult, this);
    }

    public ImOrderMap<K, V> replaceValues(final V[] values) {
        return mapOrderValues(new GetIndex<V>() {
            public V getMapValue(int i) {
                return values[i];
            }});
    }

    public ImList<V> valuesList() {
        MList<V> mList = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mList.add(getValue(i));
        return mList.immutableList();
    }

    public ImOrderSet<K> keyOrderSet() {
        MOrderExclSet<K> mList = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mList.add(getKey(i));
        return mList.immutableOrder();
    }

    public ImOrderMap<K, V> mergeOrder(ImOrderMap<? extends K, ? extends V> imOrderMap) {
        if(imOrderMap.isEmpty()) return this;

        MOrderMap<K, V> mResult = MapFact.mOrderMap(this);
        mResult.addAll(imOrderMap);
        return mResult.immutableOrder();
    }

    public ImOrderMap<K, V> addOrderExcl(ImOrderMap<? extends K, ? extends V> imOrderMap) {
        if(imOrderMap.isEmpty()) return this;

        MOrderExclMap<K, V> mResult = MapFact.mOrderExclMap(this);
        mResult.exclAddAll(imOrderMap);
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<M, V> map(ImMap<K, M> map) {
        return mapMergeOrderKeys(map.fnGetValue());
    }

    public <M> ImOrderMap<M, V> map(ImRevMap<K, M> map) {
        return mapOrderKeys(map.fnGetValue());
    }

    public <G> ImMap<G, ImOrderMap<K, V>> groupOrder(BaseUtils.Group<G, K> getter) {
        MExclMap<G, MOrderExclMap<K, V>> mResult = MapFact.mExclMapMax(size());
        for (int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            G group = getter.group(key);

            MOrderExclMap<K, V> groupList = mResult.get(group);
            if (groupList == null) {
                groupList = MapFact.mOrderExclMap(size);
                mResult.exclAdd(group, groupList);
            }
            groupList.exclAdd(key, getValue(i));
        }
        return MapFact.immutableOrder(mResult);
    }
    public ImOrderMap<V, ImOrderSet<K>> groupOrderValues() {
        MOrderExclMap<V, MOrderExclSet<K>> mResult = MapFact.mOrderExclMap(size());
        for (int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            V group = getValue(i);
            if(group!=null) {
                MOrderExclSet<K> groupList = mResult.get(group);
                if (groupList == null) {
                    groupList = SetFact.mOrderExclSetMax(size);
                    mResult.exclAdd(group, groupList);
                }
                groupList.add(key);
            }
        }
        return MapFact.immutableOrder(mResult);
    }


    public ImOrderMap<K, V> reverseOrder() {
        return keyOrderSet().reverseList().toOrderExclSet().mapOrderValues(getMap().fnGetValue());
    }

    public int indexOf(K key) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(getKey(i), key))
                return i;
        return -1;
    }

    public int immutableHashCode() {
        int hashCode = 1;
        for (int i=0,size=size();i<size;i++)
            hashCode = 31 * hashCode + (getKey(i).hashCode() ^ getValue(i).hashCode());
        return hashCode;
    }

    // собсно ради этого метода класс и создавался
    @Override
    public boolean equals(Object obj) {

        if(this==obj) return true;
        if(!(obj instanceof ImOrderMap)) return false;

        ImOrderMap<K, V> list = (ImOrderMap<K, V>)obj;
        if(list.size()!=size()) return false;

        for(int i=0,size=size();i<size;i++)
            if(!(BaseUtils.hashEquals(getKey(i), list.getKey(i)) && BaseUtils.hashEquals(getValue(i), list.getValue(i))))
                return false;
        return true;
    }

}
